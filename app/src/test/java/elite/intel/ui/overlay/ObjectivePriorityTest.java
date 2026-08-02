package elite.intel.ui.overlay;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One card fits on screen, so every source that has something to say is
 * competing for it. These pin who wins.
 */
class ObjectivePriorityTest {

    @Test
    void anAcceptedMissionBeatsAVolunteeredSamplingList() {
        Optional<HudObjective> shown = NativeHudOverlay.highestPriority(List.of(
                source(card("exobiology", HudObjective.PRIORITY_AMBIENT)),
                source(card("mission", HudObjective.PRIORITY_STANDING))));

        assertEquals("mission", shown.orElseThrow().id());
    }

    @Test
    void aPlottedRouteAlsoBeatsASamplingList() {
        Optional<HudObjective> shown = NativeHudOverlay.highestPriority(List.of(
                source(card("trade-route", HudObjective.PRIORITY_STANDING)),
                source(card("exobiology", HudObjective.PRIORITY_AMBIENT))));

        assertEquals("trade-route", shown.orElseThrow().id());
    }

    /**
     * With nothing else to show, the volunteered card is the card.
     */
    @Test
    void theSamplingListShowsWhenNothingElseDoes() {
        Optional<HudObjective> shown = NativeHudOverlay.highestPriority(List.of(
                source(null),
                source(card("exobiology", HudObjective.PRIORITY_AMBIENT))));

        assertEquals("exobiology", shown.orElseThrow().id());
    }

    @Test
    void aSpecialisedCardStillOutranksTheGenericOneItReplaces() {
        Optional<HudObjective> shown = NativeHudOverlay.highestPriority(List.of(
                source(card("mission", HudObjective.PRIORITY_STANDING)),
                source(card("massacre-stack", HudObjective.PRIORITY_SPECIALISED))));

        assertEquals("massacre-stack", shown.orElseThrow().id());
    }

    @Test
    void aQuietHudShowsNothing() {
        assertTrue(NativeHudOverlay.highestPriority(List.of(source(null), source(null))).isEmpty());
    }

    private static HudObjectiveSource source(HudObjective objective) {
        return () -> Optional.ofNullable(objective);
    }

    private static HudObjective card(String id, int priority) {
        return new HudObjective(id, id.toUpperCase(), null, List.of(), priority);
    }
}
