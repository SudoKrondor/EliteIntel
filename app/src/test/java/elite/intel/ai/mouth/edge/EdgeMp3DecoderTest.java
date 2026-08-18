package elite.intel.ai.mouth.edge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdgeMp3DecoderTest {
    private static final int INCOMPLETE_TRAILING_BYTES = 15;

    // First several frames of Espressif's 24 kHz mono test sample, "Furious Freak" by Kevin MacLeod,
    // CC BY 3.0: https://dl.espressif.com/dl/audio/ff-16b-1c-24000hz.mp3
    private static final String SAMPLE = """
            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
            iABmqAAx0A=
            """;

    @Test
    void decodesUpstreamFormatToLittleEndianPcm16() throws Exception {
        byte[] pcm = new EdgeMp3Decoder().decode(completeSample());

        assertTrue(pcm.length >= 2_304);
        assertEquals(0, pcm.length % 2);
    }

    @Test
    void rejectsEmptyAndMalformedAudio() {
        EdgeMp3Decoder decoder = new EdgeMp3Decoder();
        assertThrows(IOException.class, () -> decoder.decode(new byte[0]));
        assertThrows(IOException.class, () -> decoder.decode(new byte[]{1, 2, 3, 4}));
        byte[] sample = completeSample();
        assertThrows(IOException.class, () -> decoder.decode(Arrays.copyOf(sample, sample.length - 37)));
    }

    private static byte[] completeSample() {
        byte[] sample = Base64.getMimeDecoder().decode(SAMPLE);
        return Arrays.copyOf(sample, sample.length - INCOMPLETE_TRAILING_BYTES);
    }
}
