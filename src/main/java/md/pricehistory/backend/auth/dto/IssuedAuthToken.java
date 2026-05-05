package md.pricehistory.backend.auth.dto;

import java.time.Instant;

public record IssuedAuthToken(
        String accessToken,
        String tokenId,
        Instant expiresAt,
        AuthUserResponse user
) {
}
