package elite.intel.ui.overlay;

import elite.intel.db.FuzzySearch;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.dto.shiploadout.ModuleDto;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.session.PlayerSession;

import java.util.*;

/**
 * Projects a mining run into a HUD objective: what is being mined for, how much
 * of it is in the hold, and how many limpets are left to keep mining with.
 * <p>
 * Like the exobiology card this is a self-set objective rather than a contract -
 * the commander named the targets - so it ranks {@link HudObjective#PRIORITY_AMBIENT}
 * and never displaces an accepted mission or a plotted route.
 * <p>
 * Two conditions make a mining card, and both are already in the database: a
 * mining target list, and a refinery on the ship. The refinery is what makes it
 * a mining ship - nothing else refines an asteroid fragment into cargo - so a
 * hauler that happens to carry platinum never shows this card, and the card
 * disappears by itself when the commander swaps ships.
 * <p>
 * <b>Symbols, not names.</b> Mining targets are stored as English display names
 * ("Low Temperature Diamonds"); the cargo hold is keyed by the lower-cased FDev
 * symbol ("lowtemperaturediamond"). The two are matched through the commodities
 * table rather than by comparing strings, because for a good half of the
 * mineable commodities the two forms do not resemble each other.
 */
public class MiningObjectiveSource implements HudObjectiveSource {

    /**
     * Journal name of limpet drones; {@code Name_Localised} is "Limpet".
     */
    private static final String DRONES = "drones";

    /**
     * Substring identifying a refinery in a loadout's readable module names
     * (journal {@code Int_Refinery_Size2_Class4} becomes "Int Refinery Size2 Class4").
     */
    private static final String REFINERY = "refinery";

    /**
     * The cargo event's vessel when the hold being reported is the ship's. An SRV
     * hold is a different hold and would put someone else's tonnage on the card.
     */
    private static final String SHIP = "Ship";

    /**
     * Target rows the card will list. The renderer keeps 8 rows ({@code MAX_ROWS}
     * in hud.h) and drops the rest, and this card also spends one on the hold,
     * one on the overflow count and one on limpets - so raising this silently
     * loses the limpet row, which is the one row a miner cannot do without.
     */
    static final int MAX_TARGET_ROWS = 5;

    /**
     * Hold fullness at which the card starts saying so. Past this point the
     * decision in front of the commander is when to break off and sell, not what
     * to shoot next.
     */
    private static final double HOLD_WARN_FRACTION = 0.9;

    /**
     * Limpets left at which the row turns amber. Below roughly this many a
     * prospector-plus-collector cycle is no longer sustainable, and running dry
     * mid-rock is the thing that ends a session early.
     */
    private static final int LIMPETS_LOW = 5;

    private final PlayerSession playerSession;

    public MiningObjectiveSource() {
        this(PlayerSession.getInstance());
    }

    /**
     * Seam for tests.
     */
    MiningObjectiveSource(PlayerSession playerSession) {
        this.playerSession = playerSession;
    }

    @Override
    public Optional<HudObjective> currentObjective() {
        Set<String> targets = playerSession.getMiningTargets();
        if (targets == null || targets.isEmpty()) return Optional.empty();

        ShipLoadOutDto loadout = playerSession.getShipLoadout();
        if (!hasRefinery(loadout)) return Optional.empty();

        GameEvents.CargoEvent cargo = playerSession.getShipCargo();
        if (!isShipHold(cargo)) return Optional.empty();

        return card(yields(targets, cargo),
                countOf(cargo, DRONES),
                cargo == null ? 0 : cargo.getCount(),
                loadout.getCargoCapacity(),
                systemLabel());
    }

