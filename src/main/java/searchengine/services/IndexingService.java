package searchengine.services;

import searchengine.entity.SitePage;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public interface IndexingService {
    void startIndexing(); //AtomicBoolean indexingProcessing
    void refreshPage(SitePage sitePage, URL url);



    boolean isIndexing();
    void stopIndexing();
}
