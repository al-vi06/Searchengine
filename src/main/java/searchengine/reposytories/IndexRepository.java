package searchengine.reposytories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.indexing.Index;
import searchengine.model.indexing.Lemma;

public interface IndexRepository extends JpaRepository<Index, Integer> {
    void deleteByLemma(Lemma lemma);
}
