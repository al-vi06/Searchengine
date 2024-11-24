package searchengine.dto.statistics;

import lombok.Data;

@Data
public class StatisticsResponse {
    private boolean result;
    private StatisticsData statistics;

    //++
    private String errorMessage; // Сообщение об ошибке (если есть)
}
