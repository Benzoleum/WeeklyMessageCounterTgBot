package db;

public class DbQueries {
    static final String CHECK_USER_REGISTERED = "SELECT * FROM users WHERE user_id = ?";
    static final String REGISTER_NEW_USER = "INSERT INTO users (user_id, username, message_count, nickname) VALUES (?, ?, ?, ?)";
    static final String GET_MSG_COUNT = "SELECT message_count FROM users WHERE user_id = ?";
    static final String UPDATE_USR_MSG_COUNT = "UPDATE users SET message_count = ? WHERE user_id = ?";

}
