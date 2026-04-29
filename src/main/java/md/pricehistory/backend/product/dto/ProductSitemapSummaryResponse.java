package md.pricehistory.backend.product.dto;

public record ProductSitemapSummaryResponse(
        long totalItems,
        int pageSize,
        int totalPages
) {
}
