package elite.intel.gameapi.journal.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.StationName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.StringJoiner;

public class SupercruiseDestinationDropEvent extends BaseEvent {
    @SerializedName("Type")
    private String type;

    /**
     * The destination as the game names it, before anything is done to make it readable.
     * <p>
     * Kept because some destinations are not places but scenarios, and the symbol is the only
     * language-independent way to tell which: {@code $MULTIPLAYER_SCENARIO79_TITLE;} is a hazardous
     * resource extraction site whatever language the game client runs in, and the resource-site
     * grades are read from exactly these strings.
     * <p>
     * {@code transient} keeps it out of the JSON the event serialises to and {@code @JsonIgnore} out
     * of the narration payload, because an identifier in that payload can come back out of the
     * speaker - see the mission-key fields on {@code MissionAcceptedEvent}.
     */
    private transient String typeSymbol;

    @SerializedName("Threat")
    private int threat;

    @SerializedName("MarketID")
    private long marketID;

    public SupercruiseDestinationDropEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "SupercruiseDestinationDrop");
        SupercruiseDestinationDropEvent event = GsonFactory.getGson().fromJson(json, SupercruiseDestinationDropEvent.class);
        this.typeSymbol = event.type;
        this.type = readableType(event.type, text(json, "Type_Localised"));
        this.threat = event.threat;
        this.marketID = event.marketID;
    }

    /**
     * The destination in words.
     * <p>
     * WHY the localised sibling is preferred: {@link StationName#display} peels a readable name off a
     * decorated one, and falls back to spelling the symbol out when there is nothing after the
     * semicolon. That fallback is right for a station panel and wrong for a scenario, whose whole
     * name IS the symbol - it turned a hazardous resource site into "MULTIPLAYER SCENARIO79 TITLE"
     * and put that in front of the LLM. For these the game does send {@code Type_Localised}, so the
     * spelled-out symbol is now the last resort rather than the first answer.
     */
    private static String readableType(String rawType, String localised) {
        if (localised != null && !localised.isBlank()) return localised;
        return StationName.display(rawType);
    }

    private static String text(JsonObject json, String field) {
        return json.has(field) && !json.get(field).isJsonNull() ? json.get(field).getAsString() : null;
    }

    @Override
    public String getEventType() {
        return "SupercruiseDestinationDrop";
    }

    /** Routine supercruise drop; memory only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Auto-dropped from supercruise at the selected destination; carries the destination name and the local threat level.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getType() {
        return type;
    }

    /**
     * The destination's raw journal symbol, for callers that have to recognise a scenario rather than
     * read a name to the commander. Null for an ordinary named destination.
     */
    @JsonIgnore
    public String getTypeSymbol() {
        return typeSymbol;
    }

    public int getThreat() {
        return threat;
    }

    public long getMarketID() {
        return marketID;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    @Override
    public String toString() {
        return new StringJoiner("Supercruise destination drop: ")
                .add("type='" + type + "'")
                .add("threat=" + threat)
                .add("marketID=" + marketID)
                .toString();
    }
}