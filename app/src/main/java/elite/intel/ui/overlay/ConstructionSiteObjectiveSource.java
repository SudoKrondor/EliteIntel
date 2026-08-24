package elite.intel.ui.overlay;

import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.ConstructionSiteDao.Requirement;
import elite.intel.db.dao.ConstructionSiteDao.Site;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.managers.ConstructionSiteManager;
import elite.intel.db.managers.ShipManager;
import elite.intel.db.managers.StationMarketsManager;
import elite.intel.gameapi.colonisation.ActiveConstructionSite;
import elite.intel.gameapi.colonisation.ConstructionCargo;
import elite.intel.gameapi.colonisation.ManifestAge;
import elite.intel.session.DockedMarket;
import elite.intel.session.PlayerSession;
import elite.intel.ui.i18n.LocalizedNumbers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.LongFunction;
import java.util.function.Supplier;

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
     * Goods listed before the card stops naming them individually. Beyond a handful the list stops being a
     * loading order and becomes a manifest, which is what the spoken answer is for.
     */
    private static final int MAX_GOODS_LISTED = 4;

    private final Supplier<Site> site;
    private final LongFunction<List<Requirement>> manifest;
    private final Supplier<Map<String, Integer>> hold;
    private final IntSupplier holdCapacity;
    private final Supplier<Set<String>> stockedHere;

    public ConstructionSiteObjectiveSource() {
        this(() -> ConstructionSiteManager.getInstance().currentSite(),
                marketId -> ConstructionSiteManager.getInstance().requirements(marketId),
                () -> ConstructionCargo.heldBySymbol(PlayerSession.getInstance().getShipCargo()),
                ConstructionSiteObjectiveSource::shipCargoCapacity,
                ConstructionSiteObjectiveSource::stockedOnThisPad);
    }

    /**
     * Seam for tests.
     */
    ConstructionSiteObjectiveSource(Supplier<Site> site, LongFunction<List<Requirement>> manifest,
                                    Supplier<Map<String, Integer>> hold, IntSupplier holdCapacity,
                                    Supplier<Set<String>> stockedHere) {
        this.site = site;
        this.manifest = manifest;
        this.hold = hold;
        this.holdCapacity = holdCapacity;
        this.stockedHere = stockedHere;
    }

    /**
     * The same figure the commodity search sizes its basket with, read from the same place, so the card and
     * the spoken answer cannot disagree about what fits.
     */
    private static int shipCargoCapacity() {
        ShipDao.Ship ship = ShipManager.getInstance().getShip();
        return ship == null ? 0 : ship.getCargoCapacity();
    }

    /**
     * What the port under the ship has on its shelves, by bare journal symbol. Empty in flight, and empty at
     * a market whose screen the commander has never opened - in both cases we know nothing about what can be
     * bought here, and ordering the card on a guess would be worse than not ordering it at all.
     */
    private static Set<String> stockedOnThisPad() {
        String pad = DockedMarket.getInstance().stationName();
        if (pad == null) return Set.of();
        return StationMarketsManager.getInstance().stockedAt(pad)
                .map(snapshot -> snapshot.stockBySymbol().keySet())
                .orElse(Set.of());
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

        List<HudRow> rows = new ArrayList<>();
        // Provided over required across the whole manifest, which is exactly what the journal's
        // ConstructionProgress is - so the bar and any spoken percentage cannot disagree.
        rows.add(HudRow.progress(HudText.get("overlay.card.row.progress"),
                (int) Math.round(current.getProgress() * 100), 100));
        // The good's own name is the row's label: a loading order is read down the left-hand column, and
        // repeating the word COMMODITY says nothing. Same shape as the shopping-list card.
        for (Load load : loadingOrder(stillToBuy, holdCapacity.getAsInt(), stockedHere.get())) {
            rows.add(HudRow.of(load.name().toUpperCase(), loadValue(load),
                    load.held() > 0 ? HudRow.State.GOOD : HudRow.State.NORMAL));
        }
        // The whole build's shortfall, next to one trip's worth of it. Without this the card would read as
        // if 640 tonnes of steel finished the job, when it is 640 of the 2542 the site is still short.
        long shortfall = stillToBuy.stream().mapToLong(ConstructionCargo.Outstanding::shortfall).sum();
        rows.add(HudRow.of(HudText.get("overlay.card.row.outstanding"),
                HudText.amount(shortfall, "overlay.card.unit.tonnes")));
        if (ManifestAge.hoursSince(current.getVisitedAt()) >= STALE_AFTER_HOURS) {
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
     * One good, the tonnes of it this trip would buy, and the tonnes of it already aboard.
     */
    private record Load(String name, int tonnes, int held) {
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
                                           Set<String> stockedHere) {
        if (capacity <= 0) {
            ConstructionCargo.Outstanding first = stillToBuy.getFirst();
            return List.of(new Load(displayName(first), first.shortfall(), first.held()));
        }
        List<Load> order = new ArrayList<>();
        int remaining = capacity;
        for (ConstructionCargo.Outstanding line : soldHereFirst(stillToBuy, stockedHere)) {
            if (remaining <= 0 || order.size() == MAX_GOODS_LISTED) break;
            int tonnes = Math.min(line.shortfall(), remaining);
            if (tonnes <= 0) continue;
            order.add(new Load(displayName(line), tonnes, line.held()));
            remaining -= tonnes;
        }
        return order;
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
