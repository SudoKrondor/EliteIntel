package elite.intel.ai.mouth.kokoro;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The radio channel draws from the whole cast - male voices and other accents included, because that variety
 * is what makes the galaxy sound populated - but never the commander's own voice, and never one reserved for
 * a carrier.
 * <p>
 * The cast is curated by hand: a voice that breaks immersion is removed from {@link KokoroVoices}. So nothing
 * here names a voice. Each test derives what it expects from {@code values()}, and the assertion that a long
 * run of draws returns the whole cast minus the excluded voices is what proves the draw filters nobody out by
 * gender or by language - which is what the named assertions this replaced were reaching for.
 */
class KokoroRadioVoiceTest {

    /**
     * Stands in for the commander's own ship voice; any member of the cast will do.
     */
    private static final KokoroVoices OWN = KokoroVoices.values()[0];

    /**
     * Enough draws to exhaust a cast of any size many times over: a voice is missed with probability
     * {@code (1 - 1/n)^200n}, which is vanishing for every n.
     */
    private static final int DRAWS = 200 * KokoroVoices.values().length;

    @Test
    void neverDrawsTheCommandersOwnVoice() {
        for (int i = 0; i < DRAWS; i++) {
            assertNotEquals(OWN, KokoroVoices.randomRadioVoice(OWN.name()));
        }
    }

    @Test
    void drawsEveryOtherVoiceInTheCast() {
        Set<KokoroVoices> drawn = draw(() -> KokoroVoices.randomRadioVoice(OWN.name()));

        assertEquals(everyoneExcept(OWN), drawn, "every voice but the commander's");
    }

    /**
     * A voice given to a carrier's traffic control belongs to that carrier. Recognising it is the whole point
     * of assigning it, and a passing station answering in it takes that away.
     */
    @Test
    void neverDrawsAVoiceReservedForACarrier() {
        Set<KokoroVoices> carriers = twoVoicesOtherThan(OWN);
        Set<String> reserved = carriers.stream().map(Enum::name).collect(Collectors.toSet());

        Set<KokoroVoices> drawn = draw(() -> KokoroVoices.randomRadioVoice(OWN.name(), reserved));

        assertTrue(Collections.disjoint(drawn, carriers), "a carrier's voice is nobody else's");
        Set<KokoroVoices> expected = everyoneExcept(OWN);
        expected.removeAll(carriers);
        assertEquals(expected, drawn, "everyone else still speaks");
    }

    // -- one speaker, one voice ------------------------------------------------

    /**
     * The complaint this answers: a pirate is named on every line they transmit, so a fresh draw per
     * transmission made one attacker sound like several, which mid-fight reads as several attackers.
     */
    @Test
    void aNamedSpeakerKeepsOneVoice() {
        KokoroVoices first = KokoroVoices.radioVoiceFor("Dave Knowles", OWN.name(), Set.of());

        for (int i = 0; i < DRAWS; i++) {
            assertEquals(first, KokoroVoices.radioVoiceFor("Dave Knowles", OWN.name(), Set.of()));
        }
        // The name as the journal spells it, not as we happen to have trimmed it.
        assertEquals(first, KokoroVoices.radioVoiceFor("  dave knowles  ", OWN.name(), Set.of()));
    }

    @Test
    void differentSpeakersSpreadAcrossTheCast() {
        Set<KokoroVoices> heard = EnumSet.noneOf(KokoroVoices.class);
        for (int i = 0; i < 200; i++) {
            heard.add(KokoroVoices.radioVoiceFor("Pilot " + i, OWN.name(), Set.of()));
        }

        assertTrue(heard.size() > KokoroVoices.values().length / 2,
                "a wing of pirates must not collapse onto a handful of voices, was " + heard.size());
    }

