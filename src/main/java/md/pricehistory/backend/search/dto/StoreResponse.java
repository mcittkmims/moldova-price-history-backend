package md.pricehistory.backend.search.dto;

public record StoreResponse(
        String id,
        String name,
        String logoPath,
        String faviconPath
) {
}
