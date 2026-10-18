package com.estacionamento.api.domain.gate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GateOperationRepository extends JpaRepository<GateOperation, Long>, JpaSpecificationExecutor<GateOperation> {

    @org.springframework.data.jpa.repository.Query(value = "SELECT g.* FROM gate_operations g JOIN movements m ON m.id=g.movement_id WHERE g.status='PENDING' AND g.next_attempt_at<=:now AND m.passage_state='GATE_REQUESTED' ORDER BY g.created_at LIMIT 1 FOR UPDATE OF g SKIP LOCKED", nativeQuery = true)
    Optional<GateOperation> claimNext(@org.springframework.data.repository.query.Param("now") OffsetDateTime now);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select g from GateOperation g where g.id=:id")
    Optional<GateOperation> findLocked(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Query("select g.id from GateOperation g where g.status=com.estacionamento.api.domain.gate.GateOperationStatus.PROCESSING and g.leaseUntil<:now")
    List<Long> expiredClaimIds(@org.springframework.data.repository.query.Param("now") OffsetDateTime now);
    @org.springframework.data.jpa.repository.Query("select g.movement.id from GateOperation g where g.id=:id")
    Optional<Long> findMovementId(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select g from GateOperation g where g.movement.id=:movementId")
    Optional<GateOperation> lockByMovementId(@org.springframework.data.repository.query.Param("movementId") Long movementId);

    Optional<GateOperation> findByMovementId(Long movementId);

    @Override
    @EntityGraph(attributePaths = {"movement"})
    Optional<GateOperation> findById(Long id);

    @EntityGraph(attributePaths = {"movement", "movement.vehicle", "movement.camera"})
    Page<GateOperation> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<GateOperation> findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            GateOperationStatus status, OffsetDateTime now);

    long countByStatus(GateOperationStatus status);
    long countByStatusIn(Collection<GateOperationStatus> statuses);

    @EntityGraph(attributePaths = {"movement", "movement.vehicle", "movement.camera"})
    List<GateOperation> findTop10ByStatusOrderByCreatedAtDesc(GateOperationStatus status);
}
