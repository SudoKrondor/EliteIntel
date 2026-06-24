package elite.intel.companion.input;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.eventbus.GameEventBus;

import java.util.function.Consumer;

/**
 * Barge-in node (§2.15), separate from the SpeechGateway. On a {@link BargeInEvent} it sends a split
 * signal to two independent addressees - a speech interrupt ({@code TTSInterruptEvent} stops/clears the
 * current TTS) and a thought interrupt (the dispatcher interrupts its live thoughts) - without deciding
 * either one's lifecycle itself. It is not a gameplay path and does not bypass tool access, confirmation
 * or execution.
 */
public final class BargeInController {

    private final ThoughtDispatcher dispatcher;
    private final Consumer<Object> publisher;

    /** Production: publishes the speech interrupt on the shared {@link GameEventBus}. */
    public BargeInController(ThoughtDispatcher dispatcher) {
        this(dispatcher, GameEventBus::publish);
    }

    /** Test seam: inject a capturing publisher to avoid the real event bus. */
    BargeInController(ThoughtDispatcher dispatcher, Consumer<Object> publisher) {
        this.dispatcher = dispatcher;
        this.publisher = publisher;
    }

    /** Splits the barge-in into a speech interrupt and a (separate) thought interrupt. */
    @Subscribe
    public void onBargeIn(BargeInEvent event) {
        publisher.accept(new TTSInterruptEvent());
        dispatcher.interruptLiveThoughts();
    }
}
