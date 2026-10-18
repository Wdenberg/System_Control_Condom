package com.estacionamento.api.web.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank @Size(min = 12, max = 200) String newPassword
) {}