    @Test
    void aNamedSpeakerNeverTakesTheCommandersOrACarriersVoice() {
        Set<KokoroVoices> carriers = twoVoicesOtherThan(OWN);
        Set<String> reserved = carriers.stream().map(Enum::name).collect(Collectors.toSet());

        for (int i = 0; i < 200; i++) {
            KokoroVoices drawn = KokoroVoices.radioVoiceFor("Pilot " + i, OWN.name(), reserved);
            assertNotEquals(OWN, drawn);
            assertFalse(carriers.contains(drawn), "a carrier's voice is nobody else's");
        }
    }

    /**
     * Giving a carrier a voice mid-session must not re-cast everyone else: the walk moves only the speakers
     * that actually landed on the newly reserved voice.
     */
    @Test
    void reservingOneVoiceLeavesTheOtherSpeakersWhereTheyWere() {
        List<String> speakers = IntStream.range(0, 200).mapToObj(i -> "Pilot " + i).toList();
        Map<String, KokoroVoices> before = speakers.stream()
                .collect(Collectors.toMap(name -> name, name -> KokoroVoices.radioVoiceFor(name, OWN.name(), Set.of())));

        KokoroVoices givenToACarrier = before.get(speakers.get(0));
        Set<String> reserved = Set.of(givenToACarrier.name());
        long moved = speakers.stream()
                .filter(name -> before.get(name) != KokoroVoices.radioVoiceFor(name, OWN.name(), reserved))
                .count();

        assertEquals(speakers.stream().filter(name -> before.get(name) == givenToACarrier).count(), moved,
                "only the speakers who had that voice should have moved");
    }

    /**
     * A transmission nobody is attributed to is a stranger, and strangers stay strangers.
     */
    @Test
    void anUnattributedTransmissionStillDrawsAtRandom() {
        Set<KokoroVoices> drawn = draw(() -> KokoroVoices.radioVoiceFor(null, OWN.name(), Set.of()));
        drawn.addAll(draw(() -> KokoroVoices.radioVoiceFor("  ", OWN.name(), Set.of())));

        assertEquals(everyoneExcept(OWN), drawn);
    }

    /**
     * Reserving is best-effort: a channel with nothing left to say would be worse than a repeated voice.
     */
    @Test
    void reservingEveryVoiceStillLeavesSomeoneToSpeak() {
        Set<String> everyone = Arrays.stream(KokoroVoices.values()).map(Enum::name).collect(Collectors.toSet());

        assertNotNull(KokoroVoices.randomRadioVoice(OWN.name(), everyone));
    }

    @Test
    void anUnknownOwnVoiceStillLeavesTheFullCast() {
        Set<KokoroVoices> drawn = draw(() -> KokoroVoices.randomRadioVoice(null));

        assertEquals(EnumSet.allOf(KokoroVoices.class), drawn);
    }

    /**
     * The other side of curating the cast: a commander who was already using a voice that has since been
     * removed has that name stored in the database, and gets the default rather than an exception.
     */
    @Test
    void aVoiceThatHasLeftTheCastFallsBackToTheDefault() {
        assertEquals(KokoroVoices.DEFAULT_VOICE, KokoroVoices.voiceOrDefault("ZH_YUNYANG"));
        assertEquals(KokoroVoices.DEFAULT_VOICE, KokoroVoices.voiceOrDefault(null));
    }

    private static Set<KokoroVoices> draw(Supplier<KokoroVoices> draw) {
        Set<KokoroVoices> drawn = EnumSet.noneOf(KokoroVoices.class);
        for (int i = 0; i < DRAWS; i++) {
            drawn.add(draw.get());
        }
        return drawn;
    }

    private static Set<KokoroVoices> everyoneExcept(KokoroVoices voice) {
        return EnumSet.complementOf(EnumSet.of(voice));
    }

    /**
     * Two stand-ins for carrier-owned voices. Both differ from the commander's, so the draw has someone left.
     */
    private static Set<KokoroVoices> twoVoicesOtherThan(KokoroVoices own) {
        assertTrue(KokoroVoices.values().length >= 3,
                "the cast needs three voices for a reservation to leave anyone speaking");
        List<KokoroVoices> others = List.copyOf(everyoneExcept(own));
        return EnumSet.of(others.get(0), others.get(1));
    }
}
