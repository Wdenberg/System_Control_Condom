package com.estacionamento.api.application.gate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GateOperationDispatcher {

    private final GateDispatchWorker worker;

    public GateOperationDispatcher(GateDispatchWorker worker) {
        this.worker = worker;
    }

    @Scheduled(fixedDelayString = "${app.gate.operation-poll-delay-ms:2000}")
    public void dispatch() {
        worker.dispatchPending();
    }
}
