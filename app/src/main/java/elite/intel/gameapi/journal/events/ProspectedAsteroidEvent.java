package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.session.PlayerSession;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.Set;

import static elite.intel.util.StringUtls.capitalizeWords;

public class ProspectedAsteroidEvent extends BaseEvent {
    @SerializedName("Materials")
    public Material[] materials;

    /**
     * Present only on a core (motherlode) asteroid, and the only field that identifies one: the raw
     * FDev commodity symbol of what sits in the core, e.g. {@code Platinum}, {@code Monazite},
     * {@code LowTemperatureDiamond}. Never localized by the game, and absent - not empty - on an
     * ordinary rock. {@code Content} is not a substitute: cores turn up at every content level.
     */
    @SerializedName("MotherlodeMaterial")
    public String motherlodeMaterial;

    @SerializedName("Content")
    public String content;

    @SerializedName("Content_Localised")
    public String contentLocalised;

    @SerializedName("Remaining")
    public double remaining;

    public ProspectedAsteroidEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofMinutes(10), "ProspectedAsteroid");
        ProspectedAsteroidEvent event = GsonFactory.getGson().fromJson(json, ProspectedAsteroidEvent.class);
        this.materials = event.materials;
        this.motherlodeMaterial = event.motherlodeMaterial;
        this.content = event.content;
        this.contentLocalised = event.contentLocalised;
        this.remaining = event.remaining;
    }

    @Override
    public String getEventType() {
        return "ProspectedAsteroid";
    }

    /**
     * Payload-dependent. Mirrors ProspectorSubscriber: an asteroid is only worth a word when it
     * holds one of the commander's tracked mining targets. We prospect and discard a great many
     * rocks, so without this filter the companion would comment on every prospector hit.
     * <p>
     * A core asteroid always clears the bar, whatever it holds - it is rare enough, and short-lived
     * enough, that the commander wants to hear about it before deciding whether the contents matter.
     */
    @Override
    public Importance importance() {
        if (isCore()) return Importance.NORMAL;
        if (materials == null) return Importance.LOW;
        Set<String> miningTargets = PlayerSession.getInstance().getMiningTargets();
        for (Material material : materials) {
            if (material == null || material.getName() == null || material.getName().isEmpty()) continue;
            if (miningTargets.contains(capitalizeWords(material.getName()))) return Importance.NORMAL;
        }
        return Importance.LOW;
    }

    @Override
    public String llmDescription() {
        return "A prospector limpet analysed an asteroid; carries the materials present with their proportions, the overall content level, and - on a core (motherlode) asteroid only - the material sealed in the core.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public Material[] getMaterials() {
        return materials;
    }

    /**
     * Whether this is a core (motherlode) asteroid - one that has to be cracked with seismic charges.
     */
    public boolean isCore() {
        return motherlodeMaterial != null && !motherlodeMaterial.isBlank();
    }

    public String getMotherlodeMaterial() {
        return motherlodeMaterial;
    }

    public String getContent() {
        return content;
    }

    public String getContentLocalised() {
        return contentLocalised;
    }

    public double getRemaining() {
        return remaining;
    }

    public String getMaterialSummary() {
        StringBuilder materialSummary = new StringBuilder();
        materialSummary.append("Prospector identified: ");
        if (materials != null) {
            for (Material m : materials) {
                materialSummary.append(String.format("%.2f%%", m.proportion)).append(" percent ").append(m.name).append(", ");
            }
        }
        return materialSummary.toString();
    }

    @Override
    public String toString() {
        String core = isCore() ? String.format(" Motherlode core: %s.", motherlodeMaterial) : "";
        return String.format("%s: Prospected asteroid (%s) with materials: %s%s", timestamp, contentLocalised, getMaterialSummary(), core);
    }

    public static class Material {
        @SerializedName("Name")
        public String name;

        @SerializedName("Name_Localised")
        public String nameLocalised;

        @SerializedName("Proportion")
        public double proportion;

        public String getName() {
            return name;
        }

        public String getNameLocalised() {
            return nameLocalised;
        }

        public double getProportion() {
            return proportion;
        }
    }
}