package md.pricehistory.backend.product.dto;

import java.util.List;

public record ProductSitemapPageResponse(
        int page,
        int pageSize,
        long totalItems,
        int totalPages,
        List<ProductSitemapEntryResponse> items
) {
}
