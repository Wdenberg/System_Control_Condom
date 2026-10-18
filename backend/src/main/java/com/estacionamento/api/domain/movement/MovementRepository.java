package com.estacionamento.api.domain.movement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MovementRepository extends JpaRepository<Movement, Long>, JpaSpecificationExecutor<Movement> {

    @EntityGraph(attributePaths = {"vehicle", "camera", "parkingSpace", "operator"})
    Page<Movement> findAllByOrderByEventTimeDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"vehicle", "camera", "parkingSpace", "operator"})
    Optional<Movement> findById(Long id);

    boolean existsByVehicle_IdAndReservationActiveTrue(Long vehicleId);
    boolean existsByParkingSpace_IdAndReservationActiveTrue(Long parkingSpaceId);
    @Query("select count(m) > 0 from Movement m where m.plate=:plate and (m.reservationActive=true or m.passageState=com.estacionamento.api.domain.movement.PassageState.UNKNOWN)")
    boolean hasUnresolvedPassageForPlate(@Param("plate") String plate);
    @Query("select count(m) > 0 from Movement m where m.parkingSpace.id=:spaceId and (m.reservationActive=true or m.passageState=com.estacionamento.api.domain.movement.PassageState.UNKNOWN)")
    boolean hasUnresolvedPassageForSpace(@Param("spaceId") Long spaceId);
    boolean existsByParkingSpace_IdAndReservationActiveTrueAndDirection(Long parkingSpaceId, MovementDirection direction);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Movement m where m.id = :id")
    Optional<Movement> findByIdForUpdate(@Param("id") Long id);

    Optional<Movement> findByHikvisionEventId(Long hikvisionEventId);

    @Query("select count(m) > 0 from Movement m " +
            "where m.vehicle.id = :vehicleId " +
            "and m.direction = :direction " +
            "and m.status in :statuses")
    boolean existsByVehicleAndDirectionAndStatuses(@Param("vehicleId") Long vehicleId,
                                                   @Param("direction") MovementDirection direction,
                                                   @Param("statuses") Collection<MovementStatus> statuses);

    @Query("select count(m) from Movement m where m.eventTime >= :from and m.eventTime < :to")
    long countBetween(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("select count(m) from Movement m where m.eventTime >= :from and m.eventTime < :to and m.direction = :direction")
    long countByDirectionBetween(@Param("from") OffsetDateTime from,
                                 @Param("to") OffsetDateTime to,
                                 @Param("direction") MovementDirection direction);

    @Query("select count(m) from Movement m where m.eventTime >= :from and m.eventTime < :to and m.status = :status")
    long countByStatusBetween(@Param("from") OffsetDateTime from,
                              @Param("to") OffsetDateTime to,
                              @Param("status") MovementStatus status);

    @EntityGraph(attributePaths = {"vehicle", "camera", "parkingSpace", "operator"})
    List<Movement> findTop20ByStatusOrderByEventTimeDesc(MovementStatus status);

    @EntityGraph(attributePaths = {"vehicle", "camera", "parkingSpace", "operator"})
    List<Movement> findTop20ByOrderByEventTimeDesc();
}
