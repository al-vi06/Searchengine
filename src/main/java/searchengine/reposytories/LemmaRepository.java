package searchengine.reposytories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entity.indexing.Lemma;

import java.util.List;

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    //    @Transactional
//    void deleteBySiteUrl(String url);
    @Query(value = "select * from lemma t where t.lemma = :lemma and t.site_id = :siteId for update", nativeQuery = true)
    Lemma lemmaExist(String lemma, Integer siteId);

    @Query(value = "select count(l) from Lemma l where l.site.id = :siteId")
    Integer findCountRecordBySiteId(Integer siteId);

    @Query(value = "select l.frequency from Lemma l where l.lemma = :lemma and (:siteId is null or l.site = :siteId)")
    Integer findCountPageByLemma(String lemma, Integer siteId);

    @Query(value = "select l.id from Lemma l where l.lemma = :lemma")
    Integer findIdLemma(String lemma);

    @Query(value = "select l from Lemma l where l.lemma = :lemma and (:siteId is null or l.site = :siteId)")
    List<Lemma> findLemmasByLemmaAndSiteId(String lemma, Integer siteId);
}
