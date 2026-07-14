package elite.intel.ai.mouth.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.ears.IsSpeakingEvent;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.eventbus.GameEventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards routing into the Mouth-owned handle instead of producer-owned speaking-state events. */
class VocalisationRouterTest {

    private final List<Object> registered = new ArrayList<>();
    private final VocalisationRouter router = new VocalisationRouter();

    @AfterEach
    void unregister() {
        registered.forEach(GameEventBus::unregister);
    }

    @Test
    void activeMouthClaimsAndOwnsTheSpeakingLifecycle() {
        FakeMouth mouth = register(new FakeMouth());
        SpeakingRecorder speaking = register(new SpeakingRecorder());

        router.onAiVoxResponseEvent(new AiVoxResponseEvent("course plotted"));

        assertNotNull(mouth.request);
        assertEquals(List.of(true), speaking.states);
        mouth.request.handle().complete();
        assertEquals(List.of(true, false), speaking.states);
    }

    @Test
    void reentrantRoutingWaitsForTheMouthBeforeCheckingForAnUnclaimedRequest() {
        FakeMouth mouth = register(new FakeMouth());
        register(router);

        GameEventBus.publish(new AiVoxResponseEvent("startup greeting"));

        assertNotNull(mouth.request);
        assertTrue(mouth.request.handle().isHandled());
        assertFalse(mouth.request.handle().isDone(),
                "the post-dispatch no-Mouth check must not reject a request the Mouth claimed");
        mouth.request.handle().complete();
    }

    @Test
    void callerFutureFailsWhenNoMouthAcceptsTheRequest() {
        CompletableFuture<Void> completion = new CompletableFuture<>();

        router.onAiVoxResponseEvent(new AiVoxResponseEvent("course plotted", completion));

        assertTrue(completion.isCompletedExceptionally());
    }

    private <T> T register(T subscriber) {
        GameEventBus.register(subscriber);
        registered.add(subscriber);
        return subscriber;
    }

    private static final class FakeMouth {
        private VocalisationRequestEvent request;

        @Subscribe
        public void onRequest(VocalisationRequestEvent event) {
            event.handle().claimForPlayback();
            request = event;
        }
    }

    private static final class SpeakingRecorder {
        private final List<Boolean> states = new ArrayList<>();

        @Subscribe
        public void onSpeaking(IsSpeakingEvent event) {
            states.add(event.isSpeaking());
        }
    }
}
