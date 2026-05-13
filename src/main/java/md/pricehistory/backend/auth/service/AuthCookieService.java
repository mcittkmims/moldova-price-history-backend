package md.pricehistory.backend.auth.service;

import java.time.Duration;
import md.pricehistory.backend.auth.dto.IssuedAuthToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@Profile("!swagger")
public class AuthCookieService {

    private final String authCookieName;
    private final Duration tokenExpiration;
    private final boolean secureCookie;
    private final String sameSite;

    public AuthCookieService(
            @Value("${price-history.auth-cookie-name:pricehistory_access}") String authCookieName,
            @Value("${price-history.auth-expiration:15m}") Duration tokenExpiration,
            @Value("${price-history.auth-cookie-secure:false}") boolean secureCookie,
            @Value("${price-history.auth-cookie-same-site:Lax}") String sameSite
    ) {
        this.authCookieName = authCookieName;
        this.tokenExpiration = tokenExpiration;
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
    }

    public ResponseCookie createSessionCookie(IssuedAuthToken token) {
        return baseCookieBuilder()
                .value(token.accessToken())
                .maxAge(tokenExpiration)
                .build();
    }

    public ResponseCookie clearSessionCookie() {
        return baseCookieBuilder()
                .value("")
                .maxAge(Duration.ZERO)
                .build();
    }

    public String authCookieName() {
        return authCookieName;
    }

    private ResponseCookie.ResponseCookieBuilder baseCookieBuilder() {
        return ResponseCookie.from(authCookieName)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path("/");
    }
}
