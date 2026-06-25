package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.StringJoiner;

public class LaunchDroneEvent extends BaseEvent {
    @SerializedName("Type")
    private String Type;

    @SerializedName("DroneId")
    private String droneId;

    public LaunchDroneEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofMinutes(5), "LaunchDrone");
        LaunchDroneEvent event = GsonFactory.getGson().fromJson(json, LaunchDroneEvent.class);
        this.Type = event.Type;
        this.droneId = String.valueOf(json.get("timestamp").getAsString());
    }

    @Override
    public String getEventType() {
        return "LaunchDrone";
    }

    @Override
    public String llmDescription() {
        return "Launched a limpet drone; carries the drone type (collector, prospector, and so on).";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public String getDroneId() {
        return droneId;
    }

    public void setDroneId(String droneId) {
        this.droneId = droneId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LaunchDroneEvent that = (LaunchDroneEvent) o;
        return Objects.equals(getType(), that.getType()) && Objects.equals(getDroneId(), that.getDroneId());
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(getType());
        result = 31 * result + Objects.hashCode(getDroneId());
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LaunchDroneEvent.class.getSimpleName() + "[", "]")
                .add("timestamp='" + timestamp + "'")
                .add("endOfLife=" + endOfLife)
                .toString();
    }
}