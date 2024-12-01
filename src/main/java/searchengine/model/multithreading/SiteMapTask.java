package searchengine.model.multithreading;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;
import org.jsoup.HttpStatusException;

public class SiteMapTask extends RecursiveTask<Links> {
    private final String url;
    private final Queue<String> visitedUrls;
    private final HttpConfig httpConfig;
    private static String BASE_URL;
    private static final int SLEEP_TIME = 100;
    private static final Pattern FILE_PATTERN = Pattern.compile(".*\\.(jpg|jpeg|png|gif|bmp|pdf|doc|docx|xls|xlsx|ppt|pptx|zip|rar|tar|gz|7z|mp3|wav|mp4|mkv|avi|mov|sql)$", Pattern.CASE_INSENSITIVE);


    public SiteMapTask(String url, HttpConfig httpConfig) {
        this(url, new ConcurrentLinkedQueue<>(), httpConfig);
        BASE_URL = url;
    }
    public SiteMapTask(String url, Queue<String> visitedUrls, HttpConfig httpConfig) {
        this.url = url;
        this.visitedUrls = visitedUrls;
        this.httpConfig = httpConfig;
    }

    @Override
    protected Links compute() {
        Links currentLink = new Links(url);
        System.out.println("Processing URL: " + url);

        synchronized (visitedUrls) {
            if (visitedUrls.contains(url)) {
                return currentLink;
            }
            visitedUrls.add(url);
        }

        List<SiteMapTask> tasks = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(httpConfig.getUserAgent())
                    .referrer(httpConfig.getReferrer())
                    .timeout(10000)//время ответа от сервера = 10 сек.
                    .get();
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String href = link.attr("abs:href");
                if (isValidLink(href.trim())) {
                    SiteMapTask task = new SiteMapTask(href, visitedUrls, httpConfig);
                    task.fork();

                    tasks.add(task);
                    Thread.sleep(SLEEP_TIME);
                }
            }

            currentLink.setContent(doc.html());//установим содержимое страницы

            for (SiteMapTask task : tasks) {
                Links childLink = task.join();
                currentLink.addChildLink(childLink);
            }

        }
        catch (HttpStatusException e) {
            if (e.getStatusCode() == 500) {
                System.out.println("Ошибка 500 на странице: " + url);
                // Логирование ошибки или продолжение с другими страницами
            }
        }

        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return currentLink;
    }

    private boolean isValidLink(String link) {
        return link.startsWith(BASE_URL) && !link.contains("#") && !visitedUrls.contains(link) &&
                !FILE_PATTERN.matcher(link).matches();
    }

}
