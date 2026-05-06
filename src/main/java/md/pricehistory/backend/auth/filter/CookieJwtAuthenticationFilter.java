package md.pricehistory.backend.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CookieJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final Converter<Jwt, Authentication> jwtAuthenticationConverter;
    private final String authCookieName;
    private final DefaultBearerTokenResolver bearerTokenResolver = new DefaultBearerTokenResolver();

    public CookieJwtAuthenticationFilter(
            JwtDecoder jwtDecoder,
            Converter<Jwt, Authentication> jwtAuthenticationConverter,
            @Value("${price-history.auth-cookie-name:pricehistory_access}") String authCookieName
    ) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.authCookieName = authCookieName;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            resolveToken(request)
                    .ifPresent(this::authenticateQuietly);
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> resolveToken(HttpServletRequest request) {
        String authorizationToken = bearerTokenResolver.resolve(request);
        if (authorizationToken != null && !authorizationToken.isBlank()) {
            return Optional.of(authorizationToken);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter((cookie) -> authCookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter((value) -> value != null && !value.isBlank())
                .findFirst();
    }

    private void authenticateQuietly(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            SecurityContextHolder.getContext().setAuthentication(jwtAuthenticationConverter.convert(jwt));
        } catch (JwtException ignored) {
            SecurityContextHolder.clearContext();
        }
    }
}
