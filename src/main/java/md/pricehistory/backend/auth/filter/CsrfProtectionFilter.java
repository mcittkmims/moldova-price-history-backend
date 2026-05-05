package md.pricehistory.backend.auth.filter;

import java.io.IOException;
import java.util.Set;
import md.pricehistory.backend.auth.service.CsrfTokenService;
import md.pricehistory.backend.common.ApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Profile("!swagger")
public class CsrfProtectionFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of(
            HttpMethod.GET.name(),
            HttpMethod.HEAD.name(),
            HttpMethod.OPTIONS.name(),
            HttpMethod.TRACE.name()
    );

    private final CsrfTokenService csrfTokenService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public CsrfProtectionFilter(
            CsrfTokenService csrfTokenService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.csrfTokenService = csrfTokenService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (SAFE_METHODS.contains(request.getMethod())) {
            return true;
        }

        String path = request.getServletPath();
        return "/api/auth/login".equals(path) || "/api/auth/register".equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        String csrfHeader = request.getHeader(CsrfTokenService.HEADER_NAME);
        if (csrfHeader == null || csrfHeader.isBlank()) {
            handlerExceptionResolver.resolveException(
                    request,
                    response,
                    null,
                    new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
                            "CSRF token is missing, invalid, or expired")
            );
            return;
        }

        try {
            csrfTokenService.validateToken(csrfHeader, authentication);
            filterChain.doFilter(request, response);
        } catch (ApiException exception) {
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }
    }
}
