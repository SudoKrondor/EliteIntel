package elite.intel.gameapi.gamestate.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.PlayerSession;

/**
 * Keeps the Odyssey suit inventory in the session.
 * <p>
 * These are the only record the game keeps of an on-foot mission item: a salvage or collect objective
 * is picked up into the backpack and stowed in the ship's locker on boarding, and never touches the
 * cargo hold, so nothing in {@code Cargo.json} can measure it.
 */
@SuppressWarnings("unused")//registered in SubscriberRegistration
public class SuitInventoryEventSubscriber {

    @Subscribe
    public void onBackpackEvent(GameEvents.BackpackEvent event) {
        PlayerSession.getInstance().setBackpack(event);
    }

    @Subscribe
    public void onShipLockerEvent(GameEvents.ShipLockerEvent event) {
        PlayerSession.getInstance().setShipLocker(event);
    }
}
