package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.VegaConfig;
import elite.intel.ai.brain.vega.input.BargeInEvent;
import elite.intel.ai.ears.IsSpeakingEvent;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.gameapi.journal.events.SendTextEvent;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The typed command: a line the commander sends in the game chat that opens with {@code @Vega} (any case, or the
 * name as the app language spells it) is handed to VEGA as though speech recognition had heard it. It is the way
 * round the words recognition keeps getting wrong - "Tritium" typed is "Tritium" received.
 * <p>
 * Only {@code SendText} - the commander's own outgoing line - is read. What other commanders say arrives as
 * {@code ReceiveText}, so nobody else in the channel can give VEGA an order.
 */
@SuppressWarnings("unused")
public class TypedCommandSubscriber {

    private final AtomicBoolean vegaSpeaking = new AtomicBoolean(false);

    @Subscribe
    public void onSendText(SendTextEvent event) {
        if (!event.wasSent()) return;
        String command = commandFor(event.getMessage(), VegaConfig.vegaNameForms());
        if (command == null) return;
        // A typed line is a deliberate order, as a held push-to-talk is: it talks over VEGA rather than
        // waiting for her to finish, exactly as the speech path does.
        GameEventBus.publish(vegaSpeaking.get() ? new BargeInEvent() : new TTSInterruptEvent());
        GameEventBus.publish(new UserInputEvent(command));
    }

    @Subscribe
    public void onIsSpeaking(IsSpeakingEvent event) {
        vegaSpeaking.set(event.isSpeaking());
    }

    /**
     * The order in a chat line addressed to VEGA, or null when the line is not for her: it must open with
     * {@code @} and one of her names as a whole word, and say something after it.
     */
    static String commandFor(String message, List<String> vegaNames) {
        if (message == null) return null;
        String line = message.strip();
        if (!line.startsWith("@")) return null;
        String afterAt = line.substring(1);
        String lowered = afterAt.toLowerCase(Locale.ROOT);
        for (String name : vegaNames) {
            if (name == null || name.isBlank()) continue;
            String form = name.strip().toLowerCase(Locale.ROOT);
            if (!lowered.startsWith(form)) continue;
            String rest = afterAt.substring(form.length());
            // "@Vegas" is somebody else.
            if (!rest.isEmpty() && Character.isLetterOrDigit(rest.codePointAt(0))) continue;
            String command = rest.replaceFirst("^[\\s,.:;!?-]+", "").strip();
            return command.isEmpty() ? null : command;
        }
        return null;
    }
}
