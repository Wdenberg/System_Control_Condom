package com.estacionamento.api.web.user;

import com.estacionamento.api.application.user.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserManagementService service;

    public UserController(UserManagementService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public List<UserResponse> list() {
        return service.list().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public UserResponse get(@PathVariable Long id) {
        return UserResponse.from(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public UserResponse create(@Valid @RequestBody UserRequest request, Authentication authentication) {
        return UserResponse.from(service.create(
                request.username(), request.password(), request.fullName(), request.role(), request.active(), authentication));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication
    ) {
        return UserResponse.from(service.update(id, request.fullName(), request.role(), request.active(), authentication));
    }

    @PostMapping("/{id}/password")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public UserResponse resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request, Authentication authentication) {
        return UserResponse.from(service.changePassword(id, request.newPassword(), authentication));
    }
}
