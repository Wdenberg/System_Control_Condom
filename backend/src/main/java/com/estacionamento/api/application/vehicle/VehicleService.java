package com.estacionamento.api.application.vehicle;

import com.estacionamento.api.application.shared.DuplicateResourceException;
import com.estacionamento.api.application.shared.InvalidAssignmentException;
import com.estacionamento.api.application.shared.ResourceNotFoundException;
import com.estacionamento.api.domain.parking.ParkingSpace;
import com.estacionamento.api.domain.parking.ParkingSpaceType;
import com.estacionamento.api.domain.parking.ParkingSpaceStatus;
import com.estacionamento.api.domain.parking.ParkingSpaceRepository;
import com.estacionamento.api.domain.resident.Resident;
import com.estacionamento.api.domain.resident.ResidentRepository;
import com.estacionamento.api.domain.vehicle.Vehicle;
import com.estacionamento.api.domain.vehicle.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final ResidentRepository residentRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final com.estacionamento.api.domain.movement.MovementRepository movements;

    public VehicleService(VehicleRepository vehicleRepository,
                          ResidentRepository residentRepository,
                          ParkingSpaceRepository parkingSpaceRepository, com.estacionamento.api.domain.movement.MovementRepository movements) {
        this.vehicleRepository = vehicleRepository;
        this.residentRepository = residentRepository;
        this.parkingSpaceRepository = parkingSpaceRepository;
        this.movements = movements;
    }

    @Transactional(readOnly = true)
    public List<Vehicle> list() {
        return vehicleRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public Vehicle get(Long id) {
        return vehicleRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));
    }

    @Transactional(readOnly = true)
    public Vehicle findByPlate(String plate) {
        return vehicleRepository.findByPlateWithDetails(normalizePlate(plate))
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found for plate: " + normalizePlate(plate)));
    }

    public Vehicle create(String plate, String brand, String model, String color, Set<Long> residentIds, Long parkingSpaceId) { return create(plate, brand, model, color, residentIds, parkingSpaceId, true); }

    public Vehicle create(String plate, String brand, String model, String color, Set<Long> residentIds, Long parkingSpaceId, boolean active) {
        String normalized = normalizePlate(plate);
        if (vehicleRepository.existsByPlate(normalized)) {
            throw new DuplicateResourceException("Vehicle plate already exists: " + normalized);
        }
        Set<Resident> residents = resolveResidents(residentIds, active);
        ParkingSpace parkingSpace = resolveParkingSpace(parkingSpaceId, active);
        validateAssignments(residents, parkingSpace);
        if (parkingSpace != null && active && vehicleRepository.existsByParkingSpaceIdAndActiveTrueAndIdNot(parkingSpace.getId(), -1L)) {
            throw new InvalidAssignmentException("Parking space is already assigned to an active vehicle: " + parkingSpace.getNumber());
        }
        Vehicle vehicle = new Vehicle(normalized, brand, model, color, parkingSpace, residents);
        vehicle.setActive(active);
        Vehicle saved = vehicleRepository.save(vehicle);
        return vehicleRepository.findWithDetailsById(saved.getId()).orElse(saved);
    }

    public Vehicle update(Long id, String plate, String brand, String model, String color,
                          Set<Long> residentIds, Long parkingSpaceId, boolean active) {
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        if (movements.hasUnresolvedPassageForPlate(vehicle.getPlate())) throw new InvalidAssignmentException("Vehicle has an unresolved passage");
        String normalized = normalizePlate(plate);
        if (vehicleRepository.existsByPlateAndIdNot(normalized, id)) {
            throw new DuplicateResourceException("Vehicle plate already exists: " + normalized);
        }
        Set<Resident> residents = resolveResidents(residentIds, active);
        ParkingSpace parkingSpace = vehicle.isInsideGarage()
                && vehicle.getParkingSpace() != null
                && vehicle.getParkingSpace().getId().equals(parkingSpaceId)
                ? vehicle.getParkingSpace()
                : resolveParkingSpace(parkingSpaceId, active);
        validateAssignments(residents, parkingSpace);
        if (vehicle.isInsideGarage()
                && ((vehicle.getParkingSpace() == null && parkingSpace != null)
                    || (vehicle.getParkingSpace() != null && (parkingSpace == null
                        || !vehicle.getParkingSpace().getId().equals(parkingSpace.getId()))))) {
            throw new InvalidAssignmentException("Vehicle parking assignment cannot change while the vehicle is inside the garage");
        }
        if (vehicle.isInsideGarage() && !active) {
            throw new InvalidAssignmentException("Vehicle cannot be deactivated while it is inside the garage");
        }
        if (parkingSpace != null && active && vehicleRepository.existsByParkingSpaceIdAndActiveTrueAndIdNot(parkingSpace.getId(), id)) {
            throw new InvalidAssignmentException("Parking space is already assigned to an active vehicle: " + parkingSpace.getNumber());
        }
        vehicle.update(normalized, brand, model, color, parkingSpace, residents);
        vehicle.setActive(active);
        vehicleRepository.save(vehicle);
        return vehicleRepository.findWithDetailsById(vehicle.getId()).orElse(vehicle);
    }

    private Set<Resident> resolveResidents(Set<Long> ids, boolean active) {
        if (ids == null || ids.isEmpty()) {
            throw new InvalidAssignmentException("At least one resident must be linked to the vehicle");
        }
        List<Resident> residents = residentRepository.findAllByIdIn(ids);
        if (residents.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more residents were not found");
        }
        if (active && residents.stream().anyMatch(resident -> !resident.isActive())) {
            throw new InvalidAssignmentException("Vehicle cannot be linked to an inactive resident");
        }
        if (active && residents.stream().anyMatch(resident -> !resident.getApartment().isActive())) {
            throw new InvalidAssignmentException("Vehicle cannot be linked to an inactive apartment");
        }
        return new HashSet<>(residents);
    }

    private ParkingSpace resolveParkingSpace(Long id, boolean active) {
        if (id == null) {
            return null;
        }
        ParkingSpace parkingSpace = parkingSpaceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking space not found: " + id));
        if ((active && !parkingSpace.isActive()) || parkingSpace.getStatus() != ParkingSpaceStatus.AVAILABLE) {
            throw new InvalidAssignmentException("Parking space is not available: " + parkingSpace.getNumber());
        }
        return parkingSpace;
    }

    private void validateAssignments(Set<Resident> residents, ParkingSpace parkingSpace) {
        Long apartmentId = residents.iterator().next().getApartment().getId();
        if (residents.stream().anyMatch(resident -> !resident.getApartment().getId().equals(apartmentId))) {
            throw new InvalidAssignmentException("All linked residents must belong to the same apartment");
        }
        if (parkingSpace != null && parkingSpace.getType() == ParkingSpaceType.VISITOR) {
            throw new InvalidAssignmentException("VISITOR parking spaces cannot be permanently assigned to a vehicle");
        }
        if (parkingSpace != null && parkingSpace.getApartment() == null) {
            throw new InvalidAssignmentException("A vehicle-assigned parking space must belong to an apartment");
        }
        if (parkingSpace != null && !parkingSpace.getApartment().getId().equals(apartmentId)) {
            throw new InvalidAssignmentException("Vehicle parking space must belong to the residents' apartment");
        }
    }

    private String normalizePlate(String plate) {
        String normalized = plate == null ? "" : plate.replaceAll("[^A-Za-z0-9]", "").toUpperCase(java.util.Locale.ROOT);
        if (!normalized.matches("[A-Z0-9]{7}")) {
            throw new IllegalArgumentException("Vehicle plate must contain exactly 7 alphanumeric characters");
        }
        return normalized;
    }
}
