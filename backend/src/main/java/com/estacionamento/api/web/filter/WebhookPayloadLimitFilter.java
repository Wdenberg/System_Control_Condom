package com.estacionamento.api.web.filter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class WebhookPayloadLimitFilter extends OncePerRequestFilter {
    private static final long MAX_XML_BYTES=1_048_576;
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String type=request.getContentType();
        return !"/api/v1/integrations/hikvision/events".equals(request.getRequestURI())
                || type==null || !type.toLowerCase(java.util.Locale.ROOT).contains("xml");
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws IOException,ServletException {
        if(request.getContentLengthLong()>MAX_XML_BYTES) {response.sendError(413);return;}
        chain.doFilter(new HttpServletRequestWrapper(request) {
            private ServletInputStream limited;
            @Override public ServletInputStream getInputStream() throws IOException {
                if(limited!=null)return limited;
                var delegate=super.getInputStream();
                limited=new ServletInputStream() {
                    private long consumed;
                    private void count(int bytes) {if(bytes>0 && (consumed+=bytes)>MAX_XML_BYTES)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"XML exceeds 1 MB");}
                    @Override public int read() throws IOException {int value=delegate.read();if(value>=0)count(1);return value;}
                    @Override public int read(byte[] bytes,int offset,int length) throws IOException {int value=delegate.read(bytes,offset,(int)Math.min(length,MAX_XML_BYTES-consumed+1));count(value);return value;}
                    @Override public boolean isFinished(){return delegate.isFinished();}
                    @Override public boolean isReady(){return delegate.isReady();}
                    @Override public void setReadListener(ReadListener listener){delegate.setReadListener(listener);}
                };
                return limited;
            }
            @Override public BufferedReader getReader() throws IOException {return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
        },response);
    }
}
