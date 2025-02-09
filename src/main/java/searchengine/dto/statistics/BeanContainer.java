package searchengine.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import searchengine.config.Connection;
import searchengine.entity.SitePage;
import searchengine.reposytories.PageRepository;
import searchengine.reposytories.SiteRepository;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexerService;

import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@AllArgsConstructor
public class BeanContainer {
    private final SitePage siteDomain;
    private final Connection connection;
    private final PageIndexerService pageIndexerService;
    private final LemmaService lemmaService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final AtomicBoolean indexingProcessing;
}
