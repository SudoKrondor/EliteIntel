package elite.intel.ai.mouth;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.ears.IsSpeakingEvent;
import elite.intel.eventbus.GameEventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the Mouth-owned, process-wide speaking-state and exactly-once completion contract. */
class VocalisationHandleTest {

    private final SpeakingRecorder recorder = new SpeakingRecorder();

    @BeforeEach
    void register() {
        GameEventBus.register(recorder);
    }

    @AfterEach
    void unregister() {
        GameEventBus.unregister(recorder);
    }

    @Test
    void firstCompletionCannotReportSilenceWhileAnotherRequestRemainsActive() {
        VocalisationHandle first = handle("first");
        VocalisationHandle second = handle("second");

        assertTrue(first.claimForPlayback());
        assertTrue(second.claimForPlayback());
        assertEquals(List.of(true), recorder.states);

        first.complete();
        assertEquals(List.of(true), recorder.states);

        second.complete();
        assertEquals(List.of(true, false), recorder.states);
    }

    @Test
    void failureAndCancellationBothReleaseSpeakingState() {
        VocalisationHandle failed = handle("failed");
        assertTrue(failed.claimForPlayback());
        failed.fail(new IllegalStateException("synthesis failed"));

        VocalisationHandle cancelled = handle("cancelled");
        assertTrue(cancelled.claimForPlayback());
        cancelled.completion().cancel(true);

        assertEquals(List.of(true, false, true, false), recorder.states);
        assertTrue(failed.completion().isCompletedExceptionally());
        assertTrue(cancelled.completion().isCancelled());
    }

    @Test
    void unclaimedRejectionNeverEntersSpeakingState() {
        VocalisationHandle handle = handle("missing-mouth");

        assertTrue(handle.rejectIfUnclaimed(new IllegalStateException("no mouth")));

        assertTrue(handle.completion().isCompletedExceptionally());
        assertTrue(recorder.states.isEmpty());
        assertFalse(handle.claimForPlayback());
    }

    @Test
    void aSecondMouthCannotClaimTheSameRequest() {
        VocalisationHandle handle = handle("one-owner");

        assertTrue(handle.claimForPlayback());
        assertFalse(handle.claimForPlayback());
        handle.complete();

        assertEquals(List.of(true, false), recorder.states);
    }

    private static VocalisationHandle handle(String id) {
        return new VocalisationHandle(id, true, null);
    }

    private static final class SpeakingRecorder {
        private final List<Boolean> states = new ArrayList<>();

        @Subscribe
        public void onSpeaking(IsSpeakingEvent event) {
            states.add(event.isSpeaking());
        }
    }
}
