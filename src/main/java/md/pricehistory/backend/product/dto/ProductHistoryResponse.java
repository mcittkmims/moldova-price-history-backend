package md.pricehistory.backend.product.dto;

import java.util.List;

public record ProductHistoryResponse(List<PricePointResponse> history) {
}
