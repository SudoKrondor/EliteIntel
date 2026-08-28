package elite.intel.gameapi.search.spansh.commodity;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.FuzzySearch;
import elite.intel.db.managers.StationMarketsManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.search.spansh.station.DockingEffort;
import elite.intel.gameapi.search.spansh.station.StationSearchClient;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchCriteria;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchResultDto;
import elite.intel.gameapi.search.spansh.traderoute.TradeRouteSearchCriteria;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static elite.intel.util.StringUtls.localizedEvent;
import static elite.intel.util.StringUtls.localizedEventPlural;

/**
 * Finds the markets trading a commodity - selling it to the commander, or buying it from them - through one
 * Spansh stations search.
 * <p>
 * WHY one search serves both directions: a station's market entry carries both halves of every pair, so
 * "where can I buy tritium" and "where can I sell tritium" are the same request with the other half read.
 * Which half is {@link TradeSide}, and it is the ONLY difference: the escalation ladder, the first-hand
 * market override and the ranking are one behaviour, and a second copy of them would be a second set of
 * answers to the same question.
 * <p>
 * WHY one search and not a scan: this used to walk EDSM - every system in range, then every station in
 * every system, then every market in every station, filtering the commodity out in Java. Spansh answers
 * the same question in a single request because its stations index already carries the markets, and its
 * {@code marketplace} filter asks about stock and price on the commodity itself.
 * <p>
 * WHY the page is asked for in distance order even when the commander wants the best price: Spansh cannot
 * sort by the price of one commodity (a {@code buy_price} sort is accepted and quietly ignored), so the
 * cheapest is picked here, out of the nearest {@link #BEST_PRICE_CANDIDATES}. Trading the whole radius for
 * the near part of it is deliberate - the saving on a hold of cargo is rarely worth a hundred extra jumps.
 * <p>
 * WHY the trade profile does not choose the station types here: it sizes the hold and says what the ship
 * can physically dock at, but its surface RULES are a trade route preference, and a commander asking where
 * to buy mission cargo has not asked about trade routes. Gating this search on them made 140 of the 440
 * goods in our commodities table unbuyable - see
 * {@link TradeStationSearchCriteria.StationType#EVERY_STATIC_TRADE_TYPE}.
 * <p>
 * WHY fleet carriers come second and only when nothing else has the good: a carrier jumps. Its owner can
 * move it hundreds of light years between one Spansh sync and the commander arriving, so a static market is
 * always the better answer and is preferred whenever one exists. But measured live, whole categories - the
 * mined gems, the Thargoid parts, most of the rares - are sold by carriers and by NOTHING else: Alexandrite
 * is stocked by 241 carriers and 0 starports, Thargoid Sensors by 339 and 0. Refusing to name a carrier
 * would tell the commander those goods do not exist anywhere in the galaxy. So the carrier is offered, and
 * {@link CommoditySearchResult#isFleetCarrier()} makes the caller say out loud that it may have moved on.
 */
public final class SpanshCommoditySearch {

    /**
     * Comfortably above any station's large-pad count; the pad range is really "one or more".
     */
    private static final int LARGE_PADS_MANY = 100;
    /**
     * Upper end of an otherwise open range. Spansh has no "at least" comparison that works - see
     * {@link TradeStationSearchCriteria.Marketplace} - so an unbounded ask is written as a range that
     * nothing can exceed.
     */
    private static final int UNBOUNDED = Integer.MAX_VALUE;
    /**
     * The market has to be trading the good, not merely listing it: one tonne in stock when the commander
     * is buying, one tonne of demand when they are selling.
     */
    private static final int TRADING_AT_ALL = 1;
    /**
     * Ask for a hold's worth: what a caller that is filling the ship rather than topping up a mission wants.
     */
    public static final int WANT_FULL_HOLD = 0;
    /**
     * Stations to weigh for price. Each result carries the station's ENTIRE market (~50 KB of it), so this
     * is a page size paid for in megabytes, not a free widening of the search.
     */
    private static final int BEST_PRICE_CANDIDATES = 50;
    /**
     * Human space is roughly this wide around Sol, so a sweep this far covers every fixed market there is.
     * The last widening before the search gives up on stations and starts naming mobile ones.
     */
    private static final int INHABITED_BUBBLE_LY = 1000;
    /**
     * How recently Spansh must have seen a carrier for it to be worth flying to.
     * <p>
     * A carrier's recorded position is only its last sighting, and it jumps. Measured live, 147 carriers are
     * listed as selling "Hardware Diagnostic Sensor" but only 13 were seen in the last day - so nine out of
     * ten of those answers are a system the carrier has already left. This is the same window
     * {@code FindNearestFleetCarrierCommand} has always used for the same reason.
     */
    private static final int CARRIER_SEEN_WITHIN_DAYS = 1;
    /**
     * Stations to fetch when only the nearest one is wanted. The page comes back in distance order, so the
     * answer is the first row; the rest are here only so a station rejected below has a successor.
     */
    private static final int NEAREST_CANDIDATES = 10;

