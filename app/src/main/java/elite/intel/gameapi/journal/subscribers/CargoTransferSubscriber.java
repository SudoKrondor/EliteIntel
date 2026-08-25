package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.carrier.CarrierHoldLedger;
import elite.intel.gameapi.journal.events.CargoTransferEvent;

/**
 * Keeps the carrier's hold level as the commander loads and unloads it.
 * <p>
 * The work is {@link CarrierHoldLedger}'s, including deciding WHICH carrier the move belongs to - this
 * event names no station, and a transfer made planetside is an SRV's, not a carrier's.
 */
public class CargoTransferSubscriber {

    @Subscribe
    public void onCargoTransfer(CargoTransferEvent event) {
        if (event == null) return;
        Thread.ofVirtual().start(() -> CarrierHoldLedger.transferred(event.getTransfers()));
    }
}
