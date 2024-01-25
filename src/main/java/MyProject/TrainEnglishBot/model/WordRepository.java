package MyProject.TrainEnglishBot.model;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface WordRepository extends CrudRepository<Word, Long> {
    List<Word> findAllByChatId(long chatId);
    List<Word> findAllByEnWord(String enWord);
}
