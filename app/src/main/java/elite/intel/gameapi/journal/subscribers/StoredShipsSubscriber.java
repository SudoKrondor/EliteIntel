package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.journal.events.StoredShipsEvent;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.ShipProfileChangedEvent;

/**
 * Fills the fleet from the game's list of stored ships each time the commander opens a shipyard. Before this the
 * fleet learned a ship only from its Loadout, so a ship never flown since the fleet lost its row stayed missing.
 * Silent: filling a ledger is never announced.
 */
public class StoredShipsSubscriber {

    @Subscribe
    public void onStoredShips(StoredShipsEvent event) {
        Thread.ofVirtual().start(() -> {
            String commanderName = PlayerSession.getInstance().getInGameName();
            int added = StoredShips.addMissing(event,
                    SystemSession.getInstance().getTtsProvider().defaultVoiceName(),
                    commanderName == null || commanderName.isBlank() ? null : commanderName);
            if (added > 0) {
                UiBus.publish(new ShipProfileChangedEvent());
            }
        });
    }
}
