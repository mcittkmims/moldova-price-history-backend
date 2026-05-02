package md.pricehistory.backend.product.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import md.pricehistory.backend.product.dto.ProductHistoryResponse;
import md.pricehistory.backend.product.dto.ProductResponse;
import md.pricehistory.backend.product.dto.ProductSitemapPageResponse;
import md.pricehistory.backend.product.dto.ProductSitemapSummaryResponse;
import md.pricehistory.backend.product.dto.SearchQuery;
import md.pricehistory.backend.product.service.ProductService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private static final String PRODUCT_CACHE_CONTROL = "public, max-age=30, stale-while-revalidate=60";
    private static final String SITEMAP_CACHE_CONTROL = "public, max-age=3600, stale-while-revalidate=86400";
    private static final int DEFAULT_SITEMAP_PAGE_SIZE = 5000;
    private static final int MAX_SITEMAP_PAGE_SIZE = 10000;

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> search(
            @RequestParam("q") @NotBlank String query,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "page_size", defaultValue = "24") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String store,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort
    ) {
        SearchQuery searchQuery = new SearchQuery(query, page, pageSize, store, category, sort);
        return cacheableResponse(PRODUCT_CACHE_CONTROL, productService.search(searchQuery));
    }

    @GetMapping("/lookup")
    public ResponseEntity<ProductResponse> productByUrl(
            @RequestParam("url") @NotBlank String url
    ) {
        return cacheableResponse(PRODUCT_CACHE_CONTROL, productService.productByUrl(url));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProductResponse> product(@PathVariable String slug) {
        return productService.product(slug)
                .map((product) -> cacheableResponse(PRODUCT_CACHE_CONTROL, product))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{slug}/history")
    public ResponseEntity<ProductHistoryResponse> productHistory(@PathVariable String slug) {
        return cacheableResponse(PRODUCT_CACHE_CONTROL, productService.productHistory(slug));
    }

    @GetMapping("/sitemap")
    public ResponseEntity<ProductSitemapSummaryResponse> sitemapSummary(
            @RequestParam(name = "page_size", defaultValue = "5000") @Min(1) @Max(MAX_SITEMAP_PAGE_SIZE) int pageSize
    ) {
        return cacheableResponse(SITEMAP_CACHE_CONTROL, productService.sitemapSummary(pageSize));
    }

    @GetMapping("/sitemap/page")
    public ResponseEntity<ProductSitemapPageResponse> sitemapPage(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "page_size", defaultValue = "5000") @Min(1) @Max(MAX_SITEMAP_PAGE_SIZE) int pageSize
    ) {
        int resolvedPageSize = pageSize > 0 ? pageSize : DEFAULT_SITEMAP_PAGE_SIZE;
        return cacheableResponse(SITEMAP_CACHE_CONTROL, productService.sitemapPage(page, resolvedPageSize));
    }

    private <T> ResponseEntity<T> cacheableResponse(String cacheControl, T body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .body(body);
    }
}
