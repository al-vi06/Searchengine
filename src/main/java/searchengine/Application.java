package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.List;


@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

//        LuceneMorphology luceneMorph = null;
//        try {
//            luceneMorph = new RussianLuceneMorphology();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        //List<String> wordBaseForms = luceneMorph.getNormalForms("леса");
//        List<String> wordBaseForms = luceneMorph.getMorphInfo("или");
//        wordBaseForms.forEach(System.out::println);

    }

}
