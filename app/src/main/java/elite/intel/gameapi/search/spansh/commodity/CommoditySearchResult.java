package elite.intel.gameapi.search.spansh.commodity;

import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.ToJsonConvertible;

public class CommoditySearchResult extends BaseJsonDto implements ToJsonConvertible {

    private String commodity;
    private double price;
    private String starSystem;
    private String stationName;
    private String stationType;

    /**
     * True when this market is a fleet carrier, which is player-owned and JUMPS.
     * <p>
     * Carried on the result, and not left for the caller to infer from {@link #stationType}, because it is
     * a warning the commander has to hear: the carrier was in this system when Spansh last synced and may
     * be a thousand light years away by the time he arrives. It is also what a narration payload has to
     * carry for the spoken line to mention it - an instruction to warn, with nothing in the payload saying
     * whether there is anything to warn about, is an invitation to invent one.
     */
    private boolean fleetCarrier;

    /**
     * Units on sale. Carried because the search will settle for a part load rather than report an ordinary
     * good as nonexistent, so how much is actually there is something the commander has to be told.
     */
    private long supply;

    /**
     * When Spansh last had this market's contents uploaded to it, ISO-8601, or null when it does not say.
     * <p>
     * Spansh is crowd-sourced: this is the age of the claim that the market sells the good, and it is what
     * decides whether the commander's own last look at that market is the better answer. See
     * {@link SpanshCommoditySearch#correctWithFirstHandData}.
     */
    private String marketUpdatedAt;

    /**
     * True when the price and quantity on this result came from a market board the commander opened
     * themselves, rather than from Spansh's crowd-sourced copy of it.
     * <p>
     * Carried so the answer can stop hedging when it does not need to: a Spansh row is a claim of unknown
     * age - measured live, Bari Gateway was quoted at 57,844 for Tritium from a row 10 days old while the
     * game was paying 53,992 - but a figure the game itself gave us needs no such warning.
     */
    private boolean seenFirstHand;

    /// transient
    private double distanceFromPlayer;

    public boolean isSeenFirstHand() {
        return seenFirstHand;
    }

    public void setSeenFirstHand(boolean seenFirstHand) {
        this.seenFirstHand = seenFirstHand;
    }

    public String getMarketUpdatedAt() {
        return marketUpdatedAt;
    }

    public void setMarketUpdatedAt(String marketUpdatedAt) {
        this.marketUpdatedAt = marketUpdatedAt;
    }

    public double getDistanceFromPlayer() {
        return distanceFromPlayer;
    }

    public void setDistanceFromPlayer(double distanceFromPlayer) {
        this.distanceFromPlayer = distanceFromPlayer;
    }

    public String getCommodity() {
        return commodity;
    }

    public void setCommodity(String commodity) {
        this.commodity = commodity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getStarSystem() {
        return starSystem;
    }

    public void setStarSystem(String starSystem) {
        this.starSystem = starSystem;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getStationType() {
        return stationType;
    }

    public long getSupply() {
        return supply;
    }

    public void setSupply(long supply) {
        this.supply = supply;
    }

    public boolean isFleetCarrier() {
        return fleetCarrier;
    }

    public void setFleetCarrier(boolean fleetCarrier) {
        this.fleetCarrier = fleetCarrier;
    }

    public void setStationType(String stationType) {
        this.stationType = stationType;
    }

    @Override public String toString() {
        return toJson();
    }
}
