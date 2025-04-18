package searchengine.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

import searchengine.config.Connection;
import searchengine.dto.BeanContainer;
import searchengine.entity.Page;
import searchengine.entity.SitePage;
import searchengine.entity.Status;
import searchengine.reposytories.PageRepository;
import searchengine.reposytories.SiteRepository;
import searchengine.services.PageIndexerService;

@Slf4j
public class PageFinder extends RecursiveAction{
    private static String BASE_URL;
    private static final int SLEEP_TIME = 100;
    private static final Pattern FILE_PATTERN = Pattern.compile(".*\\.(jpg|jpeg|png|gif|bmp|pdf|doc|docx|xls|xlsx|ppt|pptx|zip|rar|tar|gz|7z|mp3|wav|mp4|mkv|avi|mov|sql)$", Pattern.CASE_INSENSITIVE);

    //переменные для работы с сервисом и репозиторием
    private final BeanContainer beanContainer;

    //такой констр был ранее до бина
    //public PageFinder(String url, Queue<String> visitedUrls, SitePage siteDomain,
//                      Connection connection,
//                      SiteRepository siteRepository, PageRepository pageRepository, LemmaService lemmaService,
//                      PageIndexerService pageIndexerService, AtomicBoolean indexingProcessing) {

//        BASE_URL = siteDomain.getUrl();
//        this.url = url;
//        this.visitedUrls = visitedUrls;
//        this.siteDomain = siteDomain;
//
//        this.connection = connection;
//        this.siteRepository = siteRepository;
//        this.pageRepository = pageRepository;
//        this.lemmaService = lemmaService;
//        this.pageIndexerService = pageIndexerService;
//        this.indexingProcessing = indexingProcessing;
//    }

    public PageFinder(BeanContainer beanContainer) {
        BASE_URL = beanContainer.getSiteDomain().getUrl();
        this.beanContainer = beanContainer;
    }


    @Override
    protected void compute() {
        if (!beanContainer.getIndexingProcessing().get()) {
            return;
        }

        Page indexingPage = new Page();
        SitePage siteDomain = beanContainer.getSiteDomain();
        indexingPage.setSite(siteDomain);
        String url = beanContainer.getUrl();
        indexingPage.setPath(url.substring(BASE_URL.length())); //url

        System.out.println("Processing URL: " + url);

        Queue<String> visitedUrls = beanContainer.getVisitedUrls();
        synchronized (visitedUrls) {
            if (visitedUrls.contains(url)) {
                return;
            }
            visitedUrls.add(url);
        }
        beanContainer.setVisitedUrls(visitedUrls);

        List<PageFinder> tasks = new ArrayList<>();

        SiteRepository siteRepository = beanContainer.getSiteRepository();
        Connection connection = beanContainer.getConnection();
        PageRepository pageRepository = beanContainer.getPageRepository();
        PageIndexerService pageIndexerService = beanContainer.getPageIndexerService();
        //ForkJoinPool pool = beanContainer.getPool();

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(connection.userAgent())
                    .referrer(connection.referrer())
                    .timeout(connection.timeout())
                    .get();

            String html = doc.html();
            if (html != null) {
                indexingPage.setContent(html);
            }

            if (indexingPage.getContent() == null || indexingPage.getContent().isEmpty() || indexingPage.getContent().isBlank()) {
                throw new Exception("Content of site id:" + indexingPage.getSite() + ", page:" + indexingPage.getPath() + " is null or empty");
            }
            indexingPage.setCode(doc.connection().response().statusCode());
            SitePage sitePage = siteRepository.getSiteByUrl(siteDomain.getUrl());
            sitePage.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
            siteRepository.save(sitePage);
            pageRepository.save(indexingPage);
            pageIndexerService.indexHtml(indexingPage.getContent(), indexingPage);

            //инициализируем bean container
            BeanContainer newBeanContainer = new BeanContainer(
                    connection, siteRepository, pageRepository,
                    beanContainer.getLemmaService(), pageIndexerService,
                    beanContainer.getIndexingProcessing()
            );

            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String href = link.attr("abs:href");
                if (isValidLink(href.trim())) {
                    //beanContainer.setUrl(href);
                    newBeanContainer.setUrl(href);
                    newBeanContainer.setVisitedUrls(visitedUrls);
                    newBeanContainer.setSiteDomain(siteDomain);
                    PageFinder task = new PageFinder(newBeanContainer);

                    //для многопоточки!
                    //task.compute();

                    //task.fork();
                    tasks.add(task);
                    Thread.sleep(SLEEP_TIME);
                }
            }

