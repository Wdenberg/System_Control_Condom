package com.estacionamento.api.web.apartment;

import com.estacionamento.api.application.apartment.ApartmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/apartments")
public class ApartmentController {

    private final ApartmentService service;

    public ApartmentController(ApartmentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('APARTMENT_READ')")
    public List<ApartmentResponse> list() {
        return service.list().stream().map(ApartmentResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('APARTMENT_READ')")
    public ApartmentResponse get(@PathVariable Long id) {
        return ApartmentResponse.from(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('APARTMENT_WRITE')")
    public ApartmentResponse create(@Valid @RequestBody ApartmentRequest request) {
        return ApartmentResponse.from(service.create(request.number(), request.active()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('APARTMENT_WRITE')")
    public ApartmentResponse update(@PathVariable Long id, @Valid @RequestBody ApartmentRequest request) {
        return ApartmentResponse.from(service.update(id, request.number(), request.active()));
    }

    @GetMapping("/{id}/residents")
    @PreAuthorize("hasAuthority('APARTMENT_READ')")
    public List<ResidentResponse> listResidents(@PathVariable Long id) {
        return service.listResidents(id).stream().map(ResidentResponse::from).toList();
    }

    @PostMapping("/{id}/residents")
    @PreAuthorize("hasAuthority('APARTMENT_WRITE')")
    public ResidentResponse createResident(@PathVariable Long id, @Valid @RequestBody ResidentRequest request) {
        return ResidentResponse.from(service.createResident(id, request.fullName(), request.active()));
    }

    @PutMapping("/{id}/residents/{residentId}")
    @PreAuthorize("hasAuthority('APARTMENT_WRITE')")
    public ResidentResponse updateResident(@PathVariable Long id, @PathVariable Long residentId,
                                            @Valid @RequestBody ResidentRequest request) {
        return ResidentResponse.from(service.updateResident(id, residentId, request.fullName(), request.active()));
    }
}
