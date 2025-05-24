import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class Bot extends TelegramLongPollingBot {

    private String botToken;
    private String botUsername;
    protected Long selfId;
    private boolean screaming = false;


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
        System.out.println("Sending message to user: " + user.getId());
        if (msg.isCommand()) {
            if (msg.getText().equals("/scream"))         //If the command was /scream, we switch gears
                screaming = true;
            else if (msg.getText().equals("/whisper"))  //Otherwise, we return to normal
                screaming = false;

            return;                                     //We don't want to echo commands, so we exit
        }
        if (screaming)                            //If we are screaming
            scream(selfId, update.getMessage());     //Call a custom method
        else
            copyMessage(selfId, msg.getMessageId()); //Else proceed normally
    }

    private void scream(Long id, Message msg) {
        if (msg.hasText())
            sendText(id, msg.getText().toUpperCase());
        else
            copyMessage(id, msg.getMessageId());  //We can't really scream a sticker
    }

    public void sendText(Long who, String what) {
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString()) //Who are we sending a message to
                .text(what).build();    //Message content
        try {
            execute(sm);                        //Actually sending the message
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);      //Any error will be printed here
        }
    }

    public void copyMessage(Long who, Integer msgId) {
        CopyMessage cm = CopyMessage.builder()
                .fromChatId(who.toString())  //We copy from the user
                .chatId(who.toString())      //And send it back to him
                .messageId(msgId)            //Specifying what message
                .build();
        try {
            execute(cm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
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
