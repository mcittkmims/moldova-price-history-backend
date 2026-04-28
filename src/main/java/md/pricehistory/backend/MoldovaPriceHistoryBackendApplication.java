package md.pricehistory.backend;

import md.pricehistory.backend.config.PriceHistoryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(PriceHistoryProperties.class)
public class MoldovaPriceHistoryBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoldovaPriceHistoryBackendApplication.class, args);
    }
}
