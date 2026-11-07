package elite.intel.ai.brain.vega.input;

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
import elite.intel.ai.ears.WakeBypass;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.StringUtls;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A new install starts asleep, and once a language model is set up the notice tells the commander what to say. That
 * phrase has to work even while the model is not answering, say with LM Studio still starting: past the sleep gate,
 * then to the wake command by reflex, without ever asking a model.
 */
class FreshInstallWakePhraseTest {

    private VegaSubsystemGate gate;

    @BeforeAll
    static void loadTheCommands() {
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
    }

    @AfterEach
    void cleanUp() {
        if (gate != null) {
            gate.stop();
        }
        VegaRuntimeTestSupport.clearInstalledGraph();
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @ParameterizedTest(name = "[{0}] \"{1}\"")
    @CsvSource({
            "EN, wake up",
            "DE, wach auf",
            "ES, despierta",
            "FR, réveille-toi",
            "IT, svegliati",
            "PT, acorda",
            "PTBZ, acorda",
            "RU, проснись",
            "UK, прокинься"
    })
    void theWordsTheNoticeNamesWakeHerWithNoModel(Language language, String phrase) throws Exception {
        SystemSession.getInstance().setLanguage(language);

        String notice = StringUtls.localizedSpeech("speech.sleepModeStartupNotice").toLowerCase(Locale.ROOT);
        assertTrue(notice.contains(phrase), "the notice names the phrase: " + notice);
        assertEquals(phrase, WakeBypass.forCurrentLanguage().admit(phrase), "the sleep gate lets it through");

        AtomicBoolean askedTheModel = new AtomicBoolean();
        CompletableFuture<String> executed = new CompletableFuture<>();
        LlmGateway noModel = new LlmGateway() {
            @Override
            public CompletableFuture<LlmResult> submit(LlmRequest request) {
                askedTheModel.set(true);
                return CompletableFuture.completedFuture(new LlmResult(LlmResult.Status.SERVICE_UNAVAILABLE, List.of()));
            }

            @Override
            public CompletableFuture<String> completePlainText(LlmRequest request) {
                askedTheModel.set(true);
                return CompletableFuture.completedFuture(null);
            }
        };
        ExecutionGateway execution = request -> {
            executed.complete(request.toolName());
            return CompletableFuture.completedFuture(new JsonObject());
        };
        SpeechGateway speech = request -> CompletableFuture.completedFuture(null);
        gate = new VegaSubsystemGate(noModel, execution, speech);
        gate.start();

        gate.onUserInput(new UserInputEvent(phrase));

        assertEquals(WakeupCommand.ID, executed.get(5, TimeUnit.SECONDS));
        assertFalse(askedTheModel.get(), "the wake phrase must not depend on a language model");
    }
}
