package com.estacionamento.api.application.gate;
import com.estacionamento.api.infrastructure.gate.*;
import com.estacionamento.api.domain.movement.MovementDirection;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
class GateDispatchWorkerTest {
    private GateDispatchTransactions.Claim claim() { return new GateDispatchTransactions.Claim(1L,"claim","HTTP",new GateOpenCommand("stable-id",MovementDirection.ENTRY,"ABC1D23",1000)); }
    @Test void driverExceptionIsRecordedAsUnknownWithoutAutomaticResend() {
        var transactions=mock(GateDispatchTransactions.class);var claim=claim();when(transactions.claim()).thenReturn(Optional.of(claim),Optional.empty());
        GateDriver driver=mock(GateDriver.class);when(driver.open(any())).thenThrow(new IllegalStateException("uncertain"));
        new GateDispatchWorker(transactions,Map.of("HTTP",driver)).dispatchPending();
        verify(driver,times(1)).open(claim.command());verify(transactions).finish(eq(claim),argThat(result->result.uncertain()));
    }
    @Test void emptyQueueDoesNotInvokePhysicalDriver() {
        var transactions=mock(GateDispatchTransactions.class);when(transactions.claim()).thenReturn(Optional.empty());var driver=mock(GateDriver.class);
        new GateDispatchWorker(transactions,Map.of("HTTP",driver)).dispatchPending();verifyNoInteractions(driver);verify(transactions).recoverExpired();
    }
}
