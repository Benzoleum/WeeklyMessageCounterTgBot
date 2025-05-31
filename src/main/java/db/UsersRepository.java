package db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

public class UsersRepository {
    Connection conn;
    private static final Logger logger = LoggerFactory.getLogger(UsersRepository.class);

    public void initialiseDbConnection() {
        Yaml yaml = new Yaml();
        String url;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.yaml")) {
            Map<String, Map<String, String>> config = yaml.load(inputStream);
            String dbFile = config.get("db").get("url");
            url = "jdbc:sqlite:" + dbFile;

            conn = DriverManager.getConnection(url);
            if (conn != null) {
                logger.info("DB connection established");
            } else {
                logger.error("Failed to establish DB connection");
                throw new RuntimeException("Failed to establish DB connection");
            }
        } catch (Exception e) {
            logger.error("An error occurred when connecting to the DB", e);
            throw new RuntimeException(e);
        }
    }

    public boolean isUserRegistered(Long userId) {
        try {
            var pstmt = conn.prepareStatement(DbQueries.CHECK_USER_REGISTERED);
            pstmt.setLong(1, userId);
            var rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.error("Failed to check if user is registered: " + userId, e);
            throw new RuntimeException(e);
        }
    }

    public void insertNewUser(Long userId, String username, String nickname) {
        try {
            var pstmt = conn.prepareStatement(DbQueries.REGISTER_NEW_USER);
            pstmt.setLong(1, userId);
            pstmt.setString(2, username);
            pstmt.setInt(3, 1);
            pstmt.setString(4, nickname);
            pstmt.executeUpdate();
            logger.info("Inserted new user: " + userId);
        } catch (SQLException e) {
            logger.error("Failed to insert new user: " + userId, e);
            throw new RuntimeException(e);
        }
    }

    public int getMessageCount(Long userId) {
        try {
            var pstmt = conn.prepareStatement(DbQueries.GET_MSG_COUNT);
            pstmt.setLong(1, userId);
            var rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("message_count");
            }
        } catch (SQLException e) {
            logger.error("Failed to get message count for user: " + userId, e);
            throw new RuntimeException(e);
        }
        logger.info("No message count found for user: " + userId);
        return 0;
    }

    public void updateUserMessageCount(Long userId, int messageCount) {
        try {
            var pstmt = conn.prepareStatement(DbQueries.UPDATE_USR_MSG_COUNT);
            pstmt.setInt(1, messageCount);
            pstmt.setLong(2, userId);
            pstmt.executeUpdate();
            logger.trace("Updated user message count: " + userId);
        } catch (SQLException e) {
            logger.error("Failed to update user message count: " + userId, e);
            throw new RuntimeException(e);
        }
    }

}
