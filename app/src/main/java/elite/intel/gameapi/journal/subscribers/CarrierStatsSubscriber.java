package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.journal.events.CarrierStatsEvent;

public class CarrierStatsSubscriber {

    @Subscribe
    public void onCarrierStatsEvent(CarrierStatsEvent event) {
        CarrierArrival.applyCarrierStats(event);
    }
}
