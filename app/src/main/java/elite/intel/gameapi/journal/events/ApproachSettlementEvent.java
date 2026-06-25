package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;

public class ApproachSettlementEvent extends BaseEvent {
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

    @SerializedName("Name")
    private String name;

    @SerializedName("MarketID")
    private long marketID;

    @SerializedName("StationFaction")
    private StationFaction stationFaction;

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
    private List<StationEconomy> stationEconomies;

    @SerializedName("SystemAddress")
    private long systemAddress;

    @SerializedName("BodyID")
    private int bodyID;

    @SerializedName("BodyName")
    private String bodyName;

    @SerializedName("Latitude")
    private double latitude;

    @SerializedName("Longitude")
    private double longitude;

    public ApproachSettlementEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "ApproachSettlement");
        ApproachSettlementEvent event = GsonFactory.getGson().fromJson(json, ApproachSettlementEvent.class);
        this.name = event.name;
        this.marketID = event.marketID;
        this.stationFaction = event.stationFaction;
        this.stationGovernment = event.stationGovernment;
        this.stationGovernmentLocalised = event.stationGovernmentLocalised;
        this.stationAllegiance = event.stationAllegiance;
        this.stationServices = event.stationServices;
        this.stationEconomy = event.stationEconomy;
        this.stationEconomyLocalised = event.stationEconomyLocalised;
        this.stationEconomies = event.stationEconomies;
        this.systemAddress = event.systemAddress;
        this.bodyID = event.bodyID;
        this.bodyName = event.bodyName;
        this.latitude = event.latitude;
        this.longitude = event.longitude;
    }

    @Override
    public String getEventType() {
        return "ApproachSettlement";
    }

    /** Routine approach; memory only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Approached a planetary settlement; carries the settlement name and the body it sits on.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getName() {
        return name;
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

    public List<StationEconomy> getStationEconomies() {
        return stationEconomies;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public int getBodyID() {
        return bodyID;
    }

    public String getBodyName() {
        return bodyName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}