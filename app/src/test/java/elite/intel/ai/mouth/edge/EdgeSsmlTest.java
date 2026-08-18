package elite.intel.ai.mouth.edge;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EdgeSsmlTest {
    @Test
    void escapesXmlAndUnsupportedControlsWithoutDamagingUnicode() {
        String escaped = EdgeSsml.escape("A&B <tag> \"quote\" 'single'\u0001 Привіт 😀");

        assertEquals("A&amp;B &lt;tag&gt; &quot;quote&quot; &apos;single&apos;  Привіт 😀", escaped);
        assertFalse(escaped.contains("\u0001"));
    }

    @Test
    void translatesApplicationSpeedToEdgeProsodyRate() {
        assertEquals("+25%", EdgeSsml.rate(0.25f));
        assertEquals("-25%", EdgeSsml.rate(-0.25f));
        assertEquals("+0%", EdgeSsml.rate(0f));
    }

    /**
     * Volume is applied to decoded PCM instead, and Edge has no equivalent of the Google WaveNet pitch.
     */
    @Test
    void prosodyVolumeAndPitchAreNeutral() {
        String ssml = EdgeSsml.build("Anything", "voice", "+10%");

        assertTrue(ssml.contains("volume='+0%'"));
        assertTrue(ssml.contains("pitch='+0Hz'"));
    }

    /**
     * {@code EdgeSentenceSplitter} measures text against Edge's byte cap one code point at a time, so the cost
     * function has to agree with the escaping itself for every character a commander could ever be told.
     */
    @Test
    void escapedByteLengthAgreesWithEscapingForEveryCodePointInTheBmp() {
        for (int codePoint = 0; codePoint <= 0xFFFF; codePoint++) {
            if (Character.isSurrogate((char) codePoint)) {
                continue; // not a code point on its own
            }
            assertEscapedLengthMatches(codePoint);
        }
        for (int codePoint : new int[]{0x10000, 0x1F600, 0x10FFFF}) {
            assertEscapedLengthMatches(codePoint);
        }
    }

    private static void assertEscapedLengthMatches(int codePoint) {
        int measured = EdgeSsml.escape(new String(Character.toChars(codePoint)))
                .getBytes(StandardCharsets.UTF_8).length;
        assertEquals(measured, EdgeSsml.escapedByteLength(codePoint),
                "escapedByteLength disagrees with escape() for U+" + Integer.toHexString(codePoint));
    }

    @Test
    void buildsProtocolVoiceAndFramedSsmlMessages() {
        String voice = EdgeSsml.protocolVoiceName("en-US-EmmaMultilingualNeural");
        String ssml = EdgeSsml.build("Use <this>", voice, "+10%");

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
