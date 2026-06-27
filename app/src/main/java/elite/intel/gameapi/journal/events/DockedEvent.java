package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;

public class DockedEvent extends BaseEvent {
    public static class StationFaction {
        @SerializedName("Name")
        private String name;

        public String getName() {
            return name;
        }
    }

    public static class StationEconomy {
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

    public static class LandingPads {
        @SerializedName("Small")
        private int small;

        @SerializedName("Medium")
        private int medium;

        @SerializedName("Large")
        private int large;

        public int getSmall() {
            return small;
        }

        public int getMedium() {
            return medium;
        }

        public int getLarge() {
            return large;
        }
    }

    @SerializedName("StationName")
    private String stationName;

    @SerializedName("StationType")
    private String stationType;

    @SerializedName("Taxi")
    private boolean taxi;

    @SerializedName("Multicrew")
    private boolean multicrew;

    @SerializedName("StarSystem")
    private String starSystem;

    @SerializedName("SystemAddress")
    private long systemAddress;

    @SerializedName("MarketID")
    private long marketID;

    @SerializedName("StationFaction")
    private StationFaction stationFaction;

    @SerializedName("StationGovernment")
    private String stationGovernment;

    @SerializedName("StationGovernment_Localised")
    private String stationGovernmentLocalised;

    @SerializedName("StationServices")
    private List<String> stationServices;

    @SerializedName("StationEconomy")
    private String stationEconomy;

    @SerializedName("StationEconomy_Localised")
    private String stationEconomyLocalised;

    @SerializedName("StationEconomies")
    private List<StationEconomy> stationEconomies;

    @SerializedName("DistFromStarLS")
    private double distFromStarLS;

    @SerializedName("LandingPads")
    private LandingPads landingPads;

    public DockedEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "Docked");
        DockedEvent event = GsonFactory.getGson().fromJson(json, DockedEvent.class);
        this.stationName = event.stationName;
        this.stationType = event.stationType;
        this.taxi = event.taxi;
        this.multicrew = event.multicrew;
        this.starSystem = event.starSystem;
        this.systemAddress = event.systemAddress;
        this.marketID = event.marketID;
        this.stationFaction = event.stationFaction;
        this.stationGovernment = event.stationGovernment;
        this.stationGovernmentLocalised = event.stationGovernmentLocalised;
        this.stationServices = event.stationServices;
        this.stationEconomy = event.stationEconomy;
        this.stationEconomyLocalised = event.stationEconomyLocalised;
        this.stationEconomies = event.stationEconomies;
        this.distFromStarLS = event.distFromStarLS;
        this.landingPads = event.landingPads;
    }

    @Override
    public String getEventType() {
        return "Docked";
    }

    /** Routine arrival; FSDJump carries the system-change beat. Memory only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Docked at a station, outpost, or surface settlement; carries the station name, its type, and the services it offers.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getStationName() {
        return stationName;
    }

    public String getStationType() {
        return stationType;
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

    public long getMarketID() {
        return marketID;
    }

    public StationFaction getStationFaction() {
        return stationFaction;
    }

    public String getStationGovernment() {
        return stationGovernment;
    }

    public String getStationGovernmentLocalised() {
        return stationGovernmentLocalised;
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

    public List<StationEconomy> getStationEconomies() {
        return stationEconomies;
    }

    public double getDistFromStarLS() {
        return distFromStarLS;
    }

    public LandingPads getLandingPads() {
        return landingPads;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}