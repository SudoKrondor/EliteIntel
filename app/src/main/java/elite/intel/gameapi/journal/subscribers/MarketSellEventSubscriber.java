package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.managers.MonetizeRouteManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.TradeRouteManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.MarketSellEvent;
import elite.intel.search.spansh.station.marketstation.TradeStopDto;
import elite.intel.search.spansh.traderoute.TradeCommodity;
import elite.intel.session.PlayerSession;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static elite.intel.util.StringUtls.localizedEvent;

public class MarketSellEventSubscriber {

    private static final int DEBOUNCE_MS = 2000;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final List<MarketSellEvent> pending = new ArrayList<>();
    private ScheduledFuture<?> pendingFlush;

    @Subscribe
    public void onMarketSellEvent(MarketSellEvent event) {
        TradeRouteManager.getInstance().deleteForMarketId(event.getMarketID());

        synchronized (pending) {
            pending.add(event);
            if (pendingFlush != null) pendingFlush.cancel(false);
            pendingFlush = scheduler.schedule(this::flush, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void flush() {
        synchronized (pending) {
            if (pending.isEmpty()) return;

            if (pending.size() == 1) {
                MarketSellEvent e = pending.getFirst();
                EventBusManager.publish(new AiVoxResponseEvent(localizedEvent("event.market.sold.units", e.getCount(), e.getType(), e.getTotalSale())));
            } else {
                long total = pending.stream().mapToLong(MarketSellEvent::getTotalSale).sum();
                EventBusManager.publish(new AiVoxResponseEvent(localizedEvent("event.market.sold.multiple", pending.size(), total)));
            }
            pending.clear();

            final TradeRouteManager tradeRouteManager = TradeRouteManager.getInstance();
            final PlayerSession playerSession = PlayerSession.getInstance();
            final ReminderManager reminderManager = ReminderManager.getInstance();

            TradeRouteManager.TradeRouteLegTuple<Integer, TradeStopDto> nextStop = tradeRouteManager.getNextStop();

            if (nextStop != null) {
                String sourceSystem = nextStop.getTradeStopDto().getSourceSystem();
                String sourceStation = nextStop.getTradeStopDto().getSourceStation();
                String destinationSystem = nextStop.getTradeStopDto().getDestinationSystem();
                String destinationStation = nextStop.getTradeStopDto().getDestinationStation();
                String commodities = nextStop.getTradeStopDto().getCommodities().stream()
                        .map(TradeCommodity::getName)
                        .collect(Collectors.joining(", "));

                String tradeMessage;
                if (playerSession.getPrimaryStarName().equalsIgnoreCase(sourceSystem)) {
                    tradeMessage = localizedEvent("event.market.trade.buy", commodities, destinationSystem, destinationStation);
                } else {
                    tradeMessage = localizedEvent("event.market.trade.head", sourceSystem, sourceStation, commodities, destinationSystem, destinationStation);
                }

                EventBusManager.publish(new AiVoxResponseEvent(tradeMessage));
                reminderManager.setReminder(tradeMessage, destinationSystem);
            } else {
                reminderManager.clear();
            }

            MonetizeRouteManager.getInstance().clear();
        }
    }


    public record Reminder(Integer legNumber, TradeStopDto stopInfo,
                           List<TradeCommodity> commodities) implements ToJsonConvertible {
        @Override
        public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }
    }
}
