package md.pricehistory.backend.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import md.pricehistory.backend.product.entity.ProductEntity;
import md.pricehistory.backend.product.model.ScrapeKind;
import md.pricehistory.backend.product.model.ScrapedProductRecord;
import md.pricehistory.backend.product.repository.ProductPriceRepository;
import md.pricehistory.backend.product.repository.ProductRepository;
import md.pricehistory.backend.product.service.ProductCatalogWriter;
import md.pricehistory.backend.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executor;

@SpringBootTest
class ProductCatalogWriterTest {

    @Autowired
    private ProductCatalogWriter productCatalogWriter;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPriceRepository productPriceRepository;

    @BeforeEach
    void clearCatalog() {
        productPriceRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    @Test
    void upsertsProductsAndKeepsPriceHistoryPerScrape() {
        Instant firstScrape = Instant.parse("2026-05-01T10:00:00Z");
        Instant secondScrape = Instant.parse("2026-05-03T11:30:00Z");

        productCatalogWriter.persistAsync(List.of(record(
                "enter:123", "iPhone 15", new BigDecimal("13999"), new BigDecimal("14999"), firstScrape, "In stock"
        )));

        productCatalogWriter.persistAsync(List.of(record(
                "enter:123", "iPhone 15 Pro", new BigDecimal("13499"), new BigDecimal("13999"), secondScrape, "Low stock"
        )));

        // Same scrape timestamp — price row is upserted, not duplicated
        productCatalogWriter.persistAsync(List.of(record(
                "enter:123", "iPhone 15 Pro", new BigDecimal("13299"), new BigDecimal("13799"), secondScrape, "Low stock"
        )));

        // Older scrape — should not overwrite the newer product state
        Instant olderScrape = Instant.parse("2026-05-02T08:00:00Z");
        productCatalogWriter.persistAsync(List.of(record(
                "enter:123", "Older iPhone title", new BigDecimal("13699"), new BigDecimal("14199"), olderScrape, "Out of stock"
        )));

        assertThat(productRepository.count()).isEqualTo(1);
        assertThat(productPriceRepository.count()).isEqualTo(3);

        ProductEntity product = productRepository.findByStore_IdAndItemId("enter", "123").orElseThrow();
        assertThat(product.getTitle()).isEqualTo("iPhone 15 Pro");
        assertThat(product.getSlug()).isEqualTo("iphone-15-enter-123");
        assertThat(product.getAvailability()).isEqualTo("Low stock");
        assertThat(product.getLastScrapedAt()).isEqualTo(secondScrape);
        assertThat(productService.findBySlug("iphone-15-enter-123")).isPresent();
        assertThat(productService.findBySlug("enter:123")).isEmpty();
    }

    @Test
    void newerSearchKeepsDetailSnapshotOnceProductIsComplete() {
        Instant detailScrape = Instant.parse("2026-05-01T10:00:00Z");
        Instant searchScrape = Instant.parse("2026-05-03T11:30:00Z");

        productCatalogWriter.persistAsync(List.of(record(
                "enter:456", "456", "iPhone 15 Pro Detailed",
                new BigDecimal("13999"), new BigDecimal("14999"), detailScrape, "In stock",
                List.of("Brand: Apple", "Description: Detailed page"), ScrapeKind.DETAIL
        )));

        productCatalogWriter.persistAsync(List.of(record(
                "enter:456", "456", "Search Card Title",
                new BigDecimal("13499"), new BigDecimal("13999"), searchScrape, "Low stock",
                List.of("Brand: Apple"), ScrapeKind.SEARCH
        )));

        ProductEntity product = productRepository.findByStore_IdAndItemId("enter", "456").orElseThrow();
        assertThat(product.isDetailComplete()).isTrue();
        assertThat(product.getTitle()).isEqualTo("iPhone 15 Pro Detailed");
        assertThat(product.getSpecs()).containsExactly("Brand: Apple", "Description: Detailed page");
        assertThat(product.getAvailability()).isEqualTo("Low stock");
        assertThat(product.getLastScrapedAt()).isEqualTo(searchScrape);
    }

    private ScrapedProductRecord record(
            String id, String title,
            BigDecimal currentPrice, BigDecimal previousPrice,
            Instant scrapedAt, String availability
    ) {
        return record(id, "123", title, currentPrice, previousPrice, scrapedAt, availability,
                List.of("Brand: Apple", "SKU: SKU-123"), ScrapeKind.SEARCH);
    }

    private ScrapedProductRecord record(
            String id, String itemId, String title,
            BigDecimal currentPrice, BigDecimal previousPrice,
            Instant scrapedAt, String availability,
            List<String> specs, ScrapeKind scrapeKind
    ) {
        return new ScrapedProductRecord(
                id, itemId, title, "Apple", "enter", "Enter", "Phones",
                currentPrice, previousPrice, "MDL", availability,
                "https://enter.online/product/" + itemId,
                "#2f5d50", "https://cdn.example.com/iphone.jpg",
                specs, scrapedAt, "{\"source_id\":\"" + itemId + "\"}", scrapeKind
        );
    }

    @TestConfiguration
    static class SyncAsyncConfig {
        @Bean(name = "catalogWriteExecutor")
        Executor catalogWriteExecutor() {
            return Runnable::run;
        }
    }
}
