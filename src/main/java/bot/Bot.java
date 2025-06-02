package bot;

import db.UserRepository;
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
    private final UserRepository userRepository = new UserRepository();
    private long shashlChatId;
    private long testChatId;


    public Bot() {
        loadConfig();
        userRepository.initialiseDbConnection();
        scheduleDatabaseSync();
    }

    private void loadConfig() {
        Yaml yaml = new Yaml();
        Map<String, Map<String, String>> config;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.yaml")) {
            config = yaml.load(inputStream);
            shashlChatId = Long.parseLong(config.get("config").get("shashl-chat-id"));
            testChatId = Long.parseLong(config.get("config").get("test-chat-id"));
        } catch (Exception e) {
            logger.error("Failed to load configuration file", e);
            throw new RuntimeException(e);
        }
        botToken = System.getenv("WEEKLY_MSG_COUNTER_TG_BOT_TOKEN");
        botUsername = System.getenv("WEEKLY_MSG_COUNTER_TG_BOT_NAME");
        if ((botToken != null && !botToken.isEmpty()) && (botUsername != null && !botUsername.isEmpty())) {
            logger.info("Env variables set for bot token and bot name, loading from env variables");
        } else {
            logger.info("No evn variable set for bot token or bot name, loading from config file");
            botToken = config.get("config").get("bot-token"); // Fetch the bot token
            botUsername = config.get("config").get("bot-name"); // Fetch the bot name

        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update != null) {
            if (update.getMessage().getChatId() == shashlChatId || update.getMessage().getChatId() == testChatId) {
                if (update.hasMessage()) {
                    var msg = update.getMessage();
                    var user = msg.getFrom();
                    Long userId = user.getId();
                    String username = user.getUserName();

                    // if chatId is not set yet, set it on the first update the bot receives
                    if (chatId == 0) {
                        chatId = update.getMessage().getChatId();
                        logger.info("Chat ID: {}", chatId);
                        corpseOfTheWeekTaskRunner();
                        taskTimeLogger();
                    }

                    // if the user is not in the cache, add them to the cache
                    if (!userCache.containsKey(update.getMessage().getFrom().getId())) {
                        updateUserCache(userId, username);
                    } else {
                        // if the user is already in the cache, update the user's message count
                        UserData userData = userCache.get(userId);
                        userData.incrementMessageCount();
                        logger.trace("Updated message count for user {}, messages in cache: {}", userId, userData.getMessageCount());
                    }
                } else {
                    logger.info("Received an update without a message");
                    logger.debug("Update: {}", update);
                }
            } else {
                logger.info("Received an update from an unknown chat");
                logger.info("Chat ID: {}", update.getMessage().getChatId());
                logger.info("Chat title: {}", update.getMessage().getChat().getTitle());
                logger.info("Msg from: {}", update.getMessage().getFrom());
                SendMessage message = new SendMessage();
                message.setChatId(update.getMessage().getChatId());
                message.setText("Sorry, the functionality of this bot is restricted to certain chats at the moment. Please try again later.");
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            logger.info("Received an update with null value");
        }
    }

    public void updateUserCache(Long userId, String username) {
        // Check if the user is in the cache
        userCache.computeIfAbsent(userId, id -> {
            // If not in cache, load user data from DB
            if (userRepository.isUserRegistered(id)) {
                logger.info("User {} is registered in the DB, updating cache", username);
                int messageCount = userRepository.getMessageCount(id);
                return new UserData(userId, username, messageCount);
            } else {
                // New user registration
                logger.info("User {} is not registered in the DB or cache, registering user in cache and DB", username);
                UserData userData = new UserData(userId, username, 1);
                userRepository.insertNewUser(id, username, userData.getNickname());
                return userData;
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
            logger.info("Starting database synchronization with user cache");
            userCache.forEach((userId, userData) -> {
                userRepository.updateUserMessageCount(userId, userData.getMessageCount());
                logger.debug("Synchronized user {} with message count {} in the DB", userId, userData.getMessageCount());
            });
        }, 0, 5, TimeUnit.MINUTES); // Sync every 5 minutes
    }

    private void resetUserData() {
        logger.info("Resetting all user data");
        for (UserData userData : userCache.values()) {
            userData.setMessageCount(0);
        }
        logger.info("All user message data reset");
    }

    private void corpseOfTheWeekSelector() {
        logger.info("Starting corpse of the week task");
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("В Лондоне Воскресенье 16:00. Выбираю трупа...");
            execute(message);
            StringBuilder sb = new StringBuilder();

            int max = Integer.MAX_VALUE;
            long lostId = 0;

            for (UserData userData : userCache.values()) {
                if (userData.getMessageCount() < max) {
                    max = userData.getMessageCount();
                    lostId = userData.getId();
                }
                sb.append(userData.getNickname()).append(" написал ").append(userData.getMessageCount()).append(" сообщений за неделю.\n");
            }
            message.setText(sb.toString());
            execute(message);

            message.setText(userCache.get(lostId).getNickname() + " написал меньше всех. " + userCache.get(lostId).getNickname() + "- труп недели. Поздравляю.");
            execute(message);
            resetUserData();
        } catch (TelegramApiException e) {
            logger.error("Failed to send the message to chat", e);
            throw new RuntimeException(e);
        }
    }

    public void corpseOfTheWeekTaskRunner() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        // Scheduling a repeating task
        Runnable task = () -> {
            corpseOfTheWeekSelector();
        };

        long initialDelay = getDelayUntilNextSundayMidnight(); // Calculate delay
        scheduler.schedule(task, initialDelay, TimeUnit.MILLISECONDS);
    }

    private void taskTimeLogger() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            if (TimeUnit.MILLISECONDS.toDays(getDelayUntilNextSundayMidnight()) > 0) {
                logger.info("The corpse selector task is scheduled to run in {} days", TimeUnit.MILLISECONDS.toDays(getDelayUntilNextSundayMidnight()));
            } else {
                logger.info("The corpse selector task is scheduled to run in {} hours", TimeUnit.MILLISECONDS.toHours(getDelayUntilNextSundayMidnight()));
            }
        }, 0, 1, TimeUnit.HOURS);
    }

    private static long getDelayUntilNextSundayMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextSundayMidnight = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).withHour(10).withMinute(0).withSecond(0).withNano(0);
        return Duration.between(now, nextSundayMidnight).toMillis();
    }
}