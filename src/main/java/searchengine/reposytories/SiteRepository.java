package searchengine.reposytories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.entity.SitePage;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<SitePage, Integer> {
//    @Transactional
//    void deleteByUrl(String url);
//    Site findByUrl(String url);
//    List<Site> findAllByStatus(Status status);

    @EntityGraph(type = EntityGraph.EntityGraphType.FETCH, attributePaths = "pages")
    //@Query(value = "select * from site s where s.url = :host limit 1", nativeQuery = true)
    //@Query(value = "select s from SitePage s where s.url = :host")
    SitePage getSiteByUrl(@Param("host") String host);

//    @Query("SELECT s FROM SitePage s LEFT JOIN FETCH s.pages WHERE s.id = :id")
//    Optional<SitePage> findByIdWithPages(@Param("id") int id);

}
