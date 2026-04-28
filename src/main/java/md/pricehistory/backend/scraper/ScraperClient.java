package md.pricehistory.backend.scraper;

import java.util.function.Consumer;
import md.pricehistory.backend.common.ApiException;
import md.pricehistory.backend.config.PriceHistoryProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.JsonNode;

@Component
public class ScraperClient {

    private final RestClient restClient;
    private final PriceHistoryProperties properties;

    public ScraperClient(RestClient scraperRestClient, PriceHistoryProperties properties) {
        this.restClient = scraperRestClient;
        this.properties = properties;
    }

    public JsonNode searchProducts(ScraperSearchRequest request) {
        try {
            return authorizedGet("/products/search", request);
        } catch (ApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Scraper search request failed: " + exception.getMessage());
        }
    }

    public JsonNode categories() {
        try {
            return authorizedGet("/products/categories", (Consumer<UriBuilder>) null);
        } catch (ApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Scraper categories request failed: " + exception.getMessage());
        }
    }

    public JsonNode productByUrl(String url) {
        try {
            return authorizedGet("/products/by-url", (uriBuilder) -> uriBuilder.queryParam("url", url));
        } catch (ApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Scraper by-url request failed: " + exception.getMessage());
        }
    }

    private JsonNode authorizedGet(String path, ScraperSearchRequest request) {
        return authorizedGet(path, (uriBuilder) -> {
            if (request == null) {
                return;
            }
            uriBuilder.queryParam("q", request.query())
                    .queryParam("page", request.page())
                    .queryParam("page_size", request.pageSize());
            if (StringUtils.hasText(request.stores())) {
                uriBuilder.queryParam("stores", request.stores());
            }
            if (StringUtils.hasText(request.category())) {
                uriBuilder.queryParam("category", request.category());
            }
            if (StringUtils.hasText(request.sort())) {
                uriBuilder.queryParam("sort", request.sort());
            }
        });
    }

    private JsonNode authorizedGet(String path, Consumer<UriBuilder> queryCustomizer) {
        String apiKey = properties.scraper().apiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SCRAPER_API_KEY is not configured");
        }

        return restClient
                .get()
                .uri(uriBuilder -> {
                    uriBuilder.path(path);
                    if (queryCustomizer != null) {
                        queryCustomizer.accept(uriBuilder);
                    }
                    return uriBuilder.build();
                })
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .retrieve()
                .onStatus(status -> status.value() == 401 || status.value() == 403, (req, res) -> {
                    throw new ApiException(HttpStatus.BAD_GATEWAY, "Scraper API key was rejected");
                })
                .onStatus(status -> status.value() >= 400, (req, res) -> {
                    throw new ApiException(HttpStatus.BAD_GATEWAY, "Scraper request failed with status " + res.getStatusCode().value());
                })
                .body(JsonNode.class);
    }
}
