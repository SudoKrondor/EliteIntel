package elite.intel.gameapi.gamestate.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.carrier.CarrierHoldLedger;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.PlayerSession;

public class MarketSubscriber {

    @Subscribe
    public void onMarketEvent(GameEvents.MarketEvent marketEvent) {
        PlayerSession session = PlayerSession.getInstance();
        session.saveMarket(marketEvent);
        // Standing in our own carrier's market is the one moment the game gives a full account of what is
        // aboard it. The ledger starts again from here and is corrected by every transfer afterwards.
        CarrierHoldLedger.seedFrom(marketEvent);
    }
}
