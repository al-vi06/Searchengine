package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.HttpConfig;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.entity.Page;
import searchengine.entity.SitePage;
import searchengine.entity.Status;
import searchengine.entity.indexing.Index;
import searchengine.entity.indexing.Lemma;
import searchengine.entity.multithreading.Links;
import searchengine.entity.multithreading.SiteMapTask;
import searchengine.reposytories.LemmaRepository;
import searchengine.reposytories.PageRepository;
import searchengine.reposytories.SiteRepository;
import searchengine.services.IndexingService;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;


@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl_Copy implements IndexingService {
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
//    private final PageIndexerService pageIndexerService;
//    private final LemmaService lemmaService;
    //private volatile boolean indexingInProgress = false;
    private AtomicBoolean indexingProcessing;
    //private final Object lock = new Object();

    private final HttpConfig httpConfig; //Инжекция HttpConfig

    @Override
    @Async
    public void startIndexing(AtomicBoolean indexingProcessing) {
        synchronized (lock) {
            if (indexingInProgress) {
                throw new IllegalStateException("Индексация уже запущена");
            }
            indexingInProgress = true;
        }

        try {
//            sites.getSites().parallelStream()
//                    .forEach(siteConfig -> indexSingleSite(siteConfig));
            for (Site site : sites.getSites() ) {
                indexSingleSite(site);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            synchronized (lock) {
                indexingInProgress = false;
            }
        }

    }

    @Override
    public void refreshPage(SitePage siteDomain, URL url) {
        SitePage existSitePate = siteRepository.getSiteByUrl(siteDomain.getUrl());
        siteDomain.setId(existSitePate.getId());
        ConcurrentHashMap<String, Page> resultForkJoinPageIndexer = new ConcurrentHashMap<>();
        try {
            log.info("Запущена переиндексация страницы:" + url.toString());
//            PageFinder f = new PageFinder(siteRepository, pageRepository, siteDomain, url.getPath(), resultForkJoinPageIndexer, httpConfig, lemmaService, pageIndexerService, indexingProcessing);
//            f.refreshPage();
        } catch (SecurityException ex) {
            SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
            sitePage.setStatus(Status.FAILED);
            sitePage.setLastError(ex.getMessage());
            siteRepository.save(sitePage);
        }
        log.info("Проиндексирован сайт: " + siteDomain.getName());
        SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
        sitePage.setStatus(Status.INDEXED);
        siteRepository.save(sitePage);

    }

    public void indexSingleSite(SitePage sitePageConfig) {//индексация сайта
        try {
            //Удаление старых данных
            SitePage existingSitePage = siteRepository.findByUrl(sitePageConfig.getUrl());
            if (existingSitePage != null) {
                clearOldData(existingSitePage);
            }

//            String url = siteConfig.getUrl();
//            siteRepository.deleteByUrl(url);
//            pageRepository.deleteBySiteUrl(url);
//            lemmaRepository.deleteBySiteUrl(url);

            //Создание новой записи
            SitePage sitePage = new SitePage();
            sitePage.setName(sitePageConfig.getName());
            sitePage.setUrl(sitePageConfig.getUrl());
            sitePage.setStatus(Status.INDEXING);
            sitePage.setStatusTime(new Date());
            siteRepository.save(sitePage);

            //Обход страниц
            ForkJoinPool pool = new ForkJoinPool();
            Links rootLinks = pool.invoke(new SiteMapTask(sitePageConfig.getUrl(), httpConfig));
            savePages(sitePage, rootLinks);

            //Установка статуса INDEXED
            sitePage.setStatus(Status.INDEXED);
            siteRepository.save(sitePage);

        } catch (Exception e) {
            handleSiteError(sitePageConfig, e.getMessage());
        }
    }

    @Transactional
    public void clearOldData(SitePage sitePage) {
        // Принудительная инициализация ленивых коллекций
        sitePage.getPages().size();
        sitePage.getLemmas().size();

        // Удаление данных из базы
        String url = sitePage.getUrl();
        pageRepository.deleteBySiteUrl(url);
        lemmaRepository.deleteBySiteUrl(url);

        // Очистка коллекций
        sitePage.getPages().clear();
        sitePage.getLemmas().clear();
        siteRepository.save(sitePage);

//        String url = site.getUrl();
//
//        // Удаление данных через репозитории
//        pageRepository.deleteBySiteUrl(url);
//        lemmaRepository.deleteBySiteUrl(url);
//
//        // Не обращаемся к коллекциям напрямую
//        siteRepository.save(site);
    }

    public void savePages(SitePage sitePage, Links links) {//сохранение страниц
        for (Links child : links.getChildLinks()) {
            Page page = new Page();
            page.setSitePage(sitePage);
            page.setPath(child.getUrl());
            page.setCode(200); //успешный код
            page.setContent(child.getContent());

            sitePage.getPages().add(page); //Добавляем в существующую коллекцию
            pageRepository.save(page);

            sitePage.setStatusTime(new Date());
            siteRepository.save(sitePage);

            indexPage(sitePage, page);
        }

    }

    @Override
    public void handleSiteError(SitePage sitePageConfig, String errorMessage) {//метод обработки ошибок
        SitePage sitePage = siteRepository.findByUrl(sitePageConfig.getUrl());
        if (sitePage != null) {
            sitePage.setStatus(Status.FAILED);
            sitePage.setLastError(errorMessage);
            siteRepository.save(sitePage);
        }
    }

    @Override
    public boolean isIndexing() {
        return indexingInProgress;
    }

    @Override
    public void stopIndexing() {//остановка индексации
        synchronized (lock) {
            if (!indexingInProgress) {
                throw new IllegalStateException("Индексация не запущена");
            }
            indexingInProgress = false;
        }

        List<SitePage> sitesInProgresses = siteRepository.findAllByStatus(Status.INDEXING);
        for (SitePage sitePage : sitesInProgresses) {
            sitePage.setStatus(Status.FAILED);
            sitePage.setLastError("Индексация остановлена пользователем");
            siteRepository.save(sitePage);
        }
    }

    public void indexPage(SitePage sitePage, Page page) {
        LemmaFinder lemmaFinder;
        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String cleanHtml = lemmaFinder.cleanHtmlTags(page.getContent());
        Map<String, Integer> lemmas = lemmaFinder.collectLemmas(cleanHtml);
        lemmas.forEach((key, value) -> {
            Lemma lemma = new Lemma();
            lemma.setSitePage(sitePage);
            lemma.setLemma(key);
            lemma.setFrequency(value);
            //List<Index> indexes;
            lemmaRepository.save(lemma);

            Index index =new Index();
            index.setLemma(lemma);
            index.setPage(page);
            index.setRank(value);
            indexRepository.save(index);
        });


    }

}
