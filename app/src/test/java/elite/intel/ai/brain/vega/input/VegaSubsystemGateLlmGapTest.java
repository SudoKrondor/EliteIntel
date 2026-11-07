package elite.intel.ai.brain.vega.input;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.commands.builtin.WakeupCommand;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.ai.brain.vega.VegaRuntimeTestSupport;
import elite.intel.ai.brain.vega.execution.ExecutionGateway;
import elite.intel.ai.brain.vega.llm.LlmGateway;
import elite.intel.ai.brain.vega.model.llm.LlmRequest;
import elite.intel.ai.brain.vega.model.llm.LlmResult;
import elite.intel.ai.brain.vega.speech.SpeechGateway;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.util.StringUtls;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Without a language model the app is not usable, so it must not look half usable: nothing the commander says goes
 * further than the gate, a phrase the reflex could resolve on its own included, and they are told why.
 */
class VegaSubsystemGateLlmGapTest {

    /**
     * A training phrase the reflex resolves with no model, which makes it the case worth guarding.
     */
    private static final String REFLEX_PHRASE = "wake up";
    private static final String GAP_KEY = "speech.setup.noLlm";

    private final List<String> spoken = new CopyOnWriteArrayList<>();
    private final Object listener = new Object() {
        @Subscribe
        public void onVocalisation(VocalisationRequestEvent event) {
            spoken.add(event.getText());
        }
    };
    private final CompletableFuture<String> executed = new CompletableFuture<>();
    private VegaSubsystemGate gate;

    @BeforeAll
    static void loadTheCommands() {
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
    }

    @BeforeEach
    void listen() {
        GameEventBus.register(listener);
    }

    @AfterEach
    void cleanUp() {
        GameEventBus.unregister(listener);
        if (gate != null) {
            gate.stop();
        }
        VegaRuntimeTestSupport.clearInstalledGraph();
    }

    @Test
    void noModelMeansNotEvenAReflexRuns() {
        gate = startedGate(() -> Optional.of(GAP_KEY));

        gate.onUserInput(new UserInputEvent(REFLEX_PHRASE));

        assertThrows(TimeoutException.class, () -> executed.get(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void theyAreToldWhatIsMissingOnceNotOnEverySentence() {
        gate = startedGate(() -> Optional.of(GAP_KEY));

        gate.onUserInput(new UserInputEvent(REFLEX_PHRASE));
        gate.onUserInput(new UserInputEvent("what is my fuel level"));

        assertEquals(List.of(StringUtls.localizedSpeech(GAP_KEY)), spoken);
    }

    @Test
    void aModelSetUpAfterTheStartTakesEffectOnTheNextSentence() throws Exception {
        final boolean[] gap = {true};
        gate = startedGate(() -> gap[0] ? Optional.of(GAP_KEY) : Optional.empty());

        gate.onUserInput(new UserInputEvent(REFLEX_PHRASE));
        gap[0] = false;
        gate.onUserInput(new UserInputEvent(REFLEX_PHRASE));

        assertEquals(WakeupCommand.ID, executed.get(5, TimeUnit.SECONDS));
    }

    private VegaSubsystemGate startedGate(Supplier<Optional<String>> llmGap) {
        LlmGateway llm = new LlmGateway() {
            @Override
            public CompletableFuture<LlmResult> submit(LlmRequest request) {
                return CompletableFuture.completedFuture(new LlmResult(LlmResult.Status.SERVICE_UNAVAILABLE, List.of()));
            }

            @Override
            public CompletableFuture<String> completePlainText(LlmRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        };
        ExecutionGateway execution = request -> {
            executed.complete(request.toolName());
            return CompletableFuture.completedFuture(new JsonObject());
        };
        SpeechGateway speech = request -> CompletableFuture.completedFuture(null);
        VegaSubsystemGate started = new VegaSubsystemGate(llm, execution, speech, llmGap);
        started.start();
        return started;
    }
}
