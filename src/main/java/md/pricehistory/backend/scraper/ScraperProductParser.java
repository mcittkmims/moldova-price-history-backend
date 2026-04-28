package md.pricehistory.backend.scraper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import md.pricehistory.backend.product.dto.SearchQuery;
import md.pricehistory.backend.product.model.ScrapeKind;
import md.pricehistory.backend.product.model.ScrapedProductRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

@Component
public class ScraperProductParser {

    private static final Map<String, String> STORE_KEYS = Map.ofEntries(
            Map.entry("darwin", "darwin"),
            Map.entry("enter", "enter"),
            Map.entry("maximum", "maximum"),
            Map.entry("smart", "smart"),
            Map.entry("smart.md", "smart"),
            Map.entry("bomba", "bomba"),
            Map.entry("ultra", "ultra")
    );
    private static final Map<String, String> STORE_NAMES = Map.of(
            "darwin", "Darwin",
            "enter", "Enter",
            "maximum", "Maximum",
            "smart", "Smart.md",
            "bomba", "Bomba",
            "ultra", "Ultra"
    );

    private final Clock clock;

    @Autowired
    public ScraperProductParser() {
        this(Clock.systemUTC());
    }

    ScraperProductParser(Clock clock) {
        this.clock = clock;
    }

    public ScraperSearchRequest buildSearchRequest(SearchQuery query) {
        return new ScraperSearchRequest(
                query.query().trim(),
                query.page(),
                query.pageSize(),
                scraperStore(query.store()),
                scraperCategory(query.category()),
                scraperSort(query.sort())
        );
    }