    /**
     * Words an English title leaves in lower case, and so does Spansh: "Fruit and Vegetables", never
     * "Fruit And Vegetables" - which matches nothing at all.
     */
    private static final Set<String> MINOR_WORDS = Set.of("and", "of", "the", "in", "a", "an", "to", "for", "on");

    /**
     * Spansh reads the window as ISO-8601 instants.
     */
    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private static final Logger log = LogManager.getLogger(SpanshCommoditySearch.class);

    private SpanshCommoditySearch() {
    }

    /**
     * Markets within {@code maxDistanceLy} selling {@code commodityToFind} a hold's worth of it, best
     * candidate first: nearest when {@code returnClosest}, cheapest otherwise.
     *
     * @param commodityToFind the commodity by its English name; capitalisation is forgiven, see {@link #spellings}
     * @param refStarSystem   the system distances are measured from
     * @param maxDistanceLy   search radius in light years
     * @param profile         the ship's trade profile, for hold size, arrival distance and pad size only
     */
    public static List<CommoditySearchResult> search(
            String commodityToFind, String refStarSystem, int maxDistanceLy,
            TradeRouteSearchCriteria profile, boolean returnClosest) {
        return search(commodityToFind, refStarSystem, maxDistanceLy, profile, returnClosest, WANT_FULL_HOLD);
    }

    /**
     * As {@link #search(String, String, int, TradeRouteSearchCriteria, boolean)}, for a caller that knows how
     * much it actually needs.
     * <p>
     * A commander who names a good is filling the hold, so a hold's worth is what the search looks for first.
     * A mission is not: it wants a stated number of units, and after a part load at one market it wants only
     * the remainder. Looking for a full hold on behalf of a mission still owing 20 tonnes passes over every
     * nearby market holding 25 and answers with a big one further out - so the amount wanted is what the
     * first attempt asks for, capped by what the ship can carry.
     *
     * @param wantedUnits units the caller needs, or {@link #WANT_FULL_HOLD} for a hold's worth
     */
    public static List<CommoditySearchResult> search(
            String commodityToFind, String refStarSystem, int maxDistanceLy,
            TradeRouteSearchCriteria profile, boolean returnClosest, int wantedUnits) {
        return search(commodityToFind, refStarSystem, maxDistanceLy, profile, returnClosest, wantedUnits,
                TradeSide.BUY);
    }

    /**
     * As above, on a stated side of the counter.
     * <p>
     * Selling, {@code wantedUnits} is the load to shift rather than the load to acquire, and "best" means
     * the market paying most rather than charging least. Everything else - the ladder, the part-trade
     * fallback, the carrier last resort, the first-hand override - is the same, because it is the same
     * question about the same index.
     *
     * @param side which half of each market entry to read; see {@link TradeSide}
     */
    public static List<CommoditySearchResult> search(
            String commodityToFind, String refStarSystem, int maxDistanceLy,
            TradeRouteSearchCriteria profile, boolean returnClosest, int wantedUnits, TradeSide side) {

        // Our own sightings are keyed by the game's symbol, not by the English name Spansh matches on.
        String symbol = FuzzySearch.commoditySymbol(commodityToFind);

        // Corrected BEFORE ranking, not after: a first-hand price that arrives once the page is already
        // sorted leaves the order deciding on figures no longer in the results.
        return climb(commodityToFind, refStarSystem, maxDistanceLy, profile, returnClosest, wantedUnits, side,
                stations -> sortBest(
                        correctWithFirstHandData(
                                asResults(stations, commodityToFind, side),
                                symbol, StationMarketsManager.getInstance()::lastSeen, side),
                        returnClosest, side));
    }

    /**
     * The markets that can fill the most of a shopping list, fullest first.
     * <p>
     * <b>Why this needs no second request.</b> The {@code marketplace} filter decides which STATIONS come
     * back, but every row Spansh returns already carries that station's entire market - see
     * {@link TradeStationSearchResultDto.StationResult#getMarket()}, and the page size that pays for it.
     * {@link #search} throws all of it away except the one commodity it was asked about. Cross-referencing
     * the rest of the list against what is already in hand costs nothing on the wire, and needs no guess
     * about whether Spansh ANDs several {@code marketplace} filters - a guess that would fail by silently
     * matching nothing rather than by erroring.
     * <p>
     * <b>Why the list is still anchored on one good.</b> The first entry drives the Spansh filter, so every
     * candidate sells it; the rest are whatever those candidates happen to also stock. That is the right
     * bias - the anchor is the reason for the trip, and a station that cannot supply it is no use however
     * much else it has.
     *
     * @param wanted       what to buy and how much of each, in the caller's own priority order, anchor first
     * @param holdCapacity tonnes the ship can carry; what the fill is measured against
     * @return one entry per candidate market, ordered by how much of the hold it can fill
     */
    public static List<BasketResult> searchBasket(
            List<WantedCommodity> wanted, String refStarSystem, int maxDistanceLy,
            TradeRouteSearchCriteria profile, boolean returnClosest, int holdCapacity) {

        List<WantedCommodity> list = mergeDuplicates(wanted);
        if (list.isEmpty()) return List.of();
        WantedCommodity anchor = list.getFirst();

        return climb(anchor.commodity(), refStarSystem, maxDistanceLy, profile, returnClosest,
                anchor.unitsWanted(), TradeSide.BUY,
                stations -> rankBaskets(stations, list, holdCapacity, returnClosest,
                        StationMarketsManager.getInstance()::lastSeen));
    }

