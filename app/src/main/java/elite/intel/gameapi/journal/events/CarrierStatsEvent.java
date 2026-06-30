package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class CarrierStatsEvent extends BaseEvent {
    @SerializedName("CarrierID")
    private long carrierID;

    @SerializedName("CarrierType")
    private String carrierType;

    @SerializedName("Callsign")
    private String callsign;

    @SerializedName("Name")
    private String name;

    @SerializedName("DockingAccess")
    private String dockingAccess;

    @SerializedName("AllowNotorious")
    private boolean allowNotorious;

    @SerializedName("FuelLevel")
    private int fuelLevel;

    @SerializedName("JumpRangeCurr")
    private double jumpRangeCurr;

    @SerializedName("JumpRangeMax")
    private double jumpRangeMax;

    @SerializedName("PendingDecommission")
    private boolean pendingDecommission;

    @SerializedName("SpaceUsage")
    private SpaceUsage spaceUsage;

    @SerializedName("Finance")
    private Finance finance;

    @SerializedName("Crew")
    private List<Crew> crew;

    @SerializedName("ShipPacks")
    private List<Object> shipPacks;

    @SerializedName("ModulePacks")
    private List<Object> modulePacks;

    public CarrierStatsEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(60), "CarrierStats");
        CarrierStatsEvent event = GsonFactory.getGson().fromJson(json, CarrierStatsEvent.class);
        this.carrierID = event.carrierID;
        this.carrierType = event.carrierType;
        this.callsign = event.callsign;
        this.name = event.name;
        this.dockingAccess = event.dockingAccess;
        this.allowNotorious = event.allowNotorious;
        this.fuelLevel = event.fuelLevel;
        this.jumpRangeCurr = event.jumpRangeCurr;
        this.jumpRangeMax = event.jumpRangeMax;
        this.pendingDecommission = event.pendingDecommission;
        this.spaceUsage = event.spaceUsage;
        this.finance = event.finance;
        this.crew = event.crew;
        this.shipPacks = event.shipPacks;
        this.modulePacks = event.modulePacks;
    }

    @Override
    public String getEventType() {
        return "CarrierStats";
    }

    /** Carrier stats snapshot; memory context. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "A snapshot of fleet carrier statistics (balance, fuel, cargo, crew, services). Background detail.";
    }

    @Override
    public String memorySummary() {
        if (name == null || name.isBlank()) {
            return callsign == null || callsign.isBlank() ? "" : "fleet carrier " + callsign + ", fuel " + fuelLevel;
        }
        return "fleet carrier " + name
                + (callsign == null || callsign.isBlank() ? "" : " (" + callsign + ")") + ", fuel " + fuelLevel;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public static class SpaceUsage {
        @SerializedName("TotalCapacity")
        private int totalCapacity;

        @SerializedName("Crew")
        private int crew;

        @SerializedName("Cargo")
        private int cargo;

        @SerializedName("CargoSpaceReserved")
        private int cargoSpaceReserved;

        @SerializedName("ShipPacks")
        private int shipPacks;

        @SerializedName("ModulePacks")
        private int modulePacks;

        @SerializedName("FreeSpace")
        private int freeSpace;

        public int getTotalCapacity() {
            return totalCapacity;
        }

        public void setTotalCapacity(int totalCapacity) {
            this.totalCapacity = totalCapacity;
        }

        public int getCrew() {
            return crew;
        }

        public void setCrew(int crew) {
            this.crew = crew;
        }

        public int getCargo() {
            return cargo;
        }

        public void setCargo(int cargo) {
            this.cargo = cargo;
        }

        public int getCargoSpaceReserved() {
            return cargoSpaceReserved;
        }

        public void setCargoSpaceReserved(int cargoSpaceReserved) {
            this.cargoSpaceReserved = cargoSpaceReserved;
        }

        public int getShipPacks() {
            return shipPacks;
        }

        public void setShipPacks(int shipPacks) {
            this.shipPacks = shipPacks;
        }

        public int getModulePacks() {
            return modulePacks;
        }

        public void setModulePacks(int modulePacks) {
            this.modulePacks = modulePacks;
        }

        public int getFreeSpace() {
            return freeSpace;
        }

        public void setFreeSpace(int freeSpace) {
            this.freeSpace = freeSpace;
        }
    }

    public static class Finance {
        @SerializedName("CarrierBalance")
        private long carrierBalance;

        @SerializedName("ReserveBalance")
        private long reserveBalance;

        @SerializedName("AvailableBalance")
        private long availableBalance;

        @SerializedName("TaxRate_pioneersupplies")
        private int taxRatePioneerSupplies;

        @SerializedName("TaxRate_shipyard")
        private int taxRateShipyard;

        @SerializedName("TaxRate_rearm")
        private int taxRateRearm;

        @SerializedName("TaxRate_refuel")
        private int taxRateRefuel;

        @SerializedName("TaxRate_outfitting")
        private int taxRateOutfitting;

        @SerializedName("TaxRate_repair")
        private int taxRateRepair;

        @SerializedName("ReservePercent")
        private int reservePercent;

        public long getCarrierBalance() {
            return carrierBalance;
        }

        public void setCarrierBalance(long carrierBalance) {
            this.carrierBalance = carrierBalance;
        }

        public long getReserveBalance() {
            return reserveBalance;
        }

        public void setReserveBalance(long reserveBalance) {
            this.reserveBalance = reserveBalance;
        }

        public long getAvailableBalance() {
            return availableBalance;
        }

        public void setAvailableBalance(long availableBalance) {
            this.availableBalance = availableBalance;
        }

        public int getTaxRatePioneerSupplies() {
            return taxRatePioneerSupplies;
        }

        public void setTaxRatePioneerSupplies(int taxRate) {
            this.taxRatePioneerSupplies = taxRate;
        }

        public int getTaxRateShipyard() {
            return taxRateShipyard;
        }

        public void setTaxRateShipyard(int taxRate) {
            this.taxRateShipyard = taxRate;
        }

        public int getTaxRateRearm() {
            return taxRateRearm;
        }

        public void setTaxRateRearm(int taxRate) {
            this.taxRateRearm = taxRate;
        }

        public int getTaxRateRefuel() {
            return taxRateRefuel;
        }

        public void setTaxRateRefuel(int taxRate) {
            this.taxRateRefuel = taxRate;
        }

        public int getTaxRateOutfitting() {
            return taxRateOutfitting;
        }

        public void setTaxRateOutfitting(int taxRate) {
            this.taxRateOutfitting = taxRate;
        }

        public int getTaxRateRepair() {
            return taxRateRepair;
        }

        public void setTaxRateRepair(int taxRate) {
            this.taxRateRepair = taxRate;
        }

        public int getReservePercent() {
            return reservePercent;
        }

        public void setReservePercent(int reservePercent) {
            this.reservePercent = reservePercent;
        }
    }

    public static class Crew {
        @SerializedName("CrewRole")
        private String crewRole;

        @SerializedName("Activated")
        private boolean activated;

        @SerializedName("Enabled")
        private boolean enabled;

        @SerializedName("CrewName")
        private String crewName;

        public String getCrewRole() {
            return crewRole;
        }

        public void setCrewRole(String crewRole) {
            this.crewRole = crewRole;
        }

        public boolean isActivated() {
            return activated;
        }

        public void setActivated(boolean activated) {
            this.activated = activated;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCrewName() {
            return crewName;
        }

        public void setCrewName(String crewName) {
            this.crewName = crewName;
        }
    }

    public long getCarrierID() {
        return carrierID;
    }

    public void setCarrierID(long carrierID) {
        this.carrierID = carrierID;
    }

    public String getCarrierType() {
        return carrierType;
    }

    public void setCarrierType(String carrierType) {
        this.carrierType = carrierType;
    }

    public String getCallsign() {
        return callsign;
    }

    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getFuelLevel() {
        return fuelLevel;
    }

    public void setFuelLevel(int fuelLevel) {
        this.fuelLevel = fuelLevel;
    }

    public double getJumpRangeCurr() {
        return jumpRangeCurr;
    }

    public void setJumpRangeCurr(double jumpRangeCurr) {
        this.jumpRangeCurr = jumpRangeCurr;
    }

    public double getJumpRangeMax() {
        return jumpRangeMax;
    }

    public void setJumpRangeMax(double jumpRangeMax) {
        this.jumpRangeMax = jumpRangeMax;
    }

    public boolean isPendingDecommission() {
        return pendingDecommission;
    }

    public void setPendingDecommission(boolean pendingDecommission) {
        this.pendingDecommission = pendingDecommission;
    }

    public SpaceUsage getSpaceUsage() {
        return spaceUsage;
    }

    public void setSpaceUsage(SpaceUsage spaceUsage) {
        this.spaceUsage = spaceUsage;
    }

    public Finance getFinance() {
        return finance;
    }

    public void setFinance(Finance finance) {
        this.finance = finance;
    }

    public List<Crew> getCrew() {
        return crew;
    }

    public void setCrew(List<Crew> crew) {
        this.crew = crew;
    }

    public List<Object> getShipPacks() {
        return shipPacks;
    }

    public void setShipPacks(List<Object> shipPacks) {
        this.shipPacks = shipPacks;
    }

    public List<Object> getModulePacks() {
        return modulePacks;
    }

    public void setModulePacks(List<Object> modulePacks) {
        this.modulePacks = modulePacks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CarrierStatsEvent that = (CarrierStatsEvent) o;
        return carrierID == that.carrierID &&
                allowNotorious == that.allowNotorious &&
                fuelLevel == that.fuelLevel &&
                Double.compare(that.jumpRangeCurr, jumpRangeCurr) == 0 &&
                Double.compare(that.jumpRangeMax, jumpRangeMax) == 0 &&
                pendingDecommission == that.pendingDecommission &&
                Objects.equals(carrierType, that.carrierType) &&
                Objects.equals(callsign, that.callsign) &&
                Objects.equals(name, that.name) &&
                Objects.equals(dockingAccess, that.dockingAccess);
    }

    @Override
    public int hashCode() {
        return Objects.hash(carrierID, carrierType, callsign, name, dockingAccess,
                allowNotorious, fuelLevel, jumpRangeCurr, jumpRangeMax, pendingDecommission);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CarrierStatsEvent.class.getSimpleName() + "[", "]")
                .add("CarrierID=" + carrierID)
                .add("CarrierType='" + carrierType + "'")
                .add("Callsign='" + callsign + "'")
                .add("Name='" + name + "'")
                .add("DockingAccess='" + dockingAccess + "'")
                .add("AllowNotorious=" + allowNotorious)
                .add("FuelLevel=" + fuelLevel)
                .add("JumpRangeCurr=" + jumpRangeCurr)
                .add("JumpRangeMax=" + jumpRangeMax)
                .add("PendingDecommission=" + pendingDecommission)
                .toString();
    }
}