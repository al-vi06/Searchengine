package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.indexing.Index;
import searchengine.model.indexing.Lemma;
import searchengine.model.indexing.LemmaFinder;
import searchengine.model.multithreading.HttpConfig;
import searchengine.model.multithreading.Links;
import searchengine.model.multithreading.SiteMapTask;
import searchengine.model.entity.Page;
import searchengine.model.entity.Site;
import searchengine.model.entity.Status;
import searchengine.reposytories.IndexRepository;
import searchengine.reposytories.LemmaRepository;
import searchengine.reposytories.PageRepository;
import searchengine.reposytories.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;


@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
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
    @Async
    public void startIndexing() {//полная индексация
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

    public void indexSingleSite(Site siteConfig) {//индексация сайта
        try {
            //Удаление старых данных
            String url = siteConfig.getUrl();
            siteRepository.deleteByUrl(url);
            pageRepository.deleteBySiteUrl(url);
            lemmaRepository.deleteBySiteUrl(url);
            //indexRepository.deleteByLemma();

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

    //@Transactional бессмсленно
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

            indexPage1(site, page);
        }
    }

    @Override
    //@Transactional
    public void handleSiteError(Site siteConfig, String errorMessage) {//метод обработки ошибок
        Site site = siteRepository.findByUrl(siteConfig.getUrl());
        if (site != null) {
            site.setStatus(Status.FAILED);
            site.setLast_error(errorMessage);
            siteRepository.save(site);
        }
    }

    @Override
    public void indexPage() {

    }


    @Override
    public boolean isIndexing() {
        return indexingInProgress;
    }

    @Override
    //@Transactional
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

    //@Transactional
    public void indexPage1(Site site, Page page) {

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
            lemma.setSite(site);
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

    ///////////--}

}
