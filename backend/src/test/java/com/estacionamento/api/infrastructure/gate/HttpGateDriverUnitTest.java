package com.estacionamento.api.infrastructure.gate;
import com.estacionamento.api.config.properties.GateProperties;
import com.estacionamento.api.domain.movement.MovementDirection;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.net.http.*;
import java.net.ConnectException;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class HttpGateDriverUnitTest {
    private final HttpClient client=mock(HttpClient.class);
    private HttpGateDriver driver() { return new HttpGateDriver(new GateProperties(true,"HTTP","https://controller.invalid/entry","","","","",Duration.ofSeconds(1),3,Duration.ofSeconds(1),false,1000),new ObjectMapper(),client); }
    private GateOpenCommand command() { return new GateOpenCommand("stable-operation-key",MovementDirection.ENTRY,"ABC1D23",1000); }
    @Test void timeoutIsUncertainAndMustNotBeBlindlyRepeated() throws Exception { when(client.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenThrow(new HttpTimeoutException("test timeout"));assertThat(driver().open(command()).uncertain()).isTrue(); }
    @Test void connectionFailureIsKnownBeforeDelivery() throws Exception { when(client.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenThrow(new ConnectException("not connected"));var result=driver().open(command());assertThat(result.success()).isFalse();assertThat(result.uncertain()).isFalse(); }
    @Test void serverFailureIsUncertain() throws Exception { HttpResponse<String> response=mock(HttpResponse.class);when(response.statusCode()).thenReturn(500);when(client.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);assertThat(driver().open(command()).uncertain()).isTrue(); }
    @Test void acceptedCommandCarriesStableIdempotencyIdentifier() throws Exception { HttpResponse<String> response=mock(HttpResponse.class);when(response.statusCode()).thenReturn(202);when(client.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);assertThat(driver().open(command()).success()).isTrue();verify(client).send(argThat(request->request.headers().firstValue("X-Gate-Operation-Id").orElse("").equals("stable-operation-key")),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()); }
}
