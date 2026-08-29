package elite.intel.ui.overlay;

import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.ConstructionSiteDao.Requirement;
import elite.intel.db.dao.ConstructionSiteDao.Site;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.managers.ConstructionSiteManager;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.StationName;
import elite.intel.gameapi.colonisation.*;
import elite.intel.gameapi.colonisation.CarrierStockpile.Stash;
import elite.intel.gameapi.colonisation.ShoppingShelves.Shop;
import elite.intel.session.PlayerSession;
import elite.intel.ui.i18n.LocalizedNumbers;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.IntSupplier;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Projects a colonisation construction site into a HUD objective: how far along the build is, and what to
 * put in the hold on the next run.
 * <p>
 * <b>Why the card is a loading order rather than one commodity.</b> A build's largest shortfall rarely
 * comes out to a whole hold. Steel at 622 tonnes leaves 18 tonnes of a 640-tonne ship empty, and the search
 * fills that corner with the next good down - so the commander is told to buy two things and used to see
 * only the first on screen. Listing what the trip actually carries is the point of having the card at the
 * commodity market at all.
 * <p>
 * <b>Why goods this market actually sells come first.</b> The card is read standing at a commodity screen,
 * and a build's long tail is mostly things the port in front of the commander does not stock - measured at
 * Fairfax Landing, one of the four goods listed was on the shelves. Ordering by shortfall alone spent the
 * whole card on goods that could not be bought there. Availability decides the order; the shortfall still
 * decides it within each group, and nothing is hidden for being unavailable, because the next port is where
 * it will be bought.
 * <p>
 * <b>Why the shuttle run gets a different card.</b> Between a stockpiled carrier and the depot there is
 * nothing to buy, so sizing a list to the hold answers a question nobody is asking. The commander on that leg
 * wants the job: each good as what they hold over what the site still wants, largest first - the same figures
 * the game's own construction panel is showing them at that moment.
 * <p>
 * <b>Why a good already started leads the loading order.</b> The deficit decides what to haul only until
 * something is under way: tonnes already bought sit in a hold somewhere waiting to be delivered, and
 * finishing them beats opening a second front. So the loading order puts what is part bought - in the ship
 * or on the carrier - ahead of what has not been touched, and the largest shortfall decides among equals.
 * See {@link #partlyBoughtFirst}. The shopping list deliberately does NOT do this; {@code ConstructionShopping}
 * says why, and it comes down to the two lists being read at different ends of the job.
 * <p>
 * <b>Why the trip is not the whole card.</b> Sizing the list to one hold is what makes its tonnages honest,
 * and on a large build it is also what empties the card: every remaining line is bigger than a hold, so the
 * loading order collapses to a single good and says nothing about the shelves the commander is standing at.
 * The trip therefore keeps its allocation, and whatever else this market sells that the build wants follows
 * it under an ALSO HERE heading at its own outstanding shortfall - see {@link #alsoOnTheseShelves}.
 * <p>
 * <b>Why the card has a second shape.</b> Hauling with a carrier splits the job in two: fill the carrier
 * at a market, then jump the whole stockpile out to the build and shuttle it down. Sized to the ship's hold,
 * the card answers the wrong half - a commander whose carrier already holds 2,000 of the 2,542 tonnes of
 * steel a build wants is told to buy another 640, and buys cargo they own. So once {@link CarrierStockpile}
 * finds a carrier working this build, the card becomes the shop measured against the whole job: what that
 * market sells that the site still wants, over what is already bought. See {@link #shoppingList}, and
 * {@link ShoppingShelves} for why the shop outlasts the pad - the shuttle spends most of its time off it.
 * <p>
 * Ranked {@link HudObjective#PRIORITY_STANDING}, alongside missions and trade routes: hauling to a build
 * is a job the commander took on, not something the app volunteered, but it is not the moment-to-moment
 * task an active card describes either.
 * <p>
 * The card disappears on its own when the build completes or fails, when the hold already covers everything
 * outstanding (at which point the answer is "fly back", which the plotted-route card already says), and
 * when the manifest has gone unrefreshed long enough to stop being believable - see
 * {@link ActiveConstructionSite}. Docking at the depot brings it back.
 */
public class ConstructionSiteObjectiveSource implements HudObjectiveSource {

    /**
     * Beyond this the manifest is old enough to caveat: other commanders haul to the same depot, so the
     * tonnages are a claim about the last visit rather than about now.
     */
    private static final long STALE_AFTER_HOURS = 1;

    /**
     * Goods listed before the LOADING ORDER stops naming them individually. Beyond a handful the list stops
     * being a loading order and becomes a manifest, which is what the spoken answer is for. The shopping
     * list has no such limit of its own - see {@link #shoppingList} - and runs to whatever the card can
     * hold.
     */
    private static final int MAX_GOODS_LISTED = 4;

    /**
     * Rows the renderer keeps - {@code MAX_ROWS} in {@code hud.h}. Rows past the eighth are dropped where
     * they are parsed, and since the totals are written UNDER the goods, an over-long list would silently
     * cost the commander OUTSTANDING and the staleness caveat rather than the tail of the list. So the
     * goods are given what is left after those are counted out, and never more.
     */
    private static final int MAX_CARD_ROWS = 8;

    private final Supplier<Site> site;
    private final LongFunction<List<Requirement>> manifest;
    private final Supplier<Map<String, Integer>> hold;
    private final IntSupplier holdCapacity;
    /**
     * The market the commander is buying from - see {@link ShoppingShelves}, which is why this outlives the
     * pad the ship is standing on. Empty when we know nothing about what can be bought around here.
     */
    private final Supplier<Optional<Shop>> shop;
    private final BiFunction<Site, Set<String>, Optional<Stash>> stockpile;

    public ConstructionSiteObjectiveSource() {
        this(() -> ConstructionSiteManager.getInstance().currentSite(),
                marketId -> ConstructionSiteManager.getInstance().requirements(marketId),
                () -> ConstructionCargo.heldBySymbol(PlayerSession.getInstance().getShipCargo()),
                ConstructionSiteObjectiveSource::shipCargoCapacity,
                ShoppingShelves.getInstance()::current,
                CarrierStockpile::forBuild);
    }

    /**
     * Seam for tests.
     */
    ConstructionSiteObjectiveSource(Supplier<Site> site, LongFunction<List<Requirement>> manifest,
                                    Supplier<Map<String, Integer>> hold, IntSupplier holdCapacity,
                                    Supplier<Optional<Shop>> shop) {
        this(site, manifest, hold, holdCapacity, shop, (ignored, alsoIgnored) -> Optional.empty());
    }

    /**
     * Seam for tests, including the carrier the commander may be filling.
     */
    ConstructionSiteObjectiveSource(Supplier<Site> site, LongFunction<List<Requirement>> manifest,
                                    Supplier<Map<String, Integer>> hold, IntSupplier holdCapacity,
                                    Supplier<Optional<Shop>> shop,
                                    BiFunction<Site, Set<String>, Optional<Stash>> stockpile) {
        this.site = site;
        this.manifest = manifest;
        this.hold = hold;
        this.holdCapacity = holdCapacity;
        this.shop = shop;
        this.stockpile = stockpile;
    }

    /**
     * The same figure the commodity search sizes its basket with, read from the same place, so the card and
     * the spoken answer cannot disagree about what fits.
     */
    private static int shipCargoCapacity() {
        ShipDao.Ship ship = ShipManager.getInstance().getShip();
        return ship == null ? 0 : ship.getCargoCapacity();
    }

    @Override
    public Optional<HudObjective> currentObjective() {
        Site current = site.get();
        // Finished, failed, forgotten, or simply not refreshed in days - see ActiveConstructionSite for why
        // the card withdraws itself rather than sitting on screen through a fortnight of pirate hunting.
        if (!ActiveConstructionSite.isLive(current)) return Optional.empty();

        List<ConstructionCargo.Outstanding> outstanding = ConstructionCargo.outstanding(
                manifest.apply(current.getMarketId()), hold.get());
        if (outstanding.isEmpty()) return Optional.empty();

        List<ConstructionCargo.Outstanding> stillToBuy = outstanding.stream()
                .filter(line -> !line.isSatisfied())
                .toList();
        if (stillToBuy.isEmpty()) return Optional.empty();

        Optional<Shop> shop = this.shop.get();
        boolean manifestIsOld = ManifestAge.hoursSince(current.getVisitedAt()) >= STALE_AFTER_HOURS;
        // Progress and the outstanding total always appear; the caveat and the shop's name only sometimes.
        // What is left of the renderer's eight rows is the list's. A shop we turn out not to name costs the
        // list nothing, because a loading order is capped well below its budget anyway.
        int goodsBudget = MAX_CARD_ROWS - 2 - (manifestIsOld ? 1 : 0) - (shop.isPresent() ? 1 : 0);

        List<HudRow> rows = new ArrayList<>();
        // Provided over required across the whole manifest, which is exactly what the journal's
        // ConstructionProgress is - so the bar and any spoken percentage cannot disagree.
        rows.add(HudRow.progress(HudText.get("overlay.card.row.progress"),
                (int) Math.round(current.getProgress() * 100), 100));
        // The good's own name is the row's label: a loading order is read down the left-hand column, and
        // repeating the word COMMODITY says nothing. Same shape as the shopping-list card.
        Goods goods = goods(current, shop, outstanding, stillToBuy, goodsBudget);
        // Directly under the progress bar and above the goods, because it is what the goods are about: a
        // list that changes on entering a system says nothing until the commander knows which pad to fly to.
        if (goods.heading() != null) rows.add(goods.heading());
        rows.addAll(goods.rows());
        // The whole build's shortfall, next to one trip's worth of it. Without this the card would read as
        // if 640 tonnes of steel finished the job, when it is 640 of the 2542 the site is still short.
        long shortfall = stillToBuy.stream().mapToLong(ConstructionCargo.Outstanding::shortfall).sum();
        rows.add(HudRow.of(HudText.get("overlay.card.row.outstanding"),
                HudText.amount(shortfall, "overlay.card.unit.tonnes")));
        if (manifestIsOld) {
            // WARN rather than a separate line of prose: the row column is narrow, and the point is only
            // that these numbers were true at the last visit, not now.
            rows.add(HudRow.of(HudText.get("overlay.card.row.asOf"),
                    HudText.get("overlay.card.value.lastVisit"), HudRow.State.WARN));
        }

        return Optional.of(new HudObjective(
                "construction-site",
                HudText.get("overlay.card.title.constructionSite"),
                subtitle(current),
                rows,
                HudObjective.PRIORITY_STANDING));
    }

    /**
     * The site's name, which is a game noun and passes through untouched. Null when the {@code Docked}
     * that named it was never seen - the card is still worth drawing, because the manifest is the point.
     */
    private static String subtitle(Site site) {
        String name = site.getStationName();
        return name == null || name.isBlank() ? null : name.toUpperCase();
    }

    /**
     * The commodity rows, in one of three shapes, decided by what the commander can actually do where they
     * are standing:
     * <ul>
     *   <li><b>Shopping list</b> - at a market with a carrier working the build. What these shelves sell that
     *       the site still wants, over what is already bought.</li>
     *   <li><b>Loading order</b> - at a market with no carrier on the job. What this trip buys, sized to the
     *       hold, and what else is on these shelves after it.</li>
     *   <li><b>Delivery list</b> - a carrier holding cargo for this build, and no market anywhere on the trip:
     *       the carrier-to-depot shuttle. Nothing can be bought, so a trip allocation answers a question nobody
     *       is asking; what the commander wants is the job itself - each good as what they hold over what the
     *       site still wants, largest first. See {@link ConstructionShopping#toDeliver}. No shop on its own is
     *       not enough, because that is also the commander in flight TOWARDS a market, where sizing the next
     *       purchase to the hold is the whole point.</li>
     * </ul>
     * A shopping list that comes back empty is not a shopping trip worth drawing - this market sells nothing
     * the build wants - so the card falls back to the loading order rather than showing the commander a
     * progress bar with nothing under it.
     */
    private Goods goods(Site current, Optional<Shop> shop, List<ConstructionCargo.Outstanding> manifest,
                        List<ConstructionCargo.Outstanding> stillToBuy, int budget) {
        Set<String> stillWanted = manifest.stream()
                .map(ConstructionCargo.Outstanding::symbol)
                .collect(Collectors.toUnmodifiableSet());
        // Read whether or not there is a shop, because the loading order needs it too: a good already part
        // bought is usually part bought ON THE CARRIER, and the shuttle run between a carrier and a depot
        // passes no commodity market at all.
        Optional<Stash> stash = stockpile.apply(current, stillWanted);
        if (shop.isPresent() && budget > 0) {
            Goods shopping = stash
                    .map(carrier -> shoppingList(manifest, shop.get(), carrier, budget))
                    .orElse(null);
            if (shopping != null) return shopping;
        }
        // The shuttle run: a carrier is holding cargo for this build and there is no market anywhere on the
        // trip. Both halves are needed. No shop alone is also the commander in flight towards one, where the
        // trip allocation is exactly what they are about to act on; a carrier with no shop is the leg between
        // the stockpile and the depot, where nothing can be bought and the job itself is the answer.
        if (stash.isPresent() && shop.isEmpty() && budget > 0) {
            List<ConstructionShopping.Line> toDeliver = ConstructionShopping.toDeliver(manifest, stash.get());
            if (!toDeliver.isEmpty()) return new Goods(rows(toDeliver, budget), null);
        }

        List<HudRow> rows = new ArrayList<>();
        Set<String> onTheShelves = shop.map(Shop::stock).orElse(Set.of());
        boolean headed = false;
        for (Load load : loadingOrder(partlyBoughtFirst(stillToBuy, stash), holdCapacity.getAsInt(),
                onTheShelves, Math.min(budget, MAX_GOODS_LISTED))) {
            // The heading is what keeps the trip's tonnes from reading as an order for the whole shortfall,
            // so it goes in the moment the list stops being about this hold. It costs the row the budget
            // already set aside for a shop we turn out to name.
            if (!load.thisTrip() && !headed) {
                rows.add(alsoOnSale(shop));
                headed = true;
            }
            rows.add(HudRow.of(load.name().toUpperCase(), loadValue(load),
                    load.held() > 0 ? HudRow.State.GOOD : HudRow.State.NORMAL));
        }
        return new Goods(rows, null);
    }

    /**
     * The commodity rows, and the line above them saying where they come from.
     *
     * @param heading the market to fly to while there is something to buy there, and where these goods have
     *                to be looked for once there is not - null for a loading order, which is about the ship
     *                rather than about any one market
     */
    private record Goods(List<HudRow> rows, HudRow heading) {
    }

    /**
     * The shop in front of the commander: what is on these shelves that the build still wants, each read
     * against what is already bought - {@code 1.760/3.963 T}, on hand over total required.
     * <p>
     * <b>Filtered, not merely reordered.</b> Away from the carrier the card lists the whole loading order
     * and lets availability sort it, because the goods this port cannot sell will be bought at the next one
     * on the same run. Stocking a carrier has no next port - the commander stands at one commodity screen
     * for several runs - so a good this market does not stock is not part of this shop at all.
     * <p>
     * <b>What is bought drops out; what is half bought does not.</b> The list is worked down over several
     * holds, and a row that vanishes the moment its deficit slips behind another's takes with it the one
     * thing the commander is standing there to read - measured at Papin's Inheritance, where buying titanium
     * made CMM Composite the deeper deficit and titanium left the card mid-shop. So the order is fixed by
     * the build's own requirement, which does not move while anyone shops, and a good leaves only when it is
     * finished: {@code 497/497} is not something to buy, it is something already in hand.
     * <p>
     * <b>Bought out, and the card looks ahead.</b> Once everything this system can sell is aboard, the useful
     * answer is what the build still wants that cannot be bought here - the commander then chooses between
     * flying the stockpile in and moving it to another market, and the companion says as much out loud (see
     * {@code ConstructionShoppingAnnouncer}). The heading changes with the list: naming the pad the
     * commander has just emptied, over goods it does not sell, would send them back to it. Only if there is
     * nothing left to acquire at all does the card stand on the finished goods, because a card of green
     * still beats reverting to the build's largest shortfall at a market that does not sell it.
     *
     * @param manifest every line the site still wants, INCLUDING those the hold already covers
     * @param budget   rows the card can spare, counted out from the renderer's own limit
     * @return null when this market is no part of the trip - it sells nothing the build wants
     */
    private static Goods shoppingList(List<ConstructionCargo.Outstanding> manifest, Shop shop, Stash stash,
                                      int budget) {
        List<ConstructionShopping.Line> soldHere = ConstructionShopping.soldHere(manifest, shop.stock(), stash);
        if (soldHere.isEmpty()) return null;

        List<ConstructionShopping.Line> toBuyHere = ConstructionShopping.stillShort(soldHere);
        if (!toBuyHere.isEmpty()) return new Goods(rows(toBuyHere, budget), atThisMarket(shop));

        List<ConstructionShopping.Line> next = ConstructionShopping.stillToAcquire(manifest, stash);
        if (next.isEmpty()) return new Goods(rows(soldHere, budget), atThisMarket(shop));
        return new Goods(rows(next, budget), sourcedElsewhere());
    }

    private static List<HudRow> rows(List<ConstructionShopping.Line> goods, int budget) {
        return goods.stream()
                .limit(budget)
                .map(good -> HudRow.of(displayName(good.good()).toUpperCase(), stockingValue(good),
                        good.owned() > 0 ? HudRow.State.GOOD : HudRow.State.NORMAL))
                .toList();
    }

    /**
     * The line dividing the trip from the rest of the shelves, naming the market the goods under it are on.
     * The name is worth the space here for the same reason the shopping list carries it: a list of goods
     * says nothing until the commander knows which pad they are on. A shop we hold no name for still gets
     * the heading, because separating the two groups is the part that matters.
     */
    private static HudRow alsoOnSale(Optional<Shop> shop) {
        String name = shop.map(current -> StationName.display(current.stationName())).orElse(null);
        return HudRow.of(HudText.get("overlay.card.row.alsoHere"),
                name == null || name.isBlank() ? "" : name.toUpperCase());
    }

    /**
     * The pad these goods are bought at. Null for a shop we hold no name for, in which case the list stands
     * on its own rather than under a blank line.
     */
    private static HudRow atThisMarket(Shop shop) {
        String name = StationName.display(shop.stationName());
        if (name == null || name.isBlank()) return null;
        return HudRow.of(HudText.get("overlay.card.row.station"), name.toUpperCase());
    }

    /**
     * These are not sold where the commander is standing. Said rather than left blank, because a list under
     * the name of the market they have just emptied reads as a reason to stay - and the row is where that
     * name was, so its going is itself the news.
     */
    private static HudRow sourcedElsewhere() {
        return HudRow.of(HudText.get("overlay.card.row.source"), HudText.get("overlay.card.value.elsewhere"));
    }

    /**
     * One good, the tonnes of it in question, and the tonnes of it already aboard.
     *
     * @param thisTrip true when {@code tonnes} is what this trip would buy, false when the hold was already
     *                 full and {@code tonnes} is the good's whole remaining shortfall - a good the commander
     *                 can buy where they are standing, but not on this run
     */
    private record Load(String name, int tonnes, int held, boolean thisTrip) {
    }

    /**
     * Tonnes to buy, and what is already aboard for that same good when there is any.
     * <p>
     * Said on the good's own row rather than as a total underneath it. A single IN HOLD figure told the
     * commander they were carrying 44 tonnes of SOMETHING on the list, which is the one thing about it they
     * could not act on - measured live, standing at a market with 44 tonnes of superconductors aboard and no
     * way to tell from the card that superconductors were what it meant.
     */
    private static String loadValue(Load load) {
        String tonnes = HudText.amount(load.tonnes(), "overlay.card.unit.tonnes");
        if (load.held() <= 0) return tonnes;
        return tonnes + " " + HudText.get("overlay.card.value.aboard", LocalizedNumbers.grouped(load.held()));
    }

    /**
     * Owned over needed - {@code 400/2.542 T} - and the bare requirement for a good we have none of yet,
     * which is the same shape the rest of the card uses for "nothing of this aboard".
     */
    private static String stockingValue(ConstructionShopping.Line good) {
        String needed = HudText.amount(good.needed(), "overlay.card.unit.tonnes");
        if (good.owned() <= 0) return needed;
        return LocalizedNumbers.grouped(good.owned()) + "/" + needed;
    }

    /**
     * What a full hold takes off the manifest - the same allocation the commodity search makes when it goes
     * looking for a basket, so the card names the same tonnages the commander was told to buy.
     * <p>
     * Goods the port under the ship actually sells come first, then everything else; within each group the
     * largest shortfall leads. The hold is filled down that order, so a card capped at a handful of names
     * spends those names on what can be bought where the commander is standing.
     * <p>
     * A hold of unknown size - the game has not yet said which ship we are in - falls back to naming the
     * largest shortfall on its own. That is the honest answer: without a capacity there is no trip to
     * describe, only a next commodity.
     */
    private static List<Load> loadingOrder(List<ConstructionCargo.Outstanding> stillToBuy, int capacity,
                                           Set<String> stockedHere, int budget) {
        if (capacity <= 0) {
            ConstructionCargo.Outstanding first = stillToBuy.getFirst();
            return List.of(new Load(displayName(first), first.shortfall(), first.held(), true));
        }
        List<Load> order = new ArrayList<>();
        Set<String> allocated = new HashSet<>();
        int remaining = capacity;
        for (ConstructionCargo.Outstanding line : soldHereFirst(stillToBuy, stockedHere)) {
            if (remaining <= 0 || order.size() == budget) break;
            int tonnes = Math.min(line.shortfall(), remaining);
            if (tonnes <= 0) continue;
            order.add(new Load(displayName(line), tonnes, line.held(), true));
            allocated.add(line.symbol());
            remaining -= tonnes;
        }
        order.addAll(alsoOnTheseShelves(stillToBuy, stockedHere, allocated, budget - order.size()));
        return order;
    }

    /**
     * The goods on these shelves the build still wants that the trip has no room for.
     * <p>
     * WHY the card says them at all: the loading order describes one hold, and on a large build every
     * remaining line is bigger than one - measured at The Chocolate Factory, where a Panther's 880 tonnes
     * went entirely to steel and the card named nothing else, while the shelves in front of the commander
     * also held titanium and aluminium the site was 7,921 and 4,177 tonnes short of. Sizing the list to the
     * hold is what makes the trip figure honest; it is also what blinds the card at exactly the moment it is
     * being read at a commodity screen. So the trip keeps its allocation, and what else can be bought here
     * is named after it under its own heading, at its own outstanding shortfall - a shopping note, not a
     * second loading order.
     * <p>
     * Only goods this market actually sells qualify: the build's long tail is mostly things the port cannot
     * supply, and listing those would spend the card on what the commander cannot act on standing here.
     *
     * @param allocated the goods the trip already named, which must not be said twice
     * @param budget    rows left after the trip has taken its own
     */
    private static List<Load> alsoOnTheseShelves(List<ConstructionCargo.Outstanding> stillToBuy,
                                                 Set<String> stockedHere, Set<String> allocated, int budget) {
        if (budget <= 0 || stockedHere == null || stockedHere.isEmpty()) return List.of();
        List<Load> also = new ArrayList<>();
        // stillToBuy arrives largest-shortfall first, and that is the order to buy them in next.
        for (ConstructionCargo.Outstanding line : stillToBuy) {
            if (also.size() == budget) break;
            if (!stockedHere.contains(line.symbol()) || allocated.contains(line.symbol())) continue;
            also.add(new Load(displayName(line), line.shortfall(), line.held(), false));
        }
        return also;
    }

    /**
     * The manifest reordered so that goods the commander has already started on lead the ones they have not
     * touched, each group otherwise keeping the order it came in - largest shortfall first.
     * <p>
     * WHY the deficit is not the whole answer: working the largest shortfall first is what shortens the job,
     * but only until a job is under way. Measured live at Witt Hub, a commander part way through moving steel
     * off their carrier - 3,154 tonnes of it still to deliver, with more sitting in the carrier's hold - was
     * shown titanium, because titanium's 7,041 was now the larger number. The steel they were mid-shuttle on
     * had fallen off the card. A good that is already bought is not a decision the card should be reopening:
     * the tonnes are paid for and in a hold somewhere, and finishing them is the cheaper move than starting
     * something else. So part-bought leads, and the deficit decides among equals.
     * <p>
     * This does not make the list restless the way ordering by deficit did. A good only ever moves UP by
     * being started, and what is bought stays bought, so a row promoted here does not slide back.
     *
     * @param stash the carrier working this build, when one is - the tonnes are as bought as the ones in the
     *              ship's hold, and on a carrier run they are where nearly all of them are
     */
    private static List<ConstructionCargo.Outstanding> partlyBoughtFirst(
            List<ConstructionCargo.Outstanding> stillToBuy, Optional<Stash> stash) {
        List<ConstructionCargo.Outstanding> started = new ArrayList<>();
        List<ConstructionCargo.Outstanding> untouched = new ArrayList<>();
        for (ConstructionCargo.Outstanding line : stillToBuy) {
            int bought = line.held() + stash.map(carrier -> carrier.stockOf(line.symbol())).orElse(0);
            (bought > 0 ? started : untouched).add(line);
        }
        started.addAll(untouched);
        return started;
    }

    /**
     * The manifest reordered so that what this port sells leads, with the incoming order - largest shortfall
     * first - preserved inside each group.
     */
    private static List<ConstructionCargo.Outstanding> soldHereFirst(
            List<ConstructionCargo.Outstanding> stillToBuy, Set<String> stockedHere) {
        if (stockedHere == null || stockedHere.isEmpty()) return stillToBuy;

        List<ConstructionCargo.Outstanding> onTheShelves = new ArrayList<>();
        List<ConstructionCargo.Outstanding> elsewhere = new ArrayList<>();
        for (ConstructionCargo.Outstanding line : stillToBuy) {
            (stockedHere.contains(line.symbol()) ? onTheShelves : elsewhere).add(line);
        }
        onTheShelves.addAll(elsewhere);
        return onTheShelves;
    }

    /**
     * The commodity in the commander's language, falling back to the game's own name, and to the bare
     * symbol only when nothing else is available.
     */
    private static String displayName(ConstructionCargo.Outstanding line) {
        String english = FuzzySearch.commodityNameForSymbol(line.symbol());
        if (english != null) return FuzzySearch.localizedCommodityName(english);
        if (line.gameName() != null && !line.gameName().isBlank()) return line.gameName();
        return line.symbol();
    }
}
