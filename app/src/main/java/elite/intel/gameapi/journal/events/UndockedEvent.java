package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * The ship has left a pad. Carries the MarketID of the port it left, which is what makes it the counterpart
 * of {@code Docked} for anything tracking where the ship is standing.
 */
public class UndockedEvent extends BaseEvent {

    @SerializedName("StationName")
    private String stationName;

    @SerializedName("StationType")
    private String stationType;

    @SerializedName("MarketID")
    private long marketID;

    @SerializedName("Taxi")
    private boolean taxi;

    @SerializedName("Multicrew")
    private boolean multicrew;

    public UndockedEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "Undocked");
        UndockedEvent e = GsonFactory.getGson().fromJson(json, UndockedEvent.class);
        this.stationName = e.stationName;
        this.stationType = e.stationType;
        this.marketID = e.marketID;
        this.taxi = e.taxi;
        this.multicrew = e.multicrew;
    }

    @Override
    public String getEventType() {
        return "Undocked";
    }

    /**
     * Leaving a pad is ordinary flying; context, not news.
     */
    @Override
    public Importance importance() {
        return Importance.LOW;
    }

    @Override
    public String llmDescription() {
        return "Undocked from a station or fleet carrier; carries the port's name, type and market id.";
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

    public long getMarketID() {
        return marketID;
    }

    public boolean isTaxi() {
        return taxi;
    }

    public boolean isMulticrew() {
        return multicrew;
    }
}
