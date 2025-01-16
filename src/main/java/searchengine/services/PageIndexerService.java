package searchengine.services;
import searchengine.entity.Page;

public interface PageIndexerService {
    void indexHtml(String html, Page indexingPage);

    void refreshIndex(String html, Page refreshPage);
}
