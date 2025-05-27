package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class UsersRepository {
    Connection conn;

    public void initialiseDbConnection() {
        String url = "jdbc:sqlite:src/main/resources/users.db"; // Path to SQLite database
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

    public void updateUserMessageCount(Long userId, int messageCount) {
        try {
            String sql = "UPDATE users SET message_count = ?, last_message = ? WHERE user_id = ?";
            var pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, messageCount);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setLong(3, userId);
            pstmt.executeUpdate();
            System.out.println("Updated user message count: " + userId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
