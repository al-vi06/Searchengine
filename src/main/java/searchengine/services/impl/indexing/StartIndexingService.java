package searchengine.services.impl.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import searchengine.config.Connection;
import searchengine.config.Site;
import searchengine.config.SitesList;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
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

    private final Connection connection; //Инжекция Connection

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

        sitePagesAllFromDB.removeIf(sitePage ->
                !urlToIndexing.contains(sitePage.getUrl()));

        List<Thread> indexingThreadList = new ArrayList<>();

        for (SitePage siteUrl : sitePagesAllFromDB) {
            String urlSite = siteUrl.getUrl();
            //Runnable indexSite = () -> {
                //ConcurrentHashMap<String, Page> resultForkJoinPageIndexer = new ConcurrentHashMap<>();
                try {
                    log.info("Запущена индексация " + urlSite);


                    PageFinder pageFinder = new PageFinder(urlSite, new ConcurrentLinkedQueue<>(), siteUrl,
                            connection, siteRepository, pageRepository,
                            lemmaService, pageIndexerService, indexingProcessing);
                    pageFinder.compute();
                    //ForkJoinPool pool = new ForkJoinPool();
//                    pool.invoke(new PageFinder(urlSite, new ConcurrentLinkedQueue<>(), siteUrl,
//                            connection, siteRepository, pageRepository,
//                            lemmaService, pageIndexerService, indexingProcessing));


                } catch (SecurityException ex) {
                    SitePage sitePage = siteRepository.findById(siteUrl.getId()).orElseThrow();
                    sitePage.setStatus(Status.FAILED);
                    sitePage.setLastError(ex.getMessage());
                    siteRepository.save(sitePage);
                }

                if (!indexingProcessing.get()) {
                    log.warn("Indexing stopped by user, site:" + urlSite);
                    SitePage sitePage = siteRepository.findById(siteUrl.getId()).orElseThrow();
                    sitePage.setStatus(Status.FAILED);
                    sitePage.setLastError("Indexing stopped by user");
                    siteRepository.save(sitePage);
                } else {
                    log.info("Indexed site: " + urlSite);
                    SitePage sitePage = siteRepository.findById(siteUrl.getId()).orElseThrow();
                    sitePage.setStatus(Status.INDEXED);
                    siteRepository.save(sitePage);
                }

            //};

//            Thread thread = new Thread(indexSite);
//            indexingThreadList.add(thread);
//            thread.start();
        }

//        for (Thread thread : indexingThreadList) {
//            thread.join();
//        }

        indexingProcessing.set(false);

    }

    @Override
    public void refreshPage(SitePage site, URL url) {
        SitePage existSitePate = siteRepository.getSiteByUrl(site.getUrl());
        site.setId(existSitePate.getId());
        ConcurrentHashMap<String, Page> resultForkJoinPageIndexer = new ConcurrentHashMap<>();
        try {
            log.info("Запущена переиндексация страницы:" + url.toString());
            //reindexing
        } catch (SecurityException ex) {
            SitePage sitePage = siteRepository.findById(site.getId()).orElseThrow();
            sitePage.setStatus(Status.FAILED);
            sitePage.setLastError(ex.getMessage());
            siteRepository.save(sitePage);
        }
        log.info("Проиндексирован сайт: " + site.getName());
        SitePage sitePage = siteRepository.findById(site.getId()).orElseThrow();
        sitePage.setStatus(Status.INDEXED);
        siteRepository.save(sitePage);

    }

}
