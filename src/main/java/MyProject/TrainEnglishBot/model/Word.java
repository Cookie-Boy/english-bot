package MyProject.TrainEnglishBot.model;

import jakarta.persistence.*;

@Entity(name = "firstTry")
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long wordId;

    private long chatId;

    private String enWord;

    private String ruWord;

    private int hours;

    private int status;

    private boolean isUsed;

    public long getWordId() {
        return wordId;
    }

    public void setWordId(long wordId) {
        this.wordId = wordId;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public String getEnWord() {
        return enWord;
    }

    public void setEnWord(String enWord) {
        this.enWord = enWord;
    }

    public String getRuWord() {
        return ruWord;
    }

    public void setRuWord(String ruWord) {
        this.ruWord = ruWord;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }

    public Word(long wordId, long chatId, String enWord, String ruWord, int hours, int status) {
        this.wordId = wordId;
        this.chatId = chatId;
        this.enWord = enWord;
        this.ruWord = ruWord;
        this.hours = hours;
        this.status = status;
    }

    public Word(long chatId, String enWord, String ruWord) {
        this.chatId = chatId;
        this.enWord = enWord;
        this.ruWord = ruWord;
        this.hours = 1;
        this.status = 1;
        this.isUsed = false;
    }

    public Word() {

    }
}
