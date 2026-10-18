package com.estacionamento.api.web.user;

import com.estacionamento.api.domain.role.RoleCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank @Size(min = 2, max = 160) String fullName,
        @NotNull RoleCode role,
        boolean active
) {}
