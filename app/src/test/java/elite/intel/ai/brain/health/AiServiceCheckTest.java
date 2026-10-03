package elite.intel.ai.brain.health;

import elite.intel.ai.ProviderEnum;
import elite.intel.ai.brain.inference.lmstudio.LMStudioClient.ModelListing;
import elite.intel.ai.brain.vega.llm.LlmGateway;
import elite.intel.ai.brain.vega.model.llm.LlmRequest;
import elite.intel.ai.brain.vega.model.llm.LlmResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The check reads its verdict from what the transport recorded, so each fake gateway here plays the transport's
 * part: it records the outcome in the shared {@link AiServiceHealth}, exactly as {@code BaseAiClient} does before
 * the gateway's reply future completes.
 */
class AiServiceCheckTest {

    private static final Instant START = Instant.parse("2026-09-27T20:00:00Z");
    private static final String GEMMA = "google/gemma-4-e4b";
    private static final Duration SHORT_TIMEOUT = Duration.ofMillis(50);

    private final MutableClock clock = new MutableClock(START);
    private final AiServiceHealth health = new AiServiceHealth(clock);

    @Test
    void nothingConfiguredSendsNothing() {
        ScriptedGateway gateway = new ScriptedGateway(wire -> fail("nothing should be sent"));
        AiServiceVerdict verdict = check(new AiServiceCheck.Configuration(true, true, "", Optional.empty()),
                () -> fail("LM Studio should not be asked"), gateway).run();

        assertInstanceOf(AiServiceVerdict.NotConfigured.class, verdict);
        assertEquals(0, gateway.requests);
    }

    @Test
    void aLocalSetupWithNoModelNamedIsNotConfigured() {
        // LM Studio would answer an empty model name with whatever it has loaded: never probe it.
        AiServiceVerdict verdict = check(local(""), () -> fail("LM Studio should not be asked"),
                new ScriptedGateway(wire -> fail("nothing should be sent"))).run();

        assertInstanceOf(AiServiceVerdict.NotConfigured.class, verdict);
    }

    @Test
    void aCloudSetupWithNoProviderSendsNothing() {
        AiServiceVerdict verdict = check(new AiServiceCheck.Configuration(false, false, "", Optional.empty()),
                () -> fail("LM Studio should not be asked"), new ScriptedGateway(wire -> fail("nothing sent"))).run();

        assertInstanceOf(AiServiceVerdict.NoProvider.class, verdict);
        assertTrue(verdict.isSetupGap());
    }

    @Test
    void anLmStudioThatDoesNotAnswerItsModelListIsNotRunning() {
        ScriptedGateway gateway = new ScriptedGateway(wire -> fail("no chat request once the list failed"));
        AiServiceVerdict verdict = check(local(GEMMA), ModelListing.NotAnswering::new, gateway).run();

        assertInstanceOf(AiServiceVerdict.LocalHostNotRunning.class, verdict);
        assertEquals(0, gateway.requests);
    }

    @Test
    void anLmStudioWithoutTheChosenModelSaysSo() {
        AiServiceVerdict verdict = check(local(GEMMA), () -> new ModelListing.Listed(Set.of("qwen/qwen3-8b")),
                new ScriptedGateway(wire -> fail("no chat request for a missing model"))).run();

        assertInstanceOf(AiServiceVerdict.LocalModelMissing.class, verdict);
    }

    @Test
    void aModelNamedWithoutItsPublisherStillMatchesTheList() {
        AiServiceVerdict verdict = check(local("gemma-4-e4b"), () -> new ModelListing.Listed(Set.of(GEMMA)),
                answering(Duration.ofMillis(900))).run();

        assertInstanceOf(AiServiceVerdict.Connected.class, verdict);
    }

    @Test
    void aLocalModelThatAnswersIsConnectedWithItsReplyTime() {
        AiServiceVerdict verdict = check(local(GEMMA), () -> new ModelListing.Listed(Set.of(GEMMA)),
                answering(Duration.ofMillis(900))).run();

        AiServiceVerdict.Connected connected = assertInstanceOf(AiServiceVerdict.Connected.class, verdict);
        assertEquals("LM Studio", connected.service());
        assertEquals(Optional.of(Duration.ofMillis(900)), connected.typicalReply());
        assertFalse(connected.unsupportedModel());
    }

