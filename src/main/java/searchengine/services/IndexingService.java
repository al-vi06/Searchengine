package searchengine.services;

import org.springframework.scheduling.annotation.Async;
import searchengine.entity.SitePage;

import java.net.URL;

public interface IndexingService {
    @Async
    void startIndexing();

    void refreshPage(SitePage sitePage, URL url);

}
