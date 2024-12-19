package searchengine.services;

import searchengine.dto.statistics.StatisticsResponse;

import java.net.MalformedURLException;

public interface StatisticsService {
//    StatisticsResponse getStatistics();
//    void startIndexing();
//    void stopIndexing();
//    boolean isIndexing();
//    void handleSiteError(Site siteConfig, String errorMessage);
//    void indexPage();
    StatisticsResponse getStatistics() throws MalformedURLException;


}
