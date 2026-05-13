package md.pricehistory.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record TokenRequest(
        @Schema(description = "JWT subject (acts as the username)", example = "demo")
        String subject,

        @Schema(
                description = "Permissions to embed in the token. Omit for all default permissions.",
                example = "[\"account:read_self\", \"tracked:read_own\"]"
        )
        List<String> permissions
) {}
