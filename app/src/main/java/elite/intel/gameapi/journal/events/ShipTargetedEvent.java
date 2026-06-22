package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;


public class ShipTargetedEvent extends BaseEvent {
    @SerializedName("TargetLocked")
    private boolean targetLocked;

    @SerializedName("Ship")
    private String ship;

    @SerializedName("ScanStage")
    private int scanStage;

    @SerializedName("PilotName")
    private String pilotName;

    @SerializedName("PilotName_Localised")
    private String pilotNameLocalised;

    @SerializedName("PilotRank")
    private String pilotRank;

    @SerializedName("ShieldHealth")
    private float shieldHealth;

    @SerializedName("HullHealth")
    private float hullHealth;

    @SerializedName("LegalStatus")
    private String legalStatus;

    @SerializedName("Power")
    private String pledgePower;

    @SerializedName("Faction")
    private String faction;

    @SerializedName("Ship_Localised")
    private String shipLocalised;

    @SerializedName("Subsystem")
    private String subsystem;

    @SerializedName("Subsystem_Localised")
    private String subsystemLocalised;

    @SerializedName("Bounty")
    private int bounty;

    public ShipTargetedEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "ShipTargeted");
        ShipTargetedEvent event = GsonFactory.getGson().fromJson(json, ShipTargetedEvent.class);
        this.targetLocked = event.targetLocked;
        this.ship = event.ship;
        this.scanStage = event.scanStage;
        this.pilotName = event.pilotName;
        this.pilotNameLocalised = event.pilotNameLocalised;
        this.pilotRank = event.pilotRank;
        this.shieldHealth = event.shieldHealth;
        this.hullHealth = event.hullHealth;
        this.legalStatus = event.legalStatus;
        this.pledgePower = event.pledgePower;
        this.faction = event.faction;
        this.shipLocalised = event.shipLocalised;
        this.bounty = event.bounty;
        this.subsystem = event.subsystem;
        this.subsystemLocalised = event.subsystemLocalised;
    }

    @Override
    public String getEventType() {
        return "ShipTargeted";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public boolean isTargetLocked() {
        return targetLocked;
    }

    public String getShip() {
        return ship;
    }

    public int getScanStage() {
        return scanStage;
    }

    public String getPilotName() {
        return pilotName;
    }

    public String getPilotNameLocalised() {
        return pilotNameLocalised;
    }

    public String getPilotRank() {
        return pilotRank;
    }

    public float getShieldHealth() {
        return shieldHealth;
    }

    public float getHullHealth() {
        return hullHealth;
    }

    public String getLegalStatus() {
        return legalStatus;
    }

    public String getPledgePower() {
        return pledgePower;
    }

    public String getFaction() {
        return faction;
    }

    public String getShipLocalised() {
        return shipLocalised;
    }

    public int getBounty() {
        return bounty;
    }

    public String getSubsystem() {
        return subsystem;
    }

    public String getSubsystemLocalised() {
        return subsystemLocalised;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}