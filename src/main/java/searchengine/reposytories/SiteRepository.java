package searchengine.reposytories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.entity.Page;
import searchengine.entity.SitePage;

public interface SiteRepository extends JpaRepository<SitePage, Integer> {
//    @Transactional
//    void deleteByUrl(String url);
//    Site findByUrl(String url);
//    List<Site> findAllByStatus(Status status);
@Query(value = "select * from site s where s.url = :host limit 1", nativeQuery = true)
Page getPageByUrl(@Param("host") String host);

}
