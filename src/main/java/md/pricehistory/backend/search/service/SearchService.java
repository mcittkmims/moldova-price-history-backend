package md.pricehistory.backend.search.service;

import java.util.ArrayList;
import java.util.List;
import md.pricehistory.backend.scraper.ScraperClient;
import md.pricehistory.backend.search.dto.CategoryResponse;
import md.pricehistory.backend.search.dto.SortOptionResponse;
import md.pricehistory.backend.search.dto.StoreResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

@Service
public class SearchService {

    private static final List<StoreResponse> STORES = List.of(
            new StoreResponse("darwin", "Darwin", "/store-logos/darwin.png", "/store-favicons/darwin.png"),
            new StoreResponse("enter", "Enter", "/store-logos/enter.png", "/store-favicons/enter.png"),
            new StoreResponse("maximum", "Maximum", "/store-logos/maximum.png", "/store-favicons/maximum.png"),
            new StoreResponse("smart", "Smart.md", "/store-logos/smart.png", "/store-favicons/smart.png"),
            new StoreResponse("bomba", "Bomba", "/store-logos/bomba.png", "/store-favicons/bomba.png"),
            new StoreResponse("ultra", "Ultra", "/store-logos/ultra.png", "/store-favicons/ultra.png")
    );
    private static final List<SortOptionResponse> SORT_OPTIONS = List.of(
            new SortOptionResponse("default", "Store default"),
            new SortOptionResponse("price_asc", "Price ascending"),
            new SortOptionResponse("price_desc", "Price descending"),
            new SortOptionResponse("popularity", "Popularity")
    );
    private static final List<CategoryResponse> FALLBACK_CATEGORIES = List.of(
            new CategoryResponse("phones", "Phones"),
            new CategoryResponse("laptops", "Laptops"),
            new CategoryResponse("tablets", "Tablets"),
            new CategoryResponse("tvs", "TVs"),
            new CategoryResponse("headphones", "Headphones"),
            new CategoryResponse("smartwatches", "Smartwatches"),
            new CategoryResponse("refrigerators", "Refrigerators"),
            new CategoryResponse("washing_machines", "Washing machines"),
            new CategoryResponse("dishwashers", "Dishwashers"),
            new CategoryResponse("vacuums", "Vacuums")
    );

    private final ScraperClient scraperClient;

    public SearchService(ScraperClient scraperClient) {
        this.scraperClient = scraperClient;
    }

    public List<StoreResponse> stores() {
        return STORES;
    }

    public List<SortOptionResponse> sortOptions() {
        return SORT_OPTIONS;
    }

    public List<CategoryResponse> categories() {
        JsonNode items = scraperClient.categories().path("items");
        if (!items.isArray()) {
            return FALLBACK_CATEGORIES;
        }
        List<CategoryResponse> categories = new ArrayList<>();
        for (JsonNode item : items) {
            String id = text(item, "id");
            String name = text(item, "name");
            if (StringUtils.hasText(id) && StringUtils.hasText(name)) {
                categories.add(new CategoryResponse(id, name));
            }
        }
        return categories.isEmpty() ? FALLBACK_CATEGORIES : categories;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text.trim() : null;
    }
}
