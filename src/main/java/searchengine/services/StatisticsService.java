package searchengine.services;

import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.entity.Site;

public interface StatisticsService {
    StatisticsResponse getStatistics();
    void startIndexing();
    void stopIndexing();
    boolean isIndexing();
    void handleSiteError(Site siteConfig, String errorMessage);
    void indexPage();

}
