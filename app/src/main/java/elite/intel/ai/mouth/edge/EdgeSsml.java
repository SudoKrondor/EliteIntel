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

    /**
     * WHY: only the rate is a setting. Volume is deliberately neutral here and applied once to decoded PCM
     * through {@code AudioDeClicker.applyVolume}, the same way the other engines do it, so the two cannot
     * compound; pitch is neutral because the app's only pitch setting is Google WaveNet's, which Edge has no
     * equivalent for.
     */
    private static final String NEUTRAL_VOLUME = "+0%";
    private static final String NEUTRAL_PITCH = "+0Hz";

    static String build(String text, String voiceName, String rate) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Edge SSML text must not be blank");
        }
        return "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>"
                + "<voice name='" + escape(voiceName) + "'>"
                + "<prosody pitch='" + NEUTRAL_PITCH + "' rate='" + rate + "' volume='" + NEUTRAL_VOLUME + "'>"
                + escape(text) + "</prosody></voice></speak>";
    }

    /**
     * UTF-8 byte length of one code point once {@link #escape} has run, without building the escaped string.
     * Escaping is per code point, so a scan can accumulate this instead of re-measuring a growing prefix.
     * It has to agree with {@link #escape} exactly, and {@code EdgeSsmlTest} asserts that it does for every
     * code point in the BMP.
     */
    static int escapedByteLength(int codePoint) {
        if (isUnsupportedControl(codePoint)) {
            return 1; // replaced by a single space
        }
        return switch (codePoint) {
            case '&' -> 5;        // &amp;
            case '<', '>' -> 4;   // &lt; &gt;
            case '\"', '\'' -> 6;  // &quot; &apos;
            default -> utf8Length(codePoint);
        };
    }

    private static int utf8Length(int codePoint) {
        if (codePoint < 0x80) return 1;
        if (codePoint < 0x800) return 2;
        if (codePoint < 0x10000) return 3;
        return 4;
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
