package com.estacionamento.api.application.gate;
import com.estacionamento.api.infrastructure.gate.*;
import org.springframework.stereotype.Service;
import java.util.Map;
@Service
public class GateDispatchWorker {
    private final GateDispatchTransactions transactions;
    private final Map<String,GateDriver> drivers;
    public GateDispatchWorker(GateDispatchTransactions transactions, Map<String,GateDriver> drivers) { this.transactions=transactions;this.drivers=drivers; }
    public void dispatchPending() {
        transactions.recoverExpired();
        for (int i=0;i<20;i++) {
            var next=transactions.claim();
            if (next.isEmpty()) return;
            var claim=next.get();
            GateCommandResult result;
            GateDriver driver=drivers.get(claim.driver());
            if(driver==null) result=GateCommandResult.failure("Unsupported gate driver");
            else {
                try { result=driver.open(claim.command()); }
                catch(RuntimeException exception) { result=GateCommandResult.unknown("Driver failed after claim; physical outcome is unknown"); }
                if(result==null) result=GateCommandResult.unknown("Driver did not provide an outcome");
            }
            // HTTP is deliberately outside the claim and completion transactions.
            transactions.finish(claim,result);
        }
    }
}
