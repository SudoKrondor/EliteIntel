package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.carrier.CarrierHoldLedger;
import elite.intel.gameapi.journal.events.MarketBuyEvent;
import elite.intel.gameapi.journal.events.MarketSellEvent;

/**
 * Keeps the carrier's hold level when the commander trades at their own carrier's market rather than
 * transferring cargo across.
 * <p>
 * The two are the same movement of goods by a different route - selling a hold of steel onto the carrier
 * leaves it exactly as loaded as carrying it across would - so a ledger that watched only
 * {@code CargoTransfer} would go wrong for anyone who works that way.
 * <p>
 * Deliberately separate from {@code FinanceSubscriber}, which is the single home for what a trade did to
 * the commander's credits and has nothing to say about whose shelves the cargo came off. Trades at anyone
 * else's market are ignored - see {@link CarrierHoldLedger}, which decides that.
 */
public class CarrierMarketTradeSubscriber {

    @Subscribe
    public void onMarketBuy(MarketBuyEvent event) {
        if (event == null) return;
        CarrierHoldLedger.bought(event.getMarketID(), event.getType(), event.getCount());
    }

    @Subscribe
    public void onMarketSell(MarketSellEvent event) {
        if (event == null) return;
        CarrierHoldLedger.sold(event.getMarketID(), event.getType(), event.getCount());
    }
}
