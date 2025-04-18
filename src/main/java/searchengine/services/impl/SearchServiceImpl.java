package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.responses.NotOkResponse;
import searchengine.dto.responses.SearchDataResponse;
import searchengine.dto.responses.SearchResponse;
import searchengine.dto.statistics.RankDto;
import searchengine.entity.SitePage;
import searchengine.entity.Status;
import searchengine.entity.indexing.Index;
import searchengine.entity.indexing.Lemma;
import searchengine.reposytories.IndexRepository;
import searchengine.reposytories.LemmaRepository;
import searchengine.reposytories.PageRepository;
import searchengine.reposytories.SiteRepository;
import searchengine.services.LemmaService;
import searchengine.services.SearchService;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaService lemmaService;
    private final Status indexSuccessStatus = Status.INDEXED;
    private final double frequencyLimitProportion = 100;

    @Override
    public ResponseEntity<Object> search(String query, String site, Integer offset, Integer limit) throws IOException {
        if (checkIndexStatusNotIndexed(site)) {
            return ResponseEntity.badRequest().body(new NotOkResponse("Индексация сайта для поиска не закончена"));
        }

        SitePage siteTarget = siteRepository.getSiteByUrl(site);
        Integer countPages = siteTarget != null ? pageRepository.getCountPages(siteTarget.getId()) : pageRepository.getCountPages(null);

        //Exclusion lemmas by frequent
        List<Lemma> lemmasForSearch = lemmaService.collectLemmas(query).keySet().stream().map(
                        it -> lemmaRepository.findLemmasByLemmaAndSiteId(it, siteTarget != null ? siteTarget.getId() : null))
                .flatMap(Collection::stream).collect(Collectors.toList());
        lemmasForSearch.removeIf(e -> {
            Integer lemmaFrequency = lemmaRepository.findCountPageByLemma(e.getLemma(), e.getSite().getId());
            if (lemmaFrequency == null) return true;
            log.info("Lemma frequency(" + e + "):" + lemmaFrequency);
            log.info("Frequency limit:" + (double) lemmaFrequency / countPages);
            return ((double) lemmaFrequency / countPages > frequencyLimitProportion);
        });

        if (lemmasForSearch.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        //Sorting lemmas by frequent
        List<Lemma> sortedLemmasToSearch = lemmasForSearch.stream().
                map(l -> new AbstractMap.SimpleEntry<>(l.getFrequency(), l)).
                sorted(Comparator.comparingInt(Map.Entry::getKey)).
                map(Map.Entry::getValue).toList();


        //Search pages by first lemma
        Map<Integer, Index> indexesByLemmas = indexRepository.findIndexesByLemma(sortedLemmasToSearch.get(0).getId())
                .stream().collect(Collectors.toMap(index -> index.getPage().getId(), index -> index));

        for (int i = 1; i <= sortedLemmasToSearch.size() - 1; i++) {
            List<Index> indexNextLemma = indexRepository.findIndexesByLemma(sortedLemmasToSearch.get(i).getId());
            List<Integer> pagesToSave = new ArrayList<>();
            for (Index indexNext : indexNextLemma) {
                if (indexesByLemmas.containsKey(indexNext.getPage().getId())) {
                    pagesToSave.add(indexNext.getPage().getId());
                }
            }
            indexesByLemmas.entrySet().removeIf(entry -> !pagesToSave.contains(entry.getKey()));
        }

        //Output if empty result
        if (indexesByLemmas.isEmpty()) {
            return ResponseEntity.ok().body(new SearchResponse(true, 0, Collections.emptyList()));
        }

        //Rank calculation
        Set<RankDto> pagesRelevance = new HashSet<>();
        int pageId = indexesByLemmas.values().stream().toList().get(0).getPage().getId();
        RankDto rankPage = new RankDto();
        for (Index index : indexesByLemmas.values()) {
            if (index.getPage().getId() == pageId) {
                rankPage.setPage(index.getPage());
            } else {
                rankPage.setRelativeRelevance(rankPage.getAbsRelevance() / rankPage.getMaxLemmaRank());
                pagesRelevance.add(rankPage);
                rankPage = new RankDto();
                rankPage.setPage(index.getPage());
                pageId = index.getPage().getId();
            }
            rankPage.setAbsRelevance(rankPage.getAbsRelevance() + index.getRank());
            if (rankPage.getMaxLemmaRank() < index.getRank()) {
                rankPage.setMaxLemmaRank(index.getRank());
            }
        }

        rankPage.setRelativeRelevance(rankPage.getAbsRelevance() / rankPage.getMaxLemmaRank());
        pagesRelevance.add(rankPage);

        //Sort pages Relevance
        List<RankDto> pagesRelevanceSorted = pagesRelevance.stream().sorted(Comparator.comparingDouble(RankDto::getRelativeRelevance).reversed()).toList();

        //Converting pages relevance to searchDataResponses
        List<String> simpleLemmasFromSearch = new ArrayList<>(lemmasForSearch.stream().map(Lemma::getLemma).toList());
        List<SearchDataResponse> searchDataResponses = new ArrayList<>();
        for (RankDto rank : pagesRelevanceSorted) {
            Document doc = Jsoup.parse(rank.getPage().getContent());
            List<String> sentences = doc.body().getElementsMatchingOwnText("[\\p{IsCyrillic}]").stream().map(Element::text).toList();
            for (String sentence : sentences) {
                StringBuilder textFromElement = new StringBuilder(sentence);
                List<String> words = List.of(sentence.split("[\s:punct]"));
                int searchWords = 0;
                for (String word : words) {
                    String lemmaFromWord = lemmaService.getLemmaByWord(word.replaceAll("\\p{Punct}", ""));
                    if (simpleLemmasFromSearch.contains(lemmaFromWord)) {
                        markWord(textFromElement, word, 0);
                        searchWords += 1;
                    }
                }
                if (searchWords != 0) {
                    SitePage sitePage = siteRepository.findById(pageRepository.findById(rank.getPage().getId()).get().getSite().getId()).get();
                    searchDataResponses.add(new SearchDataResponse(
                            sitePage.getUrl(),
                            sitePage.getName(),
                            rank.getPage().getPath(),
                            doc.title(),
                            textFromElement.toString(),
                            rank.getRelativeRelevance(),
                            searchWords));
                }
            }
        }

        List<SearchDataResponse> sortedSearchDataResponse = searchDataResponses.stream().sorted(Comparator.comparingDouble(SearchDataResponse::getRelevance).reversed()).toList();
        List<SearchDataResponse> result = new ArrayList<>();
        for (
                int i = limit * offset;
                i <= limit * offset + limit; i++) {
            try {
                result.add(sortedSearchDataResponse.get(i));
            } catch (IndexOutOfBoundsException ex) {
                break;
            }
        }

        result = result.stream()
                .sorted(Comparator.comparingInt(SearchDataResponse::getWordsFound)
                        .reversed())
                .toList();

        return ResponseEntity.ok(result);
    }

    private Boolean checkIndexStatusNotIndexed(String site) {
        if (site == null || site.isBlank()) {
            List<SitePage> sites = siteRepository.findAll();
            return sites.stream().anyMatch(s -> !s.getStatus().equals(indexSuccessStatus));
        }
        return !siteRepository.getSiteByUrl(site).getStatus().equals(indexSuccessStatus);
    }

    private void markWord(StringBuilder textFromElement, String word, int startPosition) {
        int start = textFromElement.indexOf(word, startPosition);
        if (textFromElement.indexOf("<b>", start - 3) == (start - 3)) {
            markWord(textFromElement, word, start + word.length());
            return;
        }
        int end = start + word.length();
        textFromElement.insert(start, "<b>");
        if (end == -1) {
            textFromElement.insert(textFromElement.length(), "</b>");
        } else textFromElement.insert(end + 3, "</b>");
    }
}
