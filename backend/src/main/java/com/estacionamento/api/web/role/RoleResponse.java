package com.estacionamento.api.web.role;

import com.estacionamento.api.domain.role.Permission;
import com.estacionamento.api.domain.role.Role;
import com.estacionamento.api.domain.role.RoleCode;
import com.estacionamento.api.domain.role.RolePermissions;

import java.util.List;

public record RoleResponse(
        Long id,
        RoleCode code,
        boolean active,
        List<Permission> permissions
) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.isActive(),
                RolePermissions.of(role.getCode()).stream().sorted().toList()
        );
    }
}
