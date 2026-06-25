package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class StartJumpEvent extends BaseEvent {
    @SerializedName("JumpType")
    private String jumpType;

    @SerializedName("Taxi")
    private boolean taxi;

    @SerializedName("StarSystem")
    private String starSystem;

    @SerializedName("SystemAddress")
    private long systemAddress;

    @SerializedName("StarClass")
    private String starClass;

    private final boolean isScoopable;

    private static final Set<String> SCOOPABLE_STARS = new HashSet<>(Set.of("K", "G", "B", "F", "O", "A", "M"));

    public StartJumpEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "StartJump");
        StartJumpEvent event = GsonFactory.getGson().fromJson(json, StartJumpEvent.class);
        this.jumpType = event.jumpType;
        this.taxi = event.taxi;
        this.starSystem = event.starSystem;
        this.systemAddress = event.systemAddress;
        this.starClass = event.starClass;
        this.isScoopable = starClass != null && SCOOPABLE_STARS.contains(starClass.toUpperCase());
    }

    @Override
    public String getEventType() {
        return "StartJump";
    }

    /** The game voices the jump countdown; record only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "The frame shift drive started charging for a jump; JumpType says hyperspace (with destination system and star class) or supercruise.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getJumpType() {
        return jumpType;
    }

    public boolean isTaxi() {
        return taxi;
    }

    public String getStarSystem() {
        return starSystem;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public String getStarClass() {
        return starClass;
    }

    public boolean isScoopable() {
        return isScoopable;
    }
}