package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * The Nomad leaving the ship. Since the 2026-09 game patch the Nomad's deploy is written as {@code LaunchVessel}
 * (with {@code "VesselType":"lander01"}) rather than {@code LaunchSRV} - while its recovery is still a
 * {@code DockSRV}. Before that patch it was not journalled at all.
 */
public class LaunchVesselEvent extends BaseEvent {
    @SerializedName("VesselType")
    private String vesselType;

    @SerializedName("VesselType_Localised")
    private String vesselTypeLocalised;

    @SerializedName("Loadout")
    private String loadout;

    @SerializedName("ID")
    private int id;

    @SerializedName("PlayerControlled")
    private boolean playerControlled;

    public LaunchVesselEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "LaunchVessel");
        LaunchVesselEvent event = GsonFactory.getGson().fromJson(json, LaunchVesselEvent.class);
        this.vesselType = event.vesselType;
        this.vesselTypeLocalised = event.vesselTypeLocalised;
        this.loadout = event.loadout;
        this.id = event.id;
        this.playerControlled = event.playerControlled;
    }

    @Override
    public String getEventType() {
        return "LaunchVessel";
    }

    /**
     * Routine vehicle deploy; memory only.
     */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Deployed a vehicle such as the Nomad from the ship.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getVesselType() {
        return vesselType;
    }

    public String getVesselTypeLocalised() {
        return vesselTypeLocalised;
    }

    public String getLoadout() {
        return loadout;
    }

    public int getId() {
        return id;
    }

    public boolean isPlayerControlled() {
        return playerControlled;
    }
}
