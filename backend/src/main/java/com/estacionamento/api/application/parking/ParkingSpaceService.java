package com.estacionamento.api.application.parking;

import com.estacionamento.api.application.shared.DuplicateResourceException;
import com.estacionamento.api.application.shared.InvalidAssignmentException;
import com.estacionamento.api.application.shared.ResourceNotFoundException;
import com.estacionamento.api.domain.apartment.Apartment;
import com.estacionamento.api.domain.apartment.ApartmentRepository;
import com.estacionamento.api.domain.parking.ParkingSpace;
import com.estacionamento.api.domain.parking.ParkingSpaceRepository;
import com.estacionamento.api.domain.parking.ParkingSpaceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ParkingSpaceService {

    private final ParkingSpaceRepository repository;
    private final ApartmentRepository apartmentRepository;
    private final com.estacionamento.api.domain.movement.MovementRepository movements;
    private final com.estacionamento.api.domain.vehicle.VehicleRepository vehicles;

    public ParkingSpaceService(ParkingSpaceRepository repository, ApartmentRepository apartmentRepository,
                               com.estacionamento.api.domain.movement.MovementRepository movements,
                               com.estacionamento.api.domain.vehicle.VehicleRepository vehicles) {
        this.repository = repository;
        this.apartmentRepository = apartmentRepository;
        this.movements = movements;
        this.vehicles = vehicles;
    }

    @Transactional(readOnly = true)
    public List<ParkingSpace> list() {
        return repository.findAllByOrderByNumberAsc();
    }

    @Transactional(readOnly = true)
    public ParkingSpace get(Long id) {
        return repository.findWithApartmentById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking space not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ParkingSpace> listByApartment(Long apartmentId) {
        apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found: " + apartmentId));
        return repository.findAllByApartmentIdOrderByNumberAsc(apartmentId);
    }

    public ParkingSpace create(String number, Long apartmentId, ParkingSpaceType type) { return create(number, apartmentId, type, true); }

    public ParkingSpace create(String number, Long apartmentId, ParkingSpaceType type, boolean active) {
        String normalized = number == null ? null : number.trim().toUpperCase();
        if (normalized != null && repository.existsByNumber(normalized)) {
            throw new DuplicateResourceException("Parking space number already exists: " + normalized);
        }
        Apartment apartment = resolveApartment(apartmentId);
        ParkingSpaceType effectiveType = type == null ? ParkingSpaceType.ASSIGNED : type;
        if (effectiveType == ParkingSpaceType.ASSIGNED && apartment == null) {
            throw new InvalidAssignmentException("An ASSIGNED parking space must belong to an apartment");
        }
        ParkingSpace space = new ParkingSpace(number, apartment, effectiveType);
        space.setActive(active);
        ParkingSpace saved = repository.save(space);
        return repository.findWithApartmentById(saved.getId()).orElse(saved);
    }

    public ParkingSpace update(Long id, String number, Long apartmentId, ParkingSpaceType type, boolean active) {
        ParkingSpace parkingSpace = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking space not found: " + id));
        if (movements.hasUnresolvedPassageForSpace(id)) {
            throw new InvalidAssignmentException("Parking space has an unresolved passage");
        }
        String normalized = number == null ? null : number.trim().toUpperCase();
        if (normalized != null && repository.existsByNumberAndIdNot(normalized, id)) {
            throw new DuplicateResourceException("Parking space number already exists: " + normalized);
        }
        Apartment apartment = resolveApartment(apartmentId);
        ParkingSpaceType effectiveType = type == null ? ParkingSpaceType.ASSIGNED : type;
        if (effectiveType == ParkingSpaceType.ASSIGNED && apartment == null) {
            throw new InvalidAssignmentException("An ASSIGNED parking space must belong to an apartment");
        }
        Long previousApartment = parkingSpace.getApartment() == null ? null : parkingSpace.getApartment().getId();
        if (vehicles.existsByParkingSpaceId(id) && ((parkingSpace.isActive() && !active) || effectiveType == ParkingSpaceType.VISITOR
                || !java.util.Objects.equals(previousApartment, apartmentId))) {
            throw new InvalidAssignmentException("Unassign vehicles before changing the parking space ownership, availability or use");
        }
        if (parkingSpace.getStatus() == com.estacionamento.api.domain.parking.ParkingSpaceStatus.OCCUPIED
                && (!active || !java.util.Objects.equals(previousApartment, apartmentId) || effectiveType != parkingSpace.getType())) {
            throw new InvalidAssignmentException("Occupied parking spaces require physical reconciliation before structural changes");
        }
        parkingSpace.update(number, apartment, effectiveType);
        parkingSpace.setActive(active);
        repository.save(parkingSpace);
        return repository.findWithApartmentById(parkingSpace.getId()).orElse(parkingSpace);
    }

    private Apartment resolveApartment(Long apartmentId) {
        if (apartmentId == null) {
            return null;
        }
        return apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found: " + apartmentId));
    }
}
