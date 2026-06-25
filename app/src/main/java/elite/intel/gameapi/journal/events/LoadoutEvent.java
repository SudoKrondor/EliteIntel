package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;

public class LoadoutEvent extends BaseEvent {
    @SerializedName("Ship")
    private String ship;

    @SerializedName("ShipID")
    private int shipId;

    @SerializedName("ShipName")
    private String shipName;

    @SerializedName("ShipIdent")
    private String shipIdent;

    @SerializedName("ModulesValue")
    private long modulesValue;

    @SerializedName("HullHealth")
    private float hullHealth;

    @SerializedName("UnladenMass")
    private float unladenMass;

    @SerializedName("CargoCapacity")
    private int cargoCapacity;

    @SerializedName("MaxJumpRange")
    private float maxJumpRange;

    @SerializedName("FuelCapacity")
    private FuelCapacity fuelCapacity;

    @SerializedName("Rebuy")
    private long rebuy;

    @SerializedName("Modules")
    private List<Module> modules;

    public LoadoutEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "Loadout");
        LoadoutEvent event = GsonFactory.getGson().fromJson(json, LoadoutEvent.class);
        this.ship = event.ship;
        this.shipId = event.shipId;
        this.shipName = event.shipName;
        this.shipIdent = event.shipIdent;
        this.modulesValue = event.modulesValue;
        this.hullHealth = event.hullHealth;
        this.unladenMass = event.unladenMass;
        this.cargoCapacity = event.cargoCapacity;
        this.maxJumpRange = event.maxJumpRange;
        this.fuelCapacity = event.fuelCapacity;
        this.rebuy = event.rebuy;
        this.modules = event.modules;
    }

    @Override
    public String getEventType() {
        return "Loadout";
    }

    @Override
    public String llmDescription() {
        return "A snapshot of the current ship's full loadout; written on boarding or any module change. Background detail.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getShip() {
        return ship;
    }

    public int getShipId() {
        return shipId;
    }

    public String getShipName() {
        return shipName;
    }

    public String getShipIdent() {
        return shipIdent;
    }

    public long getModulesValue() {
        return modulesValue;
    }

    public float getHullHealth() {
        return hullHealth;
    }

    public float getUnladenMass() {
        return unladenMass;
    }

    public int getCargoCapacity() {
        return cargoCapacity;
    }

    public float getMaxJumpRange() {
        return maxJumpRange;
    }

    public FuelCapacity getFuelCapacity() {
        return fuelCapacity;
    }

    public long getRebuy() {
        return rebuy;
    }

    public List<Module> getModules() {
        return modules;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    public static class FuelCapacity {
        @SerializedName("Main")
        private double main;

        @SerializedName("Reserve")
        private double reserve;

        public double getMain() {
            return main;
        }

        public double getReserve() {
            return reserve;
        }
    }

    public static class Module {
        @SerializedName("Slot")
        private String slot;

        @SerializedName("Item")
        private String item;

        @SerializedName("On")
        private boolean on;

        @SerializedName("Priority")
        private int priority;

        @SerializedName("Health")
        private float health;

        @SerializedName("Value")
        private Long value;

        @SerializedName("AmmoInClip")
        private Integer ammoInClip;

        @SerializedName("AmmoInHopper")
        private Integer ammoInHopper;

        @SerializedName("Engineering")
        private Engineering engineering;

        public String getSlot() {
            return slot;
        }

        public String getItem() {
            return item;
        }

        public boolean isOn() {
            return on;
        }

        public int getPriority() {
            return priority;
        }

        public float getHealth() {
            return health;
        }

        public Long getValue() {
            return value;
        }

        public Integer getAmmoInClip() {
            return ammoInClip;
        }

        public Integer getAmmoInHopper() {
            return ammoInHopper;
        }

        public Engineering getEngineering() {
            return engineering;
        }
    }

    public static class Engineering {
        @SerializedName("Engineer")
        private String engineer;

        @SerializedName("EngineerID")
        private Integer engineerId;

        @SerializedName("BlueprintID")
        private Integer blueprintId;

        @SerializedName("BlueprintName")
        private String blueprintName;

        @SerializedName("Level")
        private Integer level;

        @SerializedName("Quality")
        private Float quality;

        @SerializedName("ExperimentalEffect")
        private String experimentalEffect;

        @SerializedName("ExperimentalEffect_Localised")
        private String experimentalEffectLocalised;

        @SerializedName("Modifiers")
        private List<Modifier> modifiers;

        public String getEngineer() {
            return engineer;
        }

        public Integer getEngineerId() {
            return engineerId;
        }

        public Integer getBlueprintId() {
            return blueprintId;
        }

        public String getBlueprintName() {
            return blueprintName;
        }

        public Integer getLevel() {
            return level;
        }

        public Float getQuality() {
            return quality;
        }

        public String getExperimentalEffect() {
            return experimentalEffect;
        }

        public String getExperimentalEffectLocalised() {
            return experimentalEffectLocalised;
        }

        public List<Modifier> getModifiers() {
            return modifiers;
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

        public int getLessIsGood() {
            return lessIsGood;
        }
    }
}