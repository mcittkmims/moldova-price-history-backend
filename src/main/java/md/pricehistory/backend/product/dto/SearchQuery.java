package md.pricehistory.backend.product.dto;

public record SearchQuery(
        String query,
        int page,
        int pageSize,
        String store,
        String category,
        String sort
) {
}
