package md.pricehistory.backend.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PricePointResponse(
        Instant date,
        BigDecimal price
) {
}
