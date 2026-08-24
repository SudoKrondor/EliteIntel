package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.journal.events.DockedEvent;
import elite.intel.gameapi.journal.events.LocationEvent;
import elite.intel.gameapi.journal.events.UndockedEvent;
import elite.intel.session.DockedMarket;

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
 */
public class DockedMarketSubscriber {

    private final DockedMarket dockedMarket = DockedMarket.getInstance();

    @Subscribe
    public void onDocked(DockedEvent event) {
        if (event == null) return;
        dockedMarket.arrived(event.getMarketID(), event.getStationName());
    }

    /**
     * Startup, and every jump. Only a docked one says anything about a pad; an ordinary arrival in open
     * space must not clear a marker it knows nothing about, because {@code Undocked} is what does that.
     */
    @Subscribe
    public void onLocation(LocationEvent event) {
        if (event == null || !event.isDocked()) return;
        dockedMarket.arrived(event.getMarketID(), event.getStationName());
    }

    @Subscribe
    public void onUndocked(UndockedEvent event) {
        if (event == null) return;
        dockedMarket.departed();
    }
}
