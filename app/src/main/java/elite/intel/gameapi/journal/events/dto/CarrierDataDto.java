package elite.intel.gameapi.journal.events.dto;

import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.HashMap;
import java.util.Map;

public class CarrierDataDto implements ToJsonConvertible {


    private static final int MAX_CARRIER_SINGLE_JUMP_RANGE = 500;
    private static final int MAX_FUEL_PER_JUMP = 100;
    private String starName;
    private Long systemAddress;
    private long totalBalance;
    private long reserveBalance;
    /**
     * Spare tritium in the carrier's CARGO, as opposed to {@link #fuelSupply} in its tank. Tonnes the
     * commander can still top the tank up with, so the route calculator counts it as range in hand.
     * <p>
     * The game never states it: a carrier's cargo reaches a third-party tool only through the goods its
     * owner has put on the market. So this is the carrier's own tritium line where the market shows one,
     * corrected by every tonne we watch move, and whatever the commander last told us where it does not -
     * see {@code CarrierHoldLedger} and {@code SetCarrierFuelReserveCommand}.
     */
    private int fuelReserve;
    /**
     * The game's own id for this carrier, which is also its MarketID. Zero until the commander opens its
     * management panel, since {@code CarrierStats} is the only event that both names the carrier and says
     * which one it is.
     */
    private long carrierId;
    private String callSign;
    private String carrierName;
    private String carrierType;
    /**
     * The voice this carrier's traffic control speaks with, or null for a stranger drawn at random - the
     * behaviour every transmission had before a carrier could be given one. A radio-engine voice name (see
     * {@code RadioVoicing}), not a ship voice: transmissions are never voiced by the main mouth.
     */
    private String voice;
    private String dockingAccess;
    private boolean allowNotorious;
    private boolean isPendingDecommission;
    private String spaceUsage;
    private String finance;
    private String crew;
    private int cargoSpaceUsed;
    private int cargoSpaceReserved;
    private int shipRacks;
    private int modulePacks;
    private int freeSpaceInCargo;
    private int cargoCapacity;
    private long marketBalance;
    private int pioneerSupplyTax;
    private int shipYardSupplyTax;
    private int rearmSupplyTax;
    private int refuelSupplyTax;
    private int repairSupplyTax;
    private int fuelSupply=0;
    /**
     * Whether {@link #fuelSupply} is a figure the game reported. False for a level we worked out ourselves,
     * and false for a carrier we have never had a reading for, which is exactly the doubt an announcement
     * has to voice rather than hide.
     */
    private boolean fuelSupplyMeasured = false;
    private double x,y,z;
    private final Map<String, Integer> commodity = new HashMap<>();
    /**
     * Whether {@link #commodity} is an account of the hold or merely an empty map we have never filled.
     * <p>
     * The two are not the same answer and cannot be told apart by looking: a carrier we have emptied holds
     * nothing, and a carrier we have never looked at holds nothing we know of. The first must override the
     * old market snapshot; the second must defer to it. Absent from an older saved carrier, so it reads
     * false there - which is the safe way round.
     */
    private boolean holdTracked = false;

    public long getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(long totalBalance) {
        this.totalBalance = totalBalance;
    }

    public long getReserveBalance() {
        return reserveBalance;
    }

    public void setReserveBalance(long reserveBalance) {
        this.reserveBalance = reserveBalance;
    }

    public long getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(long carrierId) {
        this.carrierId = carrierId;
    }

    public String getCallSign() {
        return callSign;
    }

