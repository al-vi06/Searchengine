package searchengine.reposytories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.entity.Site;
import searchengine.model.entity.Status;

import java.util.List;

public interface SiteRepository extends JpaRepository<Site, Integer> {
    @Transactional
    void deleteByUrl(String url);
    Site findByUrl(String url);
    List<Site> findAllByStatus(Status status);
}
