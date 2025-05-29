package bot;

import db.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Bot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(Bot.class);
    private String botToken;
    private String botUsername;
    private long chatId = 0;
    ConcurrentHashMap<Long, UserData> userCache = new ConcurrentHashMap<>();
    private final UsersRepository usersRepository = new UsersRepository();


    public Bot() {
        loadConfig();
        usersRepository.initialiseDbConnection();
        scheduleDatabaseSync();
    }

    private void loadConfig() {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.yaml")) {
            Map<String, Map<String, String>> config = yaml.load(inputStream);
            botToken = config.get("config").get("bot-token"); // Fetch the bot token
            botUsername = config.get("config").get("bot-name"); // Fetch the bot name
        } catch (Exception e) {
            logger.error("Failed to load configuration file", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        var msg = update.getMessage();
        var user = msg.getFrom();
        Long userId = user.getId();
        String username = user.getUserName();

        if (chatId == 0) {
            chatId = update.getMessage().getChatId();
            dateTimeCheckerScheduler();
        }
        if (!userCache.containsKey(update.getMessage().getFrom().getId())) {
            updateUserCache(userId, username);
        } else {
            // Update the user's message count in the cache
            UserData userData = userCache.get(userId);
            userData.incrementMessageCount();
            userData.setLastMessage(System.currentTimeMillis());// Increment the in-memory count
            logger.debug("Updated message count for user {}, messages in cache: {}, last message timestamp: {}", userId, userData.getMessageCount(), userData.getLastMessage());

        }
    }

    public void updateUserCache(Long userId, String username) {
        // Check if the user is in the cache
        userCache.computeIfAbsent(userId, id -> {
            // If not in cache, load user data from DB
            if (usersRepository.isUserRegistered(id)) {
                int messageCount = usersRepository.getMessageCount(id);
                return new UserData(userId, username, messageCount, usersRepository.getFirstMessage(userId), System.currentTimeMillis());
            } else {
                // New user registration
                usersRepository.insertNewUser(id, username);
                return new UserData(userId, username, 0, System.currentTimeMillis(), System.currentTimeMillis());
            }
        });
    }


    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    private void scheduleDatabaseSync() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            logger.info("Starting database synchronization...");
            userCache.forEach((userId, userData) -> {
                usersRepository.updateUserMessageCount(userId, userData.getMessageCount(), userData.getLastMessage());
                logger.debug("Synchronized user {} with message count {}", userId, userData.getMessageCount());
            });
        }, 0, 5, TimeUnit.MINUTES); // Sync every 5 minutes
    }

    private void resetAllData() {
        for (UserData userData : userCache.values()) {
            userData.setMessageCount(0);
            userData.setFirstMessage(0);
            userData.setLastMessage(0);
        }
    }

    public void dateTimeCheckerScheduler() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        // Scheduling a repeating task
        Runnable task = () -> {
            LocalDateTime now = LocalDateTime.now();
            DayOfWeek today = now.getDayOfWeek();

            // Run task logic for Sunday at 23:59
            try {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("В Лондоне Воскресенье 23:59. Выбираем трупа...");
                execute(message);
                StringBuilder sb = new StringBuilder();

                int max = Integer.MAX_VALUE;
                long lostId = 0;

                for (UserData userData : userCache.values()) {
                    if (userData.getMessageCount() < max) {
                        max = userData.getMessageCount();
                        lostId = userData.getId();
                    }
                    sb.append(userData.getUsername()).append(" написал ").append(userData.getMessageCount()).append(" сообщений за неделю.\n");
                }
                message.setText(sb.toString());
                execute(message);
                message.setText(userCache.get(lostId).getUsername() + " написал меньше всех. " + userCache.get(lostId).getUsername() + "- труп недели. Поздравляем.");
                execute(message);
                resetAllData();
                logger.info("All user message counts refreshed");
            } catch (TelegramApiException e) {
                logger.error("Failed to send the message to chat", e);
                throw new RuntimeException(e);
            }
        };

        long initialDelay = getDelayUntilNextSundayMidnight(); // Calculate delay
        scheduler.schedule(task, initialDelay, TimeUnit.MILLISECONDS);
        logger.debug("Task scheduled for execution after {} ms", initialDelay);
    }

    private static long getDelayUntilNextSundayMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextSundayMidnight = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY))
                .withHour(23)
                .withMinute(07)
                .withSecond(0)
                .withNano(0);
        return Duration.between(now, nextSundayMidnight).toMillis();
    }

}