package md.pricehistory.backend.config;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import md.pricehistory.backend.auth.filter.CookieJwtAuthenticationFilter;
import md.pricehistory.backend.auth.filter.CsrfProtectionFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsUtils;

@Configuration
@Profile("!swagger")
public class DefaultSecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CookieJwtAuthenticationFilter cookieJwtAuthenticationFilter,
            CsrfProtectionFilter csrfProtectionFilter
    ) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling((exceptions) -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .sessionManagement((sessions) -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        .requestMatchers("/api/csrf").authenticated()
                        .requestMatchers("/api/me/**").authenticated()
                        .anyRequest().permitAll()
                );
        http.addFilterBefore(cookieJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(csrfProtectionFilter, CookieJwtAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Profile("!prod")
    SecretKey jwtSecretKey(
            @Value("${price-history.auth-secret:test-demo-secret-test-demo-secret-1234}") String authSecret
    ) {
        return new SecretKeySpec(
                authSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }

    @Bean
    @Profile("prod")
    SecretKey generatedJwtSecretKey() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        return new SecretKeySpec(secret, "HmacSHA256");
    }
}
