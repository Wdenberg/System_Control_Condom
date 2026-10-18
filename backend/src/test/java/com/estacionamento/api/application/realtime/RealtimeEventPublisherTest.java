package com.estacionamento.api.application.realtime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RealtimeEventPublisherTest {

    @Test
    void shouldPublishRealtimeEventWithPayload() {
        ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-13T15:00:00Z"), ZoneOffset.UTC);
        RealtimeEventPublisher publisher = new RealtimeEventPublisher(applicationEventPublisher, clock);

        publisher.publish("movement.created", "payload");

        ArgumentCaptor<RealtimeEvent> captor = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        RealtimeEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo("movement.created");
        assertThat(event.payload()).isEqualTo("payload");
        assertThat(event.occurredAt()).isEqualTo("2026-09-13T15:00:00Z");
        assertThat(event.id()).isNotBlank();
    }
}
