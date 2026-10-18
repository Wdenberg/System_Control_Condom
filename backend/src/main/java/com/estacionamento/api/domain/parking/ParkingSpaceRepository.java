package com.estacionamento.api.domain.parking;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {
    boolean existsByNumber(String number);
    boolean existsByNumberAndIdNot(String number, Long id);
    @EntityGraph(attributePaths = "apartment")
    List<ParkingSpace> findAllByOrderByNumberAsc();
    @EntityGraph(attributePaths = "apartment")
    List<ParkingSpace> findAllByApartmentIdOrderByNumberAsc(Long apartmentId);

    @EntityGraph(attributePaths = "apartment")
    Optional<ParkingSpace> findWithApartmentById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ps from ParkingSpace ps where ps.id = :id")
    Optional<ParkingSpace> findByIdForUpdate(@Param("id") Long id);

    long countByActiveTrueAndStatus(ParkingSpaceStatus status);
    long countByActiveTrue();
}
