package com.estacionamento.api.web.parking;

import com.estacionamento.api.application.parking.ParkingSpaceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parking-spaces")
public class ParkingSpaceController {

    private final ParkingSpaceService service;

    public ParkingSpaceController(ParkingSpaceService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PARKING_READ')")
    public List<ParkingSpaceResponse> list() {
        return service.list().stream().map(ParkingSpaceResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PARKING_READ')")
    public ParkingSpaceResponse get(@PathVariable Long id) {
        return ParkingSpaceResponse.from(service.get(id));
    }

    @GetMapping("/apartment/{apartmentId}")
    @PreAuthorize("hasAuthority('PARKING_READ')")
    public List<ParkingSpaceResponse> listByApartment(@PathVariable Long apartmentId) {
        return service.listByApartment(apartmentId).stream().map(ParkingSpaceResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PARKING_WRITE')")
    public ParkingSpaceResponse create(@Valid @RequestBody ParkingSpaceRequest request) {
        return ParkingSpaceResponse.from(service.create(request.number(), request.apartmentId(), request.type(), request.active()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PARKING_WRITE')")
    public ParkingSpaceResponse update(@PathVariable Long id, @Valid @RequestBody ParkingSpaceRequest request) {
        return ParkingSpaceResponse.from(service.update(id, request.number(), request.apartmentId(), request.type(), request.active()));
    }
}
