package com.estacionamento.api.infrastructure.hikvision;

import com.estacionamento.api.application.hikvision.HikvisionPicturePayload;
import com.estacionamento.api.application.hikvision.ParsedHikvisionAnprEvent;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class HikvisionXmlParser {

    public ParsedHikvisionAnprEvent parse(String rawXml) {
        if (rawXml == null || rawXml.isBlank()) {
            throw new HikvisionXmlParsingException("XML payload is empty");
        }

        try {
            DocumentBuilderFactory factory = secureFactory();
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(rawXml.getBytes(StandardCharsets.UTF_8)));
            document.getDocumentElement().normalize();

            Element root = document.getDocumentElement();
            String eventType = text(root, "eventType");
            if (!"ANPR".equalsIgnoreCase(eventType)) {
                throw new HikvisionXmlParsingException("Unsupported Hikvision eventType: " + eventType);
            }

            Element anpr = first(root, "ANPR");
            if (anpr == null) {
                throw new HikvisionXmlParsingException("ANPR element is missing");
            }

            List<HikvisionPicturePayload> pictures = parsePictures(anpr);

            return new ParsedHikvisionAnprEvent(
                    requiredText(root, "ipAddress"),
                    text(root, "ipv6Address"),
                    integer(root, "portNo"),
                    text(root, "protocol"),
                    text(root, "macAddress"),
                    integer(root, "channelID"),
                    requiredOffsetDateTime(root, "dateTime"),
                    integer(root, "activePostCount"),
                    requiredText(root, "eventType"),
                    text(root, "eventState"),
                    text(root, "eventDescription"),
                    text(root, "channelName"),
                    text(anpr, "country"),
                    requiredText(anpr, "licensePlate"),
                    integer(anpr, "line"),
                    text(anpr, "direction"),
                    integer(anpr, "confidenceLevel"),
                    text(anpr, "plateType"),
                    text(anpr, "plateColor"),
                    integer(anpr, "licenseBright"),
                    text(anpr, "dangmark"),
                    text(anpr, "twoWheelVehicle"),
                    text(anpr, "threeWheelVehicle"),
                    text(anpr, "plateCharBelieve"),
                    text(anpr, "vehicleType"),
                    integer(anpr, "detectDir"),
                    integer(anpr, "detectType"),
                    integer(anpr, "alarmDataType"),
                    vehicleInteger(anpr, "index"),
                    vehicleInteger(anpr, "colorDepth"),
                    vehicleText(anpr, "color"),
                    vehicleInteger(anpr, "length"),
                    vehicleInteger(anpr, "vehicleLogoRecog"),
                    vehicleInteger(anpr, "vehileSubLogoRecog"),
                    vehicleInteger(anpr, "vehileModel"),
                    text(anpr, "vehicleListName"),
                    integer(anpr, "plateSize"),
                    pictures,
                    text(anpr, "originalLicensePlate"),
                    text(anpr, "CRIndex"),
                    text(root, "UUID"),
                    integer(root, "picNum"),
                    booleanValue(root, "isDataRetransmission")
            );
        } catch (HikvisionXmlParsingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new HikvisionXmlParsingException("Invalid Hikvision XML payload", exception);
        }
    }

    private List<HikvisionPicturePayload> parsePictures(Element anpr) {
        List<HikvisionPicturePayload> pictures = new ArrayList<>();
        NodeList pictureNodes = anpr.getElementsByTagNameNS("*", "pictureInfo");
        for (int i = 0; i < pictureNodes.getLength(); i++) {
            Node node = pictureNodes.item(i);
            if (!(node instanceof Element picture)) {
                continue;
            }
            Element plateRect = first(picture, "plateRect");
            HikvisionPicturePayload.PlateRectPayload rect = plateRect == null ? null : new HikvisionPicturePayload.PlateRectPayload(
                    integer(plateRect, "X", "x"),
                    integer(plateRect, "Y", "y"),
                    integer(plateRect, "width"),
                    integer(plateRect, "height")
            );
            pictures.add(new HikvisionPicturePayload(
                    requiredText(picture, "fileName"),
                    requiredText(picture, "type"),
                    integer(picture, "dataType"),
                    integer(picture, "picRecogMode"),
                    text(picture, "absTime"),
                    text(picture, "pId"),
                    text(picture, "pictureURL"),
                    rect
            ));
        }
        return List.copyOf(pictures);
    }

    private static DocumentBuilderFactory secureFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private static String requiredText(Element parent, String localName) {
        String value = text(parent, localName);
        if (value == null || value.isBlank()) {
            throw new HikvisionXmlParsingException("Required XML field is missing: " + localName);
        }
        return value.trim();
    }

    private static String text(Element parent, String localName) {
        Element element = first(parent, localName);
        if (element == null) {
            return null;
        }
        return element.getTextContent() == null ? null : element.getTextContent().trim();
    }

    private static Element first(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS("*", localName);
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    private static Integer integer(Element parent, String... localNames) {
        for (String name : localNames) {
            String value = text(parent, name);
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                return Integer.valueOf(value.trim());
            } catch (NumberFormatException exception) {
                throw new HikvisionXmlParsingException("Invalid integer in field " + name + ": " + value, exception);
            }
        }
        return null;
    }

    private static Integer vehicleInteger(Element anpr, String localName) {
        Element vehicleInfo = first(anpr, "vehicleInfo");
        return vehicleInfo == null ? null : integer(vehicleInfo, localName);
    }

    private static String vehicleText(Element anpr, String localName) {
        Element vehicleInfo = first(anpr, "vehicleInfo");
        return vehicleInfo == null ? null : text(vehicleInfo, localName);
    }

    private static boolean booleanValue(Element parent, String localName) {
        String value = text(parent, localName);
        return value != null && Boolean.parseBoolean(value.trim());
    }

    private static OffsetDateTime requiredOffsetDateTime(Element parent, String localName) {
        String value = requiredText(parent, localName);
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw new HikvisionXmlParsingException("Invalid ISO-8601 date time in field " + localName + ": " + value, exception);
        }
    }
}
