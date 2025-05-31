package bot;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserData {
    private Long id;
    private String username;
    private int messageCount;
    private String nickname;

    UserData(Long id, String username, int messageCount) {
        this.id = id;
        this.username = username;
        this.messageCount = messageCount;
        resolveNickname(username);
    }

    public void incrementMessageCount() {
        messageCount++;
    }

    public void resolveNickname(String username) {
        if (username.equals("Ayan_A_B")) {
            this.nickname = "Величайший";
        }
        if (username.equals("Timur996")) {
            this.nickname = "Тимур";
        }
        if (username.equals("bfaiziev")) {
            this.nickname = "Бахадур";
        }
        if (username.equals("V3034V")) {
            this.nickname = "Володя";
        }
        if (username.equals("benzoleum")) {
            this.nickname = "Лук";
        }
    }

}

