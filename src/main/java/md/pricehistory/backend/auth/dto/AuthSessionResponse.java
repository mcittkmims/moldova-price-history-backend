package md.pricehistory.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.jwt.Jwt;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthSessionResponse(
        Instant expiresAt,
        AuthUserResponse user,
        @Nullable String accessToken
) {
    public static AuthSessionResponse fromIssuedToken(IssuedAuthToken token, boolean includeToken) {
        return new AuthSessionResponse(token.expiresAt(), token.user(), includeToken ? token.accessToken() : null);
    }

    public static AuthSessionResponse fromJwt(Jwt jwt) {
        return new AuthSessionResponse(
                jwt.getExpiresAt(),
                new AuthUserResponse(
                        jwt.getClaimAsString("username"),
                        jwt.getClaimAsStringList("permissions")
                ),
                null
        );
    }
}
