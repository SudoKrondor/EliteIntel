package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.journal.events.ShipyardSwapEvent;
import elite.intel.gameapi.journal.events.dto.shiploadout.LoadoutConverter;

public class ShipyardSwapSubscriber {

    @Subscribe
    public void onEvent(ShipyardSwapEvent event) {
        if (event.getShipTypeLocalised() == null) return;
        Thread.ofVirtual().start(() ->
            LoadoutConverter.upsertDisplayName(event.getShipType(), event.getShipTypeLocalised())
        );
    }
}
