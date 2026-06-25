package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

public class CarrierJumpEvent extends BaseEvent {
    @SerializedName("Docked")
    private boolean docked;

    @SerializedName("OnFoot")
    private boolean onFoot;

    @SerializedName("StarSystem")
    private String starSystem;

    @SerializedName("SystemAddress")
    private long systemAddress;

    @SerializedName("StarPos")
    private double[] starPos;

    @SerializedName("SystemAllegiance")
    private String systemAllegiance;

    @SerializedName("SystemEconomy")
    private String systemEconomy;

    @SerializedName("SystemEconomy_Localised")
    private String systemEconomyLocalised;

    @SerializedName("SystemSecondEconomy")
    private String systemSecondEconomy;

    @SerializedName("SystemSecondEconomy_Localised")
    private String systemSecondEconomyLocalised;

    @SerializedName("SystemGovernment")
    private String systemGovernment;

    @SerializedName("SystemGovernment_Localised")
    private String systemGovernmentLocalised;

    @SerializedName("SystemSecurity")
    private String systemSecurity;

    @SerializedName("SystemSecurity_Localised")
    private String systemSecurityLocalised;

    @SerializedName("Population")
    private long population;

    @SerializedName("Body")
    private String body;

    @SerializedName("BodyID")
    private Long bodyId;

    @SerializedName("BodyType")
    private String bodyType;

    @SerializedName("ControllingPower")
    private String controllingPower;

    @SerializedName("Powers")
    private List<String> powers;

    @SerializedName("PowerplayState")
    private String powerplayState;

    @SerializedName("PowerplayStateControlProgress")
    private float powerplayStateControlProgress;

    @SerializedName("PowerplayStateReinforcement")
    private int powerplayStateReinforcement;

    @SerializedName("PowerplayStateUndermining")
    private int powerplayStateUndermining;

    @SerializedName("Factions")
    private List<Faction> factions;

    @SerializedName("SystemFaction")
    private SystemFaction systemFaction;

