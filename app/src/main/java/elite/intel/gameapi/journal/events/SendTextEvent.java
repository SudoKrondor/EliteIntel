package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * A chat line the commander typed in the game. Its one use is the typed command: a line addressed to VEGA
 * ({@code @Vega ...}) goes in as though it had been spoken, for the words speech recognition keeps getting wrong.
 */
public class SendTextEvent extends BaseEvent {

    @SerializedName("To")
    public String to;

    @SerializedName("Message")
    public String message;

    @SerializedName("Sent")
    public Boolean sent;

    public SendTextEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "SendText");
        SendTextEvent event = GsonFactory.getGson().fromJson(json, SendTextEvent.class);
        this.to = event.to;
        this.message = event.message;
        this.sent = event.sent;
    }

    @Override
    public String getEventType() {
        return "SendText";
    }

    @Override
    public String llmDescription() {
        return "The commander sent a text message in the game chat.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getTo() {
        return to;
    }

    public String getMessage() {
        return message;
    }

    /**
     * True only when the game says the line went out: a missing field is not proof it did.
     */
    public boolean wasSent() {
        return Boolean.TRUE.equals(sent);
    }

    @Override
    public String toString() {
        return String.format("%s: Sent text to %s: %s", timestamp, to, message);
    }
}
