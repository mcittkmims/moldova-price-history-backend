package md.pricehistory.backend.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import md.pricehistory.backend.user.AppPermission;
import md.pricehistory.backend.auth.dto.AuthCredentialsRequest;
import md.pricehistory.backend.auth.dto.IssuedAuthToken;
import md.pricehistory.backend.user.entity.UserAccountEntity;
import md.pricehistory.backend.user.service.UserService;
import md.pricehistory.backend.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final Clock clock;

    @Autowired
    public AuthService(
            UserService userService,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this(userService, passwordEncoder, jwtTokenService, Clock.systemUTC());
    }

    AuthService(
            UserService userService,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            Clock clock
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.clock = clock;
    }

    @Transactional
    public IssuedAuthToken register(AuthCredentialsRequest request) {
        String username = normalizeUsername(request.username());
        if (userService.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "Username is already taken");
        }

        Instant now = clock.instant();
        UserAccountEntity user = new UserAccountEntity();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPermissions(defaultPermissions());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return jwtTokenService.issueToken(userService.save(user));
    }

    @Transactional(readOnly = true)
    public IssuedAuthToken login(AuthCredentialsRequest request) {
        String username = normalizeUsername(request.username());
        UserAccountEntity user;
        try {
            user = userService.findByUsername(username);
        } catch (ApiException ignored) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        return jwtTokenService.issueToken(user);
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private Set<String> defaultPermissions() {
        return new LinkedHashSet<>(AppPermission.defaultUserPermissions());
    }
}
