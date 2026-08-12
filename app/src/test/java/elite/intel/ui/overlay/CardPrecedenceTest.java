package elite.intel.ui.overlay;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One card fits, so what the commander sees is decided by which sources have something to say and in what
 * order they lose. The ladder, as specified:
 * <ol>
 *   <li>a mission,</li>
 *   <li>a trade route,</li>
 *   <li>a calculated destination — a route plotted to a material trader, broker or interstellar factors,</li>
 *   <li>otherwise the plotted route.</li>
 * </ol>
 * With mining as a corner case above the last two: a mining target on a ship with a refinery, while not in
 * supercruise.
 * <p>
 * These assert against the real registration order rather than a copy of it, so re-ordering
 * {@code NativeHudOverlay} without meaning to shows up here.
 */
class CardPrecedenceTest {

    @Test
    void aMissionOutranksEverythingBelowIt() {
        assertEquals("mission", winnerOf("mission", "trade-route", "ship-route", "mining"));
    }

    @Test
    void aTradeRouteOutranksTheStandingErrandAndThePlottedRoute() {
        assertEquals("trade-route", winnerOf("trade-route", "ship-route", "mining"));
    }

    @Test
    void withNothingElseThePlottedRouteIsTheCard() {
        assertEquals("ship-route", winnerOf("ship-route"));
    }

    @Test
    void miningOutranksBothOfTheDestinationCards() {
        assertEquals("mining", winnerOf("mining", "ship-route"));
    }

    @Test
    void aQuietHudShowsNothing() {
        assertTrue(NativeHudOverlay.highestPriority(List.of()).isEmpty());
    }

    /**
     * Runs the real source list with only {@code speaking} having anything to say, and returns the id of the
     * card that wins. Sources not named stay silent, which is how a real quiet source behaves.
     */
    private static String winnerOf(String... speaking) {
        List<String> talkative = List.of(speaking);
        List<HudObjectiveSource> sources = new ArrayList<>();
        for (HudObjectiveSource real : NativeHudOverlay.defaultSources()) {
            String id = idOf(real);
            sources.add(talkative.contains(id)
                    ? () -> Optional.of(new HudObjective(id, id, null, List.of(), priorityOf(id)))
                    : Optional::empty);
        }
        return NativeHudOverlay.highestPriority(sources).orElseThrow().id();
    }

    /**
     * The card id each source produces. A silent source cannot be asked, so the mapping is stated here; the
     * *order* still comes from the overlay itself, which is the part that decides ties.
     */
    private static String idOf(HudObjectiveSource source) {
        return switch (source.getClass().getSimpleName()) {
            case "MassacreObjectiveSource" -> "massacre-stack";
            case "MissionObjectiveSource" -> "mission";
            case "TradeRouteObjectiveSource" -> "trade-route";
            case "MonetizedRouteObjectiveSource" -> "monetized-route";
            case "MiningObjectiveSource" -> "mining";
            case "ExobiologyObjectiveSource" -> "exobiology";
            case "ShipRouteObjectiveSource" -> "ship-route";
            default -> throw new AssertionError(
                    "unmapped overlay source " + source.getClass().getSimpleName()
                            + " - add it to the ladder this test pins");
        };
    }

    /**
     * The priority each source declares, mirrored here so the ladder is asserted end to end.
     */
    private static int priorityOf(String id) {
        return switch (id) {
            case "massacre-stack" -> HudObjective.PRIORITY_SPECIALISED;
            case "mission", "trade-route", "monetized-route" -> HudObjective.PRIORITY_STANDING;
            default -> HudObjective.PRIORITY_AMBIENT;
        };
    }
}
