package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;

public class LocationEvent extends BaseEvent {
    @SerializedName("DistFromStarLS")
    private double distFromStarLS;

    @SerializedName("Docked")
    private boolean docked;

    @SerializedName("StationName")
    private String stationName;

    @SerializedName("StationType")
    private String stationType;

    @SerializedName("MarketID")
    private long marketID;

    @SerializedName("StationFaction")
    private Faction stationFaction;

    @SerializedName("StationGovernment")
    private String stationGovernment;

    @SerializedName("StationGovernment_Localised")
    private String stationGovernmentLocalised;

    @SerializedName("StationAllegiance")
    private String stationAllegiance;

    @SerializedName("StationServices")
    private List<String> stationServices;

    @SerializedName("StationEconomy")
    private String stationEconomy;

    @SerializedName("StationEconomy_Localised")
    private String stationEconomyLocalised;

    @SerializedName("StationEconomies")
    private List<Economy> stationEconomies;

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
    private Long bodyID;

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

    @SerializedName("Factions")
    private List<Faction> factions;

    @SerializedName("SystemFaction")
    private Faction systemFaction;

    public LocationEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(60), "Location");
        LocationEvent event = GsonFactory.getGson().fromJson(json, LocationEvent.class);
        this.distFromStarLS = event.distFromStarLS;
        this.docked = event.docked;
        this.stationName = event.stationName;
        this.stationType = event.stationType;
        this.marketID = event.marketID;
        this.stationFaction = event.stationFaction;
        this.stationGovernment = event.stationGovernment;
        this.stationGovernmentLocalised = event.stationGovernmentLocalised;
        this.stationAllegiance = event.stationAllegiance;
        this.stationServices = event.stationServices;
        this.stationEconomy = event.stationEconomy;
        this.stationEconomyLocalised = event.stationEconomyLocalised;
        this.stationEconomies = event.stationEconomies;
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
        this.bodyID = event.bodyID;
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
        return "Location";
    }

    /** Load or respawn location snapshot; memory context. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Reports your current location at game load or after respawn; carries the star system, body, and whether you are docked.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public double getDistFromStarLS() {
        return distFromStarLS;
    }

    public boolean isDocked() {
        return docked;
    }

    public String getStationName() {
        return stationName;
    }

    public String getStationType() {
        return stationType;
    }

    public long getMarketID() {
        return marketID;
    }

    public Faction getStationFaction() {
        return stationFaction;
    }

    public String getStationGovernment() {
        return stationGovernment;
    }

    public String getStationGovernmentLocalised() {
        return stationGovernmentLocalised;
    }

    public String getStationAllegiance() {
        return stationAllegiance;
    }

    public List<String> getStationServices() {
        return stationServices;
    }

    public String getStationEconomy() {
        return stationEconomy;
    }

    public String getStationEconomyLocalised() {
        return stationEconomyLocalised;
    }

    public List<Economy> getStationEconomies() {
        return stationEconomies;
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

    public String getSystemEconomy() {
        return systemEconomy;
    }

    public String getSystemEconomyLocalised() {
        return systemEconomyLocalised;
    }

    public String getSystemSecondEconomy() {
        return systemSecondEconomy;
    }

    public String getSystemSecondEconomyLocalised() {
        return systemSecondEconomyLocalised;
    }

    public String getSystemGovernment() {
        return systemGovernment;
    }

    public String getSystemGovernmentLocalised() {
        return systemGovernmentLocalised;
    }

    public String getSystemSecurity() {
        return systemSecurity;
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

    public Long getBodyID() {
        return bodyID;
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

    public List<Faction> getFactions() {
        return factions;
    }

    public Faction getSystemFaction() {
        return systemFaction;
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
        private List<State> activeStates;

        @SerializedName("RecoveringStates")
        private List<State> recoveringStates;

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

        public String getHappiness() {
            return happiness;
        }

        public String getHappinessLocalised() {
            return happinessLocalised;
        }

        public double getMyReputation() {
            return myReputation;
        }

        public List<State> getActiveStates() {
            return activeStates;
        }

        public List<State> getRecoveringStates() {
            return recoveringStates;
        }
    }

    public static class State {
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

    public static class Economy {
        @SerializedName("Name")
        private String name;

        @SerializedName("Name_Localised")
        private String nameLocalised;

        @SerializedName("Proportion")
        private double proportion;

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