package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.journal.events.ShipyardBuyEvent;
import elite.intel.gameapi.journal.events.dto.shiploadout.LoadoutConverter;
import elite.intel.ui.event.ShipProfileChangedEvent;

public class NewShipPurchasedHandler {

    @Subscribe
    public void onNewShipPurchased(ShipyardBuyEvent event) {
        // The journal already carries the localised display name; prefer it and only fall back
        // to resolving the internal codename when the localised value is absent.
        String displayName = event.getShipTypeLocalised() != null
                ? event.getShipTypeLocalised()
                : LoadoutConverter.toDisplayShipName(null, event.getShipType());
        GameEventBus.publish(new SensorDataEvent("New ship added to fleet. Class: " + displayName, "Congratulate User on new addition to the fleet."));
        UiBus.publish(new ShipProfileChangedEvent());
    }

}
