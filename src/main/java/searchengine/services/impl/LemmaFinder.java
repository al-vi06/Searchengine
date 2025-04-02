package searchengine.services.impl;

import lombok.extern.slf4j.Slf4j;


import java.io.IOException;
import java.util.*;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.services.LemmaService;


@Service
@Slf4j
public class LemmaFinder implements LemmaService {
    private final LuceneMorphology luceneMorphology;
    private static final String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};


    public LemmaFinder() throws IOException {
        LuceneMorphology morphology= new RussianLuceneMorphology();
        this.luceneMorphology = morphology;
    }


    /**
     * Метод разделяет текст на слова, находит все леммы и считает их количество.
     * @param text текст из которого будут выбираться леммы
     * @return ключ является леммой, а значение количеством найденных лемм
     */
    public  Map<String, Integer> collectLemmas(String text) {
        String[] words = arrayContainsRussianWords(text);
        HashMap<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);
            lemmas.put(normalWord, lemmas.containsKey(normalWord) ? lemmas.get(normalWord) + 1 : 1);
        }

        return lemmas;
    }


    /**
     * @param text текст из которого собираем все леммы
     * @return набор уникальных лемм найденных в тексте
     */
    public Set<String> getLemmaSet(String text) {
        String[] textArray = arrayContainsRussianWords(text);
        Set<String> lemmaSet = new HashSet<>();
        for (String word : textArray) {
            if (!word.isEmpty() && isCorrectWordForm(word)) {
                List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
                if (anyWordBaseBelongToParticle(wordBaseForms)) {
                    continue;
                }
                lemmaSet.addAll(luceneMorphology.getNormalForms(word));
            }
        }
        return lemmaSet;
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private boolean isCorrectWordForm(String word) {
        List<String> wordInfo = luceneMorphology.getMorphInfo(word);
        for (String morphInfo : wordInfo) {
            if (morphInfo.matches(WORD_TYPE_REGEX)) {
                return false;
            }
        }
        return true;
    }

    //++ реализовал метод очистки тегов на странице, оставляем только текст
    public String cleanHtmlTags(String htmlCode) {
        if (htmlCode == null || htmlCode.isEmpty()) {
            return "";
        }

        String text = Jsoup.parse(htmlCode).text();
        return text;

//        //Удаляем скрипты, стили и теги <noscript>
//        document.select("script, style, noscript").remove();
//
//        //Удаляем комментарии
//        document.outputSettings().prettyPrint(false); //Чтобы избежать перезаписи текста в комментариях
//        String withoutComments = document.html().replaceAll("<!--.*?-->", "");
//
//        //Убираем HTML-теги, оставляем только текст
//        Document cleanedDocument = Jsoup.parse(withoutComments);
//        String textOnly = cleanedDocument.text();
//
//        //только русские и английские символы, а также пробелы
//        String cleanedText = textOnly.replaceAll("[^а-яА-Яa-zA-Z\\s]", "");
//
//        //Убираем лишние пробелы
//        return cleanedText.trim().replaceAll("\\s{2,}", " ");
    }

    @Override
    public String getLemmaByWord(String word) {
        String preparedWord = word.toLowerCase();
        if (hasParticleProperty(preparedWord)) return "";
        try {
            List<String> normalWordForms = luceneMorphology.getNormalForms(preparedWord);
            String wordInfo = luceneMorphology.getMorphInfo(preparedWord).toString();
            if (hasParticleProperty(wordInfo)) return "";
            return normalWordForms.get(0);
        } catch (WrongCharaterException ex) {
            log.debug(ex.getMessage());
        }
        return "";
    }

}