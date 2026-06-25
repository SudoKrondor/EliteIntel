package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class DisembarkEvent extends BaseEvent {
    @SerializedName("SRV")
    private boolean srv;

    @SerializedName("Taxi")
    private boolean taxi;

    @SerializedName("Multicrew")
    private boolean multicrew;

    @SerializedName("ID")
    private int id;

    @SerializedName("StarSystem")
    private String starSystem;

    @SerializedName("SystemAddress")
    private long systemAddress;

    @SerializedName("Body")
    private String body;

    @SerializedName("BodyID")
    private int bodyId;

    @SerializedName("OnStation")
    private boolean onStation;

    @SerializedName("OnPlanet")
    private boolean onPlanet;

    public DisembarkEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "Disembark");
        DisembarkEvent event = GsonFactory.getGson().fromJson(json, DisembarkEvent.class);
        this.srv = event.srv;
        this.taxi = event.taxi;
        this.multicrew = event.multicrew;
        this.id = event.id;
        this.starSystem = event.starSystem;
        this.systemAddress = event.systemAddress;
        this.body = event.body;
        this.bodyId = event.bodyId;
        this.onStation = event.onStation;
        this.onPlanet = event.onPlanet;
    }

    @Override
    public String getEventType() {
        return "Disembark";
    }

    /** Routine on-foot transition; memory only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Disembarked on foot from the ship or SRV; carries whether it is at a station, settlement, or on a planet surface.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public boolean isSrv() {
        return srv;
    }

    public boolean isTaxi() {
        return taxi;
    }

    public boolean isMulticrew() {
        return multicrew;
    }

    public int getId() {
        return id;
    }

    public String getStarSystem() {
        return starSystem;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public String getBody() {
        return body;
    }

    public int getBodyId() {
        return bodyId;
    }

    public boolean isOnStation() {
        return onStation;
    }

    public boolean isOnPlanet() {
        return onPlanet;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}