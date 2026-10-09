package elite.intel.ui.overlay;

import java.time.Duration;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * A card the commander asked to see, shown over whatever the ranking would pick until they are done with it.
 * <p>
 * WHY not a very high priority: priority answers "which of the things going on matters most", and a card the
 * commander called up by voice is not competing on that question at all - it is on screen because they asked,
 * and only for as long as they use it. Holding it in its own slot keeps the ranking untouched underneath, so
 * closing it brings back whatever card the ranking picks at that moment, with nothing to restore.
 * <p>
 * It closes on request, or by itself after {@link #IDLE_LIMIT} without being touched, so a commander who
 * forgets to dismiss it does not lose their mission card for the rest of the session.
 */
public final class TemporaryHudCard {

    public static final Duration IDLE_LIMIT = Duration.ofMinutes(2);

    private static final TemporaryHudCard INSTANCE = new TemporaryHudCard(System::currentTimeMillis);

    private final LongSupplier clock;
    private HudObjectiveSource source;
    private long touchedAt;

    /**
     * Seam for tests.
     */
    TemporaryHudCard(LongSupplier clock) {
        this.clock = clock;
    }

    public static TemporaryHudCard getInstance() {
        return INSTANCE;
    }

    /**
     * Puts this source's card on screen, replacing any other temporary card.
     */
    public synchronized void open(HudObjectiveSource source) {
        this.source = source;
        touchedAt = clock.getAsLong();
    }

    /**
     * Restarts the idle clock - the commander is still using the card.
     */
    public synchronized void touch() {
        touchedAt = clock.getAsLong();
    }

    /**
     * Takes this source's card down. A source that is not the one showing changes nothing, so a late close
     * from one card can never take down another.
     */
    public synchronized void close(HudObjectiveSource source) {
        if (this.source == source) this.source = null;
    }

    public synchronized boolean isShowing(HudObjectiveSource source) {
        expireIfIdle();
        return source != null && this.source == source;
    }

    /**
     * The temporary card, or empty when none is open - or when its source has nothing to show, in which case
     * the ranked card shows rather than a blank.
     */
    Optional<HudObjective> current() {
        HudObjectiveSource showing;
        synchronized (this) {
            expireIfIdle();
            showing = source;
        }
        // Outside the lock: a source reads the database, and a command opening or closing the card must not
        // wait on that.
        return showing == null ? Optional.empty() : showing.currentObjective();
    }

    private void expireIfIdle() {
        if (source != null && clock.getAsLong() - touchedAt >= IDLE_LIMIT.toMillis()) source = null;
    }
}
