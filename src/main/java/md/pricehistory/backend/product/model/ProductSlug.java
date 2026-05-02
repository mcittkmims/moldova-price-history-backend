package md.pricehistory.backend.product.model;

import java.text.Normalizer;
import java.util.Locale;

public final class ProductSlug {

    private ProductSlug() {
    }

    public static String stableSlug(String title, String storeId, String itemId) {
        String titlePart = slugify(title);
        String storePart = slugify(storeId);
        String itemPart = slugify(itemId);
        String base = titlePart.isBlank() ? "product" : titlePart;
        return String.join("-", base, storePart, itemPart);
    }

    public static String slugify(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-{2,}", "-");
        return normalized;
    }
}
