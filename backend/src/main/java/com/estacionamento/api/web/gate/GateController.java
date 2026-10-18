package com.estacionamento.api.web.gate;

import com.estacionamento.api.application.gate.GateOperationResponse;
import com.estacionamento.api.application.gate.GateOperationService;
import com.estacionamento.api.application.gate.GateStatusResponse;
import com.estacionamento.api.application.gate.GateTestRequest;
import com.estacionamento.api.domain.movement.MovementDirection;
import com.estacionamento.api.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gates")
public class GateController {

    private final GateOperationService service;
    private final CurrentUserService currentUserService;

    public GateController(GateOperationService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('GATE_READ')")
    public GateStatusResponse status() {
        return service.status();
    }

    @GetMapping("/operations")
    @PreAuthorize("hasAuthority('GATE_READ')")
    public Page<GateOperationResponse> operations(@PageableDefault(size = 30) Pageable pageable) {
        return service.list(pageable).map(GateOperationResponse::from);
    }

    @PostMapping("/operations/{id}/retry")
    @PreAuthorize("hasAuthority('GATE_WRITE')")
    public GateOperationResponse retry(@PathVariable Long id,
                                       @Valid @RequestBody GateTestRequest request,
                                       Authentication authentication) {
        return GateOperationResponse.from(service.retryNow(
                id,
                currentUserService.require(authentication),
                request.justification()));
    }

    @PostMapping("/test/{direction}")
    @PreAuthorize("hasAuthority('GATE_TEST')")
    public GateStatusResponse test(@PathVariable MovementDirection direction,
                                   @Valid @RequestBody GateTestRequest request,
                                   Authentication authentication) {
        service.test(direction, currentUserService.require(authentication), request.justification());
        return service.status();
    }
}
