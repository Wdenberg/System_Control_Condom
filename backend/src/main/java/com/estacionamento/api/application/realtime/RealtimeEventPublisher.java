package com.estacionamento.api.application.realtime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
public class RealtimeEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    public RealtimeEventPublisher(ApplicationEventPublisher applicationEventPublisher, Clock clock) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.clock = clock;
    }

    public void publish(String type, Object payload) {
        applicationEventPublisher.publishEvent(
                new RealtimeEvent(type, payload, OffsetDateTime.now(clock))
        );
    }
}
