package elite.intel.gameapi.carrier;

import elite.intel.db.managers.SquadronCarrierManager;
import elite.intel.db.managers.StationMarketsManager;
import elite.intel.db.managers.StationMarketsManager.MarketSnapshot;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.session.PlayerSession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What the commander's own carrier is holding.
 * <p>
 * <b>Why this is only ever an approximation, and why it is still worth having.</b> The game gives a
 * third-party tool a full account of a carrier's cargo exactly once: it writes {@code Market.json} while
 * the commander is standing in that carrier's commodity market. Players know this - "dock and open the
 * market screen" is the standing advice for making a carrier's cargo visible to any external tool - so the
 * account is as good as the commander has kept it, and {@link Held#seenAt()} says when that was.
 * <p>
 * Cargo the commander themselves moves on or off afterwards IS journalled, and {@link CarrierHoldLedger}
 * keeps the account level with it, so this reads the ledger in preference to the raw snapshot. What no
 * event covers is somebody else buying off the carrier's sell orders - which is why the age of the last
 * market read still earns a caveat even when the ledger has been corrected since.
 * <p>
 * <b>Stock, not sell orders.</b> A carrier's market lists every good it has ever carried, most of them at
 * zero, and it reports what is aboard whether or not the owner has priced it for sale - measured against a
 * live carrier holding 42 Animal Monitors with no sell order set. So stock above zero is the test, and the
 * zeroes are the residue of what has already been hauled away.
 * <p>
 * <b>Identified by callsign, not station type.</b> A carrier's station name IS its callsign, and we know
 * our own. Fleet carriers report {@code StationType} {@code FleetCarrier}, but the squadron carrier's
 * designation is not in Frontier's published schema, so depending on it would mean guessing at a string
 * that decides whether the commander's own cargo is found. Matching the callsign needs no such guess.
 */
public final class OwnCarrierHold {

    private OwnCarrierHold() {
    }

    /**
     * A carrier's cargo as we last saw it, and where the carrier is now.
     *
     * @param callSign      the carrier's callsign, which is also its station name
     * @param starSystem    where the carrier is NOW, from the arrival events - NOT where it was standing
     *                      when its market was read. A carrier jumps; the market snapshot does not follow it
     * @param seenAt        when the commander last opened that market, or null when they never have. Not
     *                      when the ledger was last corrected: the doubt this measures is what OTHERS have
     *                      bought since, and our own hauling says nothing about that
     * @param stockBySymbol units aboard per journal symbol; only goods actually in stock appear
     */
    public record Held(String callSign, String starSystem, Instant seenAt, Map<String, Integer> stockBySymbol) {

        public Held {
            stockBySymbol = stockBySymbol == null ? Map.of() : Map.copyOf(stockBySymbol);
        }

        public int stockOf(String symbol) {
            return symbol == null ? 0 : stockBySymbol.getOrDefault(symbol, 0);
        }

        public boolean isEmpty() {
            return stockBySymbol.isEmpty();
        }
    }

    /**
     * Every carrier the commander owns that has a callsign, a known position and something aboard. A
     * commander can own one fleet carrier and one squadron carrier, and no more.
     * <p>
     * BOTH are returned rather than the first that happens to be carrying something. A fleet carrier holding
     * nothing but a few tonnes of leftover trade goods would otherwise mask a squadron carrier loaded with
     * exactly the construction materials the commander is looking for - the caller has to weigh them against
     * the list before it can know which one is the answer.
     * <p>
     * Empty when neither is carrying anything we know about, which is the ordinary state until the commander
     * docks at one and opens its market.
     */
    public static List<Held> ours() {
        List<Held> carriers = new ArrayList<>();
        fleetCarrier().ifPresent(carriers::add);
        squadronCarrier().ifPresent(carriers::add);
        return carriers;
    }

    public static Optional<Held> fleetCarrier() {
        PlayerSession session = PlayerSession.getInstance();
        // The carrier's position comes from the arrival events, never from the market snapshot: it jumps,
        // and the snapshot records where it was standing when the commander last read its shelves.
        return held(session.getFleetCarrierData(), session.getLastKnownCarrierLocation());
    }

    public static Optional<Held> squadronCarrier() {
        CarrierDataDto squadron = SquadronCarrierManager.getInstance().get();
        return held(squadron, squadron == null ? null : squadron.getStarName());
    }

    private static Optional<Held> held(CarrierDataDto carrier, String starSystem) {
        if (carrier == null) return Optional.empty();
        String callSign = carrier.getCallSign();
        if (callSign == null || callSign.isBlank()) return Optional.empty();
        if (starSystem == null || starSystem.isBlank()) return Optional.empty();

        Optional<MarketSnapshot> snapshot = StationMarketsManager.getInstance().stockedAt(callSign);

        // The ledger is the market screen plus everything moved since, so it answers for the hold wherever
        // we have one - including when it says the carrier is empty, which is the whole point: a snapshot
        // that still shows the steel aboard must not be allowed to speak for a carrier we just emptied.
        // The raw snapshot answers only for a carrier we have never tracked, which is also how a carrier
        // saved before the ledger existed keeps working until it is next docked at.
        Map<String, Integer> stock = CarrierHoldLedger.isTracking(carrier)
                ? CarrierHoldLedger.stockOf(carrier)
                : snapshot.map(MarketSnapshot::stockBySymbol).orElse(Map.of());
        if (stock.isEmpty()) return Optional.empty();

        return Optional.of(new Held(callSign, starSystem, snapshot.map(MarketSnapshot::seenAt).orElse(null),
                stock));
    }
}
