package md.pricehistory.backend.auth.controller;

import md.pricehistory.backend.auth.dto.CsrfTokenResponse;
import md.pricehistory.backend.auth.service.CsrfTokenService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!swagger")
public class CsrfController {

    private final CsrfTokenService csrfTokenService;

    public CsrfController(CsrfTokenService csrfTokenService) {
        this.csrfTokenService = csrfTokenService;
    }

    @GetMapping("/api/csrf")
    public CsrfTokenResponse csrf(Authentication authentication) {
        return csrfTokenService.issueToken(authentication);
    }
}
