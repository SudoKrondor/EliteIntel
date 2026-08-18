package elite.intel.ai.mouth.edge;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdgeSsmlTest {
    @Test
    void escapesXmlAndUnsupportedControlsWithoutDamagingUnicode() {
        String escaped = EdgeSsml.escape("A&B <tag> \"quote\" 'single'\u0001 Привіт 😀");

        assertEquals("A&amp;B &lt;tag&gt; &quot;quote&quot; &apos;single&apos;  Привіт 😀", escaped);
        assertFalse(escaped.contains("\u0001"));
    }

    @Test
    void translatesApplicationRateVolumeAndPitchToEdgeProsody() {
        assertEquals("+25%", EdgeSsml.rate(0.25f));
        assertEquals("-25%", EdgeSsml.rate(-0.25f));
        assertEquals("+0%", EdgeSsml.volume(100));
        assertEquals("-75%", EdgeSsml.volume(25));
        assertEquals("-100%", EdgeSsml.volume(-1));
        assertEquals("+0Hz", EdgeSsml.pitch(0));
        assertEquals("-7Hz", EdgeSsml.pitch(-7));
    }

    @Test
    void buildsProtocolVoiceAndFramedSsmlMessages() {
        String voice = EdgeSsml.protocolVoiceName("en-US-EmmaMultilingualNeural");
        String ssml = EdgeSsml.build("Use <this>", voice, "+10%", "+0%", "+0Hz");

        assertEquals("Microsoft Server Speech Text to Speech Voice (en-US, EmmaMultilingualNeural)", voice);
        assertTrue(ssml.contains("Use &lt;this&gt;"));
        assertTrue(ssml.contains("rate='+10%'"));
        assertTrue(EdgeSsml.speechConfig(Instant.EPOCH).contains(
                "audio-24khz-48kbitrate-mono-mp3"));
        assertTrue(EdgeSsml.speechConfig(Instant.EPOCH).contains(
                "\"sentenceBoundaryEnabled\":\"true\""));
        String message = EdgeSsml.ssmlMessage(ssml, Instant.EPOCH);
        assertTrue(message.contains("X-Timestamp:Thu Jan 01 1970 00:00:00 "
                + "GMT+0000 (Coordinated Universal Time)Z\r\n"));
        assertTrue(message.contains("Path:ssml\r\n\r\n" + ssml));
        assertThrows(IllegalArgumentException.class, () -> EdgeSsml.protocolVoiceName("bad"));
    }
}
