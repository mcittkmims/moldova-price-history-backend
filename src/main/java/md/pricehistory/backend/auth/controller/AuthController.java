package md.pricehistory.backend.auth.controller;

import jakarta.validation.Valid;
import md.pricehistory.backend.auth.dto.AuthCredentialsRequest;
import md.pricehistory.backend.auth.dto.AuthSessionResponse;
import md.pricehistory.backend.auth.dto.IssuedAuthToken;
import md.pricehistory.backend.auth.service.AuthCookieService;
import md.pricehistory.backend.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    @Value("${price-history.include-token-in-response:false}")
    private boolean includeTokenInResponse;

    public AuthController(AuthService authService, AuthCookieService authCookieService) {
        this.authService = authService;
        this.authCookieService = authCookieService;
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
}
