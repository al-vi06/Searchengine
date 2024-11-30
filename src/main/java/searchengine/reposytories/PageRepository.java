package searchengine.reposytories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.web.Page;

public interface PageRepository extends JpaRepository<Page, Integer> {
    void deleteBySiteUrl(String url);
}
