package elite.intel.gameapi.colonisation;

import elite.intel.db.dao.ConstructionSiteDao.Site;
import elite.intel.gameapi.carrier.OwnCarrierHold;
import elite.intel.gameapi.carrier.OwnCarrierHold.Held;
import elite.intel.session.PlayerSession;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The carrier a build is being stockpiled on, and what it already has aboard for it.
 * <p>
 * <b>The trip this exists for.</b> A carrier is how colonisation cargo moves at any scale: park it at a
 * market that sells what the build wants, buy by the hold-full and shuttle it across, then jump the whole
 * stockpile out to the depot. A market that sells Steel almost always sells Copper, Aluminium and Titanium
 * too - which is most of what a build eats - so the commander stands at that one commodity screen for
 * several runs, and the question in front of them is not "what fits in my ship" but "how much of this have
 * I got already".
 * <p>
 * <b>Why the ledger decides this and not the carrier's position.</b> Position is what STARTS the trip - a
 * carrier freshly parked at the market has nothing aboard yet and is still plainly being filled - but it is
 * the stockpile itself that has to be remembered afterwards. A commander who left the carrier holding 2,000
 * of the 2,542 tonnes of steel a build wants needs to know that at any commodity screen in the galaxy, or
 * they will buy the whole 2,542 again. So a carrier already holding materials for this build counts
 * wherever it is, and being parked where the shopping is done only adds the case where it holds nothing.
 * <p>
 * <b>One carrier, not two.</b> A commander may own a fleet carrier and a squadron carrier, but in practice
 * one is the lorry and the other is packed for squadron work somewhere else, so the tonnes are read off
 * whichever one is doing this job: the one parked where the shopping is, else the one carrying most of what
 * the build wants. Summing the two would report a total that sits in no single hold and cannot be delivered
 * as one.
 * <p>
 * <b>Where the ship is standing does not come into it.</b> The shuttle spends its time between the market
 * and the carrier, and this answers the same on either pad and in the space between them. Which market
 * counts as the shop - never our own carrier's, whose shelves ARE the stockpile - is
 * {@code ShoppingShelves}' question, and keeping the two apart is what lets the card survive the round trip.
 */
public final class CarrierStockpile {

    private CarrierStockpile() {
    }

    /**
     * What one carrier is holding for a build, by bare journal symbol.
     * <p>
     * Empty is a real answer rather than the absence of one: a carrier just parked at the market has nothing
     * aboard yet, and that is precisely the moment the whole list is still to buy.
     *
     * @param callSign the carrier the tonnes are on, which is also its station name
     */
    public record Stash(String callSign, Map<String, Integer> stockBySymbol) {

        public Stash {
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
     * The carrier this build is being stockpiled on, or empty when no carrier of ours has a bearing on it.
     *
     * @param neededSymbols the goods the build still wants, as bare journal symbols
     */
    public static Optional<Stash> forBuild(Site site, Set<String> neededSymbols) {
        return forBuild(site, neededSymbols, PlayerSession.getInstance().getPrimaryStarName(),
                OwnCarrierHold.oursIncludingEmpty());
    }

    /**
     * Seam for tests, and the whole rule in one place.
     *
     * @param currentSystem the system the commander is in right now
     * @param carriers      our carriers with their positions, empty ones included: see
     *                      {@link OwnCarrierHold#oursIncludingEmpty()}
     */
    public static Optional<Stash> forBuild(Site site, Set<String> neededSymbols, String currentSystem,
                                           List<Held> carriers) {
        if (site == null || neededSymbols == null || neededSymbols.isEmpty()) return Optional.empty();

        Held best = null;
        for (Held carrier : carriers == null ? List.<Held>of() : carriers) {
            if (carrier == null) continue;
            if (!parkedWhereWeAreShopping(carrier, site, currentSystem) && tonnesFor(carrier, neededSymbols) == 0) {
                continue;
            }
            if (best == null || beats(carrier, best, site, currentSystem, neededSymbols)) {
                best = carrier;
            }
        }
        return Optional.ofNullable(best).map(carrier -> new Stash(carrier.callSign(), carrier.stockBySymbol()));
    }

    /**
     * True when this carrier is sitting in the system the commander is shopping in, and that system is not
     * the build's own.
     * <p>
     * The exclusion is what tells stocking up from shuttling: parked at the depot, the carrier is being
     * emptied rather than filled and its cargo is already where it is going. It still counts through its
     * ledger, which is the half of the question that survives the carrier moving.
     */
    private static boolean parkedWhereWeAreShopping(Held carrier, Site site, String currentSystem) {
        if (currentSystem == null || carrier.starSystem() == null) return false;
        if (!currentSystem.equalsIgnoreCase(carrier.starSystem())) return false;
        // A site whose system we never learned cannot be told apart from the one we are standing in, and
        // guessing would turn a delivery run into a shopping trip.
        return site.getStarSystem() != null && !site.getStarSystem().equalsIgnoreCase(carrier.starSystem());
    }

    /**
     * The one doing this job: parked where the shopping is beats one that is not, and between two of a kind,
     * the one carrying more of what the build wants.
     */
    private static boolean beats(Held candidate, Held incumbent, Site site, String currentSystem,
                                 Set<String> neededSymbols) {
        boolean here = parkedWhereWeAreShopping(candidate, site, currentSystem);
        if (here != parkedWhereWeAreShopping(incumbent, site, currentSystem)) return here;
        return tonnesFor(candidate, neededSymbols) > tonnesFor(incumbent, neededSymbols);
    }

    /**
     * Tonnes aboard of the goods this build still wants - everything else in the hold says nothing about
     * whether the carrier is working this job.
     */
    private static int tonnesFor(Held carrier, Set<String> neededSymbols) {
        return carrier.stockBySymbol().entrySet().stream()
                .filter(entry -> neededSymbols.contains(entry.getKey()))
                .mapToInt(Map.Entry::getValue)
                .sum();
    }
}
