package searchengine.services;

import org.springframework.scheduling.annotation.Async;
import searchengine.entity.SitePage;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public interface IndexingService {
    //@Async
    //void startIndexing();
    @Async
    void startIndexing(AtomicBoolean indexingProcessing);

    void refreshPage(SitePage sitePage, URL url);

}
