package md.pricehistory.backend.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
class CorsConfig {

    @Bean
    CorsFilter corsFilter(PriceHistoryProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> configuredOrigins = properties.cors().allowedOrigins().stream()
                .flatMap((value) -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter((value) -> !value.isEmpty())
                .toList();

        configuration.setAllowedOrigins(configuredOrigins.stream()
                .filter((value) -> !value.contains("*"))
                .toList());
        configuration.setAllowedOriginPatterns(configuredOrigins.stream()
                .filter((value) -> value.contains("*"))
                .toList());
        configuration.addAllowedMethod(CorsConfiguration.ALL);
        configuration.addAllowedHeader(CorsConfiguration.ALL);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsFilter(source);
    }
}
