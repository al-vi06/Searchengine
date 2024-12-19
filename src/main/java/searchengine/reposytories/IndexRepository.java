package searchengine.reposytories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entity.indexing.Index;
import searchengine.entity.indexing.Lemma;

import java.util.List;

public interface IndexRepository extends JpaRepository<Index, Integer> {
//    @Transactional
//    void deleteByLemma(Lemma lemma);
@Query(value = "select i from Index i where i.id = :pageId and i.id = :lemmaId")
Index indexSearchExist(@Param("pageId") Integer pageId, @Param("lemmaId") Integer lemmaId);

    @Query(value = "select i from Index i where i.id = :lemmaId")
    List<Index> findIndexesByLemma(Integer lemmaId);

    @Query(value = "select i from Index i where i.id = :pageId")
    List<Index> findAllByPageId(@Param("pageId") Integer pageId);

    @Modifying
    @Transactional
    @Query(value = "delete from Index i where i.id = :pageId")
    void deleteAllByPageId(@Param("pageId") Integer pageId);
}
