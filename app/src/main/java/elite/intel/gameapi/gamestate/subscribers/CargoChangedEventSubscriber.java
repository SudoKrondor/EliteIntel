package elite.intel.gameapi.gamestate.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.CommodityCatalogue;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.PlayerSession;

@SuppressWarnings("unused")//registered in SubscriberRegistration
public class CargoChangedEventSubscriber {

    @Subscribe
    public void onCargoChangedEvent(GameEvents.CargoEvent event) {
        PlayerSession.getInstance().setShipCargo(event);
        if (event.getInventory() == null) return;
        // The hold names what it carries, symbol and display name together: a good this build has never
        // heard of is learned here so it can be asked about by name.
        CommodityCatalogue.getInstance().learnAll(event.getInventory().stream()
                .map(item -> new CommodityCatalogue.Sighting(item.getName(), item.getNameLocalised()))
                .toList());
    }
}
