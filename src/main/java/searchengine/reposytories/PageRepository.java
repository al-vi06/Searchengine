package searchengine.reposytories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.entity.Page;

public interface PageRepository extends JpaRepository<Page, Integer> {
    @Transactional
    void deleteBySiteUrl(String url);
}