    public CarrierJumpEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofDays(30), "CarrierJump");
        CarrierJumpEvent event = GsonFactory.getGson().fromJson(json, CarrierJumpEvent.class);
        this.docked = event.docked;
        this.onFoot = event.onFoot;
        this.starSystem = event.starSystem;
        this.systemAddress = event.systemAddress;
        this.starPos = event.starPos;
        this.systemAllegiance = event.systemAllegiance;
        this.systemEconomy = event.systemEconomy;
        this.systemEconomyLocalised = event.systemEconomyLocalised;
        this.systemSecondEconomy = event.systemSecondEconomy;
        this.systemSecondEconomyLocalised = event.systemSecondEconomyLocalised;
        this.systemGovernment = event.systemGovernment;
        this.systemGovernmentLocalised = event.systemGovernmentLocalised;
        this.systemSecurity = event.systemSecurity;
        this.systemSecurityLocalised = event.systemSecurityLocalised;
        this.population = event.population;
        this.body = event.body;
        this.bodyId = event.bodyId;
        this.bodyType = event.bodyType;
        this.controllingPower = event.controllingPower;
        this.powers = event.powers;
        this.powerplayState = event.powerplayState;
        this.powerplayStateControlProgress = event.powerplayStateControlProgress;
        this.powerplayStateReinforcement = event.powerplayStateReinforcement;
        this.powerplayStateUndermining = event.powerplayStateUndermining;
        this.factions = event.factions;
        this.systemFaction = event.systemFaction;
    }

    @Override
    public String getEventType() {
        return "CarrierJump";
    }

    /**
     * NORMAL: CarrierJumpCompleteSubscriber already owns the spoken "carrier jump complete" callout
     * via EventNarrator, which now narrates in every mode. Kept in memory but off the consciousness's
     * spoken channel so the arrival is not announced twice.
     */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Your fleet carrier completed a jump while you were aboard; carries the destination system and body.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public boolean isDocked() {
        return docked;
    }

    public boolean isOnFoot() {
        return onFoot;
    }

    public String getStarSystem() {
        return starSystem;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public double[] getStarPos() {
        return starPos;
    }

    public String getSystemAllegiance() {
        return systemAllegiance;
    }

    public String getSystemEconomyLocalised() {
        return systemEconomyLocalised;
    }

    public String getSystemSecondEconomyLocalised() {
        return systemSecondEconomyLocalised;
    }

    public String getSystemGovernmentLocalised() {
        return systemGovernmentLocalised;
    }

    public String getSystemSecurityLocalised() {
        return systemSecurityLocalised;
    }

    public long getPopulation() {
        return population;
    }

    public String getBody() {
        return body;
    }

    public Long getBodyId() {
        return bodyId;
    }

    public String getBodyType() {
        return bodyType;
    }

    public String getControllingPower() {
        return controllingPower;
    }

    public List<String> getPowers() {
        return powers;
    }

    public String getPowerplayState() {
        return powerplayState;
    }

    public float getPowerplayStateControlProgress() {
        return powerplayStateControlProgress;
    }

    public int getPowerplayStateReinforcement() {
        return powerplayStateReinforcement;
    }

    public int getPowerplayStateUndermining() {
        return powerplayStateUndermining;
    }

    public List<Faction> getFactions() {
        return factions;
    }

    public SystemFaction getSystemFaction() {
        return systemFaction;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    public static class Faction {
        @SerializedName("Name")
        private String name;

        @SerializedName("FactionState")
        private String factionState;

        @SerializedName("Government")
        private String government;

        @SerializedName("Influence")
        private float influence;

        @SerializedName("Allegiance")
        private String allegiance;

        @SerializedName("Happiness")
        private String happiness;

        @SerializedName("Happiness_Localised")
        private String happinessLocalised;

        @SerializedName("MyReputation")
        private float myReputation;

        @SerializedName("ActiveStates")
        private List<ActiveState> activeStates;

        @SerializedName("PendingStates")
        private List<PendingState> pendingStates;

        @SerializedName("RecoveringStates")
        private List<RecoveringState> recoveringStates;

        public String getName() {
            return name;
        }

        public String getFactionState() {
            return factionState;
        }

        public String getGovernment() {
            return government;
        }

        public float getInfluence() {
            return influence;
        }

        public String getAllegiance() {
            return allegiance;
        }

        public String getHappinessLocalised() {
            return happinessLocalised;
        }

        public float getMyReputation() {
            return myReputation;
        }

        public List<ActiveState> getActiveStates() {
            return activeStates;
        }

        public List<PendingState> getPendingStates() {
            return pendingStates;
        }

        public List<RecoveringState> getRecoveringStates() {
            return recoveringStates;
        }
    }

    public static class ActiveState {
        @SerializedName("State")
        private String state;

        public String getState() {
            return state;
        }
    }

    public static class PendingState {
        @SerializedName("State")
        private String state;

        @SerializedName("Trend")
        private int trend;

        public String getState() {
            return state;
        }

        public int getTrend() {
            return trend;
        }
    }

    public static class RecoveringState {
        @SerializedName("State")
        private String state;

        @SerializedName("Trend")
        private int trend;

        public String getState() {
            return state;
        }

        public int getTrend() {
            return trend;
        }
    }

    public static class SystemFaction {
        @SerializedName("Name")
        private String name;

        @SerializedName("FactionState")
        private String factionState;

        public String getName() {
            return name;
        }

        public String getFactionState() {
            return factionState;
        }
    }

    @Override
    public String toString() {
        return new StringJoiner("")
                .add("docked=" + docked)
                .add("onFoot=" + onFoot)
                .add("starSystem='" + starSystem + "'")
                .add("systemAddress=" + systemAddress)
                .add("starPos=" + Arrays.toString(starPos))
                .add("systemAllegiance='" + systemAllegiance + "'")
                .add("systemEconomy='" + systemEconomy + "'")
                .add("systemEconomyLocalised='" + systemEconomyLocalised + "'")
                .add("systemSecondEconomy='" + systemSecondEconomy + "'")
                .add("systemSecondEconomyLocalised='" + systemSecondEconomyLocalised + "'")
                .add("systemGovernment='" + systemGovernment + "'")
                .add("systemGovernmentLocalised='" + systemGovernmentLocalised + "'")
                .add("systemSecurity='" + systemSecurity + "'")
                .add("systemSecurityLocalised='" + systemSecurityLocalised + "'")
                .add("population=" + population)
                .add("body='" + body + "'")
                .add("bodyId=" + bodyId)
                .add("bodyType='" + bodyType + "'")
                .add("controllingPower='" + controllingPower + "'")
                .add("powers=" + powers)
                .add("powerplayState='" + powerplayState + "'")
                .add("powerplayStateControlProgress=" + powerplayStateControlProgress)
                .add("powerplayStateReinforcement=" + powerplayStateReinforcement)
                .add("powerplayStateUndermining=" + powerplayStateUndermining)
                .add("factions=" + factions)
                .add("systemFaction=" + systemFaction)
                .toString();
    }
}