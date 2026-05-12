package md.pricehistory.backend.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import md.pricehistory.backend.product.dto.ProductResponse;
import md.pricehistory.backend.product.dto.SearchQuery;
import md.pricehistory.backend.product.model.ScrapeKind;
import md.pricehistory.backend.product.model.ScrapedProductRecord;
import md.pricehistory.backend.product.service.ProductCatalogWriter;
import md.pricehistory.backend.product.service.ProductService;
import md.pricehistory.backend.scraper.ScraperClient;
import md.pricehistory.backend.scraper.ScraperProductParser;
import md.pricehistory.backend.scraper.ScraperSearchRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class SearchServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void mapsScraperSearchToFrontendProducts() throws Exception {
        ProductCatalogWriter mockWriter = mock(ProductCatalogWriter.class);
        CapturingScraperClient scraperClient = new CapturingScraperClient(objectMapper.readTree("""
                {
                  "results": {
                    "enter": {
                      "products": [
                        {
                          "source_id": "123",
                          "sku": "SKU-123",
                          "name": "Apple iPhone 15 128GB",
                          "brand": "Apple",
                          "category": "Smartphone",
                          "url": "https://enter.online/product/123",
                          "price": { "current": 13999, "old": 14999 },
                          "availability": "in_stock",
                          "short_description": "128GB, Black",
                          "last_scraped_at": "2026-05-02T10:15:30Z"
                        }
                      ]
                    }
                  }
                }
                """));
        ProductService service = new ProductService(scraperClient, new ScraperProductParser(), mockWriter);

        List<ProductResponse> products = service.search(new SearchQuery(
                "iphone", 1, 20, "Enter", "Phones", "price-low"
        ));

        assertThat(products).hasSize(1);
        ProductResponse product = products.get(0);
        assertThat(product.id()).isEqualTo("enter:123");
        assertThat(product.slug()).isEqualTo("apple-iphone-15-128gb-enter-123");
        assertThat(product.title()).isEqualTo("Apple iPhone 15 128GB");
        assertThat(product.store()).isEqualTo("Enter");
        assertThat(product.category()).isEqualTo("Phones");
        assertThat(product.currentPrice()).isEqualByComparingTo("13999");
        assertThat(product.previousPrice()).isEqualByComparingTo("14999");
        assertThat(product.availability()).isEqualTo("In stock");
        assertThat(product.lastChecked()).isEqualTo(Instant.parse("2026-05-02T10:15:30Z"));

        assertThat(scraperClient.lastRequest.stores()).isEqualTo("enter");
        assertThat(scraperClient.lastRequest.category()).isEqualTo("phones");
        assertThat(scraperClient.lastRequest.sort()).isEqualTo("price_asc");

        ArgumentCaptor<List<ScrapedProductRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockWriter).persistAsync(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).lastScrapedAt()).isEqualTo(Instant.parse("2026-05-02T10:15:30Z"));
        assertThat(captor.getValue().get(0).scrapeKind()).isEqualTo(ScrapeKind.SEARCH);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mapsScraperByUrlToFrontendProducts() throws Exception {
        ProductCatalogWriter mockWriter = mock(ProductCatalogWriter.class);
        CapturingScraperClient scraperClient = new CapturingScraperClient(objectMapper.readTree("{}"));
        scraperClient.byUrlResponse = objectMapper.readTree("""
                {
                  "store": "enter",
                  "source_id": "123",
                  "sku": "SKU-123",
                  "name": "Apple iPhone 15 128GB",
                  "brand": "Apple",
                  "category": "Smartphone",
                  "url": "https://enter.online/product/123",
                  "price": { "current": 13999, "old": 14999 },
                  "availability": "in_stock",
                  "short_description": "128GB, Black",
                  "scrape_source": "search",
                  "last_scraped_at": "2026-05-02T10:15:30Z"
                }
                """);
        ProductService service = new ProductService(scraperClient, new ScraperProductParser(), mockWriter);

        ProductResponse product = service.productByUrl("https://enter.online/product/123");

        assertThat(product.id()).isEqualTo("enter:123");
        assertThat(product.title()).isEqualTo("Apple iPhone 15 128GB");
        assertThat(scraperClient.lastUrl).isEqualTo("https://enter.online/product/123");

        ArgumentCaptor<List<ScrapedProductRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockWriter).persistAsync(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).scrapeKind()).isEqualTo(ScrapeKind.SEARCH);
    }

    private static final class CapturingScraperClient extends ScraperClient {
        private final JsonNode response;
        private ScraperSearchRequest lastRequest;
        private JsonNode byUrlResponse;
        private String lastUrl;

        private CapturingScraperClient(JsonNode response) {
            super(null, null);
            this.response = response;
        }

        @Override
        public JsonNode searchProducts(ScraperSearchRequest request) {
            this.lastRequest = request;
            return response;
        }

        @Override
        public JsonNode productByUrl(String url) {
            this.lastUrl = url;
            return byUrlResponse;
        }
    }
}