    public void setCallSign(String callSign) {
        this.callSign = callSign;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public String getCarrierType() {
        return carrierType;
    }

    public void setCarrierType(String carrierType) {
        this.carrierType = carrierType;
    }

    public String getDockingAccess() {
        return dockingAccess;
    }

    public void setDockingAccess(String dockingAccess) {
        this.dockingAccess = dockingAccess;
    }

    public boolean isAllowNotorious() {
        return allowNotorious;
    }

    public void setAllowNotorious(boolean allowNotorious) {
        this.allowNotorious = allowNotorious;
    }

    public boolean isPendingDecommission() {
        return isPendingDecommission;
    }

    public void setPendingDecommission(boolean pendingDecommission) {
        isPendingDecommission = pendingDecommission;
    }

    public String getSpaceUsage() {
        return spaceUsage;
    }

    public void setSpaceUsage(String spaceUsage) {
        this.spaceUsage = spaceUsage;
    }

    public String getFinance() {
        return finance;
    }

    public void setFinance(String finance) {
        this.finance = finance;
    }

    public String getCrew() {
        return crew;
    }

    public void setCrew(String crew) {
        this.crew = crew;
    }

    public int getCargoSpaceUsed() {
        return cargoSpaceUsed;
    }

    public void setCargoSpaceUsed(int cargoSpaceUsed) {
        this.cargoSpaceUsed = cargoSpaceUsed;
    }

    public int getCargoSpaceReserved() {
        return cargoSpaceReserved;
    }

    public void setCargoSpaceReserved(int cargoSpaceReserved) {
        this.cargoSpaceReserved = cargoSpaceReserved;
    }

    public int getShipRacks() {
        return shipRacks;
    }

    public void setShipRacks(int shipRacks) {
        this.shipRacks = shipRacks;
    }

    public int getModulePacks() {
        return modulePacks;
    }

    public void setModulePacks(int modulePacks) {
        this.modulePacks = modulePacks;
    }

    public int getFreeSpaceInCargo() {
        return freeSpaceInCargo;
    }

    public void setFreeSpaceInCargo(int freeSpaceInCargo) {
        this.freeSpaceInCargo = freeSpaceInCargo;
    }

    public int getCargoCapacity() {
        return cargoCapacity;
    }

    public void setCargoCapacity(int cargoCapacity) {
        this.cargoCapacity = cargoCapacity;
    }

    public long getMarketBalance() {
        return marketBalance;
    }

    public void setMarketBalance(long marketBalance) {
        this.marketBalance = marketBalance;
    }

    public int getPioneerSupplyTax() {
        return pioneerSupplyTax;
    }

    public void setPioneerSupplyTax(int pioneerSupplyTax) {
        this.pioneerSupplyTax = pioneerSupplyTax;
    }

    public int getShipYardSupplyTax() {
        return shipYardSupplyTax;
    }

    public void setShipYardSupplyTax(int shipYardSupplyTax) {
        this.shipYardSupplyTax = shipYardSupplyTax;
    }

    public int getRearmSupplyTax() {
        return rearmSupplyTax;
    }

    public void setRearmSupplyTax(int rearmSupplyTax) {
        this.rearmSupplyTax = rearmSupplyTax;
    }

    public int getRefuelSupplyTax() {
        return refuelSupplyTax;
    }

    public void setRefuelSupplyTax(int refuelSupplyTax) {
        this.refuelSupplyTax = refuelSupplyTax;
    }

    public int getRepairSupplyTax() {
        return repairSupplyTax;
    }

    public void setRepairSupplyTax(int repairSupplyTax) {
        this.repairSupplyTax = repairSupplyTax;
    }

    public String getStarName() {
        return starName;
    }

    public void setStarName(String starName) {
        this.starName = starName;
    }


    /**
     * Records a depot level the game itself reported (CarrierStats, or the total a fuel deposit confirms).
     * Only these figures are exact, and only until we work out the next one ourselves.
     */
    public void setMeasuredFuelLevel(int tons) {
        this.fuelSupply = tons;
        this.fuelSupplyMeasured = true;
    }

    /**
     * Charges tritium we believe a jump burned, taken from the plotted leg rather than from the game.
     * <p>
     * WHY it marks the level inexact: the game reports the depot only when the commander opens carrier
     * management, so between those moments this is arithmetic on top of an older reading, and it drifts
     * with every tonne that moves any other way - a market sale, a squadron mate's donation. Something
     * has to know the difference, or an estimate gets announced with the confidence of a measurement.
     */
    public void chargeEstimatedFuel(int tons) {
        this.fuelSupply = this.fuelSupply - tons;
        this.fuelSupplyMeasured = false;
    }

    public int getFuelLevel() {
        return fuelSupply;
    }

    /**
     * Whether {@link #getFuelLevel()} is a figure the game reported rather than one we worked out.
     */
    public boolean isFuelLevelMeasured() {
        return fuelSupplyMeasured;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        if(z == 0) return;
        this.z = z;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        if(y == 0) return;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        if(x == 0) return;
        this.x = x;
    }

    /**
     * What the carrier is holding, keyed by bare journal symbol - seeded from its market screen and kept
     * level by every {@code CargoTransfer} since. See {@code CarrierHoldLedger} for why it is maintained
     * rather than read fresh, and for what it still cannot see.
     */
    public Map<String, Integer> getCommodity() {
        return commodity == null ? new HashMap<>() : commodity;
    }

    /**
     * Replaces the whole ledger with a fresh account of the hold, and marks it an account rather than a
     * blank.
     * <p>
     * Deliberately not {@link #addCommodity} in a loop: that is for a MOVEMENT of cargo, and it reads a
     * tritium line as fuel taken aboard. Re-reading the same market screen twice would then double the
     * carrier's fuel reserve, so a wholesale account has to be a different verb from a delta.
     */
    public void replaceCommodities(Map<String, Integer> stock) {
        this.commodity.clear();
        this.holdTracked = true;
        if (stock == null) return;
        for (Map.Entry<String, Integer> entry : stock.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) continue;
            this.commodity.put(entry.getKey().toLowerCase(), entry.getValue());
        }
    }