    @Test
    void aLocalModelOtherThanTheRecommendedOneIsFlagged() {
        String qwen = "qwen/qwen3-8b";
        AiServiceVerdict verdict = check(local(qwen), () -> new ModelListing.Listed(Set.of(qwen)),
                answering(Duration.ofSeconds(1))).run();

        assertTrue(assertInstanceOf(AiServiceVerdict.Connected.class, verdict).unsupportedModel());
    }

    @Test
    void anUnreadableModelListStillLetsTheProbeDecide() {
        AiServiceVerdict verdict = check(local(GEMMA), ModelListing.Unknown::new,
                new ScriptedGateway(AiServiceHealth::recordUnreachable)).run();

        assertInstanceOf(AiServiceVerdict.LocalHostNotRunning.class, verdict);
    }

    @Test
    void anUnreachableCloudProviderIsNamed() {
        AiServiceVerdict verdict = check(cloud(ProviderEnum.MISTRAL), () -> fail("cloud has no model list"),
                new ScriptedGateway(AiServiceHealth::recordUnreachable)).run();

        assertEquals(new AiServiceVerdict.Unreachable("Mistral"), verdict);
    }

    @Test
    void aFreshRefusalHasNoLengthAndARejectedKeyIsOffered() {
        AiServiceVerdict verdict = check(cloud(ProviderEnum.ANTHROPIC), () -> fail("cloud has no model list"),
                new ScriptedGateway(wire -> wire.recordRefused(401))).run();

        assertEquals(new AiServiceVerdict.Refused("Anthropic Claude", Optional.empty(), true), verdict);
    }

    @Test
    void aStandingRefusalReportsHowLongItHasLasted() {
        health.recordRefused(429);
        clock.advance(Duration.ofHours(2));

        AiServiceVerdict verdict = check(cloud(ProviderEnum.MISTRAL), () -> fail("cloud has no model list"),
                new ScriptedGateway(wire -> wire.recordRefused(429))).run();

        // A 429 claims "slow down"; the record reports only that every request has been refused for two hours.
        assertEquals(new AiServiceVerdict.Refused("Mistral", Optional.of(Duration.ofHours(2)), false), verdict);
    }

    @Test
    void aProviderThatNeverAnswersIsReportedAsNoAnswer() {
        ScriptedGateway silent = new ScriptedGateway(null);
        AiServiceVerdict verdict = check(cloud(ProviderEnum.OPENAI), () -> fail("cloud has no model list"), silent)
                .run();

        assertEquals(new AiServiceVerdict.NoAnswer("OpenAI", SHORT_TIMEOUT), verdict);
        assertTrue(silent.pending.isCancelled(), "a probe that timed out must not stay in flight");
    }

    @Test
    void theProbeGatewayIsClosedAfterUse() {
        ScriptedGateway gateway = answering(Duration.ofSeconds(1));
        check(cloud(ProviderEnum.DEEPSEEK), () -> fail("cloud has no model list"), gateway).run();

        assertTrue(gateway.closed);
        assertEquals(1, gateway.requests);
    }

    private AiServiceCheck check(AiServiceCheck.Configuration configuration,
                                 Supplier<ModelListing> localModels, ScriptedGateway gateway) {
        return new AiServiceCheck(() -> configuration, localModels, () -> gateway, health, clock, SHORT_TIMEOUT);
    }

    private static AiServiceCheck.Configuration local(String model) {
        return new AiServiceCheck.Configuration(false, true, model, Optional.empty());
    }

    private static AiServiceCheck.Configuration cloud(ProviderEnum provider) {
        return new AiServiceCheck.Configuration(false, false, "", Optional.of(provider));
    }

    private ScriptedGateway answering(Duration replyTime) {
        return new ScriptedGateway(wire -> wire.recordAnswered(replyTime));
    }

    /**
     * Plays the transport's part for one test request: records {@code onSend} in the health record, then
     * completes. A null {@code onSend} never answers at all.
     */
    private final class ScriptedGateway implements LlmGateway {

        private final Consumer<AiServiceHealth> onSend;
        private final CompletableFuture<String> pending = new CompletableFuture<>();
        private int requests;
        private boolean closed;

        private ScriptedGateway(Consumer<AiServiceHealth> onSend) {
            this.onSend = onSend;
        }

        @Override
        public CompletableFuture<String> completePlainText(LlmRequest request) {
            requests++;
            if (onSend == null) {
                return pending;
            }
            onSend.accept(health);
            return CompletableFuture.completedFuture("OK");
        }

        @Override
        public CompletableFuture<LlmResult> submit(LlmRequest request) {
            throw new AssertionError("the probe is a plain-text turn");
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
