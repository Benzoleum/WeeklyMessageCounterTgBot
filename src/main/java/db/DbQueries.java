package db;

public class DbQueries {
    static final String CHECK_USER_REGISTERED = "SELECT * FROM users WHERE user_id = ?";
    static final String REGISTER_NEW_USER = "INSERT INTO users (user_id, username, message_count, first_message, last_message) VALUES (?, ?, ?, ?, ?)";
    static final String GET_MSG_COUNT = "SELECT message_count FROM users WHERE user_id = ?";
    static final String UPDATE_USR_MSG_COUNT = "UPDATE users SET message_count = ?, last_message = ? WHERE user_id = ?";
    static final String GET_FIRST_MSG = "SELECT first_message FROM users WHERE user_id = ?";
    static final String GET_LAST_MSG = "SELECT last_message FROM users WHERE user_id = ?";

}
