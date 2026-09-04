package elite.intel.ai.mouth.kokoro;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The radio channel draws from the whole model - male voices and foreign accents included, because that
 * variety is what makes the galaxy sound populated - but never from the commander's own voice.
 */
class KokoroRadioVoiceTest {

    @Test
    void neverDrawsTheCommandersOwnVoice() {
        for (int i = 0; i < 500; i++) {
            assertNotEquals(KokoroVoices.BELLA, KokoroVoices.randomRadioVoice(KokoroVoices.BELLA.name()));
        }
    }

    @Test
    void drawsFromTheWholeModelIncludingMaleAndNonEnglishVoices() {
        Set<KokoroVoices> drawn = new HashSet<>();
        for (int i = 0; i < 5_000; i++) {
            drawn.add(KokoroVoices.randomRadioVoice(KokoroVoices.BELLA.name()));
        }
        assertEquals(KokoroVoices.values().length - 1, drawn.size(), "every voice but the commander's");
        assertTrue(drawn.stream().anyMatch(KokoroVoices::isMale));
        assertTrue(drawn.contains(KokoroVoices.JA_KUMO));
    }

    /**
     * A voice given to a carrier's traffic control belongs to that carrier. Recognising it is the whole point
     * of assigning it, and a passing station answering in it takes that away.
     */
    @Test
    void neverDrawsAVoiceReservedForACarrier() {
        Set<String> reserved = Set.of(KokoroVoices.JA_KUMO.name(), KokoroVoices.ADAM.name());
        Set<KokoroVoices> drawn = new HashSet<>();
        for (int i = 0; i < 5_000; i++) {
            drawn.add(KokoroVoices.randomRadioVoice(KokoroVoices.BELLA.name(), reserved));
        }
        assertFalse(drawn.contains(KokoroVoices.JA_KUMO));
        assertFalse(drawn.contains(KokoroVoices.ADAM));
        assertFalse(drawn.contains(KokoroVoices.BELLA));
        assertEquals(KokoroVoices.values().length - 3, drawn.size(), "everyone else still speaks");
    }

    /**
     * Reserving is best-effort: a channel with nothing left to say would be worse than a repeated voice.
     */
    @Test
    void reservingEveryVoiceStillLeavesSomeoneToSpeak() {
        Set<String> everyone = Arrays.stream(KokoroVoices.values()).map(Enum::name).collect(Collectors.toSet());
        assertNotNull(KokoroVoices.randomRadioVoice(KokoroVoices.BELLA.name(), everyone));
    }

    @Test
    void anUnknownOwnVoiceStillLeavesTheFullCast() {
        Set<KokoroVoices> drawn = new HashSet<>(IntStream.range(0, 5_000)
                .mapToObj(i -> KokoroVoices.randomRadioVoice(null))
                .toList());
        assertEquals(KokoroVoices.values().length, drawn.size());
    }
}
