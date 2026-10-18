package com.estacionamento.api.web.realtime;

import com.estacionamento.api.application.realtime.RealtimeBroadcaster;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/realtime")
public class RealtimeController {

    private final RealtimeBroadcaster broadcaster;

    public RealtimeController(RealtimeBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('MOVEMENT_READ')")
    public SseEmitter events(@org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        return broadcaster.register(jwt);
    }
}
