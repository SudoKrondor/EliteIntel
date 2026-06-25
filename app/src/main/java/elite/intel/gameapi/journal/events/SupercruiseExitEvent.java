package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.StringJoiner;

public class SupercruiseExitEvent extends BaseEvent {
    @SerializedName("Taxi")
    private boolean taxi;

    @SerializedName("Multicrew")
    private boolean multicrew;

    @SerializedName("StarSystem")
    private String starSystem;

    @SerializedName("SystemAddress")
    private long systemAddress;

    @SerializedName("Body")
    private String body;

    @SerializedName("BodyID")
    private Long bodyId;

    @SerializedName("BodyType")
    private String bodyType;

    public SupercruiseExitEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "SupercruiseExit");
        SupercruiseExitEvent event = GsonFactory.getGson().fromJson(json, SupercruiseExitEvent.class);
        this.taxi = event.taxi;
        this.multicrew = event.multicrew;
        this.starSystem = event.starSystem;
        this.systemAddress = event.systemAddress;
        this.body = event.body;
        this.bodyId = event.bodyId;
        this.bodyType = event.bodyType;
    }

    @Override
    public String getEventType() {
        return "SupercruiseExit";
    }

    /** Routine flight transition; memory only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Dropped from supercruise into normal space; carries the star system and the body you arrived at.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public boolean isTaxi() {
        return taxi;
    }

    public void setTaxi(boolean taxi) {
        this.taxi = taxi;
    }

    public boolean isMulticrew() {
        return multicrew;
    }

    public void setMulticrew(boolean multicrew) {
        this.multicrew = multicrew;
    }

    public String getStarSystem() {
        return starSystem;
    }

    public void setStarSystem(String starSystem) {
        this.starSystem = starSystem;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public void setSystemAddress(long systemAddress) {
        this.systemAddress = systemAddress;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Long getBodyId() {
        return bodyId;
    }

    public void setBodyId(Long bodyId) {
        this.bodyId = bodyId;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SupercruiseExitEvent that = (SupercruiseExitEvent) o;
        return taxi == that.taxi &&
                multicrew == that.multicrew &&
                systemAddress == that.systemAddress &&
                bodyId == that.bodyId &&
                Objects.equals(starSystem, that.starSystem) &&
                Objects.equals(body, that.body) &&
                Objects.equals(bodyType, that.bodyType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taxi, multicrew, starSystem, systemAddress, body, bodyId, bodyType);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SupercruiseExitEvent.class.getSimpleName() + "[", "]")
                .add("taxi=" + taxi)
                .add("multicrew=" + multicrew)
                .add("starSystem='" + starSystem + "'")
                .add("systemAddress=" + systemAddress)
                .add("body='" + body + "'")
                .add("bodyId=" + bodyId)
                .add("bodyType='" + bodyType + "'")
                .toString();
    }
}