package elite.intel.companion.llm;

import com.google.gson.JsonObject;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.companion.model.llm.PromptCacheProfile;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gateway orchestration with stubbed dialect/transport: validity rules, the single repair/retry, and
 * unknown-tool rejection. A synchronous executor makes the future resolve in-thread.
 */
class CompanionLlmGatewayTest {

    /** Adapter stub: renders nothing (but records each request's messages), returns scripted parse results in order. */
    private static final class ScriptedAdapter implements LlmProviderAdapter {
        private final Deque<LlmResult> results = new ArrayDeque<>();
        private LlmRequest lastRequest;

        ScriptedAdapter(LlmResult... scripted) {
            for (LlmResult r : scripted) {
                results.add(r);
            }
        }

        @Override
        public String buildRequestBody(LlmRequest request) {
            lastRequest = request;
            return "{}";
        }

        @Override
        public LlmResult parse(JsonObject response) {
            return results.poll();
        }

        @Override
        public String parseText(JsonObject response) {
            return null;
        }
    }

    private final AtomicInteger sends = new AtomicInteger();
    private final LlmTransport countingTransport = body -> {
        sends.incrementAndGet();
        return new JsonObject();
    };

    private static LlmRequest request() {
        return requestOffering("speak");
    }

    /** A request offering exactly the given tools; offering "classify_turn" makes it a classifying turn. */
    private static LlmRequest requestOffering(String... toolNames) {
        List<LlmToolDefinition> tools = new ArrayList<>();
        for (String name : toolNames) {
            tools.add(new LlmToolDefinition(name, "d", "", List.of()));
        }
        return new LlmRequest("req-1",
                List.of(LlmMessage.of(LlmMessageRole.SYSTEM, "rules")),
                List.copyOf(tools),
                PromptCacheProfile.COMMANDER);
    }

    private static LlmRequest requestWithMessages(List<LlmMessage> messages, String... toolNames) {
        List<LlmToolDefinition> tools = new ArrayList<>();
        for (String name : toolNames) {
            tools.add(new LlmToolDefinition(name, "d", "", List.of()));
        }
        return new LlmRequest("req-2", messages, List.copyOf(tools), PromptCacheProfile.COMMANDER);
    }

    private static LlmResult ok(String... toolNames) {
        List<LlmToolInvocation> calls = new ArrayList<>();
        int id = 1;
        for (String name : toolNames) {
            calls.add(new LlmToolInvocation("c" + id++, name, new JsonObject()));
        }
        return new LlmResult(LlmResult.Status.OK, List.copyOf(calls));
    }

    private static LlmResult invalid() {
        return new LlmResult(LlmResult.Status.INVALID_RESPONSE, List.of());
    }

    private LlmResult run(LlmProviderAdapter adapter) throws Exception {
        return run(adapter, request());
    }

    private LlmResult run(LlmProviderAdapter adapter, LlmRequest request) throws Exception {
        return new CompanionLlmGateway(adapter, countingTransport, Runnable::run).submit(request).get();
    }

    @Test
    void validToolCallSucceedsOnFirstTry() throws Exception {
        LlmResult result = run(new ScriptedAdapter(ok("speak")));
        assertTrue(result.isValid());
        assertEquals("speak", result.toolInvocations().get(0).name());
        assertEquals(1, sends.get());
    }

    @Test
    void invalidFirstThenValidSucceedsAfterOneRetry() throws Exception {
        LlmResult result = run(new ScriptedAdapter(invalid(), ok("speak")));
        assertTrue(result.isValid());
        assertEquals(2, sends.get());
    }

    @Test
    void twoInvalidResponsesYieldInvalidResult() throws Exception {
        LlmResult result = run(new ScriptedAdapter(invalid(), invalid()));
        assertFalse(result.isValid());
        assertEquals(LlmResult.Status.INVALID_RESPONSE, result.status());
        assertEquals(2, sends.get());
    }

    @Test
    void callToUnofferedToolIsRejectedThenRetried() throws Exception {
        // "jump" was never offered this turn, so even an OK-status parse is not usable.
        LlmResult result = run(new ScriptedAdapter(ok("jump"), ok("jump")));
        assertFalse(result.isValid());
        assertEquals(2, sends.get());
    }

