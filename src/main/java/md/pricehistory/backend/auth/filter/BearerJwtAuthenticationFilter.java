package md.pricehistory.backend.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile("swagger")
public class BearerJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final Converter<Jwt, Authentication> jwtAuthenticationConverter;
    private final DefaultBearerTokenResolver bearerTokenResolver = new DefaultBearerTokenResolver();

    public BearerJwtAuthenticationFilter(
            JwtDecoder jwtDecoder,
            Converter<Jwt, Authentication> jwtAuthenticationConverter
    ) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            resolveToken(request).ifPresent(this::authenticateQuietly);
        }
        filterChain.doFilter(request, response);
    }

    private java.util.Optional<String> resolveToken(HttpServletRequest request) {
        try {
            String token = bearerTokenResolver.resolve(request);
            if (token != null && !token.isBlank()) {
                return java.util.Optional.of(token);
            }
        } catch (OAuth2AuthenticationException ignored) {
        }
        return java.util.Optional.empty();
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
