package searchengine.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.entity.Page;
import searchengine.services.PageIndexerService;


@Service
@Slf4j
public class PageIndexerServiceImpl implements PageIndexerService {
    @Override
    public void indexHtml(String html, Page indexingPage) {

    }

    @Override
    public void refreshIndex(String html, Page refreshPage) {

    }
}
