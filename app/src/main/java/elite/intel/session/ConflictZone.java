package elite.intel.session;

import elite.intel.gameapi.signals.ConflictZoneIntensity;

import java.util.concurrent.atomic.AtomicReference;

/**
 * The conflict zone fight the commander is currently in, if any.
 * <p>
 * The twin of {@link ResourceSite} for the other kind of fighting, with the same scope and for the
 * same reason: the fight starts when the ship drops into a conflict zone and lasts until the bonds
 * are cashed in or the ship leaves the system, so that boosting out to supercruise between zones
 * or flying to the station to cash in does not blink the card off.
 * <p>
 * <b>In memory only.</b> A restart taken mid-fight gets it back because
 * {@code ConflictZoneSubscriber} is registered on the pre-scan bus, so replaying the last journals
 * rebuilds the marker before anything asks.
 */
public final class ConflictZone {

    private static final ConflictZone INSTANCE = new ConflictZone();

    private final AtomicReference<Engagement> engagement = new AtomicReference<>();

    private ConflictZone() {
    }

    public static ConflictZone getInstance() {
        return INSTANCE;
    }

    /**
     * Records a drop into a conflict zone, starting a fight or re-describing the one in progress
     * with the zone just entered.
     */
    public void droppedIn(long systemAddress, ConflictZoneIntensity intensity) {
        if (systemAddress <= 0 || intensity == null) return;
        engagement.set(new Engagement(systemAddress, intensity));
    }

    /**
     * Ends the fight. Called when the bonds are cashed in, which is the commander saying they are
     * done rather than merely between zones.
     */
    public void cashedIn() {
        engagement.set(null);
    }

    /**
     * The fight in this system, or null when there is none - either because none was started or
     * because it was started somewhere the ship has since left.
     */
    public Engagement in(long systemAddress) {
        Engagement current = engagement.get();
        return current != null && current.systemAddress() == systemAddress ? current : null;
    }

    /**
     * @param intensity of the last zone dropped into, which is what the commander is fighting in now
     */
    public record Engagement(long systemAddress, ConflictZoneIntensity intensity) {
    }
}
