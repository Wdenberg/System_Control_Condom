package com.estacionamento.api.domain.movement;
import com.estacionamento.api.domain.vehicle.Vehicle;
import com.estacionamento.api.domain.parking.ParkingSpace;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class PassageLifecycleTest {
    private Movement movement(MovementStatus status) { return new Movement(mock(Vehicle.class),null,null,null,null,"ABC1D23",MovementDirection.ENTRY,MovementSource.MANUAL,status,AccessDecision.AUTHORIZED,null,OffsetDateTime.now(),"Observed passage"); }
    @Test void authorizationDoesNotConfirmPassage() { var movement=movement(MovementStatus.AUTHORIZED);movement.reservePassage();assertThat(movement.getPassageState()).isEqualTo(PassageState.AUTHORIZED);assertThat(movement.isReservationActive()).isTrue();verifyNoInteractions(movement.getVehicle()); }
    @Test void observedPassageCompletesAndReleasesReservation() { var movement=movement(MovementStatus.AUTHORIZED);movement.reservePassage();movement.reconcile(true,OffsetDateTime.now());assertThat(movement.getPassageState()).isEqualTo(PassageState.COMPLETED);assertThat(movement.isReservationActive()).isFalse(); }
    @Test void absenceOfPassageIsNotConfirmedOccupancy() { var movement=movement(MovementStatus.AUTHORIZED);movement.reconcile(false,OffsetDateTime.now());assertThat(movement.getPassageState()).isEqualTo(PassageState.FAILED); }
    @Test void deniedMovementCannotBecomeAConfirmedPassage() { assertThatThrownBy(()->movement(MovementStatus.DENIED).reconcile(true,OffsetDateTime.now())).isInstanceOf(IllegalStateException.class); }
    @Test void overrideBindsRegisteredResourcesBeforeReservingPassage() {
        var movement=movement(MovementStatus.DENIED);
        Vehicle vehicle=mock(Vehicle.class);when(vehicle.getPlate()).thenReturn("ABC1D23");
        ParkingSpace space=mock(ParkingSpace.class);
        movement.override(null,"Operator checked the recognition",vehicle,space);movement.reservePassage();
        assertThat(movement.getVehicle()).isSameAs(vehicle);
        assertThat(movement.getParkingSpace()).isSameAs(space);
        assertThat(movement.isReservationActive()).isTrue();
        assertThat(movement.getPassageState()).isEqualTo(PassageState.AUTHORIZED);
    }
    @Test void overrideCannotAuthorizeAnUnregisteredVehicle() {
        assertThatThrownBy(()->movement(MovementStatus.DENIED).override(null,"Operator checked the recognition",null,null))
                .isInstanceOf(IllegalStateException.class);
    }
}
