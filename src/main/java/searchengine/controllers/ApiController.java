package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.responses.NotOkResponse;
import searchengine.dto.statistics.responses.OkResponse;
import searchengine.entity.SitePage;

import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final SitesList sitesList;
    private final IndexingService indexingService;
    private final SearchService searchService;
    private final AtomicBoolean indexingProcessing = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() throws MalformedURLException {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        if (indexingProcessing.get()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "result", false,
                    "error", "Индексация уже запущена"
            ));
        }
//        indexingService.startIndexing();
//        return ResponseEntity.ok(Map.of("result", true));
        executor.submit(() -> {
            indexingProcessing.set(true);
            indexingService.startIndexing(indexingProcessing);
        });
        return ResponseEntity.status(HttpStatus.OK).body(new OkResponse());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        if (!indexingProcessing.get()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "result", false,
                    "error", "Индексация не запущена"
            ));
        }
        indexingProcessing.set(false);
        return ResponseEntity.ok(Map.of("result", true));
    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestBody String url) throws IOException {
        URL refUrl = new URL(url);
        SitePage sitePage = new SitePage();
        try {
            sitesList.getSites().stream().filter(site -> refUrl.getHost().equals(site.getUrl().getHost())).findFirst().map(site -> {
                sitePage.setName(site.getName());
                sitePage.setUrl(site.getUrl().toString());
                return sitePage;
            }).orElseThrow();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                    body(new NotOkResponse("Данная страница находится за пределами сайтов указанных в конфигурационном файле"));
        }

        indexingService.refreshPage(sitePage, refUrl);
        return ResponseEntity.status(HttpStatus.OK).body(new OkResponse());

    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String site,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @RequestParam(required = false, defaultValue = "20") Integer limit
    ) throws IOException {
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body(new NotOkResponse("Задан пустой поисковый запрос"));
        }
        return searchService.search(query, site, offset, limit);
    }

}
