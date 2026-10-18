package com.estacionamento.api.application.realtime;

import org.springframework.scheduling.annotation.Scheduled;
import io.micrometer.core.instrument.MeterRegistry;
import com.estacionamento.api.observability.GarageMetrics;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RealtimeBroadcaster {

    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
    private final java.util.Map<SseEmitter, org.springframework.security.oauth2.jwt.Jwt> sessionsByEmitter = new ConcurrentHashMap<>();
    private final com.estacionamento.api.security.PersistentSessionValidator sessions;
    private final Clock clock;
    private final GarageMetrics metrics;

    public RealtimeBroadcaster(Clock clock) {
        this(clock, new com.estacionamento.api.observability.GarageMetrics(), new io.micrometer.core.instrument.simple.SimpleMeterRegistry(), null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public RealtimeBroadcaster(Clock clock, GarageMetrics metrics, MeterRegistry meterRegistry, com.estacionamento.api.security.PersistentSessionValidator sessions) {
        this.clock = clock;
        this.sessions = sessions;
        this.metrics = metrics;
        meterRegistry.gauge("garage_realtime_connected_clients", emitters, Set::size);
    }

    public synchronized SseEmitter register(org.springframework.security.oauth2.jwt.Jwt jwt) {
        if (sessions == null || !authorized(jwt)) throw new org.springframework.security.access.AccessDeniedException("Session revoked");
        long perUser = sessionsByEmitter.values().stream().filter(token -> token.getSubject().equals(jwt.getSubject())).count();
        if (emitters.size() >= 200 || perUser >= 3) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "Realtime connection limit");
        long remaining = java.time.Duration.between(clock.instant(), jwt.getExpiresAt()).toMillis();
        SseEmitter emitter = new SseEmitter(Math.min(remaining, 900_000L));
        emitters.add(emitter);
        sessionsByEmitter.put(emitter, jwt);
        emitter.onCompletion(() -> remove(emitter));
        emitter.onTimeout(() -> { remove(emitter); emitter.complete(); });
        emitter.onError(error -> remove(emitter));
        metrics.realtimeConnectionOpened();
        send(emitter, new RealtimeEvent("connection", java.util.Map.of("status", "CONNECTED"), OffsetDateTime.now(clock)));
        return emitter;
    }

    private void remove(SseEmitter emitter) {
        sessionsByEmitter.remove(emitter);
        if (emitters.remove(emitter)) metrics.realtimeConnectionClosed();
    }

    private boolean authorized(org.springframework.security.oauth2.jwt.Jwt jwt) {
        if (jwt == null || jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(clock.instant())) return false;
        try {
            return sessions.current(jwt).filter(user -> com.estacionamento.api.domain.role.RolePermissions.of(user.getRole().getCode())
                    .contains(com.estacionamento.api.domain.role.Permission.MOVEMENT_READ)).isPresent();
        } catch (org.springframework.dao.DataAccessException exception) { return false; }
    }

    public int connectedClients() {
        return emitters.size();
    }

    public void broadcast(RealtimeEvent event) {
        for (SseEmitter emitter : emitters) {
            if (!authorized(sessionsByEmitter.get(emitter))) { remove(emitter); emitter.complete(); continue; }
            if (!send(emitter, event)) {
                remove(emitter);
            } else {
                metrics.realtimeEventBroadcast();
            }
        }
    }

    @Scheduled(fixedRateString = "${app.realtime.heartbeat-interval-ms:15000}")
    public void heartbeat() {
        RealtimeEvent heartbeat = new RealtimeEvent(
                "heartbeat",
                java.util.Map.of("status", "UP"),
                OffsetDateTime.now(clock)
        );
        broadcast(heartbeat);
    }

    private boolean send(SseEmitter emitter, RealtimeEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .id(event.id())
                    .name(event.type())
                    .data(event.payload()));
            return true;
        } catch (IOException | IllegalStateException exception) {
            try {
                emitter.completeWithError(exception);
            } catch (IllegalStateException ignored) {
                // Emitter is already closed.
            }
            return false;
        }
    }
}
