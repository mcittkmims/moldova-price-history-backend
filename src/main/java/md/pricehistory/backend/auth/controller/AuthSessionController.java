package md.pricehistory.backend.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import md.pricehistory.backend.auth.dto.AuthSessionResponse;
import md.pricehistory.backend.auth.service.AuthCookieService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!swagger")
@RequestMapping("/api/auth")
public class AuthSessionController {

    private final AuthCookieService authCookieService;
    private final JwtDecoder jwtDecoder;

    public AuthSessionController(AuthCookieService authCookieService, JwtDecoder jwtDecoder) {
        this.authCookieService = authCookieService;
        this.jwtDecoder = jwtDecoder;
    }

    @GetMapping("/session")
    public ResponseEntity<AuthSessionResponse> session(HttpServletRequest request) {
        Optional<String> token = findCookieValue(request, authCookieService.authCookieName());
        if (token.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        try {
            Jwt jwt = jwtDecoder.decode(token.orElseThrow());
            return ResponseEntity.ok(AuthSessionResponse.fromJwt(jwt));
        } catch (JwtException ignored) {
            return ResponseEntity.noContent().build();
        }
    }

    private Optional<String> findCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }
}
