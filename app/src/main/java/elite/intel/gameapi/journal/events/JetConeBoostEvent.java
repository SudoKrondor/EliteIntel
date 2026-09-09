package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * The ship charged its FSD in a neutron star's jet cone.
 * <p>
 * The line is the only signal that a commander flying a neutron highway has reached the part of the
 * waypoint they came for, and it is written the moment the charge lands - which is why
 * {@link elite.intel.gameapi.journal.subscribers.JetConeBoostSubscriber} can hang the automatic plot of
 * the next waypoint off it. The 30 second time to live keeps a boost the app read late from opening the
 * galaxy map long after the supercharge it belonged to was spent.
 */
public class JetConeBoostEvent extends BaseEvent {

    @SerializedName("BoostValue")
    private double boostValue;

    public JetConeBoostEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "JetConeBoost");
        JetConeBoostEvent event = GsonFactory.getGson().fromJson(json, JetConeBoostEvent.class);
        this.boostValue = event.boostValue;
    }

    @Override
    public String getEventType() {
        return "JetConeBoost";
    }

    /**
     * Its voice is owned by the auto-plot callout, so the consciousness keeps it in memory only.
     */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Supercharged the frame shift drive in a neutron star jet cone; carries the boost multiplier.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    /**
     * How many times the ship's normal jump range this charge is worth - 6 for a neutron star.
     */
    public double getBoostValue() {
        return boostValue;
    }
}
