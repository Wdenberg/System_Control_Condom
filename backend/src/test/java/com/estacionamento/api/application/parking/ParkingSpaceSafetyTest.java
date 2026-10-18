package com.estacionamento.api.application.parking;

import com.estacionamento.api.application.shared.InvalidAssignmentException;
import com.estacionamento.api.domain.apartment.Apartment;
import com.estacionamento.api.domain.apartment.ApartmentRepository;
import com.estacionamento.api.domain.movement.MovementRepository;
import com.estacionamento.api.domain.parking.ParkingSpace;
import com.estacionamento.api.domain.parking.ParkingSpaceRepository;
import com.estacionamento.api.domain.parking.ParkingSpaceType;
import com.estacionamento.api.domain.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ParkingSpaceSafetyTest {
    private final ParkingSpaceRepository spaces = mock(ParkingSpaceRepository.class);
    private final ApartmentRepository apartments = mock(ApartmentRepository.class);
    private final MovementRepository movements = mock(MovementRepository.class);
    private final VehicleRepository vehicles = mock(VehicleRepository.class);
    private final ParkingSpaceService service = new ParkingSpaceService(spaces, apartments, movements, vehicles);
    private ParkingSpace space;

    @BeforeEach void prepare() {
        Apartment apartment = mock(Apartment.class);
        when(apartment.getId()).thenReturn(1L);
        when(apartments.findById(1L)).thenReturn(Optional.of(apartment));
        space = new ParkingSpace("101", apartment, ParkingSpaceType.ASSIGNED);
        when(spaces.findByIdForUpdate(5L)).thenReturn(Optional.of(space));
    }

    @Test void pendingPassagePreventsParkingChanges() {
        when(movements.hasUnresolvedPassageForSpace(5L)).thenReturn(true);
        assertThatThrownBy(() -> service.update(5L, "102", 1L, ParkingSpaceType.ASSIGNED, false))
                .isInstanceOf(InvalidAssignmentException.class);
        assertThat(space.isActive()).isTrue();
        assertThat(space.getNumber()).isEqualTo("101");
    }

    @Test void assignedVehiclesPreventDeactivation() {
        when(vehicles.existsByParkingSpaceId(5L)).thenReturn(true);
        assertThatThrownBy(() -> service.update(5L, "101", 1L, ParkingSpaceType.ASSIGNED, false))
                .isInstanceOf(InvalidAssignmentException.class);
        assertThat(space.isActive()).isTrue();
    }

    @Test void occupiedSpaceCannotBeConvertedToAnotherUse() {
        space.markOccupied();
        assertThatThrownBy(() -> service.update(5L, "101", 1L, ParkingSpaceType.VISITOR, true))
                .isInstanceOf(InvalidAssignmentException.class);
        assertThat(space.getType()).isEqualTo(ParkingSpaceType.ASSIGNED);
    }
}
