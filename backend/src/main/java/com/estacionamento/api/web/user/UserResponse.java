package com.estacionamento.api.web.user;

import com.estacionamento.api.domain.role.Permission;
import com.estacionamento.api.domain.role.RoleCode;
import com.estacionamento.api.domain.role.RolePermissions;
import com.estacionamento.api.domain.user.User;

import java.time.Instant;
import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        RoleCode role,
        List<Permission> permissions,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole().getCode(),
                RolePermissions.of(user.getRole().getCode()).stream().sorted().toList(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
