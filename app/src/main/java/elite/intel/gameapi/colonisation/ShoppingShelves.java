package elite.intel.gameapi.colonisation;

import elite.intel.db.managers.StationMarketsManager;
import elite.intel.db.managers.StationMarketsManager.MarketSnapshot;
import elite.intel.gameapi.carrier.OurCarriers;
import elite.intel.session.DockedMarket;
import elite.intel.session.PlayerSession;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * What is on the shelves of the market the commander is buying a build's cargo from, by bare journal symbol.
 * <p>
 * <b>Why this outlives the pad.</b> Stocking a carrier is a shuttle run: buy at the station, undock, dock at
 * the carrier, transfer, go back. Read straight off the pad under the ship, the shop exists for only the
 * first of those four - so the moment the commander lifted off, the construction card lost its shelves and
 * fell back to naming the build's largest shortfall, which at Papin's Inheritance was a good sold nowhere in
 * the system. The shop is a fact about the trip, not about the pad, and it holds until the commander leaves
 * the system it is in.
 * <p>
 * <b>Our own carrier is never the shop.</b> Its shelves ARE the stockpile - a list built from them would
 * tell the commander to buy their own cargo. Standing on it is the unloading end of the shuttle, and the
 * shop it is being unloaded from is the one worth remembering.
 * <p>
 * Empty means we know nothing about what can be bought around here - in flight having never opened a market
 * screen, or a system away from the one we shopped in - and a card ordered on a guess would be worse than
 * one not ordered at all.
 * <p>
 * Shared between the HUD card and the companion's callout so that the screen and the voice cannot disagree
 * about which market the commander is shopping at, which is why it is a singleton rather than a field of
 * either.
 */
public final class ShoppingShelves {

    private static final ShoppingShelves INSTANCE = new ShoppingShelves();

    public static ShoppingShelves getInstance() {
        return INSTANCE;
    }

    /**
     * One pad: its MarketID, which is the only unambiguous handle a port has, and its name, which is what
     * tells a carrier from a station.
     */
    public record Pad(long marketId, String stationName) {
        boolean isKnown() {
            return marketId != 0 || (stationName != null && !stationName.isBlank());
        }
    }

    /**
     * The market the commander is buying the build's cargo from.
     *
     * @param stock what it had on its shelves when we last looked, by bare journal symbol
     */
    public record Shop(long marketId, String stationName, String starSystem, Set<String> stock) {
        public Shop {
            stock = stock == null ? Set.of() : Set.copyOf(stock);
        }
    }

    private final Supplier<Pad> pad;
    private final Predicate<Pad> ours;
    private final Function<Pad, Optional<MarketSnapshot>> market;
    private final Supplier<String> currentSystem;

    private volatile Shop lastShop;

    private ShoppingShelves() {
        this(ShoppingShelves::padUnderTheShip, ShoppingShelves::isOneOfOurs, ShoppingShelves::marketOn,
                () -> PlayerSession.getInstance().getPrimaryStarName());
    }

    /**
     * Seam for tests.
     */
    ShoppingShelves(Supplier<Pad> pad, Predicate<Pad> ours, Function<Pad, Optional<MarketSnapshot>> market,
                    Supplier<String> currentSystem) {
        this.pad = pad;
        this.ours = ours;
        this.market = market;
        this.currentSystem = currentSystem;
    }

    /**
     * The market the commander is shopping at, or empty when there is none we can speak for.
     */
    public Optional<Shop> current() {
        Pad here = pad.get();
        if (here != null && here.isKnown() && !ours.test(here)) {
            Optional<MarketSnapshot> found = market.apply(here);
            if (found.isPresent()) {
                Shop shop = remember(here, found.get());
                if (shop != null) return Optional.of(shop);
            }
        }
        return whereWeWereShopping();
    }

    /**
     * Just the shelves, for a caller that only asks what can be bought.
     */
    public Set<String> stocked() {
        return current().map(Shop::stock).orElse(Set.of());
    }

    /**
     * The shop we last stood in, while we are still in its system. A jump away it says nothing about what
     * can be bought here, so it stops answering rather than sending the commander to a market they have left
     * behind.
     */
    private Optional<Shop> whereWeWereShopping() {
        Shop remembered = lastShop;
        if (remembered == null) return Optional.empty();
        String system = currentSystem.get();
        if (system == null || !system.equalsIgnoreCase(remembered.starSystem())) return Optional.empty();
        return Optional.of(remembered);
    }

    /**
     * A snapshot whose system we do not know cannot be told apart from a market in another one, and a shelf
     * that answers everywhere is worse than one that answers nowhere.
     */
    private Shop remember(Pad pad, MarketSnapshot found) {
        if (found.starSystem() == null || found.starSystem().isBlank()) return null;
        String name = found.stationName() == null ? pad.stationName() : found.stationName();
        Shop shop = new Shop(pad.marketId(), name, found.starSystem(), found.stockBySymbol().keySet());
        lastShop = shop;
        return shop;
    }

    private static Pad padUnderTheShip() {
        DockedMarket docked = DockedMarket.getInstance();
        return new Pad(docked.marketId(), docked.stationName());
    }

    private static boolean isOneOfOurs(Pad pad) {
        return OurCarriers.byCallSign(pad.stationName()).isPresent();
    }

    /**
     * By MarketID first: it names THIS port and no other station that happens to share its name, and it is
     * what a restart on the pad is most likely to still have. The name is the fallback for a pad we were
     * told the id of and never the name of.
     */
    private static Optional<MarketSnapshot> marketOn(Pad pad) {
        StationMarketsManager markets = StationMarketsManager.getInstance();
        Optional<MarketSnapshot> byId = markets.stockedAt(pad.marketId());
        if (byId.isPresent() || pad.stationName() == null) return byId;
        return markets.stockedAt(pad.stationName());
    }
}
