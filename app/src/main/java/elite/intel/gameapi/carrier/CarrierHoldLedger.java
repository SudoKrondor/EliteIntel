package elite.intel.gameapi.carrier;

import elite.intel.db.managers.StationMarketsManager;
import elite.intel.gameapi.JournalSymbol;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.carrier.OurCarriers.Ours;
import elite.intel.gameapi.journal.events.CargoTransferEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.session.DockedMarket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What the commander's own carrier is holding, kept level between the rare times the game says so.
 * <p>
 * <b>The gap this closes.</b> {@code Market.json} is the only full account of a carrier's cargo a
 * third-party tool ever gets, and it is written only while the commander stands in that carrier's market -
 * see {@link OwnCarrierHold}. Left at that, the snapshot goes wrong the moment they use the carrier for
 * what it is for: load 640 tonnes of steel out of it, haul them to a build, and the app still believes the
 * steel is aboard, so the next "what does the site need" sends them back to an empty hold. Measured live:
 * that is exactly what happened.
 * <p>
 * <b>Why the ledger is the answer and not a shorter staleness window.</b> The move itself IS journalled -
 * {@code CargoTransfer} names the good, the count and the direction. So the cargo is not unobservable, only
 * unobserved. Seeding from each {@code Market.json} and applying every transfer since keeps an account
 * that is right whenever the commander moved the cargo themselves, which is nearly always.
 * <p>
 * The commander's own market trades at the carrier count too: buying off its shelves and selling onto them
 * move exactly the same cargo a transfer would, and both name the market they happened at.
 * <p>
 * <b>What it still cannot see</b> is somebody else buying off the carrier's sell orders, which is why
 * {@link CarrierSupply#snapshotIsStale} still measures age from the last market read rather than the last
 * transfer. A ledger that is right about our own hauling is not the same as a ledger that is right.
 * <p>
 * <b>Tritium is the one line that means something beyond cargo.</b> A carrier burns it to jump, and the
 * tonnes sitting in its hold are the tonnes its tank can still be topped up with - which is why the ledger
 * keeps {@code CarrierDataDto.getFuelReserve()} level with its own tritium line. See
 * {@link #reserveFromMarket} for why a market that shows none is not the same as a carrier that has none.
 * <p>
 * <b>Why the stored {@code Market.json} is left alone.</b> Those rows are also the app's first-hand market
 * data, used to veto stale Spansh listings - they are a record of what the game said, and editing them
 * would turn a record into a guess. The correction belongs on top of the snapshot, not inside it.
 */
public final class CarrierHoldLedger {

    /**
     * The carrier's fuel, which is also an ordinary commodity. Bare journal symbol, as everything here is.
     */
    private static final String TRITIUM = "tritium";

    private CarrierHoldLedger() {
    }

    /**
     * Replaces a carrier's ledger with what its market screen just reported, when the market is one of
     * ours. Anything else - a station, another commander's carrier - is not our cargo and is ignored.
     * <p>
     * A wholesale replacement, not a merge: the game has just given a complete account, so anything our
     * ledger holds that it does not mention is gone.
     */
    public static void seedFrom(GameEvents.MarketEvent market) {
        if (market == null) return;
        Optional<Ours> ours = OurCarriers.byCallSign(market.getStationName());
        if (ours.isEmpty()) return;

        ours.get().update(carrier -> {
            Map<String, Integer> stock = stockOf(market);
            carrier.replaceCommodities(stock);
            reserveFromMarket(carrier, stock);
        });
    }

    /**
     * Takes the carrier's spare-tritium figure from its own market listing, when that listing says anything.
     * <p>
     * <b>Why only when the stock is above zero.</b> A carrier's market shows a good at zero for two reasons
     * that look identical from outside: the owner has none, or the owner never put it up. Most commanders
     * never list their tritium at all - it is fuel, not merchandise - so reading a zero as "the tank money is
     * gone" would wipe a figure the commander has been keeping by hand for the sake of one they never asked
     * for. A number above zero is unambiguous, and it is the only case worth acting on.
     * <p>
     * That the manual figure survives is the point: {@code SetCarrierFuelReserveCommand} stays the answer for
     * everyone who does not trade their fuel, and this quietly does the work for everyone who does.
     */
    private static void reserveFromMarket(CarrierDataDto carrier, Map<String, Integer> stock) {
        int tritium = stock.getOrDefault(TRITIUM, 0);
        if (tritium <= 0) return;
        carrier.setFuelReserve(tritium);
    }

    /**
     * Applies one good moving on or off the carrier, and keeps the fuel reserve level when that good is the
     * fuel.
     * <p>
     * A movement we watched is worth acting on in BOTH directions, unlike a market listing: tritium carried
     * off to top up the tank has left the hold as surely as tritium carried aboard has joined it. Only
     * adding was how the reserve came to climb for ever.
     */
    private static void moved(CarrierDataDto carrier, String symbol, int units) {
        if (units > 0) {
            carrier.addCommodity(symbol, units);
        } else {
            carrier.removeCommodity(symbol, -units);
        }
        if (TRITIUM.equals(symbol)) {
            carrier.adjustFuelReserve(units);
        }
    }

    /**
     * Applies a cargo move between the ship and the carrier it is standing on.
     * <p>
     * <b>Attributed by the pad, never assumed.</b> A {@code CargoTransfer} names no station, and a commander
     * can own both a fleet carrier and a squadron carrier - so the carrier is the one whose callsign matches
     * the port under the ship. That guard also keeps SRV cargo out of the ledger: those transfers happen
     * planetside, on no pad at all, and would otherwise be deducted from a carrier that never held the goods.
     */
    public static void transferred(List<CargoTransferEvent.Transfer> transfers) {
        if (transfers == null || transfers.isEmpty()) return;
        Optional<Ours> ours = OurCarriers.byCallSign(DockedMarket.getInstance().stationName());
        if (ours.isEmpty()) return;

        ours.get().update(carrier -> {
            adoptSnapshot(carrier);
            for (CargoTransferEvent.Transfer transfer : transfers) {
                if (transfer == null) continue;
                String symbol = JournalSymbol.normalize(transfer.getType());
                if (symbol == null || transfer.getCount() <= 0) continue;
                if ("tocarrier".equalsIgnoreCase(transfer.getDirection())) {
                    moved(carrier, symbol, transfer.getCount());
                } else if ("toship".equalsIgnoreCase(transfer.getDirection())) {
                    moved(carrier, symbol, -transfer.getCount());
                }
            }
        });
    }

    /**
     * Applies a purchase off a market's shelves: cargo leaves the seller, so our carrier's hold falls when
     * the seller was our carrier.
     * <p>
     * Worth applying even though the commander has the market screen open at the time: {@code Market.json}
     * is written when that screen OPENS, so it records the shelves before the trade, and every tonne bought
     * afterwards is invisible until the screen is opened again.
     */
    public static void bought(long marketId, String type, int count) {
        traded(marketId, type, -count);
    }

    /**
     * Applies a sale onto a market's shelves: cargo joins the buyer, so our carrier's hold rises when the
     * buyer was our carrier.
     */
    public static void sold(long marketId, String type, int count) {
        traded(marketId, type, count);
    }

    private static void traded(long marketId, String type, int units) {
        String symbol = JournalSymbol.normalize(type);
        if (symbol == null || units == 0) return;
        Optional<Ours> ours = atMarket(marketId);
        if (ours.isEmpty()) return;

        ours.get().update(carrier -> {
            adoptSnapshot(carrier);
            moved(carrier, symbol, units);
        });
    }

    /**
     * Which of our carriers owns this market, if either does.
     * <p>
     * By id first, which is exact: a carrier's id and its MarketID are the same number. A carrier whose
     * management panel has never been opened has no id on file yet, and for that one the pad is the fallback
     * - the MarketID has to be the pad the ship is standing on before the pad's NAME can be trusted to say
     * whose market it is. The trade names the market but not its owner; the pad names the owner but not the
     * market, and is only good while the two agree.
     */
    private static Optional<Ours> atMarket(long marketId) {
        Optional<Ours> byId = OurCarriers.byId(marketId);
        if (byId.isPresent()) return byId;

        DockedMarket pad = DockedMarket.getInstance();
        if (marketId == 0 || pad.marketId() != marketId) return Optional.empty();
        return OurCarriers.byCallSign(pad.stationName());
    }

    /**
     * Starts an untracked carrier's ledger off from the last market screen we stored for it, so the first
     * transfer is a correction to a real account rather than the whole of one.
     * <p>
     * Without this, taking 640 tonnes of steel off a carrier we had never tracked would subtract from
     * nothing and leave the ledger empty - and an empty ledger would let the old snapshot, which still shows
     * the steel aboard, answer for it. This is also the upgrade path for a carrier saved before the ledger
     * existed.
     */
    private static void adoptSnapshot(CarrierDataDto carrier) {
        if (carrier.isHoldTracked()) return;
        carrier.replaceCommodities(StationMarketsManager.getInstance().stockedAt(carrier.getCallSign())
                .map(StationMarketsManager.MarketSnapshot::stockBySymbol)
                .orElse(Map.of()));
    }

    /**
     * Whether we have an account of this carrier's hold at all, as opposed to no idea.
     * <p>
     * The distinction the caller cannot make for itself: an emptied carrier and an unexamined one both hold
     * nothing, and only the first should silence the market snapshot.
     */
    public static boolean isTracking(CarrierDataDto carrier) {
        return carrier != null && carrier.isHoldTracked();
    }

    /**
     * A carrier's ledger, keyed by bare journal symbol, holding only what is actually aboard.
     * <p>
     * Empty for a carrier we have neither read a market for nor moved cargo on or off - which is honest,
     * and is what makes it safe for the caller to fall back to the raw snapshot.
     */
    public static Map<String, Integer> stockOf(CarrierDataDto carrier) {
        if (carrier == null) return Map.of();
        Map<String, Integer> stock = new HashMap<>();
        for (Map.Entry<String, Integer> entry : carrier.getCommodity().entrySet()) {
            String symbol = JournalSymbol.normalize(entry.getKey());
            if (symbol == null || entry.getValue() == null || entry.getValue() <= 0) continue;
            stock.merge(symbol, entry.getValue(), Integer::sum);
        }
        return stock;
    }

    private static Map<String, Integer> stockOf(GameEvents.MarketEvent market) {
        Map<String, Integer> stock = new HashMap<>();
        if (market.getItems() == null) return stock;
        for (GameEvents.MarketEvent.MarketItem item : market.getItems()) {
            // Zero is not stock: a carrier's market goes on listing a good long after the last tonne of it
            // was hauled away.
            if (item == null || item.getStock() <= 0) continue;
            String symbol = JournalSymbol.normalize(item.getName());
            if (symbol == null) continue;
            stock.merge(symbol, item.getStock(), Integer::sum);
        }
        return stock;
    }
}
