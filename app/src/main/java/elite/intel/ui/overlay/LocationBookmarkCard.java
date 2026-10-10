package elite.intel.ui.overlay;

import elite.intel.db.managers.LocationBookmarkManager;
import elite.intel.gameapi.bookmarks.LocationBookmark;
import elite.intel.ui.i18n.LocalizedNumbers;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The commander's bookmarks, a page at a time, as a {@link TemporaryHudCard}.
 * <p>
 * Shown only on request ("show bookmarks") and never ranked: a list of places is reference material, not
 * something the commander is doing, so it has no business competing with a mission card. Each row is the
 * bookmark's number - the one "navigate to bookmark N" takes - and its {@link #label}.
 * <p>
 * Never read aloud. Star system names are long strings of catalogue letters and digits, and a list of them
 * spoken one after another is noise. With no overlay on screen the commands say so instead.
 */
public final class LocationBookmarkCard implements HudObjectiveSource {

    /**
     * One bookmark per row and a row per slot the renderer has - {@code MAX_ROWS} in {@code overlay/src/hud.h}.
     * The page number goes in the subtitle so it costs no row.
     */
    public static final int PAGE_SIZE = 8;

    private static final LocationBookmarkCard INSTANCE =
            new LocationBookmarkCard(LocationBookmarkManager.getInstance()::inOrder, TemporaryHudCard.getInstance());

    /**
     * What a page turn did, so the command can say "that's the last page" rather than nothing.
     */
    public enum Turn {
        MOVED,
        AT_END
    }

    private final Supplier<List<LocationBookmark>> bookmarks;
    private final TemporaryHudCard slot;
    private volatile int page;

    /**
     * Seam for tests.
     */
    LocationBookmarkCard(Supplier<List<LocationBookmark>> bookmarks, TemporaryHudCard slot) {
        this.bookmarks = bookmarks;
        this.slot = slot;
    }

    public static LocationBookmarkCard getInstance() {
        return INSTANCE;
    }

    /**
     * Shows the first page - the top of the list.
     */
    public void open() {
        page = 0;
        slot.open(this);
    }

    public void close() {
        slot.close(this);
    }

    public boolean isOpen() {
        return slot.isShowing(this);
    }

    public Turn nextPage() {
        return turn(1);
    }

    public Turn previousPage() {
        return turn(-1);
    }

    private Turn turn(int step) {
        slot.touch();
        int last = pageCount(bookmarks.get().size()) - 1;
        int wanted = Math.clamp(page + step, 0, last);
        if (wanted == page) return Turn.AT_END;
        page = wanted;
        return Turn.MOVED;
    }

    @Override
    public Optional<HudObjective> currentObjective() {
        return card(bookmarks.get(), page);
    }

    /**
     * The card for one page of the list. Pure so it can be tested without a database; a page past the end of
     * a list that shrank shows the last page instead.
     */
    static Optional<HudObjective> card(List<LocationBookmark> all, int page) {
        if (all.isEmpty()) return Optional.empty();
        int pages = pageCount(all.size());
        int shown = Math.clamp(page, 0, pages - 1);
        int from = shown * PAGE_SIZE;
        List<HudRow> rows = new ArrayList<>();
        for (int i = from; i < Math.min(from + PAGE_SIZE, all.size()); i++) {
            rows.add(HudRow.of(HudText.count(i + 1), label(all.get(i))));
        }
        String subtitle = HudText.get("overlay.card.subtitle.bookmarkPage",
                String.valueOf(shown + 1), String.valueOf(pages));
        return Optional.of(new HudObjective("bookmarks", HudText.get("overlay.card.title.bookmarks"), subtitle,
                rows, HudObjective.PRIORITY_ACTIVE));
    }

    /**
     * What the card calls a bookmark: the commander's own name for it, or else the place - with the planet for a
     * port on the ground, which is where the commander has to fly, and for a spot on the ground the coordinates
     * too, since two bookmarks on one moon are only told apart by where on it they are. The Bookmarks page shows the same text, so the commander renames exactly what they see here.
     */
    public static String label(LocationBookmark bookmark) {
        if (bookmark.isRenamed()) return bookmark.displayName();
        return switch (bookmark.kind()) {
            case PLANETARY_PORT -> bookmark.stationName() + " (" + bookmark.planetName() + ")";
            case SURFACE -> !bookmark.hasCoordinates() ? bookmark.planetName()
                    : bookmark.planetName() + " - " + degrees(bookmark.latitude()) + " / " + degrees(bookmark.longitude());
            case SYSTEM, STATION, PLANET -> bookmark.placeName();
        };
    }

    /**
     * A latitude or longitude the way bookmarks show one: four places, in the app language's digits.
     */
    public static String degrees(double value) {
        NumberFormat format = NumberFormat.getNumberInstance(LocalizedNumbers.locale());
        format.setMinimumFractionDigits(4);
        format.setMaximumFractionDigits(4);
        format.setGroupingUsed(false);
        return format.format(value);
    }

    private static int pageCount(int bookmarkCount) {
        return Math.max(1, (bookmarkCount + PAGE_SIZE - 1) / PAGE_SIZE);
    }
}
