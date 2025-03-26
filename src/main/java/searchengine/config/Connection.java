package searchengine.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

//@Component
//@Data
//@ConfigurationProperties(prefix = "connection-settings")
//public class Connection {
//    @Value("${connection-settings.user-agent}")
//    private String userAgent;
//
//    @Value("${connection-settings.referrer}")
//    private String referrer;
//
//    @Value("${connection-settings.timeout}")
//    private int timeout;
//
//    @PostConstruct
//    public void init() {
//        System.out.println("User-Agent: " + userAgent);
//        System.out.println("Referrer: " + referrer);
//        System.out.println("Timeout: " + timeout);
//    }
//}

@ConstructorBinding
@ConfigurationProperties(prefix = "connection-settings")
public record Connection(String userAgent, String referrer, int timeout) {
}

