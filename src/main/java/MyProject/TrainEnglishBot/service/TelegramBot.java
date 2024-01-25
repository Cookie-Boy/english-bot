package MyProject.TrainEnglishBot.service;

import MyProject.TrainEnglishBot.config.BotConfig;
import MyProject.TrainEnglishBot.model.Word;
import MyProject.TrainEnglishBot.model.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private WordRepository wordRepository;

    private boolean isTrain;

    private boolean isAdd;

    final BotConfig config;

    public TelegramBot(BotConfig config) {
        this.config = config;
        this.isTrain = false;
        this.isAdd = false;
        List<BotCommand> menu = new ArrayList<>();
        menu.add(new BotCommand("/start", "get started"));
        menu.add(new BotCommand("/showmywords", "show all learning words you have"));
        menu.add(new BotCommand("/stop", "stops any mode"));
        menu.add(new BotCommand("/deleteallwords", "delete all your learning words"));
        menu.add(new BotCommand("/deleteword", "delete one word"));
        menu.add(new BotCommand("/practice", "start training mode"));
        try {
            this.execute(new SetMyCommands(menu, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            System.out.println("Bot start error!");
        }
    }

    @Override
    public String getBotUsername()  {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText().toLowerCase();
            long chatId = update.getMessage().getChatId();

            if (this.isTrain) {
                generateQuestion(chatId);
                return;
            }

            if (this.isAdd && !messageText.equals("/stop")) {
                String[] pair = messageText.split(" ");
                if (pair.length == 2) {
                    addNewWord(chatId, pair);
                } else {
                    sendMessage(chatId, "Use example:\n'word' 'translate' (space between these two words)");
                }
                return;
            }

            if (messageText.contains("/deleteword") || messageText.contains("удалить")) {
                sendMessage(chatId, deleteWord(chatId, messageText.split(" ")[1]));
                return;
            }

            switch (messageText) {
                case "start" -> {
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                }
                case "/stop" -> {
                    if (this.isTrain) {
                        this.isTrain = false;
                        sendMessage(chatId, "Training mode has been stopped.");
                    } else if (this.isAdd) {
                        stopAddMode(chatId);
                    }
                }
                case "/showmywords" -> {
                    sendMessage(chatId, getLearningWords(chatId));
                }
                case "/deleteallwords" -> {
                    sendMessage(chatId, deleteAllWords(chatId));
                }
                case "some new words" -> {
                    this.isAdd = true;
                    sendMessage(chatId, "The mode of adding new words is enabled! Enter '/stop' or press the button to end it.");
                    // чтобы добавлялись вот так: новое слово - перевод, слово слово перевод
                }
                case "stop adding new words" -> {
                    stopAddMode(chatId);
                }
                case "/practice", "practice" -> {
                    this.isTrain = true;
                    turnOnAllWords(chatId);
                    sendMessage(chatId, "Training mode is on! Enter /stop to disable it.");
                    generateQuestion(chatId);
                }
                default -> sendMessage(chatId, "Sorry, this command doesn't work yet.");
            }
        }
        else if (update.hasCallbackQuery()) {
            String[] callBackData = update.getCallbackQuery().getData().split(" ");
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String oldText = update.getCallbackQuery().getMessage().getText();
            List<Word> wordList = wordRepository.findAllByChatId(chatId);

            // format: word + " " + correctAnswer
            EditMessageText message = new EditMessageText();
            message.setChatId(chatId);
            message.setMessageId((int) messageId);
            String text;

            int correctIdx = 0;
            for (int i = 0; i < wordList.size(); i++) {
                String word = wordList.get(i).getEnWord();
                if (word.equals(callBackData[1])) {
                    correctIdx = i;
                    break;
                }
            }

            if (callBackData[0].equals(callBackData[1])) {
                wordList.get(correctIdx).setUsed(false);
                wordRepository.save(wordList.get(correctIdx));
                text = "✅ Correct! ";
            } else {
                text = "❌ You are wrong! ";
            }
            text += oldText + " " + callBackData[1];
            message.setText(text);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

            generateQuestion(chatId);
        }
    }

    private List<InlineKeyboardButton> createRow(List<Word> wordList, List<Integer> usedList, int correctIdx) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        String correctAns = wordList.get(correctIdx).getEnWord();

        for (Integer index : usedList) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(wordList.get(index).getEnWord());
            System.out.println(wordList.get(index).getEnWord() + " " + correctAns);
            button.setCallbackData(wordList.get(index).getEnWord() + " " + correctAns);
            row.add(button);
        }

        return row;
    }

    private void generateQuestion(long chatId) {
        List<Word> wordList = wordRepository.findAllByChatId(chatId);
        if (wordList.size() < 4) {
            sendMessage(chatId, "You don't have enough words to learn! Need at least 4 words.");
            this.isTrain = false;
            return;
        }

        // 0 - training word, 1-2-3-4 - options in buttons
        List<Integer> usedList = new ArrayList<>();
        Random random = new Random();
        int correctIdx = random.nextInt(wordList.size());
        if (!isAllTurnOff(wordList)) {
            while (!wordList.get(correctIdx).isUsed()) {
                correctIdx = random.nextInt(wordList.size());
            }
        } else {
            this.isTrain = false;
            sendMessage(chatId, "All words are repeated. The training is over.");
            return;
        }
        int randValue = correctIdx;
        usedList.add(randValue);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Translation for \"" + wordList.get(randValue).getRuWord() + "\":");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            while (usedList.contains(randValue)) {
                randValue = random.nextInt(wordList.size());
            }
            usedList.add(randValue);
        }

        Collections.shuffle(usedList);

        List<InlineKeyboardButton> row = createRow(wordList, usedList, correctIdx);
        rows.add(row.subList(0, 2));
        rows.add(row.subList(2, 4));

        markupInLine.setKeyboard(rows);
        message.setReplyMarkup(markupInLine);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private boolean isAllTurnOff(List<Word> wordList) {
        for (Word word : wordList) {
            if (word.isUsed()) {
                return false;
            }
        }
        return true;
    }

    private void turnOnAllWords(long chatId) {
        List<Word> wordList = wordRepository.findAllByChatId(chatId);
        for (Word word : wordList) {
            word.setUsed(true);
        }
        wordRepository.saveAll(wordList);
    }

    private void turnOffAllWords(long chatId) {
        List<Word> wordList = wordRepository.findAllByChatId(chatId);
        for (Word word : wordList) {
            word.setUsed(false);
        }
    }

    private String getLearningWords(long chatId) {
        StringBuilder result = new StringBuilder();
        List<Word> wordList = wordRepository.findAllByChatId(chatId);
        for (Word word : wordList) {
            result.append(word.getEnWord()).append(", ");
        }
        if (result.length() > 0) {
            return "Your words for learning: \n\n" + result.substring(0, result.length() - 2);
        } else {
            return "Your don't have any words to learn yet.";
        }
    }

    private void addNewWord(long chatId, String[] pair) {
        String russianWord, englishWord;
        if (isRussianSymbol(pair[0].charAt(0))) {
            russianWord = pair[0];
            englishWord = pair[1];
        } else {
            russianWord = pair[1];
            englishWord = pair[0];
        }

        wordRepository.save(new Word(chatId, englishWord, russianWord));
    }

    private String deleteAllWords(long chatId) {
        List<Word> wordList = wordRepository.findAllByChatId(chatId);
        for (Word word : wordList) {
            wordRepository.deleteById(word.getWordId());
        }
        return "All your learning words have been deleted.";
    }

    private String deleteWord(long chatId, String enWord) {
        List<Word> wordList = wordRepository.findAllByEnWord(enWord);
        for (Word word : wordList) {
            if (word.getChatId() == chatId) {
                wordRepository.deleteById(word.getWordId());
                return "Word deleted.";
            }
        }
        return "Word not found.";
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Hi, " + name + ", nice to meet you!";
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(createDefaultKeyboard());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private boolean isRussianSymbol(char symbol) {
        return (symbol >= 'А' && symbol <= 'Я') || (symbol >= 'а' && symbol <= 'я');
    }

    private void stopAddMode(long chatId) {
        this.isAdd = false;
        sendMessage(chatId, "Adding mode has been stopped.");
    }

    private ReplyKeyboardMarkup createDefaultKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Practice");
        if (this.isAdd) {
            row.add("Stop adding new words");
        } else {
            row.add("Some new words");
        }
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }
}
