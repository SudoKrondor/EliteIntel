package elite.intel.gameapi.missions;

import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.dto.MissionDto;

import java.util.*;

/**
 * What the commander still has to buy to finish the missions they are holding.
 * <p>
 * Source-and-return missions ("Source 45 units of H.E. Suits") are taken in stacks and are the one
 * mission family the journal gives no progress signal for: the goods are bought on the open market as
 * ordinary cargo, so no {@code CargoDepot} update ties the purchase to the mission and no
 * {@code Cargo.json} row carries its MissionID. The only way to know what is still owed is to compare
 * each mission's requirement against the hold, which is what this does.
 */
public final class MissionCargo {

    private MissionCargo() {
    }

    /**
     * One mission's cargo requirement measured against the hold.
     *
     * @param mission   the mission it belongs to
     * @param symbol    the bare journal symbol of the commodity, joinable with the cargo hold
     * @param required  units the mission asks for
     * @param held      units of that commodity in the hold that this mission can claim
     * @param shortfall units still to buy; zero once the hold covers the requirement
     */
    public record Outstanding(MissionDto mission, String symbol, int required, int held, int shortfall) {

        public boolean isSatisfied() {
            return shortfall <= 0;
        }
    }

    /**
     * Every mission that wants cargo, soonest expiry first, each measured against the hold.
     * <p>
     * The hold is allocated in that same order rather than compared mission by mission: a stack of two
     * Haematite missions for 18 and 72 units needs 90 units between them, and 72 in the hold covers the
     * first and leaves the second still owing 18. Comparing each mission against the raw hold would call
     * both of them finished.
     */
    public static List<Outstanding> outstanding(Collection<MissionDto> missions, Map<String, Integer> heldBySymbol) {
        Map<String, Integer> pool = new HashMap<>(heldBySymbol == null ? Map.of() : heldBySymbol);
        List<Outstanding> result = new ArrayList<>();
        for (MissionDto mission : sortedCargoMissions(missions)) {
            String symbol = mission.getCommoditySymbol();
            int required = (int) mission.getCount();
            int available = pool.getOrDefault(symbol, 0);
            int claimed = Math.min(available, required);
            pool.put(symbol, available - claimed);
            result.add(new Outstanding(mission, symbol, required, claimed, required - claimed));
        }
        return result;
    }

    /**
     * The next mission to go shopping for: the one expiring soonest whose commodity is not already in
     * the hold. Empty when every cargo mission is covered, or when none of them wants cargo at all -
     * two answers the caller has to tell apart, which is why {@link #outstanding} is public too.
     */
    public static Optional<Outstanding> nextToSource(Collection<MissionDto> missions, Map<String, Integer> heldBySymbol) {
        return outstanding(missions, heldBySymbol).stream()
                .filter(item -> !item.isSatisfied())
                .findFirst();
    }

    /**
     * The hold as a symbol-to-count map. The journal writes inventory names lower-cased, which is the
     * same shape {@link elite.intel.gameapi.JournalSymbol} normalises a mission's commodity to.
     */
    public static Map<String, Integer> heldBySymbol(GameEvents.CargoEvent cargo) {
        Map<String, Integer> held = new HashMap<>();
        if (cargo == null || cargo.getInventory() == null) return held;
        for (GameEvents.Inventory item : cargo.getInventory()) {
            if (item == null || item.getName() == null) continue;
            held.merge(item.getName().toLowerCase(Locale.ROOT), (int) item.getCount(), Integer::sum);
        }
        return held;
    }

    /**
     * The missions that actually want cargo: a symbol to buy and a count to buy it in. A donation or a
     * massacre contract has neither and is not a shopping list.
     */
    private static List<MissionDto> sortedCargoMissions(Collection<MissionDto> missions) {
        if (missions == null) return List.of();
        return missions.stream()
                .filter(Objects::nonNull)
                .filter(mission -> mission.getCommoditySymbol() != null && !mission.getCommoditySymbol().isBlank())
                .filter(mission -> mission.getCount() > 0)
                .sorted(MissionSelection.EXPIRY_ORDER)
                .toList();
    }

}
