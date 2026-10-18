package com.estacionamento.api.web;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @GetMapping("/version")
    @PreAuthorize("hasAuthority('DASHBOARD_READ')")
    public Map<String, String> version() {
        return Map.of(
                "application", "garage-control-backend",
                "phase", "19-release",
                "status", "UP",
                "version", "1.0.0"
        );
    }

    @GetMapping("/authorization/user-write")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public Map<String, String> userWriteCheck() {
        return Map.of("permission", "USER_WRITE", "allowed", "true");
    }
}
