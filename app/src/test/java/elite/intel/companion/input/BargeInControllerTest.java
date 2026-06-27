package elite.intel.companion.input;

import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.companion.mind.ThoughtDispatcher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A barge-in fans out a speech interrupt (TTSInterruptEvent) and a thought interrupt (the dispatcher).
 * The dispatcher interrupt is covered by ThoughtDispatcherTest; here an unstarted dispatcher makes that
 * half a safe no-op so the speech-interrupt half is isolated.
 */
class BargeInControllerTest {

    @Test
    void bargeInPublishesSpeechInterrupt() {
        List<Object> published = new ArrayList<>();
        BargeInController controller = new BargeInController(new ThoughtDispatcher(null), published::add);

        controller.onBargeIn(new BargeInEvent());

        assertTrue(published.stream().anyMatch(e -> e instanceof TTSInterruptEvent));
    }
}
