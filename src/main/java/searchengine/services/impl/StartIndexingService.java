package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import searchengine.config.Connection;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.BeanContainer;
import searchengine.entity.SitePage;
import searchengine.entity.Status;
import searchengine.reposytories.LemmaRepository;
import searchengine.reposytories.PageRepository;
import searchengine.reposytories.SiteRepository;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexerService;


import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class StartIndexingService {
    private final SitesList sites;

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;

    private final SitesList sitesToIndexing;

    private final Set<SitePage> sitePagesAllFromDB;

    private final PageIndexerService pageIndexerService;

    private final LemmaService lemmaService;
    private final AtomicBoolean indexingProcessing = new AtomicBoolean(false);

    private final Connection connection;

    private ForkJoinPool pool = new ForkJoinPool();

    //@Override
    @Async
    public void startIndexing() {
        indexingProcessing.set(true);
        //pool = new ForkJoinPool();
        try {
            deleteSitePagesAndPagesInDB();
            addSitePagesToDB();
            indexAllSitePages();
        } catch (RuntimeException | InterruptedException ex) {
            indexingProcessing.set(false);
            log.error("Error: ", ex);
        }
    }

    public boolean stopIndexing() {
        try {
            if (!pool.isShutdown()) {
                pool.shutdown();
                indexingProcessing.set(false);
                log.info("Индексация была остановлена пользователем");
                return true;
            }
        } catch (RuntimeException ex){
            log.error("Ошибка завершения индексации: ", ex);
            return false;
        }

        return false;
    }

    public boolean getIndexingProcessing(){
        return indexingProcessing.get();
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
//            Runnable indexSite = () -> {
                try {
                    log.info("Запущена индексация " + urlSite);
                    //инициализируем bean container
                    BeanContainer beanContainer = new BeanContainer(connection, siteRepository, pageRepository,
                            lemmaService, pageIndexerService, indexingProcessing);
                    beanContainer.setUrl(urlSite);
                    beanContainer.setVisitedUrls(new ConcurrentLinkedQueue<>());
                    beanContainer.setSiteDomain(siteUrl);

                    //ForkJoinPool pool = new ForkJoinPool();
                    //pool.invoke(new PageFinder(beanContainer));
                    PageFinder pageFinder =  new PageFinder(beanContainer);
                    pageFinder.compute();

                } catch (SecurityException ex) {
                    SitePage sitePage = siteRepository.getSiteByUrl(siteUrl.getUrl());
                    sitePage.setStatus(Status.FAILED);
                    sitePage.setLastError(ex.getMessage());
                    siteRepository.save(sitePage);
                }

                if (!indexingProcessing.get()) {
                    log.warn("Indexing stopped by user, site:" + urlSite);
                    SitePage sitePage = siteRepository.getSiteByUrl(siteUrl.getUrl());
                    sitePage.setStatus(Status.FAILED);
                    sitePage.setLastError("Indexing stopped by user");
                    siteRepository.save(sitePage);
                } else {
                    log.info("Indexed site: " + urlSite);
                    SitePage sitePage = siteRepository.getSiteByUrl(siteUrl.getUrl());
                    sitePage.setStatus(Status.INDEXED);
                    siteRepository.save(sitePage);
                }

//            };

//            Thread thread = new Thread(indexSite);
//            indexingThreadList.add(thread);
//            thread.start();
        }

//        for (Thread thread : indexingThreadList) {
//            thread.join();
//        }

        indexingProcessing.set(false);

    }

    //@Override
    public void refreshPage(SitePage site, URL url) {
        SitePage existSitePate = siteRepository.getSiteByUrl(site.getUrl());
        site.setId(existSitePate.getId());
        //ConcurrentHashMap<String, Page> resultForkJoinPageIndexer = new ConcurrentHashMap<>();
        try {
            log.info("Запущена переиндексация страницы:" + url.toString());
            BeanContainer beanContainer = new BeanContainer(connection, siteRepository, pageRepository,
                    lemmaService, pageIndexerService, indexingProcessing);
            beanContainer.setUrl(url.toString());
            beanContainer.setVisitedUrls(new ConcurrentLinkedQueue<>());
            beanContainer.setSiteDomain(existSitePate);
//            PageFinder pageFinder = new PageFinder(url.toString(), new ConcurrentLinkedQueue<>(), existSitePate,
//                    connection, siteRepository, pageRepository,
//                    lemmaService, pageIndexerService, indexingProcessing);
            PageFinder pageFinder = new PageFinder(beanContainer);
            pageFinder.refreshPage();
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
