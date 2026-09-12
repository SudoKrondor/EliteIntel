package elite.intel.ai.mouth.supertonic;

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
 * Nothing here names a voice. Each test derives what it expects from {@code values()}, and the assertion that
 * a long run of draws returns the whole cast minus the excluded voices is what proves the draw filters nobody
 * out by gender - which is what the named assertions this replaced were reaching for.
 */
class SupertonicRadioVoiceTest {

    /**
     * Stands in for the commander's own ship voice; any member of the cast will do.
     */
    private static final SupertonicVoices OWN = SupertonicVoices.values()[0];

    /**
     * Enough draws to exhaust a cast of any size many times over: a voice is missed with probability
     * {@code (1 - 1/n)^200n}, which is vanishing for every n.
     */
    private static final int DRAWS = 200 * SupertonicVoices.values().length;

    @Test
    void neverDrawsTheCommandersOwnVoice() {
        for (int i = 0; i < DRAWS; i++) {
            assertNotEquals(OWN, SupertonicVoices.randomRadioVoice(OWN.name()));
        }
    }

    @Test
    void drawsEveryOtherVoiceInTheCast() {
        Set<SupertonicVoices> drawn = draw(() -> SupertonicVoices.randomRadioVoice(OWN.name()));

        assertEquals(everyoneExcept(OWN), drawn, "every voice but the commander's");
    }

    /**
     * A voice given to a carrier's traffic control belongs to that carrier. Recognising it is the whole point
     * of assigning it, and a passing station answering in it takes that away.
     */
    @Test
    void neverDrawsAVoiceReservedForACarrier() {
        Set<SupertonicVoices> carriers = twoVoicesOtherThan(OWN);
        Set<String> reserved = carriers.stream().map(Enum::name).collect(Collectors.toSet());

        Set<SupertonicVoices> drawn = draw(() -> SupertonicVoices.randomRadioVoice(OWN.name(), reserved));

        assertTrue(Collections.disjoint(drawn, carriers), "a carrier's voice is nobody else's");
        Set<SupertonicVoices> expected = everyoneExcept(OWN);
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
        SupertonicVoices first = SupertonicVoices.radioVoiceFor("Dave Knowles", OWN.name(), Set.of());

        for (int i = 0; i < DRAWS; i++) {
            assertEquals(first, SupertonicVoices.radioVoiceFor("Dave Knowles", OWN.name(), Set.of()));
        }
        // The name as the journal spells it, not as we happen to have trimmed it.
        assertEquals(first, SupertonicVoices.radioVoiceFor("  dave knowles  ", OWN.name(), Set.of()));
    }

    @Test
    void differentSpeakersSpreadAcrossTheCast() {
        Set<SupertonicVoices> heard = EnumSet.noneOf(SupertonicVoices.class);
        for (int i = 0; i < 200; i++) {
            heard.add(SupertonicVoices.radioVoiceFor("Pilot " + i, OWN.name(), Set.of()));
        }

        assertTrue(heard.size() > SupertonicVoices.values().length / 2,
                "a wing of pirates must not collapse onto a handful of voices, was " + heard.size());
    }

    @Test
    void aNamedSpeakerNeverTakesTheCommandersOrACarriersVoice() {
        Set<SupertonicVoices> carriers = twoVoicesOtherThan(OWN);
        Set<String> reserved = carriers.stream().map(Enum::name).collect(Collectors.toSet());

        for (int i = 0; i < 200; i++) {
            SupertonicVoices drawn = SupertonicVoices.radioVoiceFor("Pilot " + i, OWN.name(), reserved);
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
        Map<String, SupertonicVoices> before = speakers.stream()
                .collect(Collectors.toMap(name -> name, name -> SupertonicVoices.radioVoiceFor(name, OWN.name(), Set.of())));

        SupertonicVoices givenToACarrier = before.get(speakers.get(0));
        Set<String> reserved = Set.of(givenToACarrier.name());
        long moved = speakers.stream()
                .filter(name -> before.get(name) != SupertonicVoices.radioVoiceFor(name, OWN.name(), reserved))
                .count();

        assertEquals(speakers.stream().filter(name -> before.get(name) == givenToACarrier).count(), moved,
                "only the speakers who had that voice should have moved");
    }

    /**
     * A transmission nobody is attributed to is a stranger, and strangers stay strangers.
     */
    @Test
    void anUnattributedTransmissionStillDrawsAtRandom() {
        Set<SupertonicVoices> drawn = draw(() -> SupertonicVoices.radioVoiceFor(null, OWN.name(), Set.of()));
        drawn.addAll(draw(() -> SupertonicVoices.radioVoiceFor("  ", OWN.name(), Set.of())));

        assertEquals(everyoneExcept(OWN), drawn);
    }

    /**
     * Reserving is best-effort: a channel with nothing left to say would be worse than a repeated voice.
     */
    @Test
    void reservingEveryVoiceStillLeavesSomeoneToSpeak() {
        Set<String> everyone = Arrays.stream(SupertonicVoices.values()).map(Enum::name).collect(Collectors.toSet());

        assertNotNull(SupertonicVoices.randomRadioVoice(OWN.name(), everyone));
    }

    @Test
    void anUnknownOwnVoiceStillLeavesTheFullCast() {
        Set<SupertonicVoices> drawn = draw(() -> SupertonicVoices.randomRadioVoice(null));

        assertEquals(EnumSet.allOf(SupertonicVoices.class), drawn);
    }

    /**
     * A commander whose stored voice name belongs to a retired engine (Kokoro, or a cast member since removed)
     * has that name stored in the database, and gets the default rather than an exception.
     */
    @Test
    void anUnknownVoiceNameFallsBackToTheDefault() {
        assertEquals(SupertonicVoices.DEFAULT_VOICE, SupertonicVoices.voiceOrDefault("BELLA"));
        assertEquals(SupertonicVoices.DEFAULT_VOICE, SupertonicVoices.voiceOrDefault(null));
    }

    private static Set<SupertonicVoices> draw(Supplier<SupertonicVoices> draw) {
        Set<SupertonicVoices> drawn = EnumSet.noneOf(SupertonicVoices.class);
        for (int i = 0; i < DRAWS; i++) {
            drawn.add(draw.get());
        }
        return drawn;
    }

    private static Set<SupertonicVoices> everyoneExcept(SupertonicVoices voice) {
        return EnumSet.complementOf(EnumSet.of(voice));
    }

    /**
     * Two stand-ins for carrier-owned voices. Both differ from the commander's, so the draw has someone left.
     */
    private static Set<SupertonicVoices> twoVoicesOtherThan(SupertonicVoices own) {
        assertTrue(SupertonicVoices.values().length >= 3,
                "the cast needs three voices for a reservation to leave anyone speaking");
        List<SupertonicVoices> others = List.copyOf(everyoneExcept(own));
        return EnumSet.of(others.get(0), others.get(1));
    }
}
