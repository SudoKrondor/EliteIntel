package elite.intel.gameapi.search.spansh.station;

import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.ShipManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.ReminderContact;
import elite.intel.gameapi.search.PermitLockedSystems;
import elite.intel.gameapi.search.spansh.station.traderandbroker.*;
import elite.intel.session.PlayerSession;
import elite.intel.util.TimeUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import static elite.intel.util.StringUtls.localizedEvent;

/**
 * Finds the nearest material trader or technology broker, preferring listings Spansh has seen RECENTLY.
 *
 * <p>Traders and brokers come and go with the background simulation: a station that lost its broker
 * to a faction change stays in Spansh's data until another commander docks there and the listing is
 * refreshed. Asked for the nearest one, Spansh used to answer with whichever listing was closest no
 * matter how old, and the commander flew to a port that no longer had the service. So the search walks
 * {@link #FRESHNESS_LADDER} from the freshest window outwards and the nearest hit of the first window
 * that has one wins. A hit older than {@link #FRESH} is announced with a warning that grows with its age.
 */
public class TradersAndBrokersSearch {

    /**
     * The windows tried in turn, freshest first, in Spansh's relative-time vocabulary: each is asked only
     * when the one before it found nothing reachable.
     *
     * <p>WHY a ladder and not one window: seven hours is inside the game's daily background-simulation
     * tick, so a listing that fresh describes the station as it is now - but Spansh's station index can
     * trail the game by about a day, and then no listing anywhere is that fresh (support bundle
     * 2026-11-19: 0 hits within 250 ly at seven hours, 283 at seven days, the nearest under 20 ly). The
     * last rung still bounds the age: a listing older than that is likelier to send the commander to a
     * demolished or redrawn port than to a trader.
     */
    static final List<String> FRESHNESS_LADDER = List.of("now-7h", "now-7d", "now-90d");

    /** A listing younger than this is announced without a warning. */
    static final Duration FRESH = Duration.ofHours(7);

    /** Up to this age the trader is probably still there. */
    static final Duration RECENT = Duration.ofDays(3);

    /** Up to this age there is a fair chance it has moved; beyond it, it may well be gone. */
    static final Duration OLD = Duration.ofDays(30);

    /**
     * Furthest from the arrival star a candidate may sit, in light seconds. Anything beyond is a
     * supercruise leg longer than the jumps to get there.
     */
    static final int MAX_ARRIVAL_LS = 10_000;

    /**
     * Comfortably above the largest pad count in the game; the range is really "one or more".
     */
    private static final int PADS_MANY = 100;

    /**
     * How many candidates to ask Spansh for, from the page sizes the endpoint accepts (5, 10, 25...).
     * Deliberately small: each hit carries the station's whole market and outfitting, over 100 KB a
     * piece, and only the first one is used once every station in the commander's own system is
     * dropped (see {@link CurrentSystemFilter}). A home system with five fresh traders of the same
     * type would have to exist for the list not to survive that.
     */
    private static final int SEARCH_PAGE_SIZE = 5;

    private static TradersAndBrokersSearch instance;
    private static PlayerSession playerSession;

    private TradersAndBrokersSearch() {
    }

    public static synchronized TradersAndBrokersSearch getInstance() {
        if (instance == null) {
            instance = new TradersAndBrokersSearch();
            playerSession = PlayerSession.getInstance();
        }
        return instance;
    }


    public String location(TraderType traderType, BrokerType brokerType, Number maxDistance) {
        int distanceInLightYears = maxDistance == null ? 250 : maxDistance.intValue();
        LocationManager locationManager = LocationManager.getInstance();
        LocationDao.Coordinates galacticCoordinates = locationManager.getGalacticCoordinates();

        if (galacticCoordinates == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(localizedEvent("event.search.noCoords")));
            return null;
        }

        // WHY: same pad rule the trade route and Interstellar Factors searches apply - a large ship is
        // only shown ports it can dock at, otherwise the route ends at an outpost it has to turn around
        // and leave, materials untraded.
        boolean requiresLargePad = ShipManager.getInstance().requireLargePad();
        String currentSystem = playerSession.currentStarSystem();
        TraderAndBrokerSearchDto.Result result = null;
        for (String window : FRESHNESS_LADDER) {
            TraderAndBrokerSearchCriteria criteria = buildCriteria(
                    galacticCoordinates.x(), galacticCoordinates.y(), galacticCoordinates.z(),
                    traderType, brokerType, distanceInLightYears, requiresLargePad, window
            );
            result = nearest(SearchForMaterialBrokerOrTrader.findMaterialTrader(criteria), currentSystem);
            if (result != null) break;
        }

        if (result == null) {
            GameEventBus.publish(new AiVoxResponseEvent(localizedEvent("event.search.noMatch")));
            return null;
        }