    public List<ScrapedProductRecord> parseSearchResponse(JsonNode payload) {
        JsonNode results = payload.path("results");
        if (!results.isObject()) {
            return Collections.emptyList();
        }
        List<ScrapedProductRecord> records = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = results.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> storeResult = fields.next();
            JsonNode products = storeResult.getValue().path("products");
            if (!products.isArray()) {
                continue;
            }
            for (JsonNode product : products) {
                ScrapedProductRecord record = parseProduct(storeResult.getKey(), product, ScrapeKind.SEARCH);
                if (record != null) {
                    records.add(record);
                }
            }
        }
        return records;
    }

    public ScrapedProductRecord parseProductByUrl(JsonNode payload) {
        return parseProduct(null, payload, ScrapeKind.DETAIL);
    }

    private ScrapedProductRecord parseProduct(String fallbackStoreKey, JsonNode product, ScrapeKind defaultKind) {
        String sourceId = text(product, "source_id");
        if (!StringUtils.hasText(sourceId)) {
            return null;
        }
        String url = text(product, "url");
        String storeKey = resolveStoreKey(text(product, "store"), fallbackStoreKey, url);
        if (!StringUtils.hasText(storeKey)) {
            return null;
        }
        String id = storeKey + ":" + sourceId;
        String title = firstText(product, "name", "title");
        String storeName = STORE_NAMES.getOrDefault(storeKey, displayName(storeKey));
        BigDecimal current = decimal(product.path("price").path("current"));
        BigDecimal previous = decimal(product.path("price").path("old"));
        if (current != null && current.compareTo(BigDecimal.ZERO) <= 0) {
            current = null;
        }
        if (previous != null && previous.compareTo(BigDecimal.ZERO) <= 0) {
            previous = null;
        }
        if (current != null && previous == null) {
            previous = current;
        }

        return new ScrapedProductRecord(
                id,
                sourceId,
                title != null ? title : "Unknown product",
                text(product, "brand"),
                storeKey,
                storeName,
                normalizeCategory(text(product, "category"), title),
                current,
                previous,
                "MDL",
                normalizeAvailability(text(product, "availability")),
                url != null ? url : "#",
                imageTone(id),
                imageUrl(product),
                specs(product),
                scrapedAt(product),
                product.toString(),
                scrapeKind(firstText(product, "scrape_source", "scrapeSource"), defaultKind)
        );
    }

    private String scraperStore(String store) {
        if (!StringUtils.hasText(store) || "all".equals(normalize(store))) {
            return null;
        }
        return STORE_KEYS.get(normalize(store));
    }

    private String scraperCategory(String category) {
        return switch (normalize(category)) {
            case "phones", "phone" -> "phones";
            case "laptops", "laptop" -> "laptops";
            case "audio", "headphones" -> "headphones";
            case "appliances", "home" -> "refrigerators";
            case "", "all" -> null;
            default -> normalize(category);
        };
    }

    private String scraperSort(String sort) {
        return switch (normalize(sort)) {
            case "price_asc", "price-asc", "price_low", "price-low", "lowest", "asc" -> "price_asc";
            case "price_desc", "price-desc", "price_high", "price-high", "highest", "desc" -> "price_desc";
            case "popularity", "popular" -> "popularity";
            default -> null;
        };
    }

    private String resolveStoreKey(String rawStore, String fallbackStoreKey, String url) {
        String fromField = STORE_KEYS.get(normalize(rawStore));
        if (fromField != null) {
            return fromField;
        }
        if (StringUtils.hasText(fallbackStoreKey)) {
            return fallbackStoreKey;
        }
        if (!StringUtils.hasText(url)) {
            return null;
        }
        try {
            String host = new java.net.URL(url).getHost().toLowerCase(Locale.ROOT);
            return STORE_KEYS.get(normalize(host));
        } catch (RuntimeException | java.net.MalformedURLException ignored) {
            return null;
        }
    }

    private String normalizeCategory(String nativeCategory, String title) {
        String text = normalize((nativeCategory != null ? nativeCategory : "") + " " + (title != null ? title : ""));
        if (containsAny(text, "phone", "smartphone", "telefon", "iphone", "samsung galaxy")) {
            return "Phones";
        }
        if (containsAny(text, "laptop", "notebook", "macbook")) {
            return "Laptops";
        }
        if (containsAny(text, "tablet", "tablete", "ipad")) {
            return "Tablets";
        }
        if (containsAny(text, "tv", "televizor", "televizoare", "television", "qled", "oled")) {
            return "TVs";
        }
        if (containsAny(text, "headphone", "headset", "earbuds", "airpods", "casti", "căști")) {
            return "Headphones";
        }
        if (containsAny(text, "smartwatch", "smart_watch", "watch", "ceas", "ceasuri")) {
            return "Smartwatches";
        }
        if (containsAny(text, "refrigerator", "frigider", "frigidere", "fridge")) {
            return "Refrigerators";
        }
        if (containsAny(text, "washing_machine", "washing", "washer", "masina_de_spalat", "masini_de_spalat")) {
            return "Washing machines";
        }
        if (containsAny(text, "dishwasher", "dishwashers", "masina_de_spalat_vase", "masini_de_spalat_vase")) {
            return "Dishwashers";
        }
        if (containsAny(text, "vacuum", "vacuums", "aspirator", "aspiratoare")) {
            return "Vacuums";
        }
        return "Home";
    }

    private String normalizeAvailability(String availability) {
        String normalized = normalize(availability);
        if (containsAny(normalized, "preorder", "pre_order", "precomanda")) {
            return "Preorder";
        }
        if (containsAny(normalized, "out", "unavailable", "stoc_epuizat", "indisponibil")) {
            return "Out of stock";
        }
        if (containsAny(normalized, "low", "limited")) {
            return "Low stock";
        }
        if (containsAny(normalized, "unknown")) {
            return "Unknown";
        }
        return "In stock";
    }

    private ScrapeKind scrapeKind(String value, ScrapeKind defaultKind) {
        return switch (normalize(value)) {
            case "detail" -> ScrapeKind.DETAIL;
            case "search" -> ScrapeKind.SEARCH;
            default -> defaultKind;
        };
    }

    // darwin.md and enter.online translate the full path slug, so /ru/ cannot be stripped reliably.
    private String imageUrl(JsonNode product) {
        String image = firstText(product, "image", "image_url", "thumbnail");
        if (StringUtils.hasText(image)) {
            return image;
        }
        JsonNode images = product.path("images");
        if (images.isArray() && !images.isEmpty()) {
            String firstImage = images.get(0).asText(null);
            if (StringUtils.hasText(firstImage)) {
                return firstImage;
            }
        }
        return null;
    }

    private List<String> specs(JsonNode product) {
        Map<String, String> values = new LinkedHashMap<>();
        putIfPresent(values, "Brand", text(product, "brand"));
        putIfPresent(values, "SKU", text(product, "sku"));
        putIfPresent(values, "Item ID", text(product, "source_id"));
        putIfPresent(values, "Description", text(product, "short_description"));
        return values.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .limit(4)
                .toList();
    }

    private void putIfPresent(Map<String, String> values, String key, String value) {
        if (StringUtils.hasText(value)) {
            values.put(key, value);
        }
    }

    private Instant scrapedAt(JsonNode node) {
        String value = firstText(node, "last_scraped_at", "lastScrapedAt");
        if (StringUtils.hasText(value)) {
            try {
                return Instant.parse(value);
            } catch (RuntimeException ignored) {
                // Fall through to the known fallback so a malformed scraper timestamp does not break search.
            }
        }
        return clock.instant();
    }

    private String imageTone(String id) {
        String[] tones = {"#2f5d50", "#9a6b3f", "#4f6785", "#7a4f65", "#5f6f3f", "#6f5f3f"};
        return tones[Math.abs(id.hashCode()) % tones.length];
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private BigDecimal decimal(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        String text = node.asText();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private String displayName(String value) {
        if (!StringUtils.hasText(value)) {
            return "Unknown";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }
}
