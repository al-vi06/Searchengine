package searchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import searchengine.config.Connection;
import searchengine.entity.SitePage;
import searchengine.reposytories.PageRepository;
import searchengine.reposytories.SiteRepository;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexerService;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class BeanContainer {
    private String url;
    private Queue<String> visitedUrls;
    private SitePage siteDomain;
    //private final ForkJoinPool pool;
    private final Connection connection;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaService lemmaService;
    private final PageIndexerService pageIndexerService;
    private final AtomicBoolean indexingProcessing;
}