import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.yaml.snakeyaml.Yaml;


import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class Bot extends TelegramLongPollingBot {

    private String botToken;
    private String botUsername;
    protected Long selfId;


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
        var msg = update.getMessage();
        var user = msg.getFrom();
        selfId = user.getId();
        System.out.println("Registering user: " + user.getId());

        System.out.println(user.getFirstName() + " wrote " + msg.getText());
    }

    public void sendText(Long who, String what){
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString()) //Who are we sending a message to
                .text(what).build();    //Message content
        try {
            execute(sm);                        //Actually sending the message
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);      //Any error will be printed here
        }
    }

    @Override
    public String getBotToken() {
        return botToken;
//        return "7373436976:AAEEbIWqvcvrRuQpHQl2vzbJkoynwKkyrb8";
    }

    @Override
    public String getBotUsername() {
        return botUsername;
//        return "weekly_message_counter_bot";
    }
}
