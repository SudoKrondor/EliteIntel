package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class ReceiveTextEvent extends BaseEvent {
    @SerializedName("From")
    public String from;

    @SerializedName("From_Localised")
    public String fromLocalised;

    @SerializedName("Message")
    public String message;

    @SerializedName("Message_Localised")
    public String messageLocalised;

    @SerializedName("Channel")
    public String channel;

    public ReceiveTextEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofMinutes(1), "ReceiveText");
        ReceiveTextEvent event = GsonFactory.getGson().fromJson(json, ReceiveTextEvent.class);
        this.from = event.from;
        this.fromLocalised = event.fromLocalised;
        this.message = event.message;
        this.messageLocalised = event.messageLocalised;
        this.channel = event.channel;
    }

    @Override
    public String getEventType() {
        return "ReceiveText";
    }

    @Override
    public String llmDescription() {
        return "Received a text message; carries the sender, the channel (npc, local, wing, squadron, direct), and the message. Very high frequency, mostly NPC chatter.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getFrom() {
        return from;
    }

    public String getFromLocalised() {
        return fromLocalised;
    }

    public String getMessage() {
        return message;
    }

    public String getMessageLocalised() {
        return messageLocalised;
    }

    public String getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return String.format("%s: Received text on channel %s: %s", timestamp, channel, messageLocalised);
    }
}