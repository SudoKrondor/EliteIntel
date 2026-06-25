package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.StringJoiner;

public class CommanderEvent extends BaseEvent {
    @SerializedName("FID")
    private String FID;

    @SerializedName("Name")
    private String Name;

    public CommanderEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofDays(30), "Commander");
        CommanderEvent event = GsonFactory.getGson().fromJson(json, CommanderEvent.class);
        this.FID = event.FID;
        this.Name = event.Name;
    }

    @Override
    public String getEventType() {
        return "Commander";
    }

    @Override
    public String llmDescription() {
        return "Identifies the commander at game load; carries the commander name. Background.";
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
        return FID;
    }

    public void setFID(String FID) {
        this.FID = FID;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        this.Name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommanderEvent commander = (CommanderEvent) o;
        return Objects.equals(FID, commander.FID) && Objects.equals(Name, commander.Name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(FID, Name);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CommanderEvent.class.getSimpleName() + "[", "]")
                .add("FID='" + FID + "'")
                .add("Name='" + Name + "'")
                .toString();
    }
}