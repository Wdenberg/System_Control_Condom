package com.estacionamento.api.web.auth;

import com.estacionamento.api.domain.role.Permission;
import com.estacionamento.api.domain.role.RoleCode;
import com.estacionamento.api.security.AuthenticatedUser;

import java.util.List;

public record UserSummary(
        Long id,
        String username,
        String fullName,
        RoleCode role,
        List<Permission> permissions
) {

    public static UserSummary from(AuthenticatedUser user) {
        return new UserSummary(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .filter(a -> !a.startsWith("ROLE_"))
                        .map(Permission::valueOf)
                        .sorted()
                        .toList()
        );
    }
}
