package elite.intel.ai.mouth.edge;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdgeAudioStreamAssemblerTest {
    @Test
    void parsesTextAndAssemblesAudioFramesInArrivalOrder() throws Exception {
        EdgeAudioStreamAssembler assembler = new EdgeAudioStreamAssembler();

        assertFalse(assembler.acceptText("Path:turn.start\r\nX-RequestId:abc\r\n\r\n{}"));
        assembler.acceptBinary(frame("Path:audio\r\nContent-Type:audio/mpeg\r\n", new byte[]{1, 2}));
        assembler.acceptBinary(frame("Path:audio\r\nContent-Type:audio/mpeg\r\n", new byte[]{3, 4}));
        assembler.acceptBinary(frame("Path:audio\r\n", new byte[0]));
        assertTrue(assembler.acceptText("Path:turn.end\r\n\r\n{}"));

        assertArrayEquals(new byte[]{1, 2, 3, 4}, assembler.audio());
    }

    @Test
    void rejectsMalformedOrUnexpectedProtocolMessages() {
        EdgeAudioStreamAssembler assembler = new EdgeAudioStreamAssembler();

        assertThrows(EdgeProtocolException.class, () -> assembler.acceptText("no separator"));
        assertThrows(EdgeProtocolException.class,
                () -> assembler.acceptText("Path:unknown\r\n\r\n{}"));
        assertThrows(EdgeProtocolException.class, () -> assembler.acceptBinary(new byte[]{0}));
        assertThrows(EdgeProtocolException.class,
                () -> assembler.acceptBinary(frame("Path:not-audio\r\n", new byte[]{1})));
        assertThrows(EdgeProtocolException.class,
                () -> assembler.acceptBinary(frame("Path:audio\r\nContent-Type:text/plain\r\n", new byte[]{1})));
        assertThrows(EdgeProtocolException.class,
                () -> assembler.acceptBinary(frame("Path:audio\r\nContent-Type:audio/mpeg\r\n", new byte[0])));
    }

    @Test
    void binaryParserRejectsInvalidHeaderLengthsAndMissingPath() {
        assertThrows(EdgeProtocolException.class,
                () -> EdgeProtocolMessageParser.parseBinary(new byte[]{0, 10, 1, 2}));
        assertThrows(EdgeProtocolException.class,
                () -> EdgeProtocolMessageParser.parseBinary(frame("Content-Type:audio/mpeg\r\n", new byte[]{1})));
    }

    private static byte[] frame(String header, byte[] body) {
        byte[] headers = header.getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(2 + headers.length + body.length)
                .putShort((short) headers.length)
                .put(headers)
                .put(body)
                .array();
    }
}
