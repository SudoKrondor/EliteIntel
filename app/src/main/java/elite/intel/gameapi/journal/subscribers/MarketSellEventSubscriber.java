package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.EventNarrator;
import elite.intel.db.FuzzySearch;
import elite.intel.db.managers.MonetizeRouteManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.TradeRouteManager;
import elite.intel.gameapi.journal.events.DockedEvent;
import elite.intel.gameapi.journal.events.MarketSellEvent;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStopDto;
import elite.intel.gameapi.search.spansh.traderoute.TradeCommodity;
import elite.intel.session.PlayerSession;
import elite.intel.util.TTSFriendlyNumberConverter;
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

    /**
     * How long the market has to stay quiet before the next leg is briefed.
     * <p>
     * The commander sells by hand, one commodity at a time, and the journal shows roughly four to five
     * seconds between those sales. The briefing must outlast that gap or it lands after the first sale while
     * the hold is still being emptied - which is the whole point of waiting. Sized well clear of the measured
     * cadence: overshooting only delays the briefing, whereas undershooting speaks it too early.
     */
    private static final int BRIEFING_QUIET_MS = 8000;

    /**
     * No market: the next docking has not briefed yet.
     */
    private static final long NOT_BRIEFED = -1L;

    private final int debounceMs;
    private final int briefingQuietMs;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final List<MarketSellEvent> pending = new ArrayList<>();
    private ScheduledFuture<?> pendingFlush;
    private ScheduledFuture<?> pendingBriefing;

    /**
     * The market the pending briefing is for. Guarded by {@code pending}.
     */
    private long briefingMarketId = NOT_BRIEFED;

    /**
     * The market whose sale already produced the next-leg briefing. Guarded by {@code pending}.
     */
    private long briefedMarketId = NOT_BRIEFED;

    public MarketSellEventSubscriber() {
        this(DEBOUNCE_MS, BRIEFING_QUIET_MS);
    }

    /**
     * Seam for tests, so a suite does not have to sit out the real quiet period.
     */
    public MarketSellEventSubscriber(int debounceMs, int briefingQuietMs) {
        this.debounceMs = debounceMs;
        this.briefingQuietMs = briefingQuietMs;
    }

    @Subscribe
    public void onMarketSellEvent(MarketSellEvent event) {
        synchronized (pending) {
            pending.add(event);
            if (pendingFlush != null) pendingFlush.cancel(false);
            pendingFlush = scheduler.schedule(this::flush, debounceMs, TimeUnit.MILLISECONDS);

            // Every sale kicks the briefing further down the road, so it is spoken once the commander has
            // finished emptying the hold rather than after whichever sale happened to close a batch. The sale
            // debounce cannot carry this: it is deliberately short so each sale is confirmed promptly.
            briefingMarketId = event.getMarketID();
            if (pendingBriefing != null) pendingBriefing.cancel(false);
            pendingBriefing = scheduler.schedule(this::brief, briefingQuietMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Drops the pending timers and the thread behind them. The live instance runs for the life of the
     * application and never needs this; a test that builds its own does, so a timer it armed cannot fire
     * into a later test.
     */
    public void shutdown() {
        synchronized (pending) {
            if (pendingFlush != null) pendingFlush.cancel(false);
            if (pendingBriefing != null) pendingBriefing.cancel(false);
            pending.clear();
        }
        scheduler.shutdownNow();
    }

    /**
     * A docking is one briefing. Arriving anywhere re-arms it, so undocking and returning to the same
     * market - a loop route bouncing between two stations - briefs again as it should.
     */
    @Subscribe
    public void onDockedEvent(DockedEvent event) {
        synchronized (pending) {
            briefedMarketId = NOT_BRIEFED;
        }
    }

    /**
     * Speaks the next leg once selling has gone quiet. The timer alone would still brief twice if the
     * commander paused longer than the quiet period and then sold more at the same station, so the docking
     * the briefing was given for is remembered as well.
     */
    private void brief() {
        synchronized (pending) {
            if (briefingMarketId == briefedMarketId) return;
            briefedMarketId = briefingMarketId;
            briefNextLeg();
        }
    }

    /**
     * Retires the route leg that ended at the station just sold at. Every pending sale is from the same
     * docking, so the market is taken once; the DAO ignores it unless that market is where the leg being
     * flown ends. Caller holds the {@code pending} lock.
     */
    private void retireFlownLeg() {
        pending.stream()
                .map(MarketSellEvent::getMarketID)
                .distinct()
                .forEach(marketId -> TradeRouteManager.getInstance().deleteForMarketId(marketId));
    }

    private void flush() {
        synchronized (pending) {
            if (pending.isEmpty()) return;

            // Retire the flown leg once per docking, not once per commodity. MarketSell fires per commodity,
            // so a hold emptied of three goods used to raise three retirements - and on a loop whose next legs
            // end at the same station, two of them took legs the commander had not flown yet.
            retireFlownLeg();

            // Both figures are spelled out before they reach the sentence. A hold sold off a carrier is
            // four and five digits - 1320 tonnes for 45,132,120 credits - and passing those as numbers let
            // MessageFormat group them by locale, so the German and Italian voices were handed "1.320" and
            // "45.132.120" to read. The amount is hedged the same way every other credit figure in the app
            // is; the tonnage is not, because a count is something the commander can check.
            if (pending.size() == 1) {
                MarketSellEvent e = pending.getFirst();
                EventNarrator.say(
                        localizedEvent(
                                "event.market.sold.units",
                                TTSFriendlyNumberConverter.formatCountForSpeech(e.getCount()),
                                FuzzySearch.localizedCommodityName(e.getType()),
                                TTSFriendlyNumberConverter.formatCreditsForSpeech(e.getTotalSale())
                        )
                );
            } else {
                long total = pending.stream().mapToLong(MarketSellEvent::getTotalSale).sum();
                EventNarrator.say(localizedEvent("event.market.sold.multiple",
                        TTSFriendlyNumberConverter.formatCountForSpeech(pending.size()),
                        TTSFriendlyNumberConverter.formatCreditsForSpeech(total)));
            }

            pending.clear();

            // The briefing is NOT spoken here. Selling is manual, so a hold emptied one commodity at a time
            // closes one batch per sale - briefing from the batch spoke it after the first sale, with two
            // still to come. It waits on its own timer instead; see onMarketSellEvent.
            MonetizeRouteManager.getInstance().clear();
        }
    }

    /**
     * Speaks - and pins as a reminder - what to buy here and where to sell it. Once the flown leg has been
     * retired the next stop is the leg the commander is about to fly.
     */
    private void briefNextLeg() {
        final PlayerSession playerSession = PlayerSession.getInstance();
        final ReminderManager reminderManager = ReminderManager.getInstance();

        TradeRouteManager.TradeRouteLegTuple<Integer, TradeStopDto> nextStop =
                TradeRouteManager.getInstance().getNextStop();

        if (nextStop == null) {
            reminderManager.clear();
            return;
        }

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

        EventNarrator.say(tradeMessage);
        reminderManager.setReminder(tradeMessage, destinationSystem);
    }

    public record Reminder(Integer legNumber, TradeStopDto stopInfo,
                           List<TradeCommodity> commodities) implements ToJsonConvertible {
        @Override
        public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }
    }
}
