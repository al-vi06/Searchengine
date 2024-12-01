package searchengine.model.multithreading;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Data
public class HttpConfig {
    @Value("${http.user-agent}")
    private String userAgent;

    @Value("${http.referrer}")
    private String referrer;

    @Value("${http.timeout}")
    private int timeout;

    @PostConstruct
    public void init() {
        System.out.println("User-Agent: " + userAgent);
        System.out.println("Referrer: " + referrer);
        System.out.println("Timeout: " + timeout);
    }
}

