package com.estacionamento.api.web.filter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.*;
class WebhookPayloadLimitFilterTest {
    @Test void rejectsOversizedXmlBeforeItReachesController() throws Exception {
        var request=new MockHttpServletRequest("POST","/api/v1/integrations/hikvision/events");request.setContentType("application/xml");request.setContent(new byte[1_048_577]);
        var response=new MockHttpServletResponse();var chain=new MockFilterChain();new WebhookPayloadLimitFilter().doFilter(request,response,chain);
        assertThat(response.getStatus()).isEqualTo(413);assertThat(chain.getRequest()).isNull();
    }
    @Test void allowsBoundedXmlStream() throws Exception {
        var request=new MockHttpServletRequest("POST","/api/v1/integrations/hikvision/events");request.setContentType("application/xml");request.setContent("<event/>".getBytes());
        var response=new MockHttpServletResponse();new WebhookPayloadLimitFilter().doFilter(request,response,(incoming,outgoing)->assertThat(new String(incoming.getInputStream().readAllBytes())).isEqualTo("<event/>"));
    }
}
