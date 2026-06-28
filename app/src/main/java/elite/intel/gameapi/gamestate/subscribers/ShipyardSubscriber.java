package elite.intel.gameapi.gamestate.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.dto.shiploadout.LoadoutConverter;

public class ShipyardSubscriber {

    @Subscribe
    public void onShipyardEvent(GameEvents.ShipyardEvent event) {
        if (event.getPriceList() == null) return;
        Thread.ofVirtual().start(() -> {
            for (GameEvents.ShipyardEvent.ShipPrice ship : event.getPriceList()) {
                if (ship.getShipTypeLocalised() != null) {
                    LoadoutConverter.upsertDisplayName(ship.getShipType(), ship.getShipTypeLocalised());
                }
            }
        });
    }
}
