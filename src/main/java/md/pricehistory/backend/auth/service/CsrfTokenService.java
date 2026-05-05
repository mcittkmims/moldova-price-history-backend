package md.pricehistory.backend.auth.service;

import java.time.Instant;
import md.pricehistory.backend.auth.dto.CsrfTokenResponse;
import md.pricehistory.backend.common.ApiException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import com.nimbusds.jose.JOSEObjectType;

@Service
@Profile("!swagger")
public class CsrfTokenService {

    public static final String HEADER_NAME = "X-CSRF-Token";
    private static final String TOKEN_USE_CLAIM = "token_use";
    private static final String TOKEN_USE_CSRF = "csrf";
    private static final String AUTH_TOKEN_ID_CLAIM = "auth_jti";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public CsrfTokenService(
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder
    ) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
    }

    public CsrfTokenResponse issueToken(Authentication authentication) {
        Jwt accessToken = authenticatedAccessToken(authentication);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(accessToken.getSubject())
                .issuedAt(accessToken.getIssuedAt())
                .expiresAt(accessToken.getExpiresAt())
                .claim(TOKEN_USE_CLAIM, TOKEN_USE_CSRF)
                .claim(AUTH_TOKEN_ID_CLAIM, accessToken.getId())
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256)
                        .type(JOSEObjectType.JWT.getType())
                        .build(),
                claims
        )).getTokenValue();

        return new CsrfTokenResponse(HEADER_NAME, token, accessToken.getExpiresAt());
    }

    public void validateToken(String token, Authentication authentication) {
        Jwt accessToken = authenticatedAccessToken(authentication);
        Jwt csrfToken;
        try {
            csrfToken = jwtDecoder.decode(token);
        } catch (JwtException exception) {
            throw invalidToken();
        }

        if (!TOKEN_USE_CSRF.equals(csrfToken.getClaimAsString(TOKEN_USE_CLAIM))) {
            throw invalidToken();
        }

        if (!accessToken.getSubject().equals(csrfToken.getSubject())) {
            throw invalidToken();
        }

        String accessTokenId = accessToken.getId();
        if (accessTokenId == null || accessTokenId.isBlank()) {
            throw invalidToken();
        }

        String csrfAccessTokenId = csrfToken.getClaimAsString(AUTH_TOKEN_ID_CLAIM);
        if (!accessTokenId.equals(csrfAccessTokenId)) {
            throw invalidToken();
        }

        Instant csrfExpiry = csrfToken.getExpiresAt();
        Instant accessExpiry = accessToken.getExpiresAt();
        if (csrfExpiry == null || accessExpiry == null || csrfExpiry.isAfter(accessExpiry)) {
            throw invalidToken();
        }
    }

    private Jwt authenticatedAccessToken(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Jwt token = jwtAuthenticationToken.getToken();
            if (token.getId() != null && !token.getId().isBlank()) {
                return token;
            }
        }

        throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required");
    }

    private ApiException invalidToken() {
        return new ApiException(HttpStatus.FORBIDDEN, "CSRF token is missing, invalid, or expired");
    }
}