    /**
     * Runs the escalation ladder for one anchor commodity, handing each page to {@code collect} and stopping
     * at the first rung that yields anything.
     * <p>
     * Shared by {@link #search} and {@link #searchBasket} so the two cannot drift: the ladder IS the search's
     * behaviour - which radius is tried when, when a part load becomes acceptable, when a fleet carrier is
     * finally considered - and a second copy of it would be a second set of answers to the same question.
     */
    private static <T> List<T> climb(
            String anchorCommodity, String refStarSystem, int maxDistanceLy, TradeRouteSearchCriteria profile,
            boolean returnClosest, int wantedUnits, TradeSide side,
            java.util.function.Function<List<TradeStationSearchResultDto.StationResult>, List<T>> collect) {

        // A hold's worth, or the commander flies out for a part load. Guarded because a floor of zero would
        // also match a market that lists the good and has none.
        int holdFull = Math.max(1, profile.getMaxCargo());
        int wanted = wantedUnits <= WANT_FULL_HOLD ? holdFull : Math.min(wantedUnits, holdFull);

        List<T> markets = List.of();
        List<Attempt> ladder = attempts(wanted, maxDistanceLy);
        for (int i = 0; i < ladder.size(); i++) {
            Attempt attempt = ladder.get(i);
            // Say what is about to be tried BEFORE trying it. The commander was told the search covers the
            // radius he named; every step past that is the search answering a question he did not ask, and a
            // silent escalation reads as a straight answer - which is how a carrier 50 ly away came back
            // looking like the nearest market rather than the last resort.
            if (i > 0) {
                announceEscalation(ladder.get(i - 1), attempt, side);
            }
            markets = collect.apply(stationsTrading(attempt, anchorCommodity, refStarSystem, profile, returnClosest, side));
            if (!markets.isEmpty()) {
                break;
            }
            log.debug("Nothing trades {} for {}; widening", anchorCommodity, attempt);
        }

        GameEventBus.publish(new MissionCriticalAnnouncementEvent(
                localizedEventPlural(markets.size(), "event.search.commodity.marketsFound")));

        return markets;
    }

    /**
     * The searches to try, in the order the commander would want them answered. Each is only run when the
     * one before it found nothing, so an ordinary good in a sensible radius is still a single request.
     * <p>
     * The radius widens twice - to double the stated one, then to the whole inhabited bubble - before a
     * fleet carrier is considered at all. A carrier IS a market, but a mobile one whose recorded position is
     * only where Spansh last saw it, so it is the desperation option rather than a peer of the starports.
     * Measured live from a commander's actual position: "Hardware Diagnostic Sensor" had nothing static
     * inside 120 ly, and the old ladder named a carrier 50 ly away - while Kanwar Gateway, a Coriolis with a
     * large pad and 5,477 units of it, sat at 202 ly and would still have been there on arrival.
     * <p>
     * WHY the wanted amount is looked for but not insisted on: measured live, "Neofabric Insulation" is sold
     * by 1,726 markets, but by only FOUR holding 300 tonnes of it. Demanding the ship's whole capacity told a
     * commander that an ordinary industrial good on sale 20 ly away does not exist, and the bigger his ship
     * the more goods vanished. The stated radius is asked for the full amount first and a part load second;
     * past that the commander is already flying a long way, and what is there is what is there. A part load
     * is a real answer rather than a failure - buy what is there, and the next search asks for the remainder.
     */
    private static List<Attempt> attempts(int wanted, int maxDistanceLy) {
        List<String> staticTypes = TradeStationSearchCriteria.StationType.EVERY_STATIC_TRADE_TYPE;
        List<String> carriers = TradeStationSearchCriteria.StationType.CARRIER_TRADE_TYPES;
        // Guarded so a commander who asks for a galaxy-wide radius does not overflow it into a negative one.
        int widened = maxDistanceLy > Integer.MAX_VALUE / 2 ? maxDistanceLy : maxDistanceLy * 2;
        // Never NARROWS an already-wide ask: a commander who said 2000 ly keeps his 4000 ly second sweep.
        int bubble = Math.max(widened, INHABITED_BUBBLE_LY);

        List<Attempt> ladder = new ArrayList<>();
        ladder.add(new Attempt(staticTypes, wanted, maxDistanceLy, false));
        ladder.add(new Attempt(staticTypes, TRADING_AT_ALL, maxDistanceLy, false));
        if (widened > maxDistanceLy) {
            ladder.add(new Attempt(staticTypes, TRADING_AT_ALL, widened, false));
        }
        if (bubble > widened) {
            ladder.add(new Attempt(staticTypes, TRADING_AT_ALL, bubble, false));
        }
        // Last resort, and only carriers seen recently enough for the sighting to mean anything.
        ladder.add(new Attempt(carriers, TRADING_AT_ALL, bubble, true));
        return List.copyOf(ladder);
    }

