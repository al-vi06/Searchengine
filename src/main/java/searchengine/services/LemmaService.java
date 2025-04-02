package searchengine.services;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface LemmaService {
    Map<String, Integer> collectLemmas(String text) throws IOException;
    String cleanHtmlTags(String htmlCode);
    String getLemmaByWord(String word);
}
