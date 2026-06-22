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

    /** Dialect stub: renders nothing, returns scripted parse results in order. */
    private static final class ScriptedDialect implements CompanionLlmDialect {
        private final Deque<LlmResult> results = new ArrayDeque<>();

        ScriptedDialect(LlmResult... scripted) {
            for (LlmResult r : scripted) {
                results.add(r);
            }
        }

        @Override
        public String buildRequestBody(LlmRequest request) {
            return "{}";
        }

        @Override
        public LlmResult parse(JsonObject response) {
            return results.poll();
        }
    }

    private final AtomicInteger sends = new AtomicInteger();
    private final LlmTransport countingTransport = body -> {
        sends.incrementAndGet();
        return new JsonObject();
    };

    private static LlmRequest request() {
        return new LlmRequest("req-1",
                List.of(LlmMessage.of(LlmMessageRole.SYSTEM, "rules")),
                List.of(new LlmToolDefinition("speak", "d", "", List.of())),
                PromptCacheProfile.COMMANDER);
    }

    private static LlmResult ok(String toolName) {
        return new LlmResult(LlmResult.Status.OK, List.of(new LlmToolInvocation("c1", toolName, new JsonObject())));
    }

    private static LlmResult invalid() {
        return new LlmResult(LlmResult.Status.INVALID_RESPONSE, List.of());
    }

    private LlmResult run(CompanionLlmDialect dialect) throws Exception {
        return new CompanionLlmGateway(dialect, countingTransport, Runnable::run).submit(request()).get();
    }

    @Test
    void validToolCallSucceedsOnFirstTry() throws Exception {
        LlmResult result = run(new ScriptedDialect(ok("speak")));
        assertTrue(result.isValid());
        assertEquals("speak", result.toolInvocations().get(0).name());
        assertEquals(1, sends.get());
    }

    @Test
    void invalidFirstThenValidSucceedsAfterOneRetry() throws Exception {
        LlmResult result = run(new ScriptedDialect(invalid(), ok("speak")));
        assertTrue(result.isValid());
        assertEquals(2, sends.get());
    }

    @Test
    void twoInvalidResponsesYieldInvalidResult() throws Exception {
        LlmResult result = run(new ScriptedDialect(invalid(), invalid()));
        assertFalse(result.isValid());
        assertEquals(LlmResult.Status.INVALID_RESPONSE, result.status());
        assertEquals(2, sends.get());
    }

    @Test
    void callToUnofferedToolIsRejectedThenRetried() throws Exception {
        // "jump" was never offered this turn, so even an OK-status parse is not usable.
        LlmResult result = run(new ScriptedDialect(ok("jump"), ok("jump")));
        assertFalse(result.isValid());
        assertEquals(2, sends.get());
    }
}
