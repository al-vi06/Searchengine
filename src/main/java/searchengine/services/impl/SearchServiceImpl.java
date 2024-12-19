package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.entity.Status;
import searchengine.services.SearchService;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final Status indexSuccessStatus = Status.INDEXED;
    private final double frequencyLimitProportion = 100;
    @Override
    public ResponseEntity<Object> search(String query, String site, Integer offset, Integer limit) throws IOException {
        return null;
    }
}
