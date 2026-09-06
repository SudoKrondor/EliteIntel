package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.StringJoiner;

/**
 * The first line of every journal file, written when the game starts.
 * <p>
 * It is the earliest and the only guaranteed statement of which edition of the game is running: {@code Odyssey}
 * is true on an Odyssey client and false on Horizons (including the Legacy 3.8 client), while {@code Horizons}
 * - which this header does not even carry - means only "owns Horizons content" and is true under Odyssey too.
 * So the Odyssey flag is the single discriminator, and this event exists to carry it to
 * {@code GameEditionCheck}. Everything else here is session identity the header happens to state.
 * <p>
 * Registered as non-timed in {@link elite.intel.gameapi.journal.EventRegistry}: the game writes this line at
 * launch and the commander may sit in the launcher for minutes before anything else is logged, so the ten
 * second recency rule would throw away the one line that identifies the edition.
 */
public class FileheaderEvent extends BaseEvent {

    @SerializedName("part")
    private int part;

    @SerializedName("language")
    private String language;

    @SerializedName("Odyssey")
    private boolean odyssey;

    @SerializedName("gameversion")
    private String gameversion;

    @SerializedName("build")
    private String build;

    public FileheaderEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofDays(30), "Fileheader");
        FileheaderEvent event = GsonFactory.getGson().fromJson(json, FileheaderEvent.class);
        this.part = event.part;
        this.language = event.language;
        this.odyssey = event.odyssey;
        this.gameversion = event.gameversion;
        this.build = event.build;
    }

    @Override
    public String getEventType() {
        return "Fileheader";
    }

    @Override
    public String llmDescription() {
        return "A new game session started and began a new journal file.";
    }

    public int getPart() {
        return part;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isOdyssey() {
        return odyssey;
    }

    public String getGameversion() {
        return gameversion;
    }

    public String getBuild() {
        return build;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "Fileheader[", "]")
                .add("odyssey=" + odyssey)
                .add("gameversion='" + gameversion + "'")
                .add("build='" + build + "'")
                .toString();
    }
}
