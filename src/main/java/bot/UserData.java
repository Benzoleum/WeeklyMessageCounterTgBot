package bot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor @Getter @Setter
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