            invokeAll(tasks);

            //for (PageFinder task : tasks) {
//                if (!beanContainer.getIndexingProcessing().get()) {
//                    return;
//                }
                //beanContainer.getPool().invoke(task);
               //task.join();
           // }
        }
        catch (Exception ex) {
            String error = ex.toString();
            errorHandling(error, indexingPage);
            SitePage sitePage = siteRepository.getSiteByUrl(siteDomain.getUrl());
            sitePage.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
            sitePage.setStatus(Status.FAILED);
            sitePage.setLastError(error);
//
            siteRepository.save(sitePage);
            pageRepository.save(indexingPage);
            log.debug("ERROR INDEXATION, siteId:" + indexingPage.getSite() + ", path:" + indexingPage.getPath() + ", code:" + indexingPage.getCode() + ", error:" + ex.getMessage());
        }

    }

    private boolean isValidLink(String link) {
        return link.startsWith(BASE_URL) && !link.contains("#") && !beanContainer.getVisitedUrls().contains(link) &&
                !FILE_PATTERN.matcher(link).matches();
    }

    public void refreshPage() {

        Page indexingPage = new Page();
        SitePage siteDomain = beanContainer.getSiteDomain();
        indexingPage.setSite(siteDomain);
        String url = beanContainer.getUrl();
        indexingPage.setPath(url.substring(BASE_URL.length()));

        SiteRepository siteRepository = beanContainer.getSiteRepository();
        Connection connection = beanContainer.getConnection();
        PageRepository pageRepository = beanContainer.getPageRepository();
        PageIndexerService pageIndexerService = beanContainer.getPageIndexerService();
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(connection.userAgent())
                    .referrer(connection.referrer())
                    .timeout(connection.timeout())
                    .get();

            indexingPage.setContent(doc.html());
            indexingPage.setCode(doc.connection().response().statusCode());

            if (indexingPage.getContent() == null || indexingPage.getContent().isEmpty() || indexingPage.getContent().isBlank()) {
                throw new Exception("Content of site id:" + indexingPage.getSite() + ", page:" + indexingPage.getPath() + " is null or empty");
            }

        } catch (Exception ex) {
            String error = ex.toString();
            errorHandling(error, indexingPage);
            SitePage sitePage = siteRepository.getSiteByUrl(siteDomain.getUrl());
            sitePage.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
            sitePage.setStatus(Status.FAILED);
            sitePage.setLastError(error);
            siteRepository.save(sitePage);
            pageRepository.save(indexingPage);
            return;
        }
        SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
        sitePage.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
        siteRepository.save(sitePage);

        Page pageToRefresh = pageRepository.findPageBySiteIdAndPath(url, sitePage.getId());
        if (pageToRefresh != null) {
            pageToRefresh.setCode(indexingPage.getCode());
            pageToRefresh.setContent(indexingPage.getContent());
            pageRepository.save(pageToRefresh);
            pageIndexerService.refreshIndex(indexingPage.getContent(), pageToRefresh);
        } else {
            pageRepository.save(indexingPage);
            pageIndexerService.refreshIndex(indexingPage.getContent(), indexingPage);
        }
    }

    void errorHandling(String ex, Page indexingPage) {
        String message = ex.toString();
        int errorCode;
        if (message.contains("UnsupportedMimeTypeException")) {
            errorCode = 415;    // Ссылка на pdf, jpg, png документы
        } else if (message.contains("Status=401")) {
            errorCode = 401;    // На несуществующий домен
        } else if (message.contains("UnknownHostException")) {
            errorCode = 401;
        } else if (message.contains("Status=403")) {
            errorCode = 403;    // Нет доступа, 403 Forbidden
        } else if (message.contains("Status=404")) {
            errorCode = 404;    // // Ссылка на pdf-документ, несущ. страница, проигрыватель
        } else if (message.contains("Status=500")) {
            errorCode = 401;    // Страница авторизации
        } else if (message.contains("ConnectException: Connection refused")) {
            errorCode = 500;    // ERR_CONNECTION_REFUSED, не удаётся открыть страницу
        } else if (message.contains("SSLHandshakeException")) {
            errorCode = 525;
        } else if (message.contains("Status=503")) {
            errorCode = 503; // Сервер временно не имеет возможности обрабатывать запросы по техническим причинам (обслуживание, перегрузка и прочее).
        } else {
            errorCode = -1;
        }
        indexingPage.setCode(errorCode);
    }

}
