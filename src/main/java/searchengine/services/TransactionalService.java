package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.indexing.Index;
import searchengine.model.indexing.Lemma;
import searchengine.model.web.Page;
import searchengine.model.web.Site;
import searchengine.model.web.Status;
import searchengine.reposytories.IndexRepository;
import searchengine.reposytories.LemmaRepository;
import searchengine.reposytories.PageRepository;
import searchengine.reposytories.SiteRepository;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionalService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Transactional
    public void saveSite(Site site) {
        siteRepository.save(site);
    }

    @Transactional
    public void savePage(Page page) {
        pageRepository.save(page);
    }

    @Transactional
    public void saveLemmaAndIndex(Site site, Page page, Map<String, Integer> lemmas) {
        lemmas.forEach((key, value) -> {
            Lemma lemma = new Lemma();
            lemma.setSite(site);
            lemma.setLemma(key);
            lemma.setFrequency(value);
            lemmaRepository.save(lemma);

            Index index = new Index();
            index.setLemma(lemma);
            index.setPage(page);
            index.setRank(value);
            indexRepository.save(index);
        });
    }

    @Transactional
    public void updateSiteError(Site site, String errorMessage) {
        site.setStatus(Status.FAILED);
        site.setLast_error(errorMessage);
        siteRepository.save(site);
    }

    @Transactional
    public void deleteOldData(String siteUrl) {
        siteRepository.deleteByUrl(siteUrl);
        pageRepository.deleteBySiteUrl(siteUrl);
        lemmaRepository.deleteBySiteUrl(siteUrl);
    }

}
