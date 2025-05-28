package db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class UsersRepository {
    Connection conn;
    private static final Logger logger = LoggerFactory.getLogger(UsersRepository.class);

    public void initialiseDbConnection() {
        String url = "jdbc:sqlite:src/main/resources/users.db"; // Path to SQLite database
        try {
            conn = DriverManager.getConnection(url);
            if (conn != null) {
                logger.info("DB connection established");
            }
        } catch (SQLException e) {
            logger.error("Failed to establish DB connection", e);
        }
    }

    public boolean isUserRegistered(Long userId) {
        try {
            String sql = "SELECT * FROM users WHERE user_id = ?";
            var pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            var rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.error("Failed to check if user is registered: " + userId, e);
            throw new RuntimeException(e);
        }
    }

    public void insertNewUser(Long userId, String username) {
        try {
            String sql = "INSERT INTO users (user_id, username, message_count, first_message, last_message) VALUES (?, ?, ?, ?, ?)";
            var pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            pstmt.setString(2, username);
            pstmt.setInt(3, 1);
            pstmt.setLong(4, System.currentTimeMillis());
            pstmt.setLong(5, System.currentTimeMillis());
            pstmt.executeUpdate();
            logger.info("Inserted new user: " + userId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getMessageCount(Long userId) {
        try {
            String sql = "SELECT message_count FROM users WHERE user_id = ?";
            var pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            var rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("message_count");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        logger.info("No message count found for user: " + userId);
        return 0;
    }

    public void updateUserMessageCount(Long userId, int messageCount, long lastMessage) {
        try {
            String sql = "UPDATE users SET message_count = ?, last_message = ? WHERE user_id = ?";
            var pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, messageCount);
            pstmt.setLong(2, lastMessage);
            pstmt.setLong(3, userId);
            pstmt.executeUpdate();
            logger.trace("Updated user message count: " + userId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long getFirstMessage(Long userId) {
        try {
            String sql = "SELECT first_message FROM users WHERE user_id = ?";
            var pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            var rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("first_message");
            }
        } catch (SQLException e) {
            logger.error("Failed to get first message for user: " + userId, e);
            throw new RuntimeException(e);
        }
        logger.info("No first message found for user: " + userId);
        return 0;
    }

    public long getLastMessage(Long userId) {
        try {
            String sql = "SELECT last_message FROM users WHERE user_id = ?";
            var pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            var rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("last_message");
            }
        } catch (SQLException e) {
            logger.error("Failed to get last message for user: " + userId, e);
            throw new RuntimeException(e);
        }
        logger.info("No last message found for user: " + userId);
        return 0;
    }
}
