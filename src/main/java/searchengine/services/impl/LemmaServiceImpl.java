package searchengine.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.services.LemmaService;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class LemmaServiceImpl implements LemmaService {
    @Override
    public Map<String, Integer> getLemmasFromText(String text) throws IOException {
        return null;
    }

    @Override
    public String getLemmaByWord(String word) {
        return null;
    }
}
