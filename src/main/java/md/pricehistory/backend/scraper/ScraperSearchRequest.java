package md.pricehistory.backend.scraper;

public record ScraperSearchRequest(
        String query,
        int page,
        int pageSize,
        String stores,
        String category,
        String sort
) {
}
