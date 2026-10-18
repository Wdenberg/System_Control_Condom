package com.estacionamento.api.web.vehicle;

import com.estacionamento.api.application.vehicle.VehicleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VEHICLE_READ')")
    public List<VehicleResponse> list() {
        return service.list().stream().map(VehicleResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VEHICLE_READ')")
    public VehicleResponse get(@PathVariable Long id) {
        return VehicleResponse.from(service.get(id));
    }

    @GetMapping("/plate/{plate}")
    @PreAuthorize("hasAuthority('VEHICLE_READ')")
    public VehicleResponse findByPlate(@PathVariable String plate) {
        return VehicleResponse.from(service.findByPlate(plate));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('VEHICLE_WRITE')")
    public VehicleResponse create(@Valid @RequestBody VehicleRequest request) {
        return VehicleResponse.from(service.create(
                request.plate(), request.brand(), request.model(), request.color(),
                request.residentIds(), request.parkingSpaceId(), request.active()
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('VEHICLE_WRITE')")
    public VehicleResponse update(@PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
        return VehicleResponse.from(service.update(
                id, request.plate(), request.brand(), request.model(), request.color(),
                request.residentIds(), request.parkingSpaceId(), request.active()
        ));
    }
}
