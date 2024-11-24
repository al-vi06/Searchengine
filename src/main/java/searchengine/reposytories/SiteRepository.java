package searchengine.reposytories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Site;

public interface SiteRepository extends JpaRepository<Site, Integer> {
    void deleteByUrl(String url);
}
