package searchengine.services.impl.indexing;

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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import searchengine.config.Connection;
import searchengine.entity.Page;
import searchengine.entity.SitePage;
import searchengine.reposytories.PageRepository;
import searchengine.reposytories.SiteRepository;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexerService;

@Slf4j
public class PageFinder extends RecursiveAction{
    private final String url;
    private final Queue<String> visitedUrls;
    private static String BASE_URL;
    private static final int SLEEP_TIME = 100;
    private static final Pattern FILE_PATTERN = Pattern.compile(".*\\.(jpg|jpeg|png|gif|bmp|pdf|doc|docx|xls|xlsx|ppt|pptx|zip|rar|tar|gz|7z|mp3|wav|mp4|mkv|avi|mov|sql)$", Pattern.CASE_INSENSITIVE);

    //переменные для работы с сервисом и репозиторием
    private final SitePage siteDomain;
    private final Connection connection;
    private final PageIndexerService pageIndexerService;
    private final LemmaService lemmaService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final AtomicBoolean indexingProcessing;

//    public PageFinder(String url, AtomicBoolean indexingProcessing, Connection connection) {
//        this(url, new ConcurrentLinkedQueue<>(), new SitePage(),  connection, null, null, null,
//                null, indexingProcessing);
//        BASE_URL = url;
//    }

    public PageFinder(String url, Queue<String> visitedUrls, SitePage siteDomain, Connection connection,
                      SiteRepository siteRepository, PageRepository pageRepository, LemmaService lemmaService,
                      PageIndexerService pageIndexerService, AtomicBoolean indexingProcessing) {
        BASE_URL = url;
        this.url = url;
        this.visitedUrls = visitedUrls;
        this.siteDomain = siteDomain;

        this.connection = connection;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaService = lemmaService;
        this.pageIndexerService = pageIndexerService;
        this.indexingProcessing = indexingProcessing;
    }

    @Override
    protected void compute() {
        if (!indexingProcessing.get()) {
            return;
        }

        Page indexingPage = new Page();
        indexingPage.setSite(siteDomain);
        indexingPage.setPath(url);

        System.out.println("Processing URL: " + url);

        synchronized (visitedUrls) {
            if (visitedUrls.contains(url)) {
                return;
            }
            visitedUrls.add(url);
        }

        List<PageFinder> tasks = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(connection.getUserAgent())
                    .referrer(connection.getReferrer())
                    .timeout(connection.getTimeout())
                    .get();

            indexingPage.setContent(doc.html());
            if (indexingPage.getContent() == null || indexingPage.getContent().isEmpty() || indexingPage.getContent().isBlank()) {
                throw new Exception("Content of site id:" + indexingPage.getSite() + ", page:" + indexingPage.getPath() + " is null or empty");
            }
            indexingPage.setCode(doc.connection().response().statusCode());

            SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
            sitePage.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
            siteRepository.save(sitePage);
            pageRepository.save(indexingPage);
            //pageIndexerService.indexHtml(indexingPage.getContent(), indexingPage); // еще не реализован

            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String href = link.attr("abs:href");
                if (isValidLink(href.trim())) {
                    PageFinder task = new PageFinder(href, visitedUrls, siteDomain, connection,
                            siteRepository, pageRepository, lemmaService,
                            pageIndexerService, indexingProcessing);
                    task.compute();
                    //task.fork();
                    tasks.add(task);
                    Thread.sleep(SLEEP_TIME);
                }
            }

            for (PageFinder task : tasks) {
                if (!indexingProcessing.get()) {
                    return;
                }
                //task.join();
            }

        }
        catch (Exception ex) {
            errorHandling(ex, indexingPage);
            SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
            sitePage.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
            siteRepository.save(sitePage);
            pageRepository.save(indexingPage);
            log.debug("ERROR INDEXATION, siteId:" + indexingPage.getSite() + ", path:" + indexingPage.getPath() + ", code:" + indexingPage.getCode() + ", error:" + ex.getMessage());
            return;
        }

    }

    private boolean isValidLink(String link) {
        return link.startsWith(BASE_URL) && !link.contains("#") && !visitedUrls.contains(link) &&
                !FILE_PATTERN.matcher(link).matches();
    }

    void errorHandling(Exception ex, Page indexingPage) {
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
