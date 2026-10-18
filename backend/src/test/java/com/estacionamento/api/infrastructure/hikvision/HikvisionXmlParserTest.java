package com.estacionamento.api.infrastructure.hikvision;

import com.estacionamento.api.application.hikvision.ParsedHikvisionAnprEvent;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HikvisionXmlParserTest {

    private final HikvisionXmlParser parser = new HikvisionXmlParser();

    @Test
    void shouldParseTheRealAnprXmlFixture() throws Exception {
        String xml = Files.readString(Path.of("src/test/resources/hikvision/anpr-entry.xml"));

        ParsedHikvisionAnprEvent event = parser.parse(xml);

        assertThat(event.sourceIpAddress()).isEqualTo("192.168.15.43");
        assertThat(event.eventType()).isEqualTo("ANPR");
        assertThat(event.plate()).isEqualTo("SOO3H61");
        assertThat(event.confidenceLevel()).isEqualTo(91);
        assertThat(event.captureDirection()).isEqualTo("forward");
        assertThat(event.vehicleType()).isEqualTo("SUVMPV");
        assertThat(event.vehicleColor()).isEqualTo("white");
        assertThat(event.pictures()).hasSize(3);
        assertThat(event.pictures().get(2).plateRect().x()).isEqualTo(512);
        assertThat(event.uuid()).isNotBlank();
        assertThat(event.dataRetransmission()).isFalse();
    }

    @Test
    void shouldRejectNonAnprEvent() {
        String xml = "<EventNotificationAlert xmlns=\"http://www.hikvision.com/ver20/XMLSchema\"><eventType>Motion</eventType></EventNotificationAlert>";

        assertThatThrownBy(() -> parser.parse(xml))
                .isInstanceOf(HikvisionXmlParsingException.class)
                .hasMessageContaining("Unsupported Hikvision eventType");
    }

    @Test
    void shouldRejectXmlWithDoctype() {
        String xml = "<?xml version=\"1.0\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><EventNotificationAlert xmlns=\"http://www.hikvision.com/ver20/XMLSchema\"><eventType>ANPR</eventType><ANPR><ipAddress>192.168.15.43</ipAddress><dateTime>2026-09-11T14:34:11-03:00</dateTime><licensePlate>&xxe;</licensePlate></ANPR></EventNotificationAlert>";

        assertThatThrownBy(() -> parser.parse(xml))
                .isInstanceOf(HikvisionXmlParsingException.class);
    }
}
