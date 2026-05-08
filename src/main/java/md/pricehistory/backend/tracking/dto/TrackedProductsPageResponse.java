package md.pricehistory.backend.tracking.dto;

import java.util.List;
import md.pricehistory.backend.product.dto.ProductResponse;

public record TrackedProductsPageResponse(
        int page,
        int pageSize,
        long totalItems,
        int totalPages,
        List<ProductResponse> items
) {
}
