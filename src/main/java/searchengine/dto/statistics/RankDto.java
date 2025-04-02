package searchengine.dto.statistics;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.entity.Page;


@Getter
@Setter
@NoArgsConstructor
public class RankDto {
    private Page page;
    private float absRelevance = 0;
    private float relativeRelevance = 0;
    private float maxLemmaRank = 0;
}
