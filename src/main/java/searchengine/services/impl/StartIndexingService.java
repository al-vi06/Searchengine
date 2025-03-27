package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import searchengine.config.Connection;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.BeanContainer;
import searchengine.entity.Page;
import searchengine.entity.SitePage;
import searchengine.entity.Status;
import searchengine.reposytories.LemmaRepository;
import searchengine.reposytories.PageRepository;
import searchengine.reposytories.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexerService;


import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class StartIndexingService implements IndexingService {
    private final SitesList sites;

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;

    private final SitesList sitesToIndexing;

    private final Set<SitePage> sitePagesAllFromDB;

    private final PageIndexerService pageIndexerService;

    private final LemmaService lemmaService;
    private AtomicBoolean indexingProcessing;

    private final Connection connection;

    @Override
    @Async
    public void startIndexing(AtomicBoolean indexingProcessing) {
        this.indexingProcessing = indexingProcessing;

        try {
            deleteSitePagesAndPagesInDB();
            addSitePagesToDB();
            indexAllSitePages();
        } catch (RuntimeException | InterruptedException ex) {
            indexingProcessing.set(false);
            log.error("Error: ", ex);
        }
    }

    private void deleteSitePagesAndPagesInDB() {
        List<SitePage> sitesFromDB = siteRepository.findAll();
        for (SitePage sitePageDb : sitesFromDB) {
            for (Site siteApp : sitesToIndexing.getSites()) {
                if (sitePageDb.getUrl().equals(siteApp.getUrl().toString())) {
                    siteRepository.deleteById(sitePageDb.getId());
                }
            }
        }
    }

    private void addSitePagesToDB() {
        for (Site siteApp : sitesToIndexing.getSites()) {
            SitePage sitePage = new SitePage();
            sitePage.setStatus(Status.INDEXING);
            sitePage.setStatusTime(new Date());
            sitePage.setUrl(siteApp.getUrl().toString());
            sitePage.setName(siteApp.getName());
            siteRepository.save(sitePage);
        }
    }

    private void indexAllSitePages() throws InterruptedException {
        sitePagesAllFromDB.addAll(siteRepository.findAll());

        List<String> urlToIndexing = new ArrayList<>();
        for (Site siteApp : sitesToIndexing.getSites()) {
            urlToIndexing.add(siteApp.getUrl().toString());
        }
        sitePagesAllFromDB.removeIf(sitePage -> !urlToIndexing.contains(sitePage.getUrl()));

        List<Thread> indexingThreadList = new ArrayList<>();
        for (SitePage siteDomain : sitePagesAllFromDB) {
            Runnable indexSite = () -> {
                ConcurrentHashMap<String, Page> resultForkJoinPageIndexer = new ConcurrentHashMap<>();
                try {
                    log.info("Запущена индексация " + siteDomain.getUrl());
                    new ForkJoinPool().invoke(new PageFinder(siteRepository, pageRepository, siteDomain,
                            "", resultForkJoinPageIndexer, connection,
                            lemmaService, pageIndexerService, indexingProcessing));
                } catch (SecurityException ex) {
                    SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    sitePage.setStatus(Status.FAILED);
                    sitePage.setLastError(ex.getMessage());
                    siteRepository.save(sitePage);
                }
                if (!indexingProcessing.get()) {
                    log.warn("Indexing stopped by user, site:" + siteDomain.getUrl());
                    SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    sitePage.setStatus(Status.FAILED);
                    sitePage.setLastError("Indexing stopped by user");
                    siteRepository.save(sitePage);
                } else {
                    log.info("Проиндексирован сайт: " + siteDomain.getUrl());
                    SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    sitePage.setStatus(Status.INDEXED);
                    siteRepository.save(sitePage);
                }

            };
            Thread thread = new Thread(indexSite);
            indexingThreadList.add(thread);
            thread.start();
        }
        for (Thread thread : indexingThreadList) {
            thread.join();
        }
        indexingProcessing.set(false);
    }


    @Override
    public void refreshPage(SitePage site, URL url) {
        SitePage existSitePate = siteRepository.getSiteByUrl(site.getUrl());
        site.setId(existSitePate.getId());
        ConcurrentHashMap<String, Page> resultForkJoinPageIndexer = new ConcurrentHashMap<>();
        try {
            log.info("Запущена переиндексация страницы:" + url.toString());
            PageFinder f = new PageFinder(siteRepository, pageRepository, site,
                    url.getPath(), resultForkJoinPageIndexer, connection,
                    lemmaService, pageIndexerService, indexingProcessing);
            f.refreshPage();
        } catch (SecurityException ex) {
            SitePage sitePage = siteRepository.getSiteByUrl(site.getUrl());
            sitePage.setStatus(Status.FAILED);
            sitePage.setLastError(ex.getMessage());
            siteRepository.save(sitePage);
        }

        log.info("Проиндексирован сайт: " + site.getName());
        SitePage sitePage = siteRepository.getSiteByUrl(site.getUrl());
        sitePage.setStatus(Status.INDEXED);
        siteRepository.save(sitePage);
    }

}
