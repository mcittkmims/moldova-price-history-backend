package md.pricehistory.backend.search.controller;

import java.util.List;
import md.pricehistory.backend.search.dto.CategoryResponse;
import md.pricehistory.backend.search.dto.SortOptionResponse;
import md.pricehistory.backend.search.dto.StoreResponse;
import md.pricehistory.backend.search.service.SearchService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SearchController {
    private static final String CATEGORIES_CACHE_CONTROL = "public, max-age=300, stale-while-revalidate=600";
    private static final String STATIC_METADATA_CACHE_CONTROL = "public, max-age=86400, stale-while-revalidate=604800";

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/stores")
    public ResponseEntity<List<StoreResponse>> stores() {
        return cacheableResponse(STATIC_METADATA_CACHE_CONTROL, searchService.stores());
    }

    @GetMapping("/sort-options")
    public ResponseEntity<List<SortOptionResponse>> sortOptions() {
        return cacheableResponse(STATIC_METADATA_CACHE_CONTROL, searchService.sortOptions());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> categories() {
        return cacheableResponse(CATEGORIES_CACHE_CONTROL, searchService.categories());
    }

    private <T> ResponseEntity<T> cacheableResponse(String cacheControl, T body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .body(body);
    }
}
