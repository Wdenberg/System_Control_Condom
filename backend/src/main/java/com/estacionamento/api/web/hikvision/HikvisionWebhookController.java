package com.estacionamento.api.web.hikvision;

import com.estacionamento.api.application.hikvision.HikvisionAttachment;
import com.estacionamento.api.application.hikvision.HikvisionEventResponse;
import com.estacionamento.api.application.hikvision.HikvisionEventService;
import com.estacionamento.api.security.HikvisionWebhookSecurityService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/integrations/hikvision")
public class HikvisionWebhookController {

    private final HikvisionEventService eventService;
    private final HikvisionWebhookSecurityService webhookSecurityService;

    public HikvisionWebhookController(
            HikvisionEventService eventService,
            HikvisionWebhookSecurityService webhookSecurityService
    ) {
        this.eventService = eventService;
        this.webhookSecurityService = webhookSecurityService;
    }

    @PostMapping(
            value = "/events",
            consumes = {
                    MediaType.APPLICATION_XML_VALUE,
                    MediaType.TEXT_XML_VALUE
            }
    )
    public ResponseEntity<HikvisionEventResponse> receiveXml(
            @RequestBody String rawXml,
            @RequestHeader(
                    value = HikvisionWebhookSecurityService.TOKEN_HEADER,
                    required = false
            ) String token,
            HttpServletRequest request
    ) {
        if (token == null || token.isBlank()) throw new HikvisionWebhookUnauthorizedException("Invalid camera credentials");
        String remoteIp = request.getRemoteAddr();
        String cameraIp = extractCameraIp(rawXml);

        webhookSecurityService.validate(
                token,
                remoteIp,
                cameraIp
        );

        return ResponseEntity.ok(
                eventService.process(
                        rawXml,
                        remoteIp,
                        List.of()
                )
        );
    }

    @PostMapping(
            value = "/events",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<HikvisionEventResponse> receiveMultipart(
            MultipartHttpServletRequest request,
            @RequestHeader(
                    value = HikvisionWebhookSecurityService.TOKEN_HEADER,
                    required = false
            ) String token
    ) {
        String rawXml = extractXml(request);


        if (token == null || token.isBlank()) throw new HikvisionWebhookUnauthorizedException("Invalid camera credentials");
        String remoteIp = request.getRemoteAddr();
        String cameraIp = extractCameraIp(rawXml);

        webhookSecurityService.validate(
                token,
                remoteIp,
                cameraIp
        );

        return ResponseEntity.ok(
                eventService.process(
                        rawXml,
                        remoteIp,
                        extractAttachments(request)
                )
        );
    }

    private String extractCameraIp(String rawXml) {
        if (rawXml == null || rawXml.getBytes(StandardCharsets.UTF_8).length > 1_048_576) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, "XML exceeds 1 MB");
        try {
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();

            /*
             * Segurança: bloquear DTD/entidades externas.
             */
            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true
            );
            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false
            );
            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false
            );
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document document = factory
                    .newDocumentBuilder()
                    .parse(
                            new ByteArrayInputStream(
                                    rawXml.getBytes(StandardCharsets.UTF_8)
                            )
                    );

            var nodes = document.getElementsByTagNameNS("*", "ipAddress");

            if (nodes.getLength() == 0) {
                throw new IllegalArgumentException(
                        "Hikvision XML does not contain ipAddress"
                );
            }

            String ip = nodes.item(0).getTextContent();

            if (ip == null || ip.isBlank()) {
                throw new IllegalArgumentException(
                        "Hikvision XML contains an empty ipAddress"
                );
            }

            return ip.trim();

        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Could not extract Hikvision camera IP from XML",
                    exception
            );
        }
    }

    private String extractXml(MultipartHttpServletRequest request) {
        for (var entry : request.getFileMap().entrySet()) {
            MultipartFile file = entry.getValue();

            if (isXml(file)) {
                try {
                    return new String(
                            file.getBytes(),
                            StandardCharsets.UTF_8
                    );
                } catch (IOException exception) {
                    throw new IllegalArgumentException(
                            "Could not read Hikvision XML multipart",
                            exception
                    );
                }
            }
        }

        throw new IllegalArgumentException(
                "Hikvision multipart payload does not contain an XML part"
        );
    }

    private List<HikvisionAttachment> extractAttachments(
            MultipartHttpServletRequest request
    ) {
        List<HikvisionAttachment> attachments = new ArrayList<>();

        MultiValueMap<String, MultipartFile> files =
                request.getMultiFileMap();

        files.forEach((partName, values) ->
                values.forEach(file -> {
                    if (!isXml(file)) {
                        try {
                            attachments.add(
                                    new HikvisionAttachment(
                                            partName,
                                            file.getOriginalFilename(),
                                            file.getContentType(),
                                            file.getBytes()
                                    )
                            );
                        } catch (IOException exception) {
                            throw new IllegalArgumentException(
                                    "Could not read Hikvision image multipart",
                                    exception
                            );
                        }
                    }
                })
        );

        return List.copyOf(attachments);
    }

    private boolean isXml(MultipartFile file) {
        String contentType = file.getContentType();
        String name = file.getOriginalFilename();

        return (contentType != null
                && contentType.toLowerCase(Locale.ROOT).contains("xml"))
                || (name != null
                && name.toLowerCase(Locale.ROOT).endsWith(".xml"));
    }
}
