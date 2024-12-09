package searchengine.reposytories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.indexing.Index;
import searchengine.model.indexing.Lemma;

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    @Transactional
    void deleteBySiteUrl(String url);
}
