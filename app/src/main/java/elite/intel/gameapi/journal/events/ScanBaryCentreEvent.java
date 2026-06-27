package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class ScanBaryCentreEvent extends BaseEvent {
    @SerializedName("StarSystem")
    private String starSystem;

    @SerializedName("SystemAddress")
    private long systemAddress;

    @SerializedName("BodyID")
    private Long bodyID;

    @SerializedName("SemiMajorAxis")
    private double semiMajorAxis;

    @SerializedName("Eccentricity")
    private double eccentricity;

    @SerializedName("OrbitalInclination")
    private double orbitalInclination;

    @SerializedName("Periapsis")
    private double periapsis;

    @SerializedName("OrbitalPeriod")
    private double orbitalPeriod;

    @SerializedName("AscendingNode")
    private double ascendingNode;

    @SerializedName("MeanAnomaly")
    private double meanAnomaly;

    public ScanBaryCentreEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "ScanBaryCentre");
        ScanBaryCentreEvent event = GsonFactory.getGson().fromJson(json, ScanBaryCentreEvent.class);
        this.starSystem = event.starSystem;
        this.systemAddress = event.systemAddress;
        this.bodyID = event.bodyID;
        this.semiMajorAxis = event.semiMajorAxis;
        this.eccentricity = event.eccentricity;
        this.orbitalInclination = event.orbitalInclination;
        this.periapsis = event.periapsis;
        this.orbitalPeriod = event.orbitalPeriod;
        this.ascendingNode = event.ascendingNode;
        this.meanAnomaly = event.meanAnomaly;
    }

    @Override
    public String getEventType() {
        return "ScanBaryCentre";
    }

    /** Background barycentre detail. Ignore. */
    @Override
    public Importance importance() {
        return Importance.LOW;
    }

    @Override
    public String llmDescription() {
        return "Scanned the barycentre (shared orbital centre) of a multi-body group; carries the system and body id. Background detail.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getStarSystem() {
        return starSystem;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public Long getBodyID() {
        return bodyID;
    }

    public double getSemiMajorAxis() {
        return semiMajorAxis;
    }

    public double getEccentricity() {
        return eccentricity;
    }

    public double getOrbitalInclination() {
        return orbitalInclination;
    }

    public double getPeriapsis() {
        return periapsis;
    }

    public double getOrbitalPeriod() {
        return orbitalPeriod;
    }

    public double getAscendingNode() {
        return ascendingNode;
    }

    public double getMeanAnomaly() {
        return meanAnomaly;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}