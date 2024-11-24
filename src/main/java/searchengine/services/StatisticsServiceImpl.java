package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.*;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.reposytories.PageRepository;
import searchengine.reposytories.SiteRepository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final Random random = new Random();

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

    @Override
    @Transactional
    public StatisticsResponse startIndexing() {
        StatisticsResponse response = new StatisticsResponse();
        try {
            // Получаем список сайтов из конфигурации
            List<Site> sitesList = sites.getSites();

            for (Site siteEl : sitesList) {
                // Удаляем старые данные для текущего сайта
                siteRepository.deleteByUrl(siteEl.getUrl());
                pageRepository.deleteBySiteUrl(siteEl.getUrl());

                // Создаем новую запись для сайта в таблице Site со статусом INDEXING
                Site site = new Site();
                site.setName(site.getName());
                site.setUrl(site.getUrl());
                site.setStatus(Status.INDEXING);
                site.setStatus_time(new Date(System.currentTimeMillis()));
                site = siteRepository.save(site);

                // Начинаем обход страниц для индексации
                try {
                    indexPages(site);
                } catch (Exception e) {
                    // Если ошибка, меняем статус на FAILED и записываем ошибку
                    site.setStatus(Status.FAILED);
                    site.setLast_error("Ошибка индексации: " + e.getMessage());
                    siteRepository.save(site);
                    throw e;  // Пробрасываем исключение дальше
                }

                // После завершения обхода меняем статус на INDEXED
                site.setStatus(Status.INDEXED);
                siteRepository.save(site);
            }

            response.setResult(true);
        } catch (DataAccessException e) {
            response.setResult(false);
            response.setErrorMessage("Ошибка при работе с базой данных: " + e.getMessage());
        } catch (Exception e) {
            response.setResult(false);
            response.setErrorMessage("Ошибка индексации: " + e.getMessage());
        }

        return response;
    }

    private void indexPages(Site site) throws Exception {
        String baseUrl = site.getUrl();

        ForkJoinPool pool = new ForkJoinPool();
        Links Link = pool.invoke(new SiteMapTask(baseUrl));

        for (Links child : Link.getChildLinks()) {
            try {
                // Создаем новую запись в таблице page
                Page page = new Page();
                page.setSite(site);
                page.setPath(baseUrl + child);
                //page.setCode(random.nextInt(200, 500));
                page.setContent("Содержимое страницы " + child);
                pageRepository.save(page);

                site.setStatus_time(new Date(System.currentTimeMillis()));
                siteRepository.save(site);
            } catch (Exception e) {
                // Ошибка на конкретной странице
                throw new Exception("Ошибка индексации страницы " + child + ": " + e.getMessage());
            }
        }
    }

}
