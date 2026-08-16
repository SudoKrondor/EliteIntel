package elite.intel.gameapi.search.edsm.monetize;

import elite.intel.db.managers.TradeProfileManager;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.gameapi.gamestate.dtos.NavRouteDto;
import elite.intel.gameapi.search.edsm.dto.data.Commodity;
import elite.intel.gameapi.search.edsm.dto.data.Station;
import elite.intel.gameapi.search.spansh.traderoute.TradeRouteSearchCriteria;
import elite.intel.util.json.ToJsonConvertible;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static elite.intel.gameapi.search.edsm.utils.EdsmUtils.ALLOWED_STATION_TYPES;
import static elite.intel.gameapi.search.edsm.utils.EdsmUtils.toStationWithMarket;

public class MonetizeRoute {

    /**
     * A stop is only worth making if the commodity can fill most of the hold, so a market is
     * ignored unless it can supply (or absorb) at least this fraction of the ship's cargo capacity.
     */
    private static final double MIN_CARGO_FILL_RATIO = 0.80;

    public static TradeTransaction findTrade(List<NavRouteDto> stops) {

        TradeProfileManager tradeProfileManager = TradeProfileManager.getInstance();
        TradeRouteSearchCriteria criteria = tradeProfileManager.getCriteria(false);
        List<String> allowedStations = new ArrayList<>(ALLOWED_STATION_TYPES);

        if (criteria.isAllowFleetCarriers()) {
            allowedStations.add("fleet carrier");
        }
        if (criteria.isAllowPlanetary()) {
            allowedStations.add("planetary port");
            allowedStations.add("odyssey settlement");
        }
        if (!criteria.isRequiresLargePad()) {
            allowedStations.add("outpost");
        }

        NavRouteDto destination = stops.getLast();
        List<Station> destinations = toStationWithMarket(destination.getName(), allowedStations);
        List<Station> sources = new ArrayList<>();
        for (NavRouteDto stop : stops) {
            sources.addAll(toStationWithMarket(stop.getName(), allowedStations));
        }
        return MonetizeRoute.findTrade(sources, destinations, minimumQuantity(criteria.getMaxCargo()));
    }

    /**
     * The smallest tradeable quantity worth a docking stop: 80 percent of the ship's cargo capacity,
     * rounded up to the next whole tonne.
     */
    static int minimumQuantity(int cargoCapacity) {
        // WHY: an unknown capacity must not reject every market, so it falls back to the floor that
        // applied before the threshold existed (more than one tonne) rather than to no floor at all.
        if (cargoCapacity < 1) return 2;
        return Math.max(1, (int) Math.ceil(cargoCapacity * MIN_CARGO_FILL_RATIO));
    }


    /**
     * Finds the best trade route between source and destination trade stations.
     * The method identifies the commodity with the maximum profit by comparing
     * the minimum buy price from source stations and maximum sell price from
     * destination stations while considering supply and demand constraints.
     *
     * @param sourceStations the list of trade stations the player can buy commodities from,
     *                       each containing market information including buy price and supply.
     * @param destStations   the list of trade stations the player can sell commodities to,
     *                       each containing market information including sell price and demand.
     * @param minQuantity    the smallest supply/demand worth stopping for; markets offering or
     *                       absorbing less than this are ignored, see {@link #minimumQuantity(int)}.
     * @return a RouteTuple representing the best trade route with maximum profit, or null
     * if no profitable trade route is available.
     */
    static TradeTransaction findTrade(List<Station> sourceStations, List<Station> destStations, int minQuantity) {
        Map<String, BuyInfo> sourceMinBuy = new HashMap<>();
        for (Station station : sourceStations) {
            String stationName = station.getName();
            String starSystem = station.getStarSystemName();
            String stationType = station.getType();
            List<Commodity> market = station.getCommodities();
            if (market == null) continue;
            for (Commodity entry : market) {
                if (entry.getStock() >= minQuantity && entry.getBuyPrice() > 0) {  // buyPrice is what player pays
                    String comm = entry.getName();
                    int price = entry.getBuyPrice();
                    BuyInfo current = sourceMinBuy.get(comm);
                    if (current == null || price < current.buyPrice) {
                        sourceMinBuy.put(comm, new BuyInfo(comm, starSystem, stationName, stationType, price, entry.getStock()));
                    }
                }
            }
        }

        Map<String, SellInfo> destMaxSell = new HashMap<>();
        for (Station station : destStations) {
            String stationName = station.getName();
            String starSystem = station.getStarSystemName();
            String stationType = station.getType();
            List<Commodity> market = station.getCommodities();
            if (market == null) continue;
            for (Commodity entry : market) {
                if (entry.getDemand() >= minQuantity && entry.getSellPrice() > 0) {  // sellPrice is what player gets
                    String comm = entry.getName();
                    int price = entry.getSellPrice();
                    SellInfo current = destMaxSell.get(comm);
                    if (current == null || price > current.sellPrice) {
                        destMaxSell.put(comm, new SellInfo(comm, starSystem, stationName, stationType, price, entry.getDemand()));
                    }
                }
            }
        }

        // Find best profit
        TradeTransaction best = null;
        int maxProfit = 0;
        for (String comm : sourceMinBuy.keySet()) {
            if (destMaxSell.containsKey(comm)) {
                BuyInfo buy = sourceMinBuy.get(comm);
                SellInfo sell = destMaxSell.get(comm);
                int profit = sell.sellPrice - buy.buyPrice;
                if (profit > maxProfit) {
                    maxProfit = profit;
                    best = new TradeTransaction(buy, sell);
                }
            }
        }

        return best;  // null if no profitable trades
    }

    public static class BuyInfo extends BaseJsonDto implements ToJsonConvertible {
        private String commodity;
        private String stationName;
        private String starSystem;
        private String stationType;
        private int buyPrice;
        private long supply;

        public BuyInfo(String commodity, String starSystem, String stationName, String stationType, int buyPrice, long supply) {
            this.commodity = commodity;
            this.starSystem = starSystem;
            this.stationName = stationName;
            this.stationType = stationType;
            this.buyPrice = buyPrice;
            this.supply = supply;
        }

        public String getCommodity() {
            return commodity;
        }

        public String getStationName() {
            return stationName;
        }

        public String getStarSystem() {
            return starSystem;
        }

        public String getStationType() {
            return stationType;
        }

        public int getBuyPrice() {
            return buyPrice;
        }

        public long getSupply() {
            return supply;
        }
    }

    public static class SellInfo extends BaseJsonDto implements ToJsonConvertible {
        private String commodity;
        private String starSystem;
        private String stationName;
        private String stationType;
        private int sellPrice;
        private long demand;

        public SellInfo(String commodity, String starSystem, String stationName, String stationType, int sellPrice, long demand) {
            this.commodity = commodity;
            this.starSystem = starSystem;
            this.stationName = stationName;
            this.stationType = stationType;
            this.sellPrice = sellPrice;
            this.demand = demand;
        }

        public String getCommodity() {
            return commodity;
        }

        public String getStarSystem() {
            return starSystem;
        }

        public String getStationName() {
            return stationName;
        }

        public String getStationType() {
            return stationType;
        }

        public int getSellPrice() {
            return sellPrice;
        }

        public long getDemand() {
            return demand;
        }
    }

    public static class TradeTransaction {
        private BuyInfo source;
        private SellInfo destination;

        public TradeTransaction(BuyInfo source, SellInfo destination) {
            this.source = source;
            this.destination = destination;
        }

        public BuyInfo getSource() {
            return source;
        }

        public SellInfo getDestination() {
            return destination;
        }
    }
}
