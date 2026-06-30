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
    private List<Crew> Crew;

    @SerializedName("ShipPacks")
    private List<Object> ShipPacks;

    @SerializedName("ModulePacks")
    private List<Object> ModulePacks;

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
        this.Crew = event.Crew;
        this.ShipPacks = event.ShipPacks;
        this.ModulePacks = event.ModulePacks;
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
        private int TotalCapacity;

        @SerializedName("Crew")
        private int Crew;

        @SerializedName("Cargo")
        private int Cargo;

        @SerializedName("CargoSpaceReserved")
        private int CargoSpaceReserved;

        @SerializedName("ShipPacks")
        private int ShipPacks;

        @SerializedName("ModulePacks")
        private int ModulePacks;

        @SerializedName("FreeSpace")
        private int FreeSpace;

        public int getTotalCapacity() {
            return TotalCapacity;
        }

        public void setTotalCapacity(int totalCapacity) {
            this.TotalCapacity = totalCapacity;
        }

        public int getCrew() {
            return Crew;
        }

        public void setCrew(int crew) {
            this.Crew = crew;
        }

        public int getCargo() {
            return Cargo;
        }

        public void setCargo(int cargo) {
            this.Cargo = cargo;
        }

        public int getCargoSpaceReserved() {
            return CargoSpaceReserved;
        }

        public void setCargoSpaceReserved(int cargoSpaceReserved) {
            this.CargoSpaceReserved = cargoSpaceReserved;
        }

        public int getShipPacks() {
            return ShipPacks;
        }

        public void setShipPacks(int shipPacks) {
            this.ShipPacks = shipPacks;
        }

        public int getModulePacks() {
            return ModulePacks;
        }

        public void setModulePacks(int modulePacks) {
            this.ModulePacks = modulePacks;
        }

        public int getFreeSpace() {
            return FreeSpace;
        }

        public void setFreeSpace(int freeSpace) {
            this.FreeSpace = freeSpace;
        }
    }

    public static class Finance {
        @SerializedName("CarrierBalance")
        private long CarrierBalance;

        @SerializedName("ReserveBalance")
        private long ReserveBalance;

        @SerializedName("AvailableBalance")
        private long AvailableBalance;

        @SerializedName("TaxRate_pioneersupplies")
        private int taxRatePioneerSupplies;

        @SerializedName("TaxRate_shipyard")
        private int TaxRateShipyard;

        @SerializedName("TaxRate_rearm")
        private int TaxRateRearm;

        @SerializedName("TaxRate_refuel")
        private int TaxRateRefuel;

        @SerializedName("TaxRate_outfitting")
        private int TaxRateOutfitting;

        @SerializedName("TaxRate_repair")
        private int TaxRateRepair;

        @SerializedName("ReservePercent")
        private int ReservePercent;

        public long getCarrierBalance() {
            return CarrierBalance;
        }

        public void setCarrierBalance(long carrierBalance) {
            this.CarrierBalance = carrierBalance;
        }

        public long getReserveBalance() {
            return ReserveBalance;
        }

        public void setReserveBalance(long reserveBalance) {
            this.ReserveBalance = reserveBalance;
        }

        public long getAvailableBalance() {
            return AvailableBalance;
        }

        public void setAvailableBalance(long availableBalance) {
            this.AvailableBalance = availableBalance;
        }

        public int getTaxRatePioneerSupplies() {
            return taxRatePioneerSupplies;
        }

        public void setTaxRatePioneerSupplies(int taxRate) {
            this.taxRatePioneerSupplies = taxRate;
        }

        public int getTaxRateShipyard() {
            return TaxRateShipyard;
        }

        public void setTaxRateShipyard(int taxRate) {
            this.TaxRateShipyard = taxRate;
        }

        public int getTaxRateRearm() {
            return TaxRateRearm;
        }

        public void setTaxRateRearm(int taxRate) {
            this.TaxRateRearm = taxRate;
        }

        public int getTaxRateRefuel() {
            return TaxRateRefuel;
        }

        public void setTaxRateRefuel(int taxRate) {
            this.TaxRateRefuel = taxRate;
        }

        public int getTaxRateOutfitting() {
            return TaxRateOutfitting;
        }

        public void setTaxRateOutfitting(int taxRate) {
            this.TaxRateOutfitting = taxRate;
        }

        public int getTaxRateRepair() {
            return TaxRateRepair;
        }

        public void setTaxRateRepair(int taxRate) {
            this.TaxRateRepair = taxRate;
        }

        public int getReservePercent() {
            return ReservePercent;
        }

        public void setReservePercent(int reservePercent) {
            this.ReservePercent = reservePercent;
        }
    }

    public static class Crew {
        @SerializedName("CrewRole")
        private String CrewRole;

        @SerializedName("Activated")
        private boolean Activated;

        @SerializedName("Enabled")
        private boolean Enabled;

        @SerializedName("CrewName")
        private String CrewName;

        public String getCrewRole() {
            return CrewRole;
        }

        public void setCrewRole(String crewRole) {
            this.CrewRole = crewRole;
        }

        public boolean isActivated() {
            return Activated;
        }

        public void setActivated(boolean activated) {
            this.Activated = activated;
        }

        public boolean isEnabled() {
            return Enabled;
        }

        public void setEnabled(boolean enabled) {
            this.Enabled = enabled;
        }

        public String getCrewName() {
            return CrewName;
        }

        public void setCrewName(String crewName) {
            this.CrewName = crewName;
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
        return Crew;
    }

    public void setCrew(List<Crew> crew) {
        this.Crew = crew;
    }

    public List<Object> getShipPacks() {
        return ShipPacks;
    }

    public void setShipPacks(List<Object> shipPacks) {
        this.ShipPacks = shipPacks;
    }

    public List<Object> getModulePacks() {
        return ModulePacks;
    }

    public void setModulePacks(List<Object> modulePacks) {
        this.ModulePacks = modulePacks;
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