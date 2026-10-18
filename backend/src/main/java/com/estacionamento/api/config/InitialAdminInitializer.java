package com.estacionamento.api.config;

import com.estacionamento.api.config.properties.InitialAdminProperties;
import com.estacionamento.api.domain.role.Role;
import com.estacionamento.api.domain.role.RoleCode;
import com.estacionamento.api.domain.role.RoleRepository;
import com.estacionamento.api.domain.user.User;
import com.estacionamento.api.domain.user.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableConfigurationProperties(InitialAdminProperties.class)
public class InitialAdminInitializer implements ApplicationRunner {

    private final InitialAdminProperties properties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public InitialAdminInitializer(
            InitialAdminProperties properties,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }

        validateConfiguration();

        String username = properties.username().trim().toLowerCase();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }

        RoleCode roleCode = RoleCode.valueOf(properties.role().trim().toUpperCase());
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + roleCode));

        User user = new User(
                username,
                passwordEncoder.encode(properties.password()),
                properties.fullName().trim(),
                role
        );
        userRepository.save(user);
    }

    private void validateConfiguration() {
        if (isBlank(properties.username()) || isBlank(properties.password()) || isBlank(properties.fullName()) || isBlank(properties.role())) {
            throw new IllegalStateException("Initial admin configuration is incomplete");
        }
        if (properties.password().length() < 12) {
            throw new IllegalStateException("Initial admin password must have at least 12 characters");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
