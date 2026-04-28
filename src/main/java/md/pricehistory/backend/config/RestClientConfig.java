package md.pricehistory.backend.config;

import java.time.Duration;
import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class RestClientConfig {

    @Bean
    RestClient scraperRestClient(PriceHistoryProperties properties) {
        Duration connectTimeout = properties.scraper().connectTimeout();
        Duration readTimeout = properties.scraper().readTimeout();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(properties.scraper().baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
