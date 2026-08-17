package elite.intel.ai.mouth.edge;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Parses Edge's header-prefixed text messages and binary audio frames. */
final class EdgeProtocolMessageParser {
    record Message(Map<String, String> headers, byte[] body) {
        String path() {
            return headers.get("path");
        }
    }

    private EdgeProtocolMessageParser() {
    }

    static Message parseText(String message) throws EdgeProtocolException {
        if (message == null) {
            throw new EdgeProtocolException("Edge returned a null text message");
        }
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        int separator = indexOf(bytes, new byte[]{'\r', '\n', '\r', '\n'}, 0);
        if (separator < 0) {
            throw new EdgeProtocolException("Edge text message has no header separator");
        }
        Map<String, String> headers = parseHeaders(bytes, 0, separator);
        return new Message(headers, Arrays.copyOfRange(bytes, separator + 4, bytes.length));
    }

    static Message parseBinary(byte[] frame) throws EdgeProtocolException {
        if (frame == null || frame.length < 2) {
            throw new EdgeProtocolException("Edge binary message is missing its header length");
        }
        int headerLength = Short.toUnsignedInt(ByteBuffer.wrap(frame, 0, 2).getShort());
        int bodyStart = 2 + headerLength;
        if (headerLength == 0 || bodyStart > frame.length) {
            throw new EdgeProtocolException("Edge binary message has an invalid header length");
        }
        Map<String, String> headers = parseHeaders(frame, 2, bodyStart);
        return new Message(headers, Arrays.copyOfRange(frame, bodyStart, frame.length));
    }

    private static Map<String, String> parseHeaders(byte[] bytes, int start, int end)
            throws EdgeProtocolException {
        String raw = new String(bytes, start, end - start, StandardCharsets.UTF_8);
        Map<String, String> headers = new LinkedHashMap<>();
        for (String line : raw.split("\\r\\n")) {
            if (line.isBlank()) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                throw new EdgeProtocolException("Malformed Edge protocol header");
            }
            headers.put(line.substring(0, colon).toLowerCase(Locale.ROOT), line.substring(colon + 1));
        }
        if (!headers.containsKey("path")) {
            throw new EdgeProtocolException("Edge protocol message is missing Path");
        }
        return Map.copyOf(headers);
    }

    private static int indexOf(byte[] haystack, byte[] needle, int start) {
        for (int i = start; i <= haystack.length - needle.length; i++) {
            boolean match = true;
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }
}
