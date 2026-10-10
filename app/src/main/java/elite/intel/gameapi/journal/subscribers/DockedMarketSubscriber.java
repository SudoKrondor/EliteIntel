package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.journal.events.ApproachBodyEvent;
import elite.intel.gameapi.journal.events.DockedEvent;
import elite.intel.gameapi.journal.events.LocationEvent;
import elite.intel.gameapi.journal.events.UndockedEvent;
import elite.intel.session.DockedMarket;

import java.util.Set;

/**
 * Keeps {@link DockedMarket} level with the journal: which port the ship is standing on, and when it is not
 * standing on one at all.
 * <p>
 * Deliberately separate from {@code DockedSubscriber}, which does the substantial work of recording the
 * place and is written around a virtual thread. This is a single assignment that has to have happened by
 * the time anything asks, so it runs on the bus thread and owns nothing else.
 * <p>
 * {@code Location} is handled as well as {@code Docked}, because a commander who quits on a pad and comes
 * back gets only the former. Without it the app would believe the ship was in open space until the next
 * undocking - and would attribute a {@code CargoTransfer} made in the meantime to the wrong carrier.
 * <p>
 * It also works out the planet a port on the ground stands on. {@code Docked} names no body, and Status.json
 * reports no surface position on a pad, but a ship cannot reach a port on the ground without entering its
 * planet's orbital cruise first - so the last {@code ApproachBody} in the system is the port's planet.
 */
public class DockedMarketSubscriber {

    /**
     * The journal's {@code StationType} values for ports on a planet's surface.
     */
    private static final Set<String> SURFACE_PORTS = Set.of(
            "CraterOutpost", "CraterPort", "OnFootSettlement", "SurfaceStation", "PlanetaryConstructionDepot");

    private final DockedMarket dockedMarket = DockedMarket.getInstance();

    private long approachedSystem;
    private String approachedBody;

    @Subscribe
    public void onApproachBody(ApproachBodyEvent event) {
        if (event == null) return;
        approachedSystem = event.getSystemAddress();
        approachedBody = event.getBody();
    }

    @Subscribe
    public void onDocked(DockedEvent event) {
        if (event == null) return;
        dockedMarket.arrived(event.getMarketID(), event.getStationName());
        if (SURFACE_PORTS.contains(event.getStationType())) {
            dockedMarket.standsOn(approachedBodyIn(event.getSystemAddress()));
        }
    }

    /**
     * Startup, and every jump. Only a docked one says anything about a pad; an ordinary arrival in open
     * space must not clear a marker it knows nothing about, because {@code Undocked} is what does that.
     */
    @Subscribe
    public void onLocation(LocationEvent event) {
        if (event == null || !event.isDocked()) return;
        dockedMarket.arrived(event.getMarketID(), event.getStationName());
        if (SURFACE_PORTS.contains(event.getStationType())) {
            // On a surface port the game files the ship under the planet itself.
            boolean namesThePlanet = "Planet".equalsIgnoreCase(event.getBodyType());
            dockedMarket.standsOn(namesThePlanet ? event.getBody() : approachedBodyIn(event.getSystemAddress()));
        }
    }

    private String approachedBodyIn(long systemAddress) {
        return systemAddress == approachedSystem ? approachedBody : null;
    }

    @Subscribe
    public void onUndocked(UndockedEvent event) {
        if (event == null) return;
        dockedMarket.departed();
    }
}
