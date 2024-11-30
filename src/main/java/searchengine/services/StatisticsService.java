package searchengine.services;

import searchengine.dto.statistics.StatisticsResponse;

public interface StatisticsService {
    StatisticsResponse getStatistics();
    void startIndexing();
    void stopIndexing();
    boolean isIndexing();

}
