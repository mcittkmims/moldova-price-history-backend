package md.pricehistory.backend.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "price-history")
public record PriceHistoryProperties(
        Cors cors,
        Scraper scraper,
        Tracking tracking
) {

    public record Cors(
            @NotEmpty List<String> allowedOrigins
    ) {
    }

    public record Scraper(
            @NotBlank String baseUrl,
            String apiKey,
            Duration connectTimeout,
            Duration readTimeout
    ) {
    }

    public record Tracking(
            Duration refreshInterval
    ) {
    }
}
