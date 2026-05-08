package md.pricehistory.backend.tracking.dto;

import md.pricehistory.backend.product.dto.ProductResponse;

public record TrackedProductResponse(
        ProductResponse product,
        boolean tracked
) {
}