    /**
     * The card, or empty when there is nothing to put on it.
     * <p>
     * Pure so the rows can be tested without a ship, a hold or a database:
     * everything it needs has already been resolved by the caller.
     */
    static Optional<HudObjective> card(List<TargetYield> yields, int limpets,
                                       int cargoUsed, int cargoCapacity, String subtitle) {
        if (yields == null || yields.isEmpty()) return Optional.empty();

        List<HudRow> rows = new ArrayList<>();
        if (cargoCapacity > 0) {
            rows.add(HudRow.progress("HOLD", Math.min(cargoUsed, cargoCapacity), cargoCapacity,
                    holdState(cargoUsed, cargoCapacity)));
        }

        List<TargetYield> ordered = new ArrayList<>(yields);
        // Whatever is actually coming in goes to the top, and stays there while
        // it keeps coming in; the rest hold a stable alphabetical order so the
        // card does not reshuffle itself between polls.
        ordered.sort(Comparator.comparingInt(TargetYield::tonnes).reversed()
                .thenComparing(TargetYield::name));

        int shown = Math.min(ordered.size(), MAX_TARGET_ROWS);
        for (TargetYield yield : ordered.subList(0, shown)) {
            rows.add(HudRow.of(yield.name().toUpperCase(Locale.ROOT), tonnage(yield.tonnes()),
                    yield.tonnes() > 0 ? HudRow.State.GOOD : HudRow.State.NORMAL));
        }
        if (ordered.size() > shown) {
            rows.add(HudRow.of("MORE TARGETS", "+" + (ordered.size() - shown)));
        }

        rows.add(HudRow.of("LIMPETS", String.format("%,d", Math.max(0, limpets)), limpetState(limpets)));

        return Optional.of(new HudObjective(
                "mining",
                "MINING",
                subtitle,
                rows,
                HudObjective.PRIORITY_AMBIENT));
    }

    /**
     * How much of each target is in the hold right now. Targets with nothing
     * mined yet stay on the card at zero - the list is what the commander is
     * looking for, not only what they have found.
     */
    private static List<TargetYield> yields(Set<String> targets, GameEvents.CargoEvent cargo) {
        List<TargetYield> yields = new ArrayList<>(targets.size());
        for (String target : targets) {
            if (target == null || target.isBlank()) continue;
            yields.add(new TargetYield(target, countOf(cargo, cargoKey(target))));
        }
        return yields;
    }

    /**
     * The hold's key for a mining target: its FDev symbol when the commodities
     * table knows one, otherwise the display name with its separators stripped -
     * which is what the symbol looks like for every commodity whose name is a
     * plain phrase, and the best guess available for anything the table missed.
     */
    static String cargoKey(String target) {
        String symbol = FuzzySearch.commoditySymbol(target);
        String key = symbol == null || symbol.isBlank() ? target : symbol;
        return key.replace(" ", "").replace("-", "").toLowerCase(Locale.ROOT);
    }

    /**
     * Tonnage of one hold key. The journal writes cargo names lower-cased, but
     * compare case-insensitively anyway rather than trust that across an update.
     */
    private static int countOf(GameEvents.CargoEvent cargo, String key) {
        if (cargo == null || cargo.getInventory() == null || key == null) return 0;
        for (GameEvents.Inventory item : cargo.getInventory()) {
            if (item == null || item.getName() == null) continue;
            if (key.equalsIgnoreCase(item.getName())) return (int) item.getCount();
        }
        return 0;
    }

    /**
     * A refinery is the one module that makes a ship a mining ship, so it is the
     * whole test. A loadout we do not hold is not a mining ship either - saying
     * nothing is better than showing a mining card over a combat build.
     */
    private static boolean hasRefinery(ShipLoadOutDto loadout) {
        if (loadout == null || loadout.getModules() == null) return false;
        for (ModuleDto module : loadout.getModules()) {
            if (module == null || module.getItem() == null) continue;
            if (module.getItem().toLowerCase(Locale.ROOT).contains(REFINERY)) return true;
        }
        return false;
    }

    /**
     * Whether the reported hold is the ship's. An older event without the field
     * is taken at face value; the SRV is the case worth excluding.
     */
    private static boolean isShipHold(GameEvents.CargoEvent cargo) {
        if (cargo == null) return false;
        String vessel = cargo.getVessel();
        return vessel == null || vessel.isBlank() || SHIP.equalsIgnoreCase(vessel);
    }

    private static HudRow.State holdState(int used, int capacity) {
        if (used >= capacity) return HudRow.State.CRITICAL;
        if (used >= capacity * HOLD_WARN_FRACTION) return HudRow.State.WARN;
        return HudRow.State.NORMAL;
    }

    private static HudRow.State limpetState(int limpets) {
        if (limpets <= 0) return HudRow.State.CRITICAL;
        if (limpets <= LIMPETS_LOW) return HudRow.State.WARN;
        return HudRow.State.NORMAL;
    }

    private static String tonnage(int tonnes) {
        return String.format("%,d T", Math.max(0, tonnes));
    }

    /**
     * Where the run is happening, or nothing when the system is not known - the
     * subtitle is optional and a blank line reads worse than no line.
     */
    private String systemLabel() {
        String system = playerSession.getPrimaryStarName();
        return system == null || system.isBlank() ? null : system.toUpperCase(Locale.ROOT);
    }

    /**
     * One mining target and the tonnage of it currently in the hold.
     */
    record TargetYield(String name, int tonnes) {
    }
}
