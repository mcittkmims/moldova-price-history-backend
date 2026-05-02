package md.pricehistory.backend.product.service;

import java.util.List;
import java.util.Optional;
import md.pricehistory.backend.common.ApiException;
import md.pricehistory.backend.product.dto.PricePointResponse;
import md.pricehistory.backend.product.dto.ProductHistoryResponse;
import md.pricehistory.backend.product.dto.ProductResponse;
import md.pricehistory.backend.product.dto.ProductSitemapEntryResponse;
import md.pricehistory.backend.product.dto.ProductSitemapPageResponse;
import md.pricehistory.backend.product.dto.ProductSitemapSummaryResponse;
import md.pricehistory.backend.product.dto.SearchQuery;
import md.pricehistory.backend.product.entity.ProductEntity;
import md.pricehistory.backend.product.entity.ProductPriceEntity;
import md.pricehistory.backend.product.model.ProductSlug;
import md.pricehistory.backend.product.model.ScrapedProductRecord;
import md.pricehistory.backend.product.repository.ProductPriceRepository;
import md.pricehistory.backend.product.repository.ProductRepository;
import md.pricehistory.backend.scraper.ScraperClient;
import md.pricehistory.backend.scraper.ScraperProductParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ScraperClient scraperClient;
    private final ScraperProductParser scraperParser;
    private final ProductCatalogWriter productCatalogWriter;
    private final ProductRepository productRepository;
    private final ProductPriceRepository productPriceRepository;

    @Autowired
    public ProductService(
            ScraperClient scraperClient,
            ScraperProductParser scraperParser,
            ProductCatalogWriter productCatalogWriter,
            ProductRepository productRepository,
            ProductPriceRepository productPriceRepository
    ) {
        this.scraperClient = scraperClient;
        this.scraperParser = scraperParser;
        this.productCatalogWriter = productCatalogWriter;
        this.productRepository = productRepository;
        this.productPriceRepository = productPriceRepository;
    }

    public ProductService(ScraperClient scraperClient, ScraperProductParser scraperParser, ProductCatalogWriter productCatalogWriter) {
        this(scraperClient, scraperParser, productCatalogWriter, null, null);
    }

    public List<ProductResponse> search(SearchQuery query) {
        return storeAndRespond(
                scraperParser.parseSearchResponse(
                        scraperClient.searchProducts(scraperParser.buildSearchRequest(query))
                )
        );
    }

    public ProductResponse productByUrl(String url) {
        ScrapedProductRecord record = scraperParser.parseProductByUrl(scraperClient.productByUrl(url));
        if (record == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Scraper by-url response did not include a valid product");
        }
        return storeAndRespond(List.of(record)).stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_GATEWAY, "Scraper by-url response was empty"));
    }

    public Optional<ProductResponse> product(String slug) {
        return findBySlug(slug);
    }

    public Optional<ProductResponse> findBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .map(entity -> {
                    java.math.BigDecimal currentPrice = productPriceRepository
                            .findTopByProduct_IdAndCurrentPriceIsNotNullOrderByScrapedAtDesc(entity.getId())
                            .map(ProductPriceEntity::getCurrentPrice)
                            .orElse(null);
                    return fromEntity(entity, currentPrice);
                });
    }

    public ProductHistoryResponse productHistory(String slug) {
        ProductEntity entity = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        List<PricePointResponse> history = productPriceRepository
                .findByProduct_IdInOrderByScrapedAtAsc(List.of(entity.getId()))
                .stream()
                .filter(p -> p.getCurrentPrice() != null)
                .map(p -> new PricePointResponse(p.getScrapedAt(), p.getCurrentPrice()))
                .toList();
        return new ProductHistoryResponse(history);
    }

    public Optional<ProductEntity> findEntityBySlug(String slug) {
        return productRepository.findBySlug(slug);
    }

    public ProductSitemapSummaryResponse sitemapSummary(int pageSize) {
        long totalItems = productRepository.count();
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);
        return new ProductSitemapSummaryResponse(totalItems, pageSize, totalPages);
    }

    public ProductSitemapPageResponse sitemapPage(int page, int pageSize) {
        Page<ProductEntity> products = productRepository.findAllForSitemap(PageRequest.of(page - 1, pageSize));
        List<ProductSitemapEntryResponse> items = products.getContent().stream()
                .map(p -> new ProductSitemapEntryResponse(p.getSlug(), p.getLastScrapedAt()))
                .toList();
        return new ProductSitemapPageResponse(page, pageSize, products.getTotalElements(), products.getTotalPages(), items);
    }

    private List<ProductResponse> storeAndRespond(List<ScrapedProductRecord> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        List<ProductResponse> responses = records.stream().map(this::fromRecord).toList();
        productCatalogWriter.persistAsync(List.copyOf(records));
        return responses;
    }

    private ProductResponse fromRecord(ScrapedProductRecord record) {
        String slug = ProductSlug.stableSlug(record.title(), record.storeId(), record.itemId());
        return new ProductResponse(
                record.id(),
                slug,
                record.title(),
                record.brand(),
                record.storeId(),
                record.store(),
                record.category(),
                record.currentPrice(),
                record.previousPrice(),
                record.currency(),
                record.availability(),
                record.url(),
                record.imageTone(),
                record.imageUrl(),
                record.specs(),
                record.lastScrapedAt()
        );
    }

    private ProductResponse fromEntity(ProductEntity entity, java.math.BigDecimal currentPrice) {
        return new ProductResponse(
                entity.getStoreId() + ":" + entity.getItemId(),
                entity.getSlug(),
                entity.getTitle(),
                entity.getBrand(),
                entity.getStoreId(),
                entity.getStoreName(),
                entity.getCategory(),
                currentPrice,
                null,
                entity.getCurrency(),
                entity.getAvailability(),
                entity.getUrl(),
                entity.getImageTone(),
                entity.getImageUrl(),
                entity.getSpecs(),
                entity.getLastScrapedAt()
        );
    }
}
