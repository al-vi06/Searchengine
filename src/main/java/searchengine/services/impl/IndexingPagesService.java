package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entity.Page;
import searchengine.entity.indexing.Index;
import searchengine.entity.indexing.Lemma;
import searchengine.reposytories.IndexRepository;
import searchengine.reposytories.LemmaRepository;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexerService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IndexingPagesService implements PageIndexerService {
    private final LemmaService lemmaService;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    @Override
    public void indexHtml(String html, Page indexingPage) {
        long start = System.currentTimeMillis();
        try {
            String cleanHtml            = lemmaService.cleanHtmlTags(html);
            Map<String, Integer> lemmas = lemmaService.collectLemmas(cleanHtml);
            lemmas.entrySet().parallelStream().forEach(entry -> saveLemma(entry.getKey(), entry.getValue(), indexingPage));
            log.debug("Индексация страницы " + (System.currentTimeMillis() - start) + " lemmas:" + lemmas.size());
        } catch (IOException e) {
            log.error(String.valueOf(e));
            throw new RuntimeException(e);
        }
    }
    @Transactional
    public void saveLemma(String lemma, Integer frequency, Page indexingPage) {
        Lemma existLemmaInDB = lemmaRepository.lemmaExist(lemma, indexingPage.getSite().getId());
        if (existLemmaInDB != null) {
            existLemmaInDB.setFrequency(existLemmaInDB.getFrequency() + frequency);
            lemmaRepository.saveAndFlush(existLemmaInDB);
            createIndex(indexingPage, existLemmaInDB, frequency);
        } else {
            try {
                Lemma newLemmaToDB = new Lemma();
                newLemmaToDB.setSite(indexingPage.getSite());
                newLemmaToDB.setLemma(lemma);
                newLemmaToDB.setFrequency(frequency);
                newLemmaToDB.setSite(indexingPage.getSite());
                lemmaRepository.saveAndFlush(newLemmaToDB);
                createIndex(indexingPage, newLemmaToDB, frequency);
            } catch (DataIntegrityViolationException ex) {
                log.debug("Ошибка при сохранении леммы, такая лемма уже существует. Вызов повторного сохранения");
                saveLemma(lemma, frequency, indexingPage);
            }
        }
    }
    private void createIndex(Page indexingPage, Lemma lemmaInDB, Integer rank) {
        Index indexSearchExist = indexRepository.indexSearchExist(indexingPage.getId(), lemmaInDB.getId());
        if (indexSearchExist != null) {
            indexSearchExist.setRank(indexSearchExist.getRank() + rank);
            indexRepository.save(indexSearchExist);
        } else {
            Index index = new Index();
            index.setPage(indexingPage);
            index.setLemma(lemmaInDB);
            index.setRank(rank);
            index.setLemma(lemmaInDB);
            index.setPage(indexingPage);
            indexRepository.save(index);
        }
    }

    @Transactional
    public void refreshLemma(Page refreshPage) {
        List<Index> indexes = indexRepository.findAllByPageId(refreshPage.getId());
        indexes.forEach(idx -> {
            Optional<Lemma> lemmaToRefresh = lemmaRepository.findById(idx.getLemma().getId());
            lemmaToRefresh.ifPresent(lemma -> {
                lemma.setFrequency((int) (lemma.getFrequency() - idx.getRank()));
                lemmaRepository.saveAndFlush(lemma);
            });
        });
    }
    @Override
    public void refreshIndex(String html, Page refreshPage) {
        long start = System.currentTimeMillis();
        try {
            String cleanHtml            = lemmaService.cleanHtmlTags(html);
            Map<String, Integer> lemmas = lemmaService.collectLemmas(cleanHtml);
            //уменьшение frequency у лемм которые присутствуют на обновляемой странице
            refreshLemma(refreshPage);
            //удаление индекса
            indexRepository.deleteAllByPageId(refreshPage.getId());
            //обновление лемм и индесов у обнолвенной страницы
            lemmas.entrySet().parallelStream().forEach(entry -> saveLemma(entry.getKey(), entry.getValue(), refreshPage));
            log.debug("Обновление индекса страницы " + (System.currentTimeMillis() - start) + " lemmas:" + lemmas.size());
        } catch (IOException e) {
            log.error(String.valueOf(e));
            throw new RuntimeException(e);
        }
    }
}
