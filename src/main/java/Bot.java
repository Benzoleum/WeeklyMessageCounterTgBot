import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Bot extends TelegramLongPollingBot {

    private String botToken;
    private String botUsername;
    Connection conn;
    HashMap<Long, Integer> userMessages = new HashMap<>();

    public Bot() {
        loadConfig();
    }

    private void loadConfig() {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.yaml")) {
            Map<String, Map<String, String>> config = yaml.load(inputStream);
            botToken = config.get("config").get("bot-token"); // Fetch the bot token
            botUsername = config.get("config").get("bot-name"); // Fetch the bot name
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load configuration file");
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        registerIncrementMessages(update);
    }

    public void registerIncrementMessages(Update update) {
        var msg = update.getMessage();
        var user = msg.getFrom();
        Long userId = user.getId();
        if (userMessages.containsKey(userId)) {
            userMessages.put(userId, userMessages.get(userId) + 1);
        }
        if (!userMessages.containsKey(userId)) {
            userMessages.put(userId, 1);
            System.out.println("New user registered: " + user.getId());
            insertNewUser(userId, user.getUserName());
        }
        System.out.println("User: " + userId + ", messages: " + userMessages.get(userId));
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    public void initialiseDbConnection() {
        String url = "jdbc:sqlite:/Users/benzoleum/Desktop/Personal_2/WeeklyMessageCounterTgBot/src/db/users.db"; // Path to SQLite database
        try {
            conn = DriverManager.getConnection(url);
            if (conn != null) {
                System.out.println("DB connection established");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void insertNewUser(Long userId, String username) {
        try {
            String sql = "INSERT INTO users (user_id, username, message_count, first_message) VALUES (?, ?, ?, ?)";
            var pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            pstmt.setString(2, username);
            pstmt.setInt(3, 1);
            pstmt.setLong(4, System.currentTimeMillis());
            pstmt.executeUpdate();
            System.out.println("Inserted new user: " + userId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
