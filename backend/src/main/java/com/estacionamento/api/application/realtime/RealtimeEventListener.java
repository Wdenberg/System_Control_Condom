package com.estacionamento.api.application.realtime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RealtimeEventListener {

    private final RealtimeBroadcaster broadcaster;

    public RealtimeEventListener(RealtimeBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRealtimeEvent(RealtimeEvent event) {
        broadcaster.broadcast(event);
    }
}
