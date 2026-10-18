package com.estacionamento.api.application.user;

import com.estacionamento.api.application.shared.DuplicateResourceException;
import com.estacionamento.api.application.shared.ResourceNotFoundException;
import com.estacionamento.api.domain.role.Role;
import com.estacionamento.api.domain.role.RoleCode;
import com.estacionamento.api.domain.role.RoleRepository;
import com.estacionamento.api.domain.user.User;
import com.estacionamento.api.domain.user.UserRepository;
import com.estacionamento.api.security.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final com.estacionamento.api.application.audit.AuditService audit;

    public UserManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService,
            com.estacionamento.api.application.audit.AuditService audit
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return userRepository.findAllByOrderByUsernameAsc();
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return find(id);
    }

    @Transactional
    public User create(String username, String password, String fullName, RoleCode roleCode, boolean active, Authentication authentication) {
        String normalizedUsername = normalizeUsername(username);
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new DuplicateResourceException("Username already exists: " + normalizedUsername);
        }
        assertCanManageRole(authentication, roleCode);
        Role role = findActiveRole(roleCode);
        User user = new User(
                normalizedUsername,
                passwordEncoder.encode(password),
                normalizeName(fullName),
                role
        );
        user.setActive(active);
        User saved = userRepository.save(user);
        audit.record(currentUserService.require(authentication), "USER_CREATE", "USER", saved.getId().toString(), null, "role=" + roleCode + ", active=" + active);
        return saved;
    }

    @Transactional
    public User update(Long id, String fullName, RoleCode roleCode, boolean active, Authentication authentication) {
        User user = find(id);
        User current = currentUserService.require(authentication);

        if (user.getId().equals(current.getId()) && (!active || user.getRole().getCode() != roleCode)) {
            throw new IllegalStateException("You cannot deactivate or change the role of your own account");
        }

        assertCanManageRole(authentication, user.getRole().getCode());
        assertCanManageRole(authentication, roleCode);
        Role role = findActiveRole(roleCode);
        boolean removingSuperAdmin = user.isActive()
                && user.getRole().getCode() == RoleCode.SUPER_ADMIN
                && (!active || roleCode != RoleCode.SUPER_ADMIN);
        if (removingSuperAdmin) {
            ensureAnotherActiveSuperAdmin(user.getId());
        }

        String oldRole = user.getRole().getCode().name();
        boolean oldActive = user.isActive();
        user.changeFullName(normalizeName(fullName));
        user.changeRole(role);
        user.setActive(active);
        User saved = userRepository.save(user);
        audit.record(current, "USER_UPDATE", "USER", id.toString(), null, "role=" + oldRole + "->" + roleCode + ", active=" + oldActive + "->" + active);
        return saved;
    }

    @Transactional
    public User changePassword(Long id, String newPassword, Authentication authentication) {
        User user = find(id);
        assertCanManageRole(authentication, user.getRole().getCode());
        user.changePassword(passwordEncoder.encode(newPassword));
        User saved = userRepository.save(user);
        audit.record(currentUserService.require(authentication), "USER_PASSWORD_RESET", "USER", id.toString(), null, "All prior sessions revoked");
        return saved;
    }

    private void assertCanManageRole(Authentication authentication, RoleCode roleCode) {
        if (roleCode == RoleCode.SUPER_ADMIN
                && (authentication == null || authentication.getAuthorities().stream().noneMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority())))) {
            throw new AccessDeniedException("Only SUPER_ADMIN can manage SUPER_ADMIN accounts");
        }
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private Role findActiveRole(RoleCode roleCode) {
        if (roleCode == null) {
            throw new IllegalArgumentException("role is required");
        }
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleCode));
        if (!role.isActive()) {
            throw new IllegalStateException("Role is inactive: " + roleCode);
        }
        return role;
    }

    private void ensureAnotherActiveSuperAdmin(Long excludedUserId) {
        long activeSuperAdmins = userRepository.lockActiveRoleUsers(RoleCode.SUPER_ADMIN).size();
        if (activeSuperAdmins <= 1) {
            throw new IllegalStateException("At least one active SUPER_ADMIN is required");
        }
    }

    private String normalizeUsername(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        return normalized;
    }

    private String normalizeName(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() < 2) {
            throw new IllegalArgumentException("fullName must have at least 2 characters");
        }
        return normalized;
    }
}
