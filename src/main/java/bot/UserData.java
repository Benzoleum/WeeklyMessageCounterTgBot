package bot;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class UserData {
    private Long id;
    private String username;
    private int messageCount;
    private long firstMessage;
    private long lastMessage;

    public void incrementMessageCount() {
        messageCount++;
    }
}

