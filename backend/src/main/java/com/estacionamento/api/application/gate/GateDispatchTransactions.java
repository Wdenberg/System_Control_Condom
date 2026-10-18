package com.estacionamento.api.application.gate;
import com.estacionamento.api.domain.gate.*;
import com.estacionamento.api.domain.movement.PassageState;
import com.estacionamento.api.config.properties.GateProperties;
import com.estacionamento.api.infrastructure.gate.*;
import com.estacionamento.api.application.realtime.RealtimeEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;
@Service
public class GateDispatchTransactions {
    public record Claim(Long id, String token, String driver, GateOpenCommand command) {}
    public record AttemptFinished(GateOperationStatus outcome) {}
    private final GateOperationRepository operations;
    private final GateProperties properties;
    private final com.estacionamento.api.domain.movement.MovementRepository movements;
    private final Clock clock;
    private final JdbcTemplate jdbc;
    private final RealtimeEventPublisher events;
    private final org.springframework.context.ApplicationEventPublisher applicationEvents;
    public GateDispatchTransactions(GateOperationRepository operations, GateProperties properties, Clock clock, JdbcTemplate jdbc, RealtimeEventPublisher events, com.estacionamento.api.domain.movement.MovementRepository movements, org.springframework.context.ApplicationEventPublisher applicationEvents) {
        this.movements=movements; this.operations=operations; this.properties=properties; this.clock=clock; this.jdbc=jdbc; this.events=events; this.applicationEvents=applicationEvents;
    }
    @Transactional
    public Optional<Claim> claim() {
        if (!properties.enabled()) return Optional.empty();
        OffsetDateTime now=OffsetDateTime.now(clock);
        return operations.claimNext(now).map(operation -> {
            String token=UUID.randomUUID().toString();
            operation.claim(token, now.plus(properties.timeout()).plusSeconds(30), now);
            jdbc.update("INSERT INTO gate_operation_attempts(operation_id,claim_token,outcome,started_at) VALUES (?,?,'PROCESSING',?)",operation.getId(),token,now);
            events.publish("gate.updated",GateOperationResponse.from(operation));
            return new Claim(operation.getId(),token,operation.getDriver(),new GateOpenCommand(operation.getOperationKey(),operation.getMovement().getDirection(),operation.getMovement().getPlate(),properties.openPulseMillis()));
        });
    }
    @Transactional
    public void finish(Claim claim, GateCommandResult result) {
        movements.findByIdForUpdate(operations.findMovementId(claim.id()).orElseThrow()).orElseThrow();
        GateOperation operation=operations.findLocked(claim.id()).orElseThrow();
        if (operation.getStatus()!=GateOperationStatus.PROCESSING || !claim.token().equals(operation.getClaimToken())) return;
        var now=OffsetDateTime.now(clock);
        if (result.uncertain()) { operation.markUnknown(result.message()); operation.getMovement().passageState(PassageState.UNKNOWN); }
        else if (result.success()) { operation.markSent(now); operation.getMovement().passageState(PassageState.COMMAND_ACCEPTED); }
        else {
            boolean exhausted=operation.getAttemptCount()>=properties.maxAttempts();
            operation.markFailure(result.message(),now.plus(properties.retryDelay()),exhausted);
            if (exhausted) operation.getMovement().passageState(PassageState.RECONCILIATION_REQUIRED);
        }
        jdbc.update("UPDATE gate_operation_attempts SET outcome=?, finished_at=?, details=? WHERE claim_token=?",operation.getStatus().name(),now,result.message(),claim.token());
        applicationEvents.publishEvent(new AttemptFinished(operation.getStatus()));
        events.publish("gate.updated",GateOperationResponse.from(operation));
    }
    @Transactional
    public void recoverExpired() {
        for (Long id:operations.expiredClaimIds(OffsetDateTime.now(clock))) {
            movements.findByIdForUpdate(operations.findMovementId(id).orElseThrow()).orElseThrow();
            GateOperation operation=operations.findLocked(id).orElseThrow();
            if (operation.getStatus()!=GateOperationStatus.PROCESSING || operation.getLeaseUntil()==null || !operation.getLeaseUntil().isBefore(OffsetDateTime.now(clock))) continue;
            operation.markUnknown("Worker lease expired; reconciliation is required before any new command");
            operation.getMovement().passageState(PassageState.UNKNOWN);
            jdbc.update("UPDATE gate_operation_attempts SET outcome='UNKNOWN', finished_at=?, details='Worker lease expired' WHERE claim_token=?",OffsetDateTime.now(clock),operation.getClaimToken());
            applicationEvents.publishEvent(new AttemptFinished(GateOperationStatus.UNKNOWN));
            events.publish("gate.updated",GateOperationResponse.from(operation));
        }
    }
}
