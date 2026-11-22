package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Every ship the commander has in storage, written each time they open a shipyard: the ones at this station
 * ({@code ShipsHere}) and everywhere else ({@code ShipsRemote}, ships in transit among them). The ship being
 * flown is not in either list - its {@code Loadout} reports it.
 */
public class StoredShipsEvent extends BaseEvent {

    public static class StoredShip {
        @SerializedName("ShipID")
        private int shipId;
        @SerializedName("ShipType")
        private String shipType;
        @SerializedName("Name")
        private String name;

        public int getShipId() {
            return shipId;
        }

        /** The journal's internal ship type, e.g. {@code federation_corvette}. */
        public String getShipType() {
            return shipType;
        }

        /** The name the commander gave the ship, or null when it has none. */
        public String getName() {
            return name;
        }
    }

    @SerializedName("ShipsHere")
    private List<StoredShip> shipsHere;
    @SerializedName("ShipsRemote")
    private List<StoredShip> shipsRemote;

    public StoredShipsEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "StoredShips");
        StoredShipsEvent e = GsonFactory.getGson().fromJson(json, StoredShipsEvent.class);
        this.shipsHere = e.shipsHere;
        this.shipsRemote = e.shipsRemote;
    }

    @Override
    public String getEventType() {
        return "StoredShips";
    }

    @Override
    public String llmDescription() {
        return "The list of the commander's stored ships, written on opening a shipyard. Background list.";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    /** Every stored ship, here and remote, without one the journal wrote with no ShipID or type. */
    public List<StoredShip> allShips() {
        List<StoredShip> all = new ArrayList<>();
        if (shipsHere != null) all.addAll(shipsHere);
        if (shipsRemote != null) all.addAll(shipsRemote);
        all.removeIf(ship -> ship == null || ship.shipType == null || ship.shipType.isBlank());
        return all;
    }
}
