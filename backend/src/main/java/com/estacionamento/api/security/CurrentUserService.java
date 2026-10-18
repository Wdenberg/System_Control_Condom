package com.estacionamento.api.security;

import com.estacionamento.api.domain.user.User;
import com.estacionamento.api.domain.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User require(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Authenticated user is unavailable");
        }
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
                .filter(user -> user.isActive() && user.getRole().isActive())
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
    }
}
