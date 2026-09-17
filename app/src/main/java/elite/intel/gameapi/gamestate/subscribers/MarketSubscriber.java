package elite.intel.gameapi.gamestate.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.CommodityCatalogue;
import elite.intel.gameapi.carrier.CarrierHoldLedger;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.PlayerSession;

import java.util.List;

public class MarketSubscriber {

    @Subscribe
    public void onMarketEvent(GameEvents.MarketEvent marketEvent) {
        PlayerSession session = PlayerSession.getInstance();
        session.saveMarket(marketEvent);
        // Standing in our own carrier's market is the one moment the game gives a full account of what is
        // aboard it. The ledger starts again from here and is corrected by every transfer afterwards.
        CarrierHoldLedger.seedFrom(marketEvent);
        learnGoods(marketEvent);
    }

    /**
     * A market board names every good it trades, symbol and display name together, so it is the widest
     * teacher of goods this build has never heard of - a commander who never mines still learns
     * everything the local refinery buys the first time they dock there.
     */
    private static void learnGoods(GameEvents.MarketEvent market) {
        List<GameEvents.MarketEvent.MarketItem> items = market.getItems();
        if (items == null) return;
        CommodityCatalogue.getInstance().learnAll(items.stream()
                .map(item -> new CommodityCatalogue.Sighting(item.getName(), item.getNameLocalised()))
                .toList());
    }
}
