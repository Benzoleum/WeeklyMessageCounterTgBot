package bot;

import db.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Bot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(Bot.class);
    private String botToken;
    private String botUsername;
    HashMap<Long, Integer> userMessages = new HashMap<>();
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
        updateUserCache(update);
    }

    public void registerIncrementMessages(Update update) {
        var msg = update.getMessage();
        var user = msg.getFrom();
        Long userId = user.getId();

        // Check if the user is registered in the database
        if (usersRepository.isUserRegistered(userId)) {
            // Load user messages into memory if not already present
            userMessages.putIfAbsent(userId, usersRepository.getMessageCount(userId));

            // Increment the message count
            int updatedMessageCount = userMessages.get(userId) + 1;
            userMessages.put(userId, updatedMessageCount);

            logger.debug("User messages in the map: {}", updatedMessageCount);
            logger.debug("Message count incremented for user: {}", userId);

            // Update the message count in the database
            usersRepository.updateUserMessageCount(userId, updatedMessageCount, System.currentTimeMillis());

        } else {
            // Register new user
            userMessages.put(userId, 1);
            logger.debug("New user registered: {}", userId);
            logger.debug("User map updated: {}", userId);

            // Insert the new user into the database
            usersRepository.insertNewUser(userId, user.getUserName());
        }
    }
    
    public void updateUserCache(Update update) {
        var msg = update.getMessage();
        var user = msg.getFrom();
        Long userId = user.getId();
        String username = user.getUserName();

        // Check if the user is in the cache
        userCache.computeIfAbsent(userId, id -> {
            // If not in cache, load user data from DB
            if (usersRepository.isUserRegistered(id)) {
                int messageCount = usersRepository.getMessageCount(id);
                return new UserData(userId, username, messageCount, usersRepository.getFirstMessage(userId), usersRepository.getLastMessage(userId)); // Existing user
            } else {
                // New user registration
                usersRepository.insertNewUser(id, user.getUserName());
                return new UserData(userId, username, 1, System.currentTimeMillis(), System.currentTimeMillis()); // Existing user
            }
        });

        // Update the user's message count in the cache
        UserData userData = userCache.get(userId);
        userData.incrementMessageCount(); // Increment the in-memory count
        logger.debug("Updated message count for user {}, messages in cache: {}, last message timestamp: {}", userId, userData.getMessageCount(), userData.getLastMessage());
    }

    public void calculateWeeklyMessageCount(long firstMessage, long lastMessage) {
        // Convert timestamp to LocalDateTime
        LocalDateTime dateTime1 = Instant.ofEpochMilli(firstMessage).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime dateTime2 = Instant.ofEpochMilli(lastMessage).atZone(ZoneId.systemDefault()).toLocalDateTime();

        // Format the LocalDateTime to a readable string
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String readableDate1 = dateTime1.format(formatter);
        String readableDate2 = dateTime2.format(formatter);

        Duration duration = Duration.between(dateTime1, dateTime2);

        logger.debug("Readable Date: " + readableDate1);
        logger.debug("Readable Date: " + readableDate2);
        logger.info("Duration in days: " + duration.toDays());
        logger.info("Duration in hours: " + duration.toHours());
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
}