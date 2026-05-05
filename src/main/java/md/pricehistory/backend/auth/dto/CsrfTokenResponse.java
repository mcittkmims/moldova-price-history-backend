package md.pricehistory.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(hidden = true)
public record CsrfTokenResponse(
        String headerName,
        String token,
        Instant expiresAt
) {
}
