package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * The commander died. Everything not yet sold dies too: the exploration data in the ship and the organic
 * samples the commander was carrying.
 * <p>
 * A crash writes the bare event, nothing but the timestamp. A kill names the killer; a wing kill names
 * several in a {@code Killers} array instead, which is not read because nothing here needs it.
 * {@code Resurrect} follows once the commander has chosen how to come back.
 */
public class DiedEvent extends BaseEvent {

    @SerializedName("KillerName")
    private String killerName;

    public DiedEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "Died");
        DiedEvent event = GsonFactory.getGson().fromJson(json, DiedEvent.class);
        this.killerName = event.killerName;
    }

    @Override
    public String getEventType() {
        return "Died";
    }

    /**
     * Death; the rebuy that follows is what gets announced.
     */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "The commander died; carries who killed them when it was not a crash.";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    /**
     * Who killed the commander, or null after a crash or a wing kill.
     */
    public String getKillerName() {
        return killerName;
    }
}