    /**
     * Voices the step from one attempt to the next, and only where it changes what the commander is being
     * offered: a wider radius, or the move from fixed markets to mobile ones. Widening the stock floor at the
     * same radius is not something he needs to hear about - the part-load note on the answer already says it.
     */
    private static void announceEscalation(Attempt previous, Attempt next, TradeSide side) {
        if (next.mustBeRecentlySeen() && !previous.mustBeRecentlySeen()) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(localizedEvent(
                    side == TradeSide.SELL
                            ? "event.search.commodity.tryingCarriersToSell"
                            : "event.search.commodity.tryingCarriers")));
        } else if (next.maxDistanceLy() > previous.maxDistanceLy()) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(localizedEvent(
                    "event.search.commodity.widening", previous.maxDistanceLy(), next.maxDistanceLy())));
        }
    }

    /**
     * Test seam: the attempt ladder is the search's contract, and its ORDER is the part worth pinning.
     */
    static List<Attempt> attemptsForTest(int wanted, int maxDistanceLy) {
        return attempts(wanted, maxDistanceLy);
    }

    /**
     * One search: which station types to ask about, how many tonnes a market must have on our side of the
     * counter - stock when buying, demand when selling - and whether the station has to have been seen recently. Only a carrier needs that last one - a starport is still where
     * Spansh last recorded it however long ago that was.
     */
    record Attempt(List<String> stationTypes, int minUnits, int maxDistanceLy, boolean mustBeRecentlySeen) {
        @Override
        public String toString() {
            return (stationTypes.size() == 1 ? stationTypes.getFirst() : "static markets")
                    + " with " + minUnits + "t within " + maxDistanceLy + " ly";
        }
    }

    /**
     * One search, over one set of station types. Returns the raw page, each row carrying that station's
     * whole market.
     * <p>
     * Separate from {@link #climb} so the fallback is a second call with different types rather than a
     * flag threaded through the request building - and so the announcement is published once, over the
     * markets the commander is actually being offered, not once per attempt.
     */
    private static List<TradeStationSearchResultDto.StationResult> stationsTrading(
            Attempt attempt, String commodityToFind, String refStarSystem,
            TradeRouteSearchCriteria profile, boolean returnClosest, TradeSide side) {

        TradeStationSearchCriteria criteria =
                searchCriteria(commodityToFind, refStarSystem, profile, returnClosest, attempt, side);
        log.debug("Commodity search criteria: {}", criteria.toJson());

        TradeStationSearchResultDto response = StationSearchClient.getInstance().searchStations(criteria);
        // A failed POST, a search that times out and an empty body all arrive here as a null.
        return response == null || response.getResults() == null ? List.of() : response.getResults();
    }

    /**
     * Where the commander has already been, what they saw there wins.
     * <p>
     * Spansh is crowd-sourced, so a market it lists as selling a good may have been emptied - or may
     * never have stocked it - since whoever uploaded that entry was last there. Measured live: Boldyr
     * Dredging Installation in Mat Zemlya is listed as selling Silver at 25,961, and the game's own
     * {@code Market.json} for that settlement reports {@code Stock: 0}. The commander flew there,
     * found nothing, asked again - and was sent to the same settlement he was standing in.
     * <p>
     * {@code Market.json} is the game speaking rather than a stranger's upload, so a market our own
     * eyes have emptied is dropped from the answer entirely; the ladder then widens as if nothing sold
     * the good there, which is the truth. A sighting that found SOME stock corrects the supply figure
     * instead of dropping the market - a part load is still worth flying to.
     * <p>
     * The sighting only wins while it is the fresher of the two. Once Spansh has been told about that
     * market more recently than the commander last stood in it, its number is the newer one and the
     * override lapses - which is what stops one empty visit blacklisting a market for good.
     *
     * @param commoditySymbol the good's journal symbol; null for a legacy good with none, which cannot
     *                        be matched against a market snapshot and is therefore left alone
     */
    static List<CommoditySearchResult> correctWithFirstHandData(
            List<CommoditySearchResult> markets, String commoditySymbol, MarketSightings sightings) {
        return correctWithFirstHandData(markets, commoditySymbol, sightings, TradeSide.BUY);
    }

    /**
     * As above, reading our own snapshot on the side being traded: the stock we saw when buying, the demand
     * we saw when selling. Reading stock on a sell search would drop every market that wants the good and
     * has none of it, which is every market worth selling to.
     */
    static List<CommoditySearchResult> correctWithFirstHandData(
            List<CommoditySearchResult> markets, String commoditySymbol, MarketSightings sightings, TradeSide side) {
        if (commoditySymbol == null || markets.isEmpty()) return markets;

        List<CommoditySearchResult> kept = new ArrayList<>();
        for (CommoditySearchResult market : markets) {
            Optional<StationMarketsManager.Sighting> seen =
                    sightings.lastSeen(market.getStarSystem(), market.getStationName(), commoditySymbol);
            if (seen.isEmpty() || !isFresherThanSpansh(seen.get(), market.getMarketUpdatedAt())) {
                kept.add(market);
                continue;
            }
            long unitsWeSaw = side == TradeSide.SELL ? seen.get().demand() : seen.get().stock();
            if (unitsWeSaw <= 0) {
                log.debug("Skipping {} - {}: we were there at {} and it had no {} to trade ({})",
                        market.getStarSystem(), market.getStationName(), seen.get().seenAt(), commoditySymbol, side);
                continue;
            }
            market.setSupply(unitsWeSaw);
            // The price is corrected on the same terms as the quantity, and matters more: Spansh quoted Bari
            // Gateway at 57,844 for Tritium from a row 10 days old while the game was paying 53,992, and the
            // commander flew there on the strength of the higher number.
            int priceWeSaw = side == TradeSide.SELL ? seen.get().sellPrice() : seen.get().buyPrice();
            if (priceWeSaw > 0) {
                market.setPrice(priceWeSaw);
                market.setSeenFirstHand(true);
            }
            kept.add(market);
        }
        return kept;
    }

    /**
     * Whether our own look at the market is the newer of the two claims. A Spansh row with no timestamp
     * at all loses: it is second-hand data of unknown age against something the game told us directly.
     */
    private static boolean isFresherThanSpansh(StationMarketsManager.Sighting seen, String marketUpdatedAt) {
        Instant spansh = parseInstant(marketUpdatedAt);
        return spansh == null || seen.seenAt() == null || !seen.seenAt().isBefore(spansh);
    }

    /**
     * Spansh stamps its market rows {@code 2026-08-18 04:56:12+00} - a space instead of the T, and an
     * offset of {@code +00} rather than a Z. {@link Instant#parse} throws on every one of them.
     * <p>
     * That mattered silently. A row whose timestamp would not parse counted as having no timestamp at all,
     * so {@link #isFresherThanSpansh} said yes to everything and the commander's own sighting won however
     * old it was - the exact "one empty visit blacklists a market for good" the override was written to
     * avoid. It also made the age of a quote unknowable, which is the number that says whether to believe
     * the price.
     */
    private static final DateTimeFormatter SPANSH_TIMESTAMP = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd").appendLiteral(' ').appendPattern("HH:mm:ss")
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true).optionalEnd()
            .appendPattern("[XXX][XX][X]")
            .toFormatter();

    static Instant parseInstant(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) return null;
        try {
            return Instant.parse(timestamp);
        } catch (DateTimeParseException e) {
            try {
                return Instant.from(SPANSH_TIMESTAMP.parse(timestamp));
            } catch (RuntimeException other) {
                return null;
            }
        }
    }

    /**
     * How stale Spansh's word on this market is, or empty when it does not say. The price is the whole
     * answer to "where do I sell this", and a price nobody has re-uploaded in ten days is a different claim
     * from one uploaded this morning.
     */
    public static OptionalLong daysSinceUpdate(String marketUpdatedAt) {
        Instant updated = parseInstant(marketUpdatedAt);
        return updated == null ? OptionalLong.empty()
                : OptionalLong.of(Math.max(0, ChronoUnit.DAYS.between(updated, Instant.now())));
    }

    /**
     * Test seam over the stored {@code Market.json} snapshots, so the override can be exercised without
     * a database.
     */
    @FunctionalInterface
    interface MarketSightings {
        Optional<StationMarketsManager.Sighting> lastSeen(String starSystem, String stationName, String commoditySymbol);
    }

    /**
     * Puts the answer the commander asked for at the head of the list: the nearest market, or the one
     * selling cheapest.
     * <p>
     * Separate from the call because the ORDER is the answer - the caller flies to {@code getFirst()} and
     * never looks at the rest - and because Spansh hands the page back in distance order regardless, so
     * a best-price search that forgot to re-sort would look entirely correct in the log.
     */
    static List<CommoditySearchResult> rank(
            List<TradeStationSearchResultDto.StationResult> stations, String commodityToFind, boolean returnClosest) {
        return rank(stations, commodityToFind, returnClosest, TradeSide.BUY);
    }

    /**
     * As above, on a stated side of the counter. "Best price" means the LOWEST when the commander is paying
     * and the HIGHEST when they are being paid, so the side chooses the direction of the same sort.
     */
    static List<CommoditySearchResult> rank(
            List<TradeStationSearchResultDto.StationResult> stations, String commodityToFind,
            boolean returnClosest, TradeSide side) {
        return sortBest(asResults(stations, commodityToFind, side), returnClosest, side);
    }

    /**
     * The page as results, in the order Spansh sent them - every station that actually trades the good on
     * this side. Split from the sort so a first-hand correction can land between the two.
     */
    static List<CommoditySearchResult> asResults(
            List<TradeStationSearchResultDto.StationResult> stations, String commodityToFind, TradeSide side) {
        List<CommoditySearchResult> results = new ArrayList<>();
        for (TradeStationSearchResultDto.StationResult station : stations) {
            TradeStationSearchResultDto.StationResult.MarketEntry entry = tradesIt(station, commodityToFind, side);
            if (entry == null) continue;
            results.add(asResult(station, entry, side));
        }
        return results;
    }

    /**
     * Puts the answer the commander asked for at the head: the nearest market, or the one trading best.
     */
    static List<CommoditySearchResult> sortBest(
            List<CommoditySearchResult> results, boolean returnClosest, TradeSide side) {
        List<CommoditySearchResult> sorted = new ArrayList<>(results);
        Comparator<CommoditySearchResult> byPrice = Comparator.comparingDouble(CommoditySearchResult::getPrice);
        sorted.sort(Comparator.comparingInt((CommoditySearchResult result) -> DockingEffort.of(result.getStationType()))
                .thenComparing(returnClosest
                        ? Comparator.comparingDouble(CommoditySearchResult::getDistanceFromPlayer)
                        : (side.dearerIsBetter() ? byPrice.reversed() : byPrice)));
        return sorted;
    }

    /**
     * One line per commodity, keeping the caller's order and summing what the repeats asked for.
     * <p>
     * A mission stack is where this bites: two Haematite contracts are two rows on the board and one good at
     * the market. Left as two lines, the allocator would sell the commander the same tonnes twice - once
     * against each - and the card would list Haematite under itself.
     */
    static List<WantedCommodity> mergeDuplicates(List<WantedCommodity> wanted) {
        if (wanted == null) return List.of();
        LinkedHashMap<String, WantedCommodity> merged = new LinkedHashMap<>();
        for (WantedCommodity want : wanted) {
            if (want == null || want.commodity() == null || want.unitsWanted() <= 0) continue;
            merged.merge(want.commodity().toLowerCase(Locale.ROOT), want,
                    (first, repeat) -> new WantedCommodity(first.symbol(), first.commodity(),
                            first.unitsWanted() + repeat.unitsWanted()));
        }
        return List.copyOf(merged.values());
    }

    /**
     * Weighs every candidate market against the whole shopping list and puts the fullest trip first.
     * <p>
     * <b>Fill outranks proximity, but only inside the radius the ladder already settled on.</b> The page
     * being ranked is the one {@link #search} would have picked its single answer from, so preferring a
     * fuller market can never send the commander further than asking for the anchor alone would have. What
     * it can do is turn nine round trips for sixty tonnes each into two.
     * <p>
     * Docking effort still breaks the tie ahead of distance or price: a station and an outpost that fill the
     * hold equally are not equal work.
     */
    static List<BasketResult> rankBaskets(
            List<TradeStationSearchResultDto.StationResult> stations, List<WantedCommodity> wanted,
            int holdCapacity, boolean returnClosest, MarketSightings sightings) {

        List<BasketResult> results = new ArrayList<>();
        for (TradeStationSearchResultDto.StationResult station : stations) {
            BasketResult basket = fill(station, wanted, holdCapacity, sightings);
            if (basket != null) results.add(basket);
        }

        results.sort(Comparator.comparingInt(BasketResult::totalUnits).reversed()
                .thenComparingInt(result -> DockingEffort.of(result.stationType()))
                .thenComparing(returnClosest
                        ? Comparator.comparingDouble(BasketResult::distanceFromPlayer)
                        : Comparator.comparingDouble(result -> result.anchor().price())));
        return results;
    }

    /**
     * Loads one market into the hold: the anchor first, then whatever else on the list it stocks, until the
     * hold is full or the list runs out.
     * <p>
     * Null when the market cannot supply the anchor at all. That is not the same as stocking nothing: a
     * market with four of the small lines and none of the steel is not where this trip is going, because the
     * steel is why the trip exists. The Spansh filter has already required the anchor, so this only rejects
     * a market our own eyes have since emptied.
     * <p>
     * The list is walked in the caller's order rather than re-sorted here. Every tonne weighs the same, so
     * any order fills the hold equally; what the order decides is WHICH goods get the last of the space, and
     * only the caller knows that - a construction site wants its long pole, a mission stack wants whatever
     * expires first.
     */
    private static BasketResult fill(
            TradeStationSearchResultDto.StationResult station, List<WantedCommodity> wanted,
            int holdCapacity, MarketSightings sightings) {

        if (station.getMarket() == null || wanted.isEmpty()) return null;

        Map<String, TradeStationSearchResultDto.StationResult.MarketEntry> onSale = new HashMap<>();
        for (TradeStationSearchResultDto.StationResult.MarketEntry entry : station.getMarket()) {
            if (entry.getCommodity() == null) continue;
            onSale.putIfAbsent(entry.getCommodity().toLowerCase(Locale.ROOT), entry);
        }

        int remaining = Math.max(1, holdCapacity);
        List<BasketResult.BasketLine> lines = new ArrayList<>();
        for (WantedCommodity want : wanted) {
            if (remaining <= 0) break;
            BasketResult.BasketLine line = lineFor(station, onSale, want, remaining, sightings);
            if (line == null) {
                // The anchor is the first entry, and a market that cannot supply it is not a candidate.
                if (lines.isEmpty()) return null;
                continue;
            }
            lines.add(line);
            remaining -= line.unitsToBuy();
        }
        if (lines.isEmpty()) return null;

        return new BasketResult(
                station.getSystemName(),
                station.getName(),
                station.getType(),
                TradeStationSearchCriteria.StationType.CARRIER_TRADE_TYPES.contains(station.getType()),
                station.getDistance() == null ? 0 : station.getDistance(),
                station.getMarketUpdatedAt(),
                lines);
    }

    /**
     * One good at one market: what it costs, how much is really there, and how much of it fits.
     * <p>
     * The supply is corrected the same way {@link #correctWithFirstHandData} corrects the single-commodity
     * answer, and for the same reason - Spansh is crowd-sourced, and {@code Market.json} is the game
     * speaking. Applied per line rather than per station: one emptied shelf should cost the commander that
     * good, not the whole trip.
     */
    private static BasketResult.BasketLine lineFor(
            TradeStationSearchResultDto.StationResult station,
            Map<String, TradeStationSearchResultDto.StationResult.MarketEntry> onSale,
            WantedCommodity want, int roomLeft, MarketSightings sightings) {

        if (want.commodity() == null || want.unitsWanted() <= 0) return null;
        TradeStationSearchResultDto.StationResult.MarketEntry entry =
                onSale.get(want.commodity().toLowerCase(Locale.ROOT));
        if (entry == null || entry.getBuyPrice() == null || entry.getBuyPrice() <= 0) return null;

        long supply = entry.getSupply() == null ? 0 : entry.getSupply();
        if (want.symbol() != null) {
            Optional<StationMarketsManager.Sighting> seen =
                    sightings.lastSeen(station.getSystemName(), station.getName(), want.symbol());
            if (seen.isPresent() && isFresherThanSpansh(seen.get(), station.getMarketUpdatedAt())) {
                supply = seen.get().stock();
            }
        }
        if (supply <= 0) return null;

        int units = (int) Math.min(Math.min(want.unitsWanted(), roomLeft), supply);
        if (units <= 0) return null;
        return new BasketResult.BasketLine(want.symbol(), entry.getCommodity(), entry.getBuyPrice(), supply, units);
    }

    /**
     * The request body.
     * <p>
     * Package-private and separate from the call so the wire shape can be asserted without a live search:
     * Spansh ignores a filter key it does not recognise and matches nothing at all against a value it does
     * not know, so either mistake narrows the search silently instead of failing it, and the commander just
     * hears that the commodity is nowhere to be found.
     */
    static TradeStationSearchCriteria searchCriteria(
            String commodityToFind, String refStarSystem,
            TradeRouteSearchCriteria profile, boolean returnClosest, Attempt attempt) {
        return searchCriteria(commodityToFind, refStarSystem, profile, returnClosest, attempt, TradeSide.BUY);
    }

    /**
     * As above, on a stated side of the counter.
     */
    static TradeStationSearchCriteria searchCriteria(
            String commodityToFind, String refStarSystem, TradeRouteSearchCriteria profile,
            boolean returnClosest, Attempt attempt, TradeSide side) {

        // The types and the stock floor are the attempt's, because the same request is sent again with a
        // wider one when it comes back empty - see attempts(). EVERY_STATIC_TRADE_TYPE says why the
        // profile's surface rules stop at the trade route and do not reach this search.
        TradeStationSearchCriteria.StationType stationType = new TradeStationSearchCriteria.StationType();
        stationType.setTypes(attempt.stationTypes());

        /// NOTE: Spansh API is very inconsistent. The light year radius takes a min/max pair of STRINGS and
        /// is silently ignored when sent as a "<=>" range - unlike every other range filter here.
        TradeStationSearchCriteria.Distance distance = new TradeStationSearchCriteria.Distance();
        distance.setMin(0);
        distance.setMax(attempt.maxDistanceLy());

        // The commander's half of each pair, and ONLY that half: asking a sell search for supply would
        // return the markets already holding the good rather than the ones that want it.
        TradeStationSearchCriteria.Marketplace marketplace = new TradeStationSearchCriteria.Marketplace(spellings(commodityToFind));
        TradeStationSearchCriteria.RangeFilter units =
                new TradeStationSearchCriteria.RangeFilter(attempt.minUnits(), UNBOUNDED);
        TradeStationSearchCriteria.RangeFilter tradingAtAll =
                new TradeStationSearchCriteria.RangeFilter(TRADING_AT_ALL, UNBOUNDED);
        if (side == TradeSide.SELL) {
            marketplace.setDemand(units);
            marketplace.setSellPrice(tradingAtAll);
        } else {
            marketplace.setSupply(units);
            marketplace.setBuyPrice(tradingAtAll);
        }

        TradeStationSearchCriteria.Filters filters = new TradeStationSearchCriteria.Filters();
        filters.setStationType(stationType);
        filters.setDistanceToStarSystem(distance);
        filters.setDistanceToArrival(new TradeStationSearchCriteria.RangeFilter(0, profile.getMaxLsFromArrival()));
        filters.setServices(List.of(new TradeStationSearchCriteria.Service(List.of(TradeStationSearchCriteria.MARKET_SERVICE))));
        filters.setMarketplace(List.of(marketplace));
        if (attempt.mustBeRecentlySeen()) {
            // Only a sighting this recent says anything about where the carrier is NOW.
            Instant now = Instant.now();
            TradeStationSearchCriteria.UpdatedAt seen = new TradeStationSearchCriteria.UpdatedAt();
            seen.setComparison("<=>");
            seen.setValue(List.of(
                    ISO.format(now.minus(CARRIER_SEEN_WITHIN_DAYS, ChronoUnit.DAYS)), ISO.format(now)));
            filters.setUpdatedAt(seen);
        }
        if (profile.isRequiresLargePad()) {
            filters.setLargePads(new TradeStationSearchCriteria.RangeFilter(1, LARGE_PADS_MANY));
        }

        TradeStationSearchCriteria criteria = new TradeStationSearchCriteria();
        criteria.setFilters(filters);
        criteria.setReferenceSystem(refStarSystem);
        criteria.setSort(List.of(new TradeStationSearchCriteria.DistanceSort()));
        criteria.setSize(returnClosest ? NEAREST_CANDIDATES : BEST_PRICE_CANDIDATES);
        criteria.setPage(0);
        return criteria;
    }

    /**
     * The commodity name in every capitalisation Spansh might be holding it under.
     * <p>
     * WHY more than one: Spansh matches the name EXACTLY - "gold" and "Fruit And Vegetables" both return
     * zero stations where "Gold" and "Fruit and Vegetables" return thousands - and our own commodity table
     * disagrees with Spansh's spelling on a handful of goods ("Liquid Oxygen" against Spansh's "Liquid
     * oxygen", "Eden Apples Of Aerial" against "Eden Apples of Aerial"). Sending the alternatives costs
     * nothing: the filter ORs the list and a name Spansh does not know contributes no stations rather than
     * poisoning the query - measured live, {@code ["Gold", "Nonexistent Widget"]} returns the Gold markets.
     * <p>
     * The name we were given always leads, because for 389 of the 398 goods Spansh sells it is already
     * right, and the variants are only ever guesses at the rest.
     */
    static List<String> spellings(String commodity) {
        String lower = commodity.toLowerCase();
        List<String> spellings = new ArrayList<>();
        for (String spelling : List.of(commodity, titleCase(lower, false), titleCase(lower, true), sentenceCase(lower))) {
            if (!spellings.contains(spelling)) spellings.add(spelling);
        }
        return spellings;
    }

    /**
     * "liquid oxygen" as "Liquid Oxygen", or as "The Waters of Shintara" when {@code keepMinorWordsLower} -
     * English titles leave "and", "of" and "the" in lower case, and Spansh's names follow suit.
     */
    private static String titleCase(String lower, boolean keepMinorWordsLower) {
        String[] words = lower.split(" ");
        StringBuilder titled = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) titled.append(" ");
            titled.append(i > 0 && keepMinorWordsLower && MINOR_WORDS.contains(words[i]) ? words[i] : capitalised(words[i]));
        }
        return titled.toString();
    }

    /**
     * "liquid oxygen" as "Liquid oxygen" - Spansh spells a few goods this way.
     */
    private static String sentenceCase(String lower) {
        return capitalised(lower);
    }

    private static String capitalised(String word) {
        return word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    /**
     * The station's market entry for the commodity, or null when it has none.
     * <p>
     * Belt and braces over the {@code marketplace} filter, which has already answered this question - but
     * the entry is where the price and the exact commodity name come from, so it has to be found anyway,
     * and a row that somehow arrives without one is dropped rather than reported at a price of zero.
     */
    private static TradeStationSearchResultDto.StationResult.MarketEntry tradesIt(
            TradeStationSearchResultDto.StationResult station, String commodity, TradeSide side) {
        if (station.getMarket() == null) return null;
        return station.getMarket().stream()
                .filter(entry -> commodity.equalsIgnoreCase(entry.getCommodity()))
                .filter(entry -> side.priceOn(entry) != null && side.priceOn(entry) > 0)
                .findFirst()
                .orElse(null);
    }

    private static CommoditySearchResult asResult(
            TradeStationSearchResultDto.StationResult station,
            TradeStationSearchResultDto.StationResult.MarketEntry entry, TradeSide side) {
        CommoditySearchResult result = new CommoditySearchResult();
        result.setCommodity(entry.getCommodity());
        result.setPrice(side.priceOn(entry));   // what the commander pays, or is paid
        result.setSupply(side.unitsOn(entry));
        result.setStarSystem(station.getSystemName());
        result.setStationName(station.getName());
        result.setStationType(station.getType());
        result.setFleetCarrier(TradeStationSearchCriteria.StationType.CARRIER_TRADE_TYPES.contains(station.getType()));
        result.setDistanceFromPlayer(station.getDistance() == null ? 0 : station.getDistance());
        result.setMarketUpdatedAt(station.getMarketUpdatedAt());
        return result;
    }
}
