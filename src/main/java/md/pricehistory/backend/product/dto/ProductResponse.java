package md.pricehistory.backend.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponse(
        String id,
        String slug,
        String title,
        String brand,
        String storeId,
        String store,
        String category,
        BigDecimal currentPrice,
        BigDecimal previousPrice,
        String currency,
        String availability,
        String url,
        String imageTone,
        String imageUrl,
        List<String> specs,
        Instant lastChecked
) {
}
