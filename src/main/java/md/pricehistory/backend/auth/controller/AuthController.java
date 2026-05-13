package md.pricehistory.backend.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Optional;
import md.pricehistory.backend.auth.dto.AuthCredentialsRequest;
import md.pricehistory.backend.auth.dto.AuthSessionResponse;
import md.pricehistory.backend.auth.dto.IssuedAuthToken;
import md.pricehistory.backend.auth.service.AuthCookieService;
import md.pricehistory.backend.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Profile("!swagger")
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final JwtDecoder jwtDecoder;

    @Value("${price-history.include-token-in-response:false}")
    private boolean includeTokenInResponse;

    public AuthController(AuthService authService, AuthCookieService authCookieService, JwtDecoder jwtDecoder) {
        this.authService = authService;
        this.authCookieService = authCookieService;
        this.jwtDecoder = jwtDecoder;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthSessionResponse> register(@Valid @RequestBody AuthCredentialsRequest request) {
        IssuedAuthToken token = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, authCookieService.createSessionCookie(token).toString())
                .body(AuthSessionResponse.fromIssuedToken(token, includeTokenInResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthSessionResponse> login(@Valid @RequestBody AuthCredentialsRequest request) {
        IssuedAuthToken token = authService.login(request);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.createSessionCookie(token).toString())
                .body(AuthSessionResponse.fromIssuedToken(token, includeTokenInResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity
                .noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.clearSessionCookie().toString())
                .build();
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
