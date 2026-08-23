package elite.intel.gameapi.search.spansh.station;

import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchCriteria;

/**
 * How much of the commander's evening a station costs to reach, once the flying is done.
 *
 * <p>Distance is only half of what a stop costs. An orbital station is a docking request and a pad; a
 * planetary port adds an approach, an orbital-cruise descent and a glide; an Odyssey settlement adds all of
 * that plus hunting a pad on a surface installation. Ranking on distance alone treats those as the same stop
 * and sends the commander down a gravity well to save a jump.
 *
 * <p>Every station search that ends in a route plot ranks on this FIRST, ahead of distance, so within the
 * radius the commander asked for a station always beats a settlement. Deliberately: the radius is theirs to
 * set, and narrowing it is how they say "no, the near one". A fleet carrier docks in space like any orbital.
 */
public final class DockingEffort {

    /**
     * Docking effort tiers, in the order a commander would choose between them.
     */
    public static final int IN_ORBIT = 0;
    public static final int SURFACE_PORT = 1;
    public static final int SETTLEMENT = 2;

    private DockingEffort() {
    }

    /**
     * The tier for a Spansh station type. An unrecognised type is taken as a surface port: the middle tier,
     * so a type we have not met neither displaces an orbital nor is buried under the settlements.
     */
    public static int of(String stationType) {
        if (stationType == null) return SURFACE_PORT;
        if (TradeStationSearchCriteria.StationType.SETTLEMENT_TRADE_TYPES.contains(stationType)) return SETTLEMENT;
        if (TradeStationSearchCriteria.StationType.PLANETARY_TRADE_TYPES.contains(stationType)) return SURFACE_PORT;
        return IN_ORBIT;
    }
}
