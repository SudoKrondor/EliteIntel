package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.db.managers.CargoHoldManager;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.Set;

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

    /**
     * Known pirate interdiction hails, stored lower-cased for case-insensitive matching. Owned here
     * (not in the subscriber) so both the audible alert and {@link #importance()} share one source.
     */
    private static final Set<String> PIRATE_HAILS = Set.of(
            "big haul like that, surprised you made it this far",
            "carrying anything nice?",
            "do you have anything of value?",
            "i hope you have something good in your hold.",
            "i'll pick your bones clean, greenhorn.",
            "i see all!",
            "i've found my next target and it's you, commander.",
            "let's see what you are carrying.",
            "let me see what you have.",
            "the scan will soon be over.",
            "what are you hauling?",
            "what do you carry, i wonder?",
            "what treats do you carry?",
            "what do you have in your cargo hold?",
            "next time you should fill your hold with gold.",
            "i'm gonna boil you up!"
    );

    /** Whether this transmission is a known pirate interdiction hail. */
    public boolean isPirateMessage() {
        return messageLocalised != null && PIRATE_HAILS.contains(messageLocalised.toLowerCase());
    }

    /**
     * Payload-dependent. Almost all text is high-frequency NPC chatter the companion ignores. The one
     * exception worth speaking is a pirate hail while we are actually carrying cargo - a real threat.
     */
    @Override
    public Importance importance() {
        if (!isPirateMessage()) return Importance.LOW;
        CargoHoldManager cargo = CargoHoldManager.getInstance();
        boolean haveCargo = cargo.get() != null && cargo.get().getCount() > 0;
        return haveCargo ? Importance.HIGH : Importance.LOW;
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