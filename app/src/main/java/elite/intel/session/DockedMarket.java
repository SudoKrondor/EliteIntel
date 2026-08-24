package elite.intel.session;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The MarketID of the port the ship is standing on, straight from the journal.
 * <p>
 * <b>Why this exists rather than a lookup.</b> The obvious way to ask "which market am I docked at" is to
 * read the current location back out of the location tables, and it does not work: those rows are keyed by
 * {@code (systemAddress, bodyId)}, {@code Docked} carries no bodyId at all, and a miss returns a freshly
 * built row whose MarketID is zero. A caller that gates on that gets a silent, permanent false - which is
 * exactly how the colonisation shopping command came to never be offered, at a depot whose manifest the app
 * was reading correctly at the same moment.
 * <p>
 * {@code Docked} and {@code Undocked} both carry the MarketID and are unambiguous, so this simply remembers
 * what they said. In memory only: the marker is worth nothing after a restart anyway, since it describes
 * where the ship is right now. It is refreshed from any event that can only happen on a pad - {@code
 * Location} on startup, and see {@code ColonisationDepotSubscriber} - so a restart made while docked
 * repairs itself rather than waiting for the commander to undock.
 * <p>
 * The station's NAME is remembered next to its id because some things can only be answered by name. A
 * carrier's station name IS its callsign, which is how {@code CargoTransfer} - an event that names no
 * station at all - is attributed to the carrier the ship is standing on rather than assumed to be the
 * fleet carrier.
 */
public final class DockedMarket {

    private static final DockedMarket INSTANCE = new DockedMarket();

    /**
     * Zero means "not on a pad, as far as we know", which is also the honest answer before the first
     * {@code Docked} of the session.
     */
    private final AtomicLong marketId = new AtomicLong(0);

    /**
     * The port's name as the journal spelled it, or null when we are not on a pad - or are on one we were
     * only told the id of, which callers must treat the same way.
     */
    private final AtomicReference<String> stationName = new AtomicReference<>();

    private DockedMarket() {
    }

    public static DockedMarket getInstance() {
        return INSTANCE;
    }

    /**
     * Records the port the ship has just docked at, or re-asserts one while it sits there.
     * <p>
     * Called with no name by the events that prove we are on a pad without naming it. Such a call keeps a
     * name we already hold for the SAME pad and drops one held for a different pad, so a stale name can
     * never be read as belonging to the port we are actually on.
     */
    public void arrived(long marketId, String stationName) {
        if (marketId == 0) return;
        long previous = this.marketId.getAndSet(marketId);
        if (stationName != null && !stationName.isBlank()) {
            this.stationName.set(stationName);
        } else if (previous != marketId) {
            this.stationName.set(null);
        }
    }

    public void arrived(long marketId) {
        arrived(marketId, null);
    }

    public void departed() {
        marketId.set(0);
        stationName.set(null);
    }

    /**
     * The MarketID of the port the ship is on, or {@code 0} when it is not on one - or when we have not been
     * told yet, which callers must treat the same way.
     */
    public long marketId() {
        return marketId.get();
    }

    /**
     * The name of the port the ship is on, or null when it is not on one or was never told the name.
     */
    public String stationName() {
        return stationName.get();
    }

    /**
     * True when the ship is standing on a port of this name. Case-insensitive, because the name is compared
     * against one the app stored from a different event.
     */
    public boolean isOn(String stationName) {
        if (stationName == null || stationName.isBlank()) return false;
        String here = this.stationName.get();
        return here != null && here.equalsIgnoreCase(stationName.trim());
    }
}
