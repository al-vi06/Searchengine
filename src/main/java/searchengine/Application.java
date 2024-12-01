package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import searchengine.model.indexing.LemmaFinder;

import java.io.IOException;
import java.util.List;
import java.util.Set;


@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

//        try {
//            LemmaFinder lemmaFinder = LemmaFinder.getInstance();
//
//            String cleanHtml = lemmaFinder.cleanHtmlTags(str);
//
//            String lemmas = lemmaFinder.collectLemmas(cleanHtml).toString();
//            System.out.println(lemmas);
//
//            Set<String> lemmasSet = lemmaFinder.getLemmaSet(cleanHtml);
//            System.out.println(lemmasSet);
//
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

    }

}
