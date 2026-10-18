package com.estacionamento.api.application.dashboard;

import com.estacionamento.api.config.properties.DashboardProperties;
import com.estacionamento.api.domain.apartment.ApartmentRepository;
import com.estacionamento.api.domain.camera.CameraRepository;
import com.estacionamento.api.domain.gate.GateOperationRepository;
import com.estacionamento.api.domain.hikvision.HikvisionEventRepository;
import com.estacionamento.api.domain.movement.MovementRepository;
import com.estacionamento.api.domain.parking.ParkingSpaceRepository;
import com.estacionamento.api.domain.resident.ResidentRepository;
import com.estacionamento.api.domain.vehicle.VehicleRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock ApartmentRepository apartmentRepository;
    @Mock ResidentRepository residentRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock ParkingSpaceRepository parkingSpaceRepository;
    @Mock MovementRepository movementRepository;
    @Mock GateOperationRepository gateOperationRepository;
    @Mock CameraRepository cameraRepository;
    @Mock HikvisionEventRepository hikvisionEventRepository;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any())).thenReturn(null);
        service = new DashboardService(
                apartmentRepository,
                residentRepository,
                vehicleRepository,
                parkingSpaceRepository,
                movementRepository,
                gateOperationRepository,
                cameraRepository,
                hikvisionEventRepository,
                new ObjectMapper(),
                redisTemplate,
                new DashboardProperties("America/Recife", Duration.ofSeconds(5)),
                Clock.fixed(Instant.parse("2026-09-13T11:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void shouldBuildOperationalSummary() {
        when(apartmentRepository.countByActiveTrue()).thenReturn(10L);
        when(residentRepository.countByActiveTrue()).thenReturn(20L);
        when(vehicleRepository.countByActiveTrue()).thenReturn(15L);
        when(vehicleRepository.countByActiveTrueAndInsideGarageTrue()).thenReturn(7L);
        when(vehicleRepository.countByActiveTrueAndInsideGarageFalse()).thenReturn(8L);
        when(parkingSpaceRepository.countByActiveTrue()).thenReturn(20L);
        when(parkingSpaceRepository.countByActiveTrueAndStatus(com.estacionamento.api.domain.parking.ParkingSpaceStatus.AVAILABLE)).thenReturn(11L);
        when(parkingSpaceRepository.countByActiveTrueAndStatus(com.estacionamento.api.domain.parking.ParkingSpaceStatus.OCCUPIED)).thenReturn(7L);
        when(parkingSpaceRepository.countByActiveTrueAndStatus(com.estacionamento.api.domain.parking.ParkingSpaceStatus.BLOCKED)).thenReturn(2L);
        when(movementRepository.countBetween(any(), any())).thenReturn(12L);
        when(movementRepository.countByDirectionBetween(any(), any(), eq(com.estacionamento.api.domain.movement.MovementDirection.ENTRY))).thenReturn(6L);
        when(movementRepository.countByDirectionBetween(any(), any(), eq(com.estacionamento.api.domain.movement.MovementDirection.EXIT))).thenReturn(6L);
        when(movementRepository.countByStatusBetween(any(), any(), eq(com.estacionamento.api.domain.movement.MovementStatus.AUTHORIZED))).thenReturn(10L);
        when(movementRepository.countByStatusBetween(any(), any(), eq(com.estacionamento.api.domain.movement.MovementStatus.DENIED))).thenReturn(2L);
        when(gateOperationRepository.countByStatus(com.estacionamento.api.domain.gate.GateOperationStatus.PENDING)).thenReturn(1L);
        when(gateOperationRepository.countByStatus(com.estacionamento.api.domain.gate.GateOperationStatus.EXHAUSTED)).thenReturn(2L);

        DashboardSummaryResponse response = service.summary();

        assertThat(response.activeApartments()).isEqualTo(10L);
        assertThat(response.activeResidents()).isEqualTo(20L);
        assertThat(response.vehiclesInside()).isEqualTo(7L);
        assertThat(response.freeParkingSpaces()).isEqualTo(11L);
        assertThat(response.entriesToday()).isEqualTo(6L);
        assertThat(response.deniedToday()).isEqualTo(2L);
        assertThat(response.pendingGateOperations()).isEqualTo(1L);
        assertThat(response.exhaustedGateOperations()).isEqualTo(2L);
        assertThat(response.generatedAt()).isNotNull();
    }
}
