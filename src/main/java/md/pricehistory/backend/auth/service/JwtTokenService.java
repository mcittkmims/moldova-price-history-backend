package md.pricehistory.backend.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import md.pricehistory.backend.auth.dto.AuthUserResponse;
import md.pricehistory.backend.auth.dto.IssuedAuthToken;
import md.pricehistory.backend.user.entity.UserAccountEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final Duration tokenExpiration;
    private final Clock clock;

    @Autowired
    public JwtTokenService(
            JwtEncoder jwtEncoder,
            @Value("${price-history.auth-expiration:15m}") Duration tokenExpiration
    ) {
        this(jwtEncoder, tokenExpiration, Clock.systemUTC());
    }

    JwtTokenService(JwtEncoder jwtEncoder, Duration tokenExpiration, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.tokenExpiration = tokenExpiration;
        this.clock = clock;
    }

    public String issueToken(String subject, List<String> permissions) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(tokenExpiration);
        List<String> sorted = new ArrayList<>(permissions);
        sorted.sort(Comparator.naturalOrder());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("permissions", sorted)
                .claim("username", subject)
                .build();

        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }

    public IssuedAuthToken issueToken(UserAccountEntity user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(tokenExpiration);
        String tokenId = UUID.randomUUID().toString();
        List<String> permissions = new ArrayList<>(user.getPermissions());
        permissions.sort(Comparator.naturalOrder());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(tokenId)
                .subject(user.getUsername())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("permissions", permissions)
                .claim("username", user.getUsername())
                .build();

        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
        return new IssuedAuthToken(
                token,
                tokenId,
                expiresAt,
                new AuthUserResponse(user.getUsername(), permissions)
        );
    }
}
