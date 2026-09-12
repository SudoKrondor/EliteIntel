package elite.intel.ai.mouth.sherpa;

import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.ai.mouth.supertonic.SupertonicVoices;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The radio channel draws from the whole cast - male voices and other accents included, because that variety
 * is what makes the galaxy sound populated - but never the commander's own voice, and never one reserved for
 * a carrier.
 * <p>
 * Nothing here names a voice. Each test derives what it expects from the cast, and the assertion that a long
 * run of draws returns the whole cast minus the excluded voices is what proves the draw filters nobody out by
 * gender or by language. Every property is checked through each engine's own entry points, so the enums'
 * delegation to {@link RadioVoiceDraw} is under test as well as the draw itself.
 */
class RadioVoiceDrawTest {

    /**
     * One engine's cast, addressed by voice name so the same tests run over any enum.
     */
    record Cast(String engine, List<String> voices, String fallback,
                RandomDraw random, SpeakerDraw forSpeaker) {

        interface RandomDraw {
            String draw(String own, Set<String> reserved);
        }

        interface SpeakerDraw {
            String draw(String speaker, String own, Set<String> reserved);
        }

        @Override
        public String toString() {
            return engine;
        }

        /**
         * Stands in for the commander's own ship voice; any member of the cast will do.
         */
        String own() {
            return voices.getFirst();
        }

        /**
         * Enough draws to exhaust a cast of any size many times over: a voice is missed with probability
         * {@code (1 - 1/n)^200n}, which is vanishing for every n.
         */
        int draws() {
            return 200 * voices.size();
        }

        Set<String> everyoneExcept(String... excluded) {
            Set<String> everyone = new HashSet<>(voices);
            everyone.removeAll(Set.of(excluded));
            return everyone;
        }

        /**
         * Two stand-ins for carrier-owned voices. Both differ from the commander's, so the draw has someone left.
         */
        Set<String> twoVoicesOtherThanOwn() {
            assertTrue(voices.size() >= 3, "the cast needs three voices for a reservation to leave anyone speaking");
            List<String> others = List.copyOf(everyoneExcept(own()));
            return Set.of(others.get(0), others.get(1));
        }

        Set<String> drawMany(Supplier<String> draw) {
            Set<String> drawn = new HashSet<>();
            for (int i = 0; i < draws(); i++) {
                drawn.add(draw.get());
            }
            return drawn;
        }
    }

    static Stream<Cast> casts() {
        return Stream.of(
                new Cast("Kokoro",
                        Arrays.stream(KokoroVoices.values()).map(Enum::name).toList(),
                        KokoroVoices.DEFAULT_VOICE.name(),
                        (own, reserved) -> KokoroVoices.randomRadioVoice(own, reserved).name(),
                        (speaker, own, reserved) -> KokoroVoices.radioVoiceFor(speaker, own, reserved).name()),
                new Cast("Supertonic",
                        Arrays.stream(SupertonicVoices.values()).map(Enum::name).toList(),
                        SupertonicVoices.DEFAULT_VOICE.name(),
                        (own, reserved) -> SupertonicVoices.randomRadioVoice(own, reserved).name(),
                        (speaker, own, reserved) -> SupertonicVoices.radioVoiceFor(speaker, own, reserved).name()));
    }

    @ParameterizedTest
    @MethodSource("casts")
    void neverDrawsTheCommandersOwnVoice(Cast cast) {
        for (int i = 0; i < cast.draws(); i++) {
            assertNotEquals(cast.own(), cast.random().draw(cast.own(), Set.of()));
        }
    }

    @ParameterizedTest
    @MethodSource("casts")
    void drawsEveryOtherVoiceInTheCast(Cast cast) {
        Set<String> drawn = cast.drawMany(() -> cast.random().draw(cast.own(), Set.of()));

        assertEquals(cast.everyoneExcept(cast.own()), drawn, "every voice but the commander's");
    }

    /**
     * A voice given to a carrier's traffic control belongs to that carrier. Recognising it is the whole point
     * of assigning it, and a passing station answering in it takes that away.
     */
    @ParameterizedTest
    @MethodSource("casts")
    void neverDrawsAVoiceReservedForACarrier(Cast cast) {
        Set<String> carriers = cast.twoVoicesOtherThanOwn();

        Set<String> drawn = cast.drawMany(() -> cast.random().draw(cast.own(), carriers));

        assertTrue(Collections.disjoint(drawn, carriers), "a carrier's voice is nobody else's");
        Set<String> expected = cast.everyoneExcept(cast.own());
        expected.removeAll(carriers);
        assertEquals(expected, drawn, "everyone else still speaks");
    }

