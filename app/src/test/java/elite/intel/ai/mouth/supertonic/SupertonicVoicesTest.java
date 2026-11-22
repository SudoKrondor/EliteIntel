package elite.intel.ai.mouth.supertonic;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pins the cast to what the model actually produces. The ship voice's sex drives how VEGA refers to itself,
 * so a flag that disagrees with the voice misgenders every line; and a sid picks a speaker by its position in
 * {@code voice.bin}, so the enum and the shipped file must agree on how many speakers there are.
 */
class SupertonicVoicesTest {

    private static final String VOICE_BIN = "distribution/tts/sherpa-onnx-supertonic-3-tts-int8-2026-05-11/voice.bin";

    @Test
    void theFlagAndTheDescriptionAgree() {
        for (SupertonicVoices voice : SupertonicVoices.values()) {
            assertEquals(voice.isMale() ? "Male" : "Female", voice.getDescription(), voice.name());
        }
    }

    /**
     * The presets keep the upstream speaker indices. Measured: sids 0-4 speak at 165-233 Hz, sids 5-9 at
     * 84-140 Hz, so F1-F5 are 0-4 and M1-M5 are 5-9.
     */
    @Test
    void thePresetsSitAtTheirUpstreamIndices() {
        String[] presets = {"F1", "F2", "F3", "F4", "F5", "M1", "M2", "M3", "M4", "M5"};
        for (int sid = 0; sid < presets.length; sid++) {
            SupertonicVoices voice = SupertonicVoices.valueOf(presets[sid]);
            assertEquals(sid, voice.getSid(), voice.name());
            assertEquals(presets[sid].startsWith("M"), voice.isMale(), voice + " sid " + sid);
        }
    }

    @Test
    void everyVoiceHasItsOwnSpeakerIndex() {
        Map<Integer, List<SupertonicVoices>> bySid = Arrays.stream(SupertonicVoices.values())
                .collect(Collectors.groupingBy(SupertonicVoices::getSid));
        bySid.forEach((sid, voices) -> {
            assertTrue(sid >= 0, voices + " has a negative sid");
            assertEquals(1, voices.size(), "sid " + sid + " is shared by " + voices);
        });
    }

    /**
     * {@code voice.bin} is packed from this enum (pack_release.py in the supertonic-voices project), one entry per
     * sid up to the highest. A file with fewer entries fails on the missing sids; a file with more means the enum
     * dropped a voice without the file being repacked, and every sid past the change plays the wrong voice.
     */
    @Test
    void theShippedVoiceFileHoldsExactlyTheEnumsSpeakers() throws IOException {
        Path bin = locate(VOICE_BIN);
        assumeTrue(bin != null, VOICE_BIN + " not found from " + Path.of("").toAbsolutePath());
        long speakers;
        try (InputStream in = Files.newInputStream(bin)) {
            byte[] header = in.readNBytes(48);
            assumeTrue(header.length == 48 && header[0] != 'v', VOICE_BIN + " is an LFS pointer; run git lfs pull");
            ByteBuffer buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            speakers = buf.getLong(0);
            assertEquals(speakers, buf.getLong(24), "voice.bin header disagrees with itself");
        }
        int highestSid = Arrays.stream(SupertonicVoices.values()).mapToInt(SupertonicVoices::getSid).max().orElseThrow();
        assertEquals(highestSid + 1, speakers, "voice.bin speakers vs the enum's highest sid + 1; repack voice.bin");
    }

    @Test
    void theShipDefaultIsAFemaleVoiceLikeEveryOtherEngine() {
        assertFalse(SupertonicVoices.DEFAULT_VOICE.isMale());
        assertSame(SupertonicVoices.DEFAULT_VOICE, SupertonicVoices.voiceOrDefault(null));
        assertSame(SupertonicVoices.DEFAULT_VOICE, SupertonicVoices.voiceOrDefault("SANTA"), "an unknown name collapses");
        assertSame(SupertonicVoices.M3, SupertonicVoices.voiceOrDefault("M3"));
        assertSame(SupertonicVoices.GEORGE, SupertonicVoices.voiceOrDefault("GEORGE"), "a pre-V1.2 voice name carries over");
    }

    /**
     * The commander picks a voice by its display name, so two speakers sharing one would be indistinguishable
     * in the list, and a display name that is just the constant ("F1") is a model index, not a name.
     */
    @Test
    void everySpeakerHasItsOwnHumanName() {
        Map<String, List<SupertonicVoices>> byName = Arrays.stream(SupertonicVoices.values())
                .collect(Collectors.groupingBy(SupertonicVoices::getDisplayName));
        byName.forEach((name, voices) -> assertEquals(1, voices.size(), "\"" + name + "\" is shown for " + voices));
        for (SupertonicVoices voice : SupertonicVoices.values()) {
            assertNotEquals(voice.name(), voice.getDisplayName(), voice + " shows its model index instead of a name");
        }
    }

    /**
     * Gradle runs tests from the module directory, an IDE sometimes from the project root.
     */
    private static Path locate(String fromRoot) {
        for (Path candidate : new Path[]{Path.of(fromRoot), Path.of("..").resolve(fromRoot)}) {
            if (Files.isRegularFile(candidate)) return candidate;
        }
        return null;
    }
}
