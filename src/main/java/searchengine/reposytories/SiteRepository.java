package searchengine.reposytories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.web.Site;
import searchengine.model.web.Status;

import java.util.List;

public interface SiteRepository extends JpaRepository<Site, Integer> {
    void deleteByUrl(String url);
    Site findByUrl(String url);
    List<Site> findAllByStatus(Status status);
}
