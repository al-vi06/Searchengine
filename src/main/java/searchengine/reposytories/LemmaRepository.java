package searchengine.reposytories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.indexing.Index;
import searchengine.model.indexing.Lemma;

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    void deleteBySiteUrl(String url);
}