        GameEventBus.publish(new AiVoxResponseEvent(announcement(result, brokerType != null, Instant.now())));
        ReminderManager.getInstance().setReminder(
                localizedEvent("event.search.reminder", result.getStationName()), result.getSystemName(),
                result.getStationName(), contactOf(traderType, brokerType)
        );
        return result.getSystemName();
    }

    /**
     * The request body.
     *
     * <p>Package-private and separate from the call so the wire shape can be asserted without a live
     * search: Spansh ignores a filter key it does not recognise and matches nothing against a shape it
     * does not expect, so either mistake narrows the search silently instead of failing it - and the
     * commander is simply told there is no trader.
     *
     * @param traderType       the material trader wanted, or null when this is a broker search
     * @param brokerType       the technology broker wanted, or null when this is a trader search
     * @param requiresLargePad when true only stations with a large pad are candidates; a small or
     *                         medium ship leaves the pad size unconstrained and sees outposts too
     * @param freshnessWindow  the oldest listing accepted, one rung of {@link #FRESHNESS_LADDER}
     */
    static TraderAndBrokerSearchCriteria buildCriteria(
            double x, double y, double z, TraderType traderType, BrokerType brokerType,
            int maxDistanceLy, boolean requiresLargePad, String freshnessWindow) {

        TraderAndBrokerSearchCriteria.Distance distance = new TraderAndBrokerSearchCriteria.Distance();
        distance.setMin(0);
        distance.setMax(maxDistanceLy);

        TraderAndBrokerSearchCriteria.Filters filters = new TraderAndBrokerSearchCriteria.Filters();
        filters.setDistance(distance);
        filters.setDistanceToArrival(new TraderAndBrokerSearchCriteria.RangeFilter(0, MAX_ARRIVAL_LS));
        filters.setUpdatedAt(TraderAndBrokerSearchCriteria.UpdatedAt.since(freshnessWindow));
        if (requiresLargePad) {
            filters.setLargePads(new TraderAndBrokerSearchCriteria.RangeFilter(1, PADS_MANY));
        }

        if (traderType != null) {
            TraderAndBrokerSearchCriteria.MaterialTrader trader = new TraderAndBrokerSearchCriteria.MaterialTrader();
            trader.setValue(List.of(traderType.getType()));
            filters.setMaterialTrader(trader);
        } else if (brokerType != null) {
            TraderAndBrokerSearchCriteria.TechnologyBroker broker = new TraderAndBrokerSearchCriteria.TechnologyBroker();
            broker.setValue(List.of(brokerType.getType()));
            filters.setTechnologyBroker(broker);
        }

        TraderAndBrokerSearchCriteria.ReferenceCoords coordinates = new TraderAndBrokerSearchCriteria.ReferenceCoords();
        coordinates.setX(x);
        coordinates.setY(y);
        coordinates.setZ(z);

        TraderAndBrokerSearchCriteria criteria = new TraderAndBrokerSearchCriteria();
        criteria.setFilters(filters);
        criteria.setReferenceCoords(coordinates);
        // WHY a server-side sort: the page is small, so without it page 0 is five ARBITRARY matches
        // out of the whole radius and the nearest one is very likely not among them.
        criteria.setSort(List.of(new TraderAndBrokerSearchCriteria.DistanceSort()));
        criteria.setSize(SEARCH_PAGE_SIZE);
        criteria.setPage(0);
        return criteria;
    }

    /**
     * What VEGA says about the hit: where to go, how old the listing is, and - once it is older than
     * {@link #FRESH} - how likely the trader or broker is to have moved on since, so a commander who
     * finds the contact gone at the end of the flight was told to expect it.
     *
     * @param broker true for a technology broker, false for a material trader
     */
    static String announcement(TraderAndBrokerSearchDto.Result result, boolean broker, Instant now) {
        Duration age = TimeUtils.ageOfUtc(result.getUpdatedAt(), now);
        String headTo = localizedEvent("event.search.headTo",
                result.getSystemName(),
                result.getStationName(),
                age == null ? "?" : TimeUtils.timeAgo(age));
        String warningKey = stalenessWarningKey(age);
        if (warningKey == null) return headTo;
        String contact = localizedEvent(broker ? "event.search.contact.broker" : "event.search.contact.trader");
        return headTo + " " + localizedEvent(warningKey, contact);
    }

    /**
     * The warning a listing this old earns, or null when it is fresh enough to need none. An age that
     * could not be read is warned about too: nothing says it is recent.
     */
    static String stalenessWarningKey(Duration age) {
        if (age == null) return "event.search.listingAge.unknown";
        if (age.compareTo(FRESH) < 0) return null;
        if (age.compareTo(RECENT) < 0) return "event.search.listingAge.recent";
        if (age.compareTo(OLD) < 0) return "event.search.listingAge.old";
        return "event.search.listingAge.stale";
    }

    /**
     * The nearest hit outside the commander's current system, or null when there is none.
     *
     * <p>Every hit is inside one freshness window - the request bounded {@code updated_at} - so nearest
     * is the whole ranking. Re-sorted here rather than trusting the page order: measured live, a page asked for
     * with no sort comes back in no order at all.
     */
    static TraderAndBrokerSearchDto.Result nearest(List<TraderAndBrokerSearchDto.Result> results, String currentSystem) {
        return PermitLockedSystems.reachable(CurrentSystemFilter.exclude(results, currentSystem))
                .stream()
                .min(Comparator.comparingDouble(TraderAndBrokerSearchDto.Result::getDistance))
                .orElse(null);
    }

    /**
     * Which contact this search was for. Known here and nowhere downstream — the result carries a
     * station, not what the commander wanted from it — so it is recorded with the reminder rather
     * than guessed at from the sentence later.
     */
    static ReminderContact contactOf(TraderType traderType, BrokerType brokerType) {
        if (traderType != null) {
            return switch (traderType) {
                case RAW -> ReminderContact.MATERIAL_TRADER_RAW;
                case MANUFACTURED -> ReminderContact.MATERIAL_TRADER_MANUFACTURED;
                case ENCODED -> ReminderContact.MATERIAL_TRADER_ENCODED;
            };
        }
        if (brokerType != null) {
            return switch (brokerType) {
                case HUMAN -> ReminderContact.TECHNOLOGY_BROKER_HUMAN;
                case GUARDIAN -> ReminderContact.TECHNOLOGY_BROKER_GUARDIAN;
            };
        }
        return null;
    }
}
