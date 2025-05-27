package bot;

import db.UsersRepository;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Bot extends TelegramLongPollingBot {

    private String botToken;
    private String botUsername;
    HashMap<Long, Integer> userMessages = new HashMap<>();
    private UsersRepository usersRepository = new UsersRepository();


    public Bot() {
        loadConfig();
        usersRepository.initialiseDbConnection();
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
        registerIncrementMessages(update);
    }

    public void registerIncrementMessages(Update update) {
        var msg = update.getMessage();
        var user = msg.getFrom();
        Long userId = user.getId();
        if (userMessages.containsKey(userId)) {
            userMessages.put(userId, userMessages.get(userId) + 1);
            usersRepository.updateUserMessageCount(userId, userMessages.get(userId));
        }
        if (!userMessages.containsKey(userId)) {
            userMessages.put(userId, 1);
            System.out.println("New user registered: " + user.getId());
            usersRepository.insertNewUser(userId, user.getUserName());
        }
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }





}
