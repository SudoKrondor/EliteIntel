package elite.intel.ui.overlay;

import java.util.List;

/**
 * A single task/objective card shown in the top section of the HUD overlay.
 * <p>
 * Produced by a {@link HudObjectiveSource}; consumed by the renderer, which
 * treats it as opaque data. Missions, exobiology sampling, massacre progress
 * and trade-route hops all project into this one shape.
 *
 * @param id       stable key for the producing source; a new objective with the
 *                 same id replaces the previous one rather than stacking
 * @param title    headline, e.g. the mission name or the organic species
 * @param subtitle context line, e.g. system and station; may be {@code null}
 * @param rows     detail lines, rendered in order
 * @param priority higher wins when several sources are active at once
 */
public record HudObjective(String id, String title, String subtitle, List<HudRow> rows, int priority) {

    /**
     * Priority for objectives tied to something the commander is doing right now.
     */
    public static final int PRIORITY_ACTIVE = 100;
    /**
     * Priority for standing objectives such as accepted missions.
     */
    public static final int PRIORITY_STANDING = 50;
    /**
     * Priority for a purpose-built card that supersedes a generic one - the
     * massacre stack, say, which says more than the same missions listed one at
     * a time by the generic mission source.
     */
    public static final int PRIORITY_SPECIALISED = 75;
    /**
     * Priority for a self-set objective the commander never committed to - an
     * exobiology sampling list, say. Below every other priority on purpose: a
     * card the app volunteered must never displace one for work the commander
     * accepted, because only one card fits.
     */
    public static final int PRIORITY_AMBIENT = 25;

    public HudObjective {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    public HudObjective(String id, String title, String subtitle, List<HudRow> rows) {
        this(id, title, subtitle, rows, PRIORITY_STANDING);
    }
}
