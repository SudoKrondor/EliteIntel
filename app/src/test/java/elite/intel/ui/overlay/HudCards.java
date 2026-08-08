package elite.intel.ui.overlay;

import java.util.List;

/**
 * Reading a {@link HudObjective} the way the commander does: by row label.
 * <p>
 * Shared by the card tests in this package because a card is only ever asserted
 * one way - find the row, check its value - and every source that grows a card
 * test needs the same three lookups.
 */
final class HudCards {

    private HudCards() {
    }

    /**
     * Every row label on the card, in render order.
     */
    static List<String> labels(HudObjective card) {
        return card.rows().stream().map(HudRow::label).toList();
    }

    /**
     * The row carrying {@code label}, failing with the labels that were actually
     * present - a missing row is the common failure and the list is the fix.
     */
    static HudRow rowOf(HudObjective card, String label) {
        return card.rows().stream()
                .filter(row -> label.equals(row.label()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + label + " row in " + labels(card)));
    }

    static String valueOf(HudObjective card, String label) {
        return rowOf(card, label).value();
    }
}
