package elite.intel.ui.overlay;

import elite.intel.ui.overlay.MiningObjectiveSource.TargetYield;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rows a miner reads mid-rock: how full the hold is, how much of each target
 * is in it, and how many limpets are left. The renderer takes them as opaque
 * data, so a value on the wrong row is invisible everywhere else in the suite.
 */
class MiningObjectiveCardTest {

    private static final String SYSTEM = "HYADES SECTOR DB-X D1-112";

    @Test
    void theCardListsEveryTargetWithWhatIsInTheHold() {
        HudObjective card = MiningObjectiveSource.card(
                List.of(new TargetYield("Platinum", 89), new TargetYield("Painite", 0)),
                137, 226, 512, SYSTEM).orElseThrow();

        assertEquals("MINING", card.title());
        assertEquals(SYSTEM, card.subtitle());
        assertEquals(HudObjective.PRIORITY_AMBIENT, card.priority(),
                "a self-set target list must never displace an accepted mission");
        assertEquals(List.of("HOLD", "PLATINUM", "PAINITE", "LIMPETS"), labels(card));
        assertEquals("89 T", valueOf(card, "PLATINUM"));
        assertEquals("0 T", valueOf(card, "PAINITE"), "a target with nothing mined yet stays listed");
        assertEquals("137", valueOf(card, "LIMPETS"));
    }

    @Test
    void theHoldRowBarsAgainstCargoCapacity() {
        HudRow hold = rowOf(card(List.of(new TargetYield("Platinum", 89)), 137, 226, 512), "HOLD");

        assertTrue(hold.hasProgress());
        assertEquals(226, hold.current(), "limpets occupy the hold too");
        assertEquals(512, hold.max());
        assertEquals(HudRow.State.NORMAL, hold.state());
    }

    @Test
    void aFullHoldSaysSoRatherThanQuietlyBarringAt100() {
        assertEquals(HudRow.State.CRITICAL,
                rowOf(card(List.of(new TargetYield("Platinum", 200)), 12, 256, 256), "HOLD").state());
    }

    @Test
    void anAlmostFullHoldIsFlagged() {
        assertEquals(HudRow.State.WARN,
                rowOf(card(List.of(new TargetYield("Platinum", 200)), 12, 240, 256), "HOLD").state());
    }

    /**
     * A ship with no cargo racks cannot bar against anything, and a bar with a
     * zero denominator would render as a full one.
     */
    @Test
    void noCargoCapacityMeansNoHoldRow() {
        HudObjective card = card(List.of(new TargetYield("Platinum", 0)), 12, 0, 0);

        assertEquals(List.of("PLATINUM", "LIMPETS"), labels(card));
    }

    @Test
    void runningOutOfLimpetsIsTheLoudestThingOnTheCard() {
        assertEquals(HudRow.State.CRITICAL,
                rowOf(card(List.of(new TargetYield("Platinum", 89)), 0, 226, 512), "LIMPETS").state());
        assertEquals(HudRow.State.WARN,
                rowOf(card(List.of(new TargetYield("Platinum", 89)), 3, 226, 512), "LIMPETS").state());
        assertEquals(HudRow.State.NORMAL,
                rowOf(card(List.of(new TargetYield("Platinum", 89)), 60, 226, 512), "LIMPETS").state());
    }

    /**
     * What is coming in is what the commander is watching, and equal yields hold
     * a stable order so the card does not reshuffle between polls.
     */
    @Test
    void theTargetYieldingMostIsListedFirst() {
        HudObjective card = card(List.of(
                new TargetYield("Painite", 4),
                new TargetYield("Platinum", 89),
                new TargetYield("Osmium", 0),
                new TargetYield("Bertrandite", 0)), 137, 226, 512);

        assertEquals(List.of("HOLD", "PLATINUM", "PAINITE", "BERTRANDITE", "OSMIUM", "LIMPETS"), labels(card));
    }

    /**
     * The overlay keeps 8 rows and drops the rest, so the overflow has to be
     * folded into a row that fits - and the limpet row has to survive it.
     */
    @Test
    void moreTargetsThanFitAreCountedInOneRow() {
        List<TargetYield> many = new ArrayList<>();
        for (int i = 0; i < MiningObjectiveSource.MAX_TARGET_ROWS + 2; i++) {
            many.add(new TargetYield("Mineral" + i, 0));
        }

        HudObjective card = card(many, 137, 226, 512);

        assertEquals(MiningObjectiveSource.MAX_TARGET_ROWS + 3, card.rows().size(),
                "hold + capped target rows + overflow + limpets");
        assertEquals("+2", valueOf(card, "MORE TARGETS"));
        assertEquals("LIMPETS", card.rows().getLast().label());
    }

    @Test
    void noTargetsMeansNoCard() {
        assertTrue(MiningObjectiveSource.card(List.of(), 137, 226, 512, SYSTEM).isEmpty());
    }

    // -- fixtures --------------------------------------------------------------

    private static HudObjective card(List<TargetYield> yields, int limpets, int used, int capacity) {
        return MiningObjectiveSource.card(yields, limpets, used, capacity, SYSTEM).orElseThrow();
    }

    private static List<String> labels(HudObjective card) {
        return card.rows().stream().map(HudRow::label).toList();
    }

    private static HudRow rowOf(HudObjective card, String label) {
        return card.rows().stream()
                .filter(row -> label.equals(row.label()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + label + " row on the card"));
    }

    private static String valueOf(HudObjective card, String label) {
        return rowOf(card, label).value();
    }
}
