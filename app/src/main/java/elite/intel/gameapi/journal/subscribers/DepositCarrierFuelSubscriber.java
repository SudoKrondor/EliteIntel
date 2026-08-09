package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.journal.events.CarrierDepositFuelEvent;

public class DepositCarrierFuelSubscriber {

    @Subscribe public void onCarrierDepositFuelEvent(CarrierDepositFuelEvent event) {
        Thread.ofVirtual().start(() -> CarrierArrival.applyFuelReading(event.getTotal()));
    }
}