    // -- one speaker, one voice ------------------------------------------------

    /**
     * The complaint this answers: a pirate is named on every line they transmit, so a fresh draw per
     * transmission made one attacker sound like several, which mid-fight reads as several attackers.
     */
    @ParameterizedTest
    @MethodSource("casts")
    void aNamedSpeakerKeepsOneVoice(Cast cast) {
        String first = cast.forSpeaker().draw("Dave Knowles", cast.own(), Set.of());

        for (int i = 0; i < cast.draws(); i++) {
            assertEquals(first, cast.forSpeaker().draw("Dave Knowles", cast.own(), Set.of()));
        }
        // The name as the journal spells it, not as we happen to have trimmed it.
        assertEquals(first, cast.forSpeaker().draw("  dave knowles  ", cast.own(), Set.of()));
    }

    @ParameterizedTest
    @MethodSource("casts")
    void differentSpeakersSpreadAcrossTheCast(Cast cast) {
        Set<String> heard = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            heard.add(cast.forSpeaker().draw("Pilot " + i, cast.own(), Set.of()));
        }

        assertTrue(heard.size() > cast.voices().size() / 2,
                "a wing of pirates must not collapse onto a handful of voices, was " + heard.size());
    }

    @ParameterizedTest
    @MethodSource("casts")
    void aNamedSpeakerNeverTakesTheCommandersOrACarriersVoice(Cast cast) {
        Set<String> carriers = cast.twoVoicesOtherThanOwn();

        for (int i = 0; i < 200; i++) {
            String drawn = cast.forSpeaker().draw("Pilot " + i, cast.own(), carriers);
            assertNotEquals(cast.own(), drawn);
            assertFalse(carriers.contains(drawn), "a carrier's voice is nobody else's");
        }
    }

    /**
     * Giving a carrier a voice mid-session must not re-cast everyone else: the walk moves only the speakers
     * that actually landed on the newly reserved voice.
     */
    @ParameterizedTest
    @MethodSource("casts")
    void reservingOneVoiceLeavesTheOtherSpeakersWhereTheyWere(Cast cast) {
        List<String> speakers = IntStream.range(0, 200).mapToObj(i -> "Pilot " + i).toList();
        Map<String, String> before = speakers.stream()
                .collect(Collectors.toMap(name -> name, name -> cast.forSpeaker().draw(name, cast.own(), Set.of())));

        String givenToACarrier = before.get(speakers.getFirst());
        Set<String> reserved = Set.of(givenToACarrier);
        long moved = speakers.stream()
                .filter(name -> !before.get(name).equals(cast.forSpeaker().draw(name, cast.own(), reserved)))
                .count();

        assertEquals(speakers.stream().filter(name -> before.get(name).equals(givenToACarrier)).count(), moved,
                "only the speakers who had that voice should have moved");
    }

    /**
     * A transmission nobody is attributed to is a stranger, and strangers stay strangers.
     */
    @ParameterizedTest
    @MethodSource("casts")
    void anUnattributedTransmissionStillDrawsAtRandom(Cast cast) {
        Set<String> drawn = cast.drawMany(() -> cast.forSpeaker().draw(null, cast.own(), Set.of()));
        drawn.addAll(cast.drawMany(() -> cast.forSpeaker().draw("  ", cast.own(), Set.of())));

        assertEquals(cast.everyoneExcept(cast.own()), drawn);
    }

    /**
     * Reserving is best-effort: a channel with nothing left to say would be worse than a repeated voice.
     */
    @ParameterizedTest
    @MethodSource("casts")
    void reservingEveryVoiceStillLeavesSomeoneToSpeak(Cast cast) {
        Set<String> everyone = new HashSet<>(cast.voices());

        assertNotNull(cast.random().draw(cast.own(), everyone));
    }

    @ParameterizedTest
    @MethodSource("casts")
    void anUnknownOwnVoiceStillLeavesTheFullCast(Cast cast) {
        Set<String> drawn = cast.drawMany(() -> cast.random().draw(null, Set.of()));

        assertEquals(new HashSet<>(cast.voices()), drawn);
    }

    /**
     * A cast of one: the commander's own voice is the only one, so the draw has nothing else to offer and
     * says so with the fallback rather than an exception.
     */
    @ParameterizedTest
    @MethodSource("casts")
    void theFallbackSpeaksWhenTheCastIsSpokenFor(Cast cast) {
        Set<String> allButOwn = cast.everyoneExcept(cast.own());

        String drawn = cast.random().draw(cast.own(), allButOwn);

        assertTrue(cast.voices().contains(drawn), "someone in the cast still speaks");
    }
}
