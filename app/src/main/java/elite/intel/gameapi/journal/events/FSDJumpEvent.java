package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

public class FSDJumpEvent extends BaseEvent {
    @SerializedName("Taxi")
    private boolean taxi;

    @SerializedName("Multicrew")
    private boolean multicrew;

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
    private long bodyId;

    @SerializedName("BodyType")
    private String bodyType;

    @SerializedName("ControllingPower")
    private String controllingPower;

    @SerializedName("Powers")
    private List<String> powers;

    @SerializedName("PowerplayState")
    private String powerplayState;

    @SerializedName("PowerplayStateControlProgress")
    private double powerplayStateControlProgress;

    @SerializedName("PowerplayStateReinforcement")
    private int powerplayStateReinforcement;

    @SerializedName("PowerplayStateUndermining")
    private int powerplayStateUndermining;

    @SerializedName("JumpDist")
    private double jumpDist;

    @SerializedName("FuelUsed")
    private double fuelUsed;

    @SerializedName("FuelLevel")
    private double fuelLevel;

    @SerializedName("Factions")
    private List<Faction> factions;

    @SerializedName("SystemFaction")
    private SystemFaction systemFaction;

    @SerializedName("Conflicts")
    private List<Conflict> conflicts;

    public FSDJumpEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(60), "FSDJump");
        FSDJumpEvent event = GsonFactory.getGson().fromJson(json, FSDJumpEvent.class);
        this.taxi = event.taxi;
        this.multicrew = event.multicrew;
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
        this.jumpDist = event.jumpDist;
        this.fuelUsed = event.fuelUsed;
        this.fuelLevel = event.fuelLevel;
        this.factions = event.factions;
        this.systemFaction = event.systemFaction;
        this.conflicts = event.conflicts;
    }

    public void setTaxi(boolean taxi) {
        this.taxi = taxi;
    }

    public void setMulticrew(boolean multicrew) {
        this.multicrew = multicrew;
    }

    public void setStarSystem(String starSystem) {
        this.starSystem = starSystem;
    }

    public void setSystemAddress(long systemAddress) {
        this.systemAddress = systemAddress;
    }

    public void setStarPos(double[] starPos) {
        this.starPos = starPos;
    }

    public void setSystemAllegiance(String systemAllegiance) {
        this.systemAllegiance = systemAllegiance;
    }

    public String getSystemEconomy() {
        return systemEconomy;
    }

    public void setSystemEconomy(String systemEconomy) {
        this.systemEconomy = systemEconomy;
    }

    public void setSystemEconomyLocalised(String systemEconomyLocalised) {
        this.systemEconomyLocalised = systemEconomyLocalised;
    }

    public String getSystemSecondEconomy() {
        return systemSecondEconomy;
    }

    public void setSystemSecondEconomy(String systemSecondEconomy) {
        this.systemSecondEconomy = systemSecondEconomy;
    }

    public void setSystemSecondEconomyLocalised(String systemSecondEconomyLocalised) {
        this.systemSecondEconomyLocalised = systemSecondEconomyLocalised;
    }

    public String getSystemGovernment() {
        return systemGovernment;
    }

    public void setSystemGovernment(String systemGovernment) {
        this.systemGovernment = systemGovernment;
    }

    public void setSystemGovernmentLocalised(String systemGovernmentLocalised) {
        this.systemGovernmentLocalised = systemGovernmentLocalised;
    }

    public String getSystemSecurity() {
        return systemSecurity;
    }

    public void setSystemSecurity(String systemSecurity) {
        this.systemSecurity = systemSecurity;
    }

    public void setSystemSecurityLocalised(String systemSecurityLocalised) {
        this.systemSecurityLocalised = systemSecurityLocalised;
    }

    public void setPopulation(long population) {
        this.population = population;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setBodyId(long bodyId) {
        this.bodyId = bodyId;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public void setControllingPower(String controllingPower) {
        this.controllingPower = controllingPower;
    }

    public void setPowers(List<String> powers) {
        this.powers = powers;
    }

    public void setPowerplayState(String powerplayState) {
        this.powerplayState = powerplayState;
    }

    public void setPowerplayStateControlProgress(double powerplayStateControlProgress) {
        this.powerplayStateControlProgress = powerplayStateControlProgress;
    }

    public void setPowerplayStateReinforcement(int powerplayStateReinforcement) {
        this.powerplayStateReinforcement = powerplayStateReinforcement;
    }

    public void setPowerplayStateUndermining(int powerplayStateUndermining) {
        this.powerplayStateUndermining = powerplayStateUndermining;
    }

    public void setJumpDist(double jumpDist) {
        this.jumpDist = jumpDist;
    }

    public void setFuelUsed(double fuelUsed) {
        this.fuelUsed = fuelUsed;
    }

    public void setFuelLevel(double fuelLevel) {
        this.fuelLevel = fuelLevel;
    }

    public void setFactions(List<Faction> factions) {
        this.factions = factions;
    }

    public void setSystemFaction(SystemFaction systemFaction) {
        this.systemFaction = systemFaction;
    }

    public void setConflicts(List<Conflict> conflicts) {
        this.conflicts = conflicts;
    }

    @Override
    public String getEventType() {
        return "FSDJump";
    }

    @Override
    public String llmDescription() {
        return "Arrived in a new star system via a hyperspace jump; carries the destination system name, jump distance in light years, and fuel used.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public boolean isTaxi() {
        return taxi;
    }

    public boolean isMulticrew() {
        return multicrew;
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

    public long getBodyId() {
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

    public double getPowerplayStateControlProgress() {
        return powerplayStateControlProgress;
    }

    public int getPowerplayStateReinforcement() {
        return powerplayStateReinforcement;
    }

    public int getPowerplayStateUndermining() {
        return powerplayStateUndermining;
    }

    public double getJumpDist() {
        return jumpDist;
    }

    public double getFuelUsed() {
        return fuelUsed;
    }

    public double getFuelLevel() {
        return fuelLevel;
    }

    public List<Faction> getFactions() {
        return factions;
    }

    public SystemFaction getSystemFaction() {
        return systemFaction;
    }

    public List<Conflict> getConflicts() {
        return conflicts;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FSDJumpEvent.class.getSimpleName() + "[", "]")
                .add("taxi=" + taxi)
                .add("multicrew=" + multicrew)
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
                .add("jumpDist=" + jumpDist)
                .add("fuelUsed=" + fuelUsed)
                .add("fuelLevel=" + fuelLevel)
                .add("factions=" + factions)
                .add("systemFaction=" + systemFaction)
                .add("conflicts=" + conflicts)
                .toString();
    }

    public static class Faction {
        @SerializedName("Name")
        private String name;

        @SerializedName("FactionState")
        private String factionState;

        @SerializedName("Government")
        private String government;

        @SerializedName("Influence")
        private double influence;

        @SerializedName("Allegiance")
        private String allegiance;

        @SerializedName("Happiness")
        private String happiness;

        @SerializedName("Happiness_Localised")
        private String happinessLocalised;

        @SerializedName("MyReputation")
        private double myReputation;

        @SerializedName("ActiveStates")
        private List<ActiveState> activeStates;

        public void setName(String name) {
            this.name = name;
        }

        public void setFactionState(String factionState) {
            this.factionState = factionState;
        }

        public void setGovernment(String government) {
            this.government = government;
        }

        public void setInfluence(double influence) {
            this.influence = influence;
        }

        public void setAllegiance(String allegiance) {
            this.allegiance = allegiance;
        }

        public String getHappiness() {
            return happiness;
        }

        public void setHappiness(String happiness) {
            this.happiness = happiness;
        }

        public void setHappinessLocalised(String happinessLocalised) {
            this.happinessLocalised = happinessLocalised;
        }

        public void setMyReputation(double myReputation) {
            this.myReputation = myReputation;
        }

        public void setActiveStates(List<ActiveState> activeStates) {
            this.activeStates = activeStates;
        }

        public String getName() {
            return name;
        }

        public String getFactionState() {
            return factionState;
        }

        public String getGovernment() {
            return government;
        }

        public double getInfluence() {
            return influence;
        }

        public String getAllegiance() {
            return allegiance;
        }

        public String getHappinessLocalised() {
            return happinessLocalised;
        }

        public double getMyReputation() {
            return myReputation;
        }

        public List<ActiveState> getActiveStates() {
            return activeStates;
        }
    }

    public static class ActiveState {
        @SerializedName("State")
        private String state;

        public String getState() {
            return state;
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

    public static class Conflict {
        @SerializedName("WarType")
        private String warType;

        @SerializedName("Status")
        private String status;

        @SerializedName("Faction1")
        private FactionSide faction1;

        @SerializedName("Faction2")
        private FactionSide faction2;

        public String getWarType() {
            return warType;
        }

        public String getStatus() {
            return status;
        }

        public FactionSide getFaction1() {
            return faction1;
        }

        public FactionSide getFaction2() {
            return faction2;
        }
    }

    public static class FactionSide {
        @SerializedName("Name")
        private String name;

        @SerializedName("Stake")
        private String stake;

        @SerializedName("WonDays")
        private Integer wonDays;

        public String getName() {
            return name;
        }

        public String getStake() {
            return stake;
        }

        public Integer getWonDays() {
            return wonDays;
        }
    }
}