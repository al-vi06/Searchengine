package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.entity.SitePage;

import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
        //indexingService.startIndexing();
        if (indexingProcessing.get()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "result", false,
                    "error", "Индексация уже запущена"
            ));
        }

        executor.submit(() -> {
            indexingProcessing.set(true);
            indexingService.startIndexing(indexingProcessing);
        });

        return ResponseEntity.ok(Map.of("result", true));
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
        String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);
        URL refUrl = new URL(decodedUrl.substring(4));
        SitePage sitePage = new SitePage();
        try {
            sitesList.getSites().stream().filter(site -> refUrl.getHost().equals(site.getUrl().getHost())).findFirst().map(site -> {
                sitePage.setName(site.getName());
                sitePage.setUrl(site.getUrl().toString());
                return sitePage;
            }).orElseThrow();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "result", false,
                    "error", "Данная страница находится за пределами сайтов указанных в конфигурационном файле"
            ));
        }

        indexingService.refreshPage(sitePage, refUrl);
        return ResponseEntity.ok(Map.of("result", true));

    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String site,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @RequestParam(required = false, defaultValue = "20") Integer limit
    ) throws IOException {
        if (query == null || query.isBlank()) {
            //return ResponseEntity.badRequest().body(new NotOkResponse("Задан пустой поисковый запрос"));
            return ResponseEntity.badRequest().body(Map.of(
                    "result", false,
                    "error", "Задан пустой поисковый запрос"));
        }

        return searchService.search(query, site, offset, limit);
    }

}