    @Test
    void missingClassifyTurnIsRepairedWithATargetedNudge() throws Exception {
        // A classifying turn (classify_turn offered): a speak-only response draws one retry whose nudge
        // names classify_turn; the repaired response is returned.
        ScriptedAdapter adapter = new ScriptedAdapter(ok("speak"), ok("classify_turn", "speak"));
        LlmResult result = run(adapter, requestOffering("speak", "classify_turn"));

        assertTrue(result.isValid());
        assertEquals(2, sends.get());
        assertEquals(2, result.toolInvocations().size());
        String nudge = adapter.lastRequest.messages().get(adapter.lastRequest.messages().size() - 1).content();
        assertTrue(nudge.contains("classify_turn"), "the repair nudge must name the omitted call, was: " + nudge);
    }

    @Test
    void classifyStillMissingAfterRetryDegradesGracefullyToTheValidResponse() throws Exception {
        // Both responses lack classify_turn but are otherwise valid: the turn must still settle with the
        // model's answer (memory stamping degrades), never fall to the INVALID service phrase.
        LlmResult result = run(new ScriptedAdapter(ok("speak"), ok("speak")),
                requestOffering("speak", "classify_turn"));

        assertTrue(result.isValid());
        assertEquals("speak", result.toolInvocations().get(0).name());
        assertEquals(2, sends.get());
    }

    @Test
    void classifyOnlyDrawsSettlingNudgeThenActs() throws Exception {
        // A classify-only response has classified but not answered/acted (it would fall silent): one retry
        // whose nudge tells the model to speak, and the repaired classify+speak response is returned.
        ScriptedAdapter adapter = new ScriptedAdapter(ok("classify_turn"), ok("classify_turn", "speak"));
        LlmResult result = run(adapter, requestOffering("speak", "classify_turn"));

        assertTrue(result.isValid());
        assertEquals(2, sends.get());
        assertEquals(2, result.toolInvocations().size());
        String nudge = adapter.lastRequest.messages().get(adapter.lastRequest.messages().size() - 1).content();
        assertTrue(nudge.contains("speak"), "the repair nudge must ask the model to speak/act, was: " + nudge);
    }

    @Test
    void classifyOnlySettlingStillMissingDegradesGracefully() throws Exception {
        // Both responses are classify-only: after one nudge the turn settles silently with the valid response
        // rather than falling to the INVALID service phrase (mirrors the missing-classify graceful path).
        LlmResult result = run(new ScriptedAdapter(ok("classify_turn"), ok("classify_turn")),
                requestOffering("speak", "classify_turn"));

        assertTrue(result.isValid());
        assertEquals("classify_turn", result.toolInvocations().get(0).name());
        assertEquals(2, sends.get());
    }

    @Test
    void turnWithoutClassifyOfferedNeverRequiresIt() throws Exception {
        // A narration-style turn does not offer classify_turn, so a speak-only response is fine as-is.
        LlmResult result = run(new ScriptedAdapter(ok("speak")), requestOffering("speak"));

        assertTrue(result.isValid());
        assertEquals(1, sends.get());
    }

    @Test
    void repairMergesNudgeIntoLeadingSystemMessage() throws Exception {
        ScriptedAdapter adapter = new ScriptedAdapter(invalid(), invalid());
        LlmRequest request = requestWithMessages(
                List.of(
                        LlmMessage.of(LlmMessageRole.SYSTEM, "rules"),
                        LlmMessage.of(LlmMessageRole.USER, "go")),
                "speak");

        run(adapter, request);

        List<LlmMessage> retried = adapter.lastRequest.messages();
        assertEquals(2, retried.size(), "repair must not append an extra system/user message");
        assertEquals(LlmMessageRole.SYSTEM, retried.get(0).role());
        assertTrue(retried.get(0).content().contains("rules"));
        assertTrue(retried.get(0).content().contains("Format correction"));
        assertEquals(LlmMessageRole.USER, retried.get(1).role());
        assertEquals("go", retried.get(1).content());
        assertEquals(1, retried.stream().filter(m -> m.role() == LlmMessageRole.SYSTEM).count());
    }
}
