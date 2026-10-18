package com.estacionamento.api.web.role;

import com.estacionamento.api.domain.role.RoleRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public List<RoleResponse> list() {
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(role -> role.getCode().name()))
                .map(RoleResponse::from)
                .toList();
    }
}
