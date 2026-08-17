package elite.intel.ai.mouth.edge;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/** Builds escaped SSML and the text messages used by the Edge Read Aloud WebSocket. */
final class EdgeSsml {
    private static final DateTimeFormatter EDGE_TIMESTAMP = DateTimeFormatter.ofPattern(
            "EEE MMM dd yyyy HH:mm:ss 'GMT+0000 (Coordinated Universal Time)'", Locale.US)
            .withZone(ZoneOffset.UTC);

    private EdgeSsml() {
    }

    static String build(String text, String voiceName, String rate, String volume, String pitch) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Edge SSML text must not be blank");
        }
        return "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>"
                + "<voice name='" + escape(voiceName) + "'>"
                + "<prosody pitch='" + pitch + "' rate='" + rate + "' volume='" + volume + "'>"
                + escape(text) + "</prosody></voice></speak>";
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> {
            if (isUnsupportedControl(codePoint)) {
                escaped.append(' ');
                return;
            }
            switch (codePoint) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '\"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&apos;");
                default -> escaped.appendCodePoint(codePoint);
            }
        });
        return escaped.toString();
    }

    static String rate(float applicationSpeed) {
        int percent = Math.round(applicationSpeed * 100f);
        return signed(percent, "%");
    }

    static String volume(int applicationVolume) {
        int bounded = Math.max(0, Math.min(100, applicationVolume));
        return signed(bounded - 100, "%");
    }

    static String pitch(int hertz) {
        return signed(hertz, "Hz");
    }

    static String protocolVoiceName(String shortName) {
        if (shortName == null || !shortName.endsWith("Neural")) {
            throw new IllegalArgumentException("Invalid Edge voice short name: " + shortName);
        }
        int voiceSeparator = shortName.lastIndexOf('-');
        if (voiceSeparator <= 0 || voiceSeparator == shortName.length() - 1) {
            throw new IllegalArgumentException("Invalid Edge voice short name: " + shortName);
        }
        String locale = shortName.substring(0, voiceSeparator);
        String name = shortName.substring(voiceSeparator + 1);
        return "Microsoft Server Speech Text to Speech Voice (" + locale + ", " + name + ")";
    }

    static String speechConfig(Instant now) {
        return "X-Timestamp:" + timestamp(now) + "\r\n"
                + "Content-Type:application/json; charset=utf-8\r\n"
                + "Path:speech.config\r\n\r\n"
                + "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{"
                + "\"sentenceBoundaryEnabled\":\"true\",\"wordBoundaryEnabled\":\"false\"},"
                + "\"outputFormat\":\"" + EdgeProtocolConstants.OUTPUT_FORMAT + "\"}}}}\r\n";
    }

    static String ssmlMessage(String ssml, Instant now) {
        return "X-RequestId:" + requestId() + "\r\n"
                + "Content-Type:application/ssml+xml\r\n"
                // WHY: the extra Z after Edge's JavaScript-style UTC timestamp is a required service quirk.
                + "X-Timestamp:" + timestamp(now) + "Z\r\n"
                + "Path:ssml\r\n\r\n" + ssml;
    }

    static String requestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String timestamp(Instant instant) {
        return EDGE_TIMESTAMP.format(instant);
    }

    private static String signed(int value, String suffix) {
        return (value >= 0 ? "+" : "") + value + suffix;
    }

    private static boolean isUnsupportedControl(int codePoint) {
        return codePoint <= 8 || (codePoint >= 11 && codePoint <= 12)
                || (codePoint >= 14 && codePoint <= 31);
    }
}
