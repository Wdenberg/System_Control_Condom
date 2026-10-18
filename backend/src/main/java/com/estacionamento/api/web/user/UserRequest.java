package com.estacionamento.api.web.user;

import com.estacionamento.api.domain.role.RoleCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank @Size(min = 3, max = 80) String username,
        @NotBlank @Size(min = 12, max = 200) String password,
        @NotBlank @Size(min = 2, max = 160) String fullName,
        @NotNull RoleCode role,
        boolean active
) {}
