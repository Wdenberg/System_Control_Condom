package com.estacionamento.api.domain.vehicle;

import com.estacionamento.api.domain.movement.MovementStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicle v where v.id = :id")
    Optional<Vehicle> findByIdForUpdate(@Param("id") Long id);

    boolean existsByPlate(String plate);
    boolean existsByPlateAndIdNot(String plate, Long id);
    boolean existsByParkingSpaceId(Long parkingSpaceId);
    boolean existsByParkingSpaceIdAndActiveTrueAndIdNot(Long parkingSpaceId, Long vehicleId);
    long countByActiveTrue();
    long countByActiveTrueAndInsideGarageTrue();
    long countByActiveTrueAndInsideGarageFalse();

    @Query("select count(v) from Vehicle v where v.active = true and v.insideGarage = false and v.parkingSpace is null")
    long countActiveWithoutParkingSpace();

    @Query("select distinct v from Vehicle v " +
            "left join fetch v.residents r " +
            "left join fetch r.apartment " +
            "left join fetch v.parkingSpace ps " +
            "left join fetch ps.apartment " +
            "where v.plate = :plate")
    Optional<Vehicle> findByPlateWithDetails(@Param("plate") String plate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicle v where v.plate = :plate")
    Optional<Vehicle> findByPlateForUpdate(@Param("plate") String plate);

    @Query("select distinct v from Vehicle v " +
            "left join fetch v.residents r " +
            "left join fetch r.apartment " +
            "left join fetch v.parkingSpace ps " +
            "left join fetch ps.apartment " +
            "where v.id = :id")
    Optional<Vehicle> findWithDetailsById(@Param("id") Long id);

    @Query("select distinct v from Vehicle v " +
            "left join fetch v.residents r " +
            "left join fetch r.apartment " +
            "left join fetch v.parkingSpace ps " +
            "order by v.plate asc")
    List<Vehicle> findAllWithDetails();
}
