package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.multithreading.HttpConfig;
import searchengine.model.multithreading.Links;
import searchengine.model.multithreading.SiteMapTask;
import searchengine.model.web.Page;
import searchengine.model.web.Site;
import searchengine.model.web.Status;
import searchengine.reposytories.PageRepository;
import searchengine.reposytories.SiteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final Random random = new Random();

    private volatile boolean indexingInProgress = false;
    private final Object lock = new Object();

    @Autowired
    private final HttpConfig httpConfig; // Инжекция HttpConfig

    @Override
    public StatisticsResponse getStatistics() {
        String[] statuses = { "INDEXED", "FAILED", "INDEXING" };
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for(int i = 0; i < sitesList.size(); i++) {
            Site site = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            int pages = random.nextInt(1_000);
            int lemmas = pages * random.nextInt(1_000);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(statuses[i % 3]);
            item.setError(errors[i % 3]);
            item.setStatusTime(System.currentTimeMillis() -
                    (random.nextInt(10_000)));
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    ///////////++{
    @Override
    @Transactional
    public void startIndexing() { //полная индексация
        synchronized (lock) {
            if (indexingInProgress) {
                throw new IllegalStateException("Индексация уже запущена");
            }
            indexingInProgress = true;
        }

        try {
            sites.getSites().parallelStream()
                    .forEach(siteConfig -> indexSingleSite(siteConfig));
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            synchronized (lock) {
                indexingInProgress = false;
            }
        }

    }

    @Transactional
    public void indexSingleSite(Site siteConfig) {//индексация сайта
        try {
            //Удаление старых данных
            siteRepository.deleteByUrl(siteConfig.getUrl());
            pageRepository.deleteBySiteUrl(siteConfig.getUrl());

            //Создание новой записи
            Site site = new Site();
            site.setName(siteConfig.getName());
            site.setUrl(siteConfig.getUrl());
            site.setStatus(Status.INDEXING);
            site.setStatus_time(new Date());
            siteRepository.save(site);

            //Обход страниц
            ForkJoinPool pool = new ForkJoinPool();
            Links rootLinks = pool.invoke(new SiteMapTask(siteConfig.getUrl(), httpConfig));
            savePages(site, rootLinks);

            //Установка статуса INDEXED
            site.setStatus(Status.INDEXED);
            siteRepository.save(site);

        } catch (Exception e) {
            handleSiteError(siteConfig, e.getMessage());
        }
    }

    @Transactional
    public void savePages(Site site, Links links) {//сохранение страниц
        for (Links child : links.getChildLinks()) {
            Page page = new Page();
            page.setSite(site);
            page.setPath(child.getUrl());
            page.setCode(200); //успешный код
            page.setContent(child.getContent());

            //site.getPages().add(page);
            pageRepository.save(page);

            site.setStatus_time(new Date());
            siteRepository.save(site);
        }
    }

    @Override
    @Transactional
    public void handleSiteError(Site siteConfig, String errorMessage) {//метод обработки ошибок
        Site site = siteRepository.findByUrl(siteConfig.getUrl());
        if (site != null) {
            site.setStatus(Status.FAILED);
            site.setLast_error(errorMessage);
            siteRepository.save(site);
        }
    }
    @Override
    public boolean isIndexing() {
        return indexingInProgress;
    }

    @Override
    @Transactional
    public void stopIndexing() {//остановка индексации
        synchronized (lock) {
            if (!indexingInProgress) {
                throw new IllegalStateException("Индексация не запущена");
            }
            indexingInProgress = false;
        }

        List<Site> sitesInProgress = siteRepository.findAllByStatus(Status.INDEXING);
        for (Site site : sitesInProgress) {
            site.setStatus(Status.FAILED);
            site.setLast_error("Индексация остановлена пользователем");
            siteRepository.save(site);
        }
    }
    ///////////--}
}
