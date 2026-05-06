package md.pricehistory.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
        @Schema(description = "Signed JWT — paste this into the Authorize dialog as a Bearer token")
        String token
) {}
