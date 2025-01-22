package searchengine.reposytories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entity.Page;

public interface PageRepository extends JpaRepository<Page, Integer> {
//    @Transactional
//    void deleteBySiteUrl(String url);

    @Query(value = "select * from page t where t.site_id = :siteId and t.path = :path limit 1", nativeQuery = true)
    Page findPageBySiteIdAndPath(@Param("path") String path, @Param("siteId") Integer siteId);

    @Query(value = "select count(p) from Page p where p.site = :siteId")
    Integer findCountRecordBySiteId(@Param("siteId") Integer siteId);

    @Query(value = "select count(p) from Page p where (:siteId is null or p.site = :siteId)")
    Integer getCountPages(@Param("siteId")Integer siteId);
}
