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
        } else if (username.equals("Timur996")) {
            this.nickname = "Тимур";
        } else if (username.equals("bfaiziev")) {
            this.nickname = "Бахадур";
        } else if (username.equals("V3034V")) {
            this.nickname = "Володя";
        } else if (username.equals("benzoleum")) {
            this.nickname = "Лук";
        } else {
            this.nickname = "not_recognized";
        }
    }

}