    public boolean isHoldTracked() {
        return holdTracked;
    }

    public void addCommodity(String commodity, Integer amount) {
        if (commodity == null) return;
        String c = commodity.toLowerCase();
        Integer existingAmount = this.commodity.get(c);
        if (existingAmount != null) {
            existingAmount = existingAmount + amount;
            this.commodity.put(c, existingAmount);
        } else {
            this.commodity.put(c, amount);
        }
    }

    public void removeCommodity(String commodity, int amount) {
        if (commodity == null) return;
        String c = commodity.toLowerCase();
        Integer existingCommodity = this.commodity.get(c);
        if (existingCommodity != null) {
            existingCommodity = existingCommodity - amount;
            if (existingCommodity > 0) {
                this.commodity.put(c, existingCommodity);
            } else {
                this.commodity.remove(c);
            }
        }
    }


    public int getFuelReserve() {
        return fuelReserve;
    }

    public void setFuelReserve(int fuelReserve) {
        this.fuelReserve = fuelReserve;
    }

    /**
     * Moves the spare-tritium figure by cargo we watched come aboard or leave, never below zero.
     * <p>
     * Separate from {@link #setFuelReserve} because the two answer different questions: that one is a
     * statement of how much is there, this one a correction by how much it changed. Only a movement we
     * actually saw may correct a figure the commander may have set by hand.
     */
    public void adjustFuelReserve(int tons) {
        this.fuelReserve = Math.max(0, this.fuelReserve + tons);
    }

    /**
     * Range in light years once the reserve is drawn on as well.
     */
    public int getRange() {
        int totalFuelAvailable = getFuelLevel() + getFuelReserve();
        return (totalFuelAvailable / MAX_FUEL_PER_JUMP) * MAX_CARRIER_SINGLE_JUMP_RANGE;
    }

    /**
     * Range in light years on the supply depot alone, leaving the reserve untouched.
     */
    public int getRangeExcludingReserve() {
        return (getFuelLevel() / MAX_FUEL_PER_JUMP) * MAX_CARRIER_SINGLE_JUMP_RANGE;
    }

    public int getFundedOperation() {
        return Math.toIntExact(reserveBalance / 31000000);
    }


    @Override public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    public Long getSystemAddress() {
        return systemAddress;
    }

    public void setSystemAddress(Long systemAddress) {
        this.systemAddress = systemAddress;
    }
}
