package md.pricehistory.backend.auth.dto;

import java.util.List;

public record AuthUserResponse(
        String username,
        List<String> permissions
) {
}
