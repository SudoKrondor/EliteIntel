package elite.intel.ai.mouth.edge;

import java.io.ByteArrayOutputStream;

/** Validates Edge WebSocket messages and assembles their MPEG audio payloads in arrival order. */
final class EdgeAudioStreamAssembler {
    private final ByteArrayOutputStream audio = new ByteArrayOutputStream();

    /**
     * @return {@code true} when the message ends the synthesis turn
     */
    boolean acceptText(String message) throws EdgeProtocolException {
        EdgeProtocolMessageParser.Message parsed = EdgeProtocolMessageParser.parseText(message);
        return switch (parsed.path()) {
            case "response", "turn.start", "audio.metadata" -> false;
            case "turn.end" -> true;
            default -> throw new EdgeProtocolException(
                    "Edge returned an unexpected text path: " + parsed.path());
        };
    }

    void acceptBinary(byte[] frame) throws EdgeProtocolException {
        EdgeProtocolMessageParser.Message parsed = EdgeProtocolMessageParser.parseBinary(frame);
        if (!"audio".equals(parsed.path())) {
            throw new EdgeProtocolException("Edge binary message path is not audio");
        }
        String contentType = parsed.headers().get("content-type");
        if (contentType == null && parsed.body().length == 0) {
            return;
        }
        if (!"audio/mpeg".equals(contentType)) {
            throw new EdgeProtocolException("Edge binary message has an unexpected Content-Type");
        }
        if (parsed.body().length == 0) {
            throw new EdgeProtocolException("Edge binary message contains no audio data");
        }
        audio.writeBytes(parsed.body());
    }

    byte[] audio() {
        return audio.toByteArray();
    }
}
