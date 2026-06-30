package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.StringJoiner;

public class CommanderEvent extends BaseEvent {
    @SerializedName("FID")
    private String fid;

    @SerializedName("Name")
    private String name;

    public CommanderEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofDays(30), "Commander");
        CommanderEvent event = GsonFactory.getGson().fromJson(json, CommanderEvent.class);
        this.fid = event.fid;
        this.name = event.name;
    }

    @Override
    public String getEventType() {
        return "Commander";
    }

    /** Load snapshot; memory context. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Identifies the commander at game load; carries the commander name. Background.";
    }

    @Override
    public String memorySummary() {
        ///NOTE: This will be problematic
        return name == null || name.isBlank() ? "" : "our commander is " + name;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getFID() {
        return fid;
    }

    public void setFID(String fid) {
        this.fid = fid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommanderEvent commander = (CommanderEvent) o;
        return Objects.equals(fid, commander.fid) && Objects.equals(name, commander.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fid, name);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CommanderEvent.class.getSimpleName() + "[", "]")
                .add("FID='" + fid + "'")
                .add("Name='" + name + "'")
                .toString();
    }
}