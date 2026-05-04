package md.pricehistory.backend.user.service;

import md.pricehistory.backend.common.ApiException;
import md.pricehistory.backend.user.entity.UserAccountEntity;
import md.pricehistory.backend.user.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserAccountRepository userAccountRepository;

    public UserService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccountEntity findByUsername(String username) {
        return userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User account was not found"));
    }

    public boolean existsByUsername(String username) {
        return userAccountRepository.existsByUsername(username);
    }

    public UserAccountEntity save(UserAccountEntity user) {
        return userAccountRepository.save(user);
    }
}
