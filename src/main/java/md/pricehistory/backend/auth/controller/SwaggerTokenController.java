package md.pricehistory.backend.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import md.pricehistory.backend.user.AppPermission;
import md.pricehistory.backend.auth.dto.TokenRequest;
import md.pricehistory.backend.auth.dto.TokenResponse;
import md.pricehistory.backend.auth.service.JwtTokenService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("swagger")
@Tag(name = "Token", description = "Demo JWT issuance — not available in production")
@RequestMapping("/token")
public class SwaggerTokenController {

    private final JwtTokenService jwtTokenService;

    public SwaggerTokenController(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping
    @Operation(
            summary = "Issue a JWT via query params",
            description = "Omit `permissions` to get all default permissions. " +
                    "Example: `?permissions=catalog:read&permissions=tracked:read_own`"
    )
    public TokenResponse getToken(
            @RequestParam(defaultValue = "demo") String subject,
            @RequestParam(required = false) List<String> permissions
    ) {
        List<String> resolved = (permissions != null && !permissions.isEmpty())
                ? permissions
                : List.copyOf(AppPermission.defaultUserPermissions());
        return new TokenResponse(jwtTokenService.issueToken(subject, resolved));
    }

    @PostMapping
    @Operation(
            summary = "Issue a JWT via JSON body",
            description = "Omit `permissions` to get all default permissions."
    )
    public TokenResponse postToken(@RequestBody TokenRequest request) {
        String subject = (request.subject() != null && !request.subject().isBlank())
                ? request.subject()
                : "demo";
        List<String> permissions = (request.permissions() != null && !request.permissions().isEmpty())
                ? request.permissions()
                : List.copyOf(AppPermission.defaultUserPermissions());
        return new TokenResponse(jwtTokenService.issueToken(subject, permissions));
    }
}
