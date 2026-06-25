package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;
import java.util.StringJoiner;

public class EngineerCraftEvent extends BaseEvent {

    @SerializedName("Slot")
    private String slot;

    @SerializedName("Module")
    private String module;

    @SerializedName("Ingredients")
    private List<Ingredient> ingredients;

    @SerializedName("Engineer")
    private String engineer;

    @SerializedName("EngineerID")
    private long engineerId;

    @SerializedName("BlueprintID")
    private long blueprintId;

    @SerializedName("BlueprintName")
    private String blueprintName;

    @SerializedName("Level")
    private int level;

    @SerializedName("Quality")
    private float quality;

    @SerializedName("Modifiers")
    private List<Modifier> modifiers;

    public EngineerCraftEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "EngineerCraft");
        EngineerCraftEvent event = GsonFactory.getGson().fromJson(json, EngineerCraftEvent.class);
        this.slot = event.slot;
        this.module = event.module;
        this.ingredients = event.ingredients;
        this.engineer = event.engineer;
        this.engineerId = event.engineerId;
        this.blueprintId = event.blueprintId;
        this.blueprintName = event.blueprintName;
        this.level = event.level;
        this.quality = event.quality;
        this.modifiers = event.modifiers;
    }

    @Override
    public String getEventType() {
        return "EngineerCraft";
    }

    @Override
    public String llmDescription() {
        return "Applied an engineer modification to a module; carries the engineer, blueprint, level, and resulting effects.";
    }

    // Getters
    public String getSlot() {
        return slot;
    }

    public String getModule() {
        return module;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public String getEngineer() {
        return engineer;
    }

    public long getEngineerId() {
        return engineerId;
    }

    public long getBlueprintId() {
        return blueprintId;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public int getLevel() {
        return level;
    }

    public float getQuality() {
        return quality;
    }

    public List<Modifier> getModifiers() {
        return modifiers;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "EngineerCraft: ", "")
                .add("engineer='" + engineer + "'")
                .add("blueprint='" + blueprintName + "'")
                .add("level=" + level)
                .add("module='" + module + "'")
                .add("slot='" + slot + "'")
                .toString();
    }

    // Nested classes
    public static class Ingredient {
        @SerializedName("Name")
        private String name;

        @SerializedName("Name_Localised")
        private String nameLocalised;

        @SerializedName("Count")
        private int count;

        public String getName() {
            return name;
        }

        public String getNameLocalised() {
            return nameLocalised;
        }

        public int getCount() {
            return count;
        }
    }

    public static class Modifier {
        @SerializedName("Label")
        private String label;

        @SerializedName("Value")
        private float value;

        @SerializedName("OriginalValue")
        private float originalValue;

        @SerializedName("LessIsGood")
        private int lessIsGood;

        public String getLabel() {
            return label;
        }

        public float getValue() {
            return value;
        }

        public float getOriginalValue() {
            return originalValue;
        }

        public boolean isLessIsGood() {
            return lessIsGood == 1;
        }
    }
}