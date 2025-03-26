package searchengine.reposytories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.entity.SitePage;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<SitePage, Integer> {
    @EntityGraph(attributePaths = {"pages"})
    @Query("SELECT s FROM SitePage s WHERE s.url = :url")
    SitePage getSiteByUrl(@Param("url") String url);

}
