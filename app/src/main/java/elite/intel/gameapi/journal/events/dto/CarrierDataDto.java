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
    private int fuelReserve;
    private String callSign;
    private String carrierName;
    private String carrierType;
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
    private double x,y,z;
    private final Map<String, Integer> commodity = new HashMap<>();

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


    public void setFuelLevel(int fuelLevel) {
        this.fuelSupply = fuelLevel;
    }

    public int getFuelLevel() {
        return fuelSupply;
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

    public Map<String, Integer> getCommodity() {
        return commodity == null ? new HashMap<>() : commodity;
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

        if("tritium".equalsIgnoreCase(commodity)) {
            this.fuelReserve = this.fuelReserve + amount;
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
