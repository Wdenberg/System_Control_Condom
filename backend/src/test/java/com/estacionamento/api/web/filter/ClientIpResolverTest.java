package com.estacionamento.api.web.filter;
import com.estacionamento.api.config.properties.SecurityHardeningProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.assertj.core.api.Assertions.*;
class ClientIpResolverTest {
    private final SecurityHardeningProperties properties=new SecurityHardeningProperties("https://garage.invalid",true,true,true,10,120,false);
    @Test void ignoresForwardedIpFromUntrustedPeer() {var request=new MockHttpServletRequest();request.setRemoteAddr("203.0.113.10");request.addHeader("X-Forwarded-For","192.168.15.43");assertThat(new ClientIpResolver(properties,"172.31.0.2").resolve(request)).isEqualTo("203.0.113.10");}
    @Test void acceptsSanitizedClientIpOnlyFromConfiguredProxy() {var request=new MockHttpServletRequest();request.setRemoteAddr("172.31.0.2");request.addHeader("X-Forwarded-For","203.0.113.10");assertThat(new ClientIpResolver(properties,"172.31.0.2").resolve(request)).isEqualTo("203.0.113.10");}
}
