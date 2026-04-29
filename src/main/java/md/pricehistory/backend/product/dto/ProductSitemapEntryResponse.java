package md.pricehistory.backend.product.dto;

import java.time.Instant;

public record ProductSitemapEntryResponse(
        String slug,
        Instant lastModified
) {
}
