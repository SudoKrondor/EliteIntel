package elite.intel.companion.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.AiTransportResult;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.companion.model.llm.PromptCacheProfile;
import elite.intel.companion.tools.ClassifyTurnFunction;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private static LlmResult ok(String toolName, JsonObject arguments) {
        return new LlmResult(LlmResult.Status.OK,
                List.of(new LlmToolInvocation("c1", toolName, arguments)));
    }

    private static LlmResult invalid() {
        return new LlmResult(LlmResult.Status.INVALID_RESPONSE, List.of());
    }

    private static LlmTransport outcomeTransport(AtomicInteger calls, AiTransportResult... scriptedOutcomes) {
        Deque<AiTransportResult> outcomes = new ArrayDeque<>(List.of(scriptedOutcomes));
        return new LlmTransport() {
            @Override
            public JsonObject send(String requestBody) {
                throw new UnsupportedOperationException("Typed transport outcome is required");
            }

            @Override
            public AiTransportResult sendOutcome(String requestBody) {
                calls.incrementAndGet();
                AiTransportResult next = outcomes.pollFirst();
                if (next == null) {
                    throw new AssertionError("Unexpected extra transport call");
                }
                return next;
            }
        };
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
    void permanentTransportFailureDoesNotEnterProtocolRepair() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        LlmTransport transport = outcomeTransport(calls,
                AiTransportResult.failure(AiTransportResult.FailureKind.PERMANENT, 401, "invalid API key"));

        LlmResult result = new CompanionLlmGateway(
                new ScriptedAdapter(ok("speak")), transport, Runnable::run).submit(request()).get();

        assertFalse(result.isValid());
        assertEquals(1, calls.get(), "an authentication failure must not be retried as malformed model output");
    }

    @Test
    void transientTransportFailureRetriesOnceBeforeParsingTheResponse() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        LlmTransport transport = outcomeTransport(calls,
                AiTransportResult.failure(AiTransportResult.FailureKind.TRANSIENT, 429, "rate limited"),
                AiTransportResult.success(new JsonObject()));

        LlmResult result = new CompanionLlmGateway(
                new ScriptedAdapter(ok("speak")), transport, Runnable::run).submit(request()).get();

        assertTrue(result.isValid());
        assertEquals(2, calls.get(), "a transient transport failure receives exactly one delayed retry");
    }

    @Test
    void malformedTransportResponseUsesTheExistingProtocolRepair() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        LlmTransport transport = outcomeTransport(calls,
                AiTransportResult.failure(AiTransportResult.FailureKind.MALFORMED_RESPONSE, 200,
                        "response is not a JSON object"),
                AiTransportResult.success(new JsonObject()));

        LlmResult result = new CompanionLlmGateway(
                new ScriptedAdapter(ok("speak")), transport, Runnable::run).submit(request()).get();

        assertTrue(result.isValid());
        assertEquals(2, calls.get(), "a malformed successful response still follows protocol repair");
    }

    @Test
    void cancellingInFlightRequestInterruptsTransportAndReleasesWorker() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        CountDownLatch neverReleased = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        LlmTransport transport = body -> {
            if (calls.incrementAndGet() == 1) {
                entered.countDown();
                try {
                    neverReleased.await();
                } catch (InterruptedException expected) {
                    interrupted.countDown();
                    Thread.currentThread().interrupt();
                }
            }
            return new JsonObject();
        };

        try (CompanionLlmGateway gateway = new CompanionLlmGateway(
                new ScriptedAdapter(ok("speak")), transport, Executors.newSingleThreadExecutor(),
                Duration.ofSeconds(5))) {
            CompletableFuture<LlmResult> first = gateway.submit(request());
            assertTrue(entered.await(2, TimeUnit.SECONDS), "the physical transport must start");

            assertTrue(first.cancel(true));
            assertTrue(interrupted.await(2, TimeUnit.SECONDS), "future cancellation must interrupt transport");

            LlmResult second = gateway.submit(request()).get(2, TimeUnit.SECONDS);
            assertTrue(second.isValid(), "the sole worker must be available for the next request");
            assertEquals(2, calls.get());
        }
    }

    @Test
    void logicalDeadlineInterruptsStalledCallWithoutRetry() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        CountDownLatch neverReleased = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        LlmTransport transport = body -> {
            calls.incrementAndGet();
            entered.countDown();
            try {
                neverReleased.await();
            } catch (InterruptedException expected) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
            }
            return new JsonObject();
        };

        try (CompanionLlmGateway gateway = new CompanionLlmGateway(
                new ScriptedAdapter(ok("speak")), transport, Executors.newSingleThreadExecutor(),
                Duration.ofSeconds(1))) {
            CompletableFuture<LlmResult> result = gateway.submit(request());
            assertTrue(entered.await(2, TimeUnit.SECONDS), "the physical transport must start");

            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> result.get(2, TimeUnit.SECONDS));
            assertInstanceOf(TimeoutException.class, failure.getCause());
            assertTrue(interrupted.await(2, TimeUnit.SECONDS), "deadline must interrupt transport");
            assertEquals(1, calls.get(), "a timed-out physical call must not enter repair");
        }
    }

    @Test
    void oneLogicalDeadlineAlsoCoversRepairAttempt() throws Exception {
        CountDownLatch repairEntered = new CountDownLatch(1);
        CountDownLatch repairInterrupted = new CountDownLatch(1);
        CountDownLatch neverReleased = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        LlmTransport transport = body -> {
            if (calls.incrementAndGet() == 2) {
                repairEntered.countDown();
                try {
                    neverReleased.await();
                } catch (InterruptedException expected) {
                    repairInterrupted.countDown();
                    Thread.currentThread().interrupt();
                }
            }
            return new JsonObject();
        };

        try (CompanionLlmGateway gateway = new CompanionLlmGateway(
                new ScriptedAdapter(invalid(), ok("speak")), transport, Executors.newSingleThreadExecutor(),
                Duration.ofSeconds(1))) {
            CompletableFuture<LlmResult> result = gateway.submit(request());
            assertTrue(repairEntered.await(2, TimeUnit.SECONDS), "repair must use the same logical request");

            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> result.get(2, TimeUnit.SECONDS));
            assertInstanceOf(TimeoutException.class, failure.getCause());
            assertTrue(repairInterrupted.await(2, TimeUnit.SECONDS), "shared deadline must interrupt repair");
            assertEquals(2, calls.get());
        }
    }

    @Test
    void callToUnofferedToolIsRejectedThenRetried() throws Exception {
        // "jump" was never offered this turn, so even an OK-status parse is not usable.
        LlmResult result = run(new ScriptedAdapter(ok("jump"), ok("jump")));
        assertFalse(result.isValid());
        assertEquals(2, sends.get());
    }

    @Test
    void settlingOnlyContinuesWithPendingToolResult() throws Exception {
        LlmResult settlingOnly = new LlmResult(LlmResult.Status.OK,
                List.of(new LlmToolInvocation("settling-1", "speak", new JsonObject())));
        LlmResult classifyOnly = new LlmResult(LlmResult.Status.OK,
                List.of(new LlmToolInvocation("classify-1", "classify_turn", new JsonObject())));
        ScriptedAdapter adapter = new ScriptedAdapter(settlingOnly, classifyOnly);
        LlmRequest request = requestWithMessages(
                List.of(
                        LlmMessage.of(LlmMessageRole.SYSTEM, "rules"),
                        LlmMessage.of(LlmMessageRole.USER, "earlier commander turn"),
                        LlmMessage.of(LlmMessageRole.ASSISTANT, "earlier companion turn"),
                        LlmMessage.of(LlmMessageRole.USER, "go")),
                "speak", "classify_turn");
        LlmResult result = run(adapter, request);

        assertTrue(result.isValid());
        assertEquals(2, sends.get());
        assertEquals(List.of("classify_turn", "speak"),
                result.toolInvocations().stream().map(LlmToolInvocation::name).toList());
        assertEquals(List.of("classify-1", "settling-1"),
                result.toolInvocations().stream().map(LlmToolInvocation::id).toList());
        assertPendingSettlingContinuation(request, adapter.lastRequest, "speak", "settling-1");
    }

    @Test
    void settlingOnlyContinuationRejectsAnotherSettlingCall() throws Exception {
        LlmResult result = run(new ScriptedAdapter(ok("speak"), ok("speak")),
                requestOffering("speak", "classify_turn"));

        assertFalse(result.isValid());
        assertEquals(LlmResult.Status.INVALID_RESPONSE, result.status());
        assertEquals(2, sends.get());
    }

    @Test
    void invalidArgumentsUseRejectedToolContinuationBeforeAValidRepair() throws Exception {
        LlmToolDefinition navigate = new LlmToolDefinition("navigate", "Navigate", "",
                List.of(new ActionParameterSpec("target", "string", true,
                        "Target system", List.of(), null)));
        LlmRequest request = new LlmRequest("req-schema",
                List.of(LlmMessage.of(LlmMessageRole.USER, "navigate")),
                List.of(navigate), PromptCacheProfile.COMMANDER);
        JsonObject invalidArguments = new JsonObject();
        invalidArguments.addProperty("target", 42);
        JsonObject validArguments = new JsonObject();
        validArguments.addProperty("target", "Sol");
        ScriptedAdapter adapter = new ScriptedAdapter(
                ok("navigate", invalidArguments), ok("navigate", validArguments));

        LlmResult result = run(adapter, request);

        assertTrue(result.isValid());
        assertEquals("Sol", result.toolInvocations().get(0).arguments().get("target").getAsString());
        assertEquals(2, sends.get());
        int repairIndex = request.messages().size();
        List<LlmMessage> repairedMessages = adapter.lastRequest.messages();
        assertEquals(repairIndex + 2, repairedMessages.size());
        assertEquals(LlmMessageRole.ASSISTANT, repairedMessages.get(repairIndex).role());
        assertEquals("navigate", repairedMessages.get(repairIndex).toolCalls().get(0).name());
        LlmMessage rejection = repairedMessages.get(repairIndex + 1);
        assertEquals(LlmMessageRole.TOOL, rejection.role());
        assertTrue(rejection.content().contains("\"status\":\"rejected\""));
        assertTrue(rejection.content().contains("exact parameter schema"));
    }

    @Test
    void optionalNullIsNormalizedToOmissionWithoutRepair() throws Exception {
        LlmToolDefinition search = new LlmToolDefinition("search", "Search", "",
                List.of(
                        new ActionParameterSpec("key", "string", true, "Commodity", List.of(), null),
                        new ActionParameterSpec("max_distance", "number", false,
                                "Optional radius", List.of(), null)));
        LlmRequest request = new LlmRequest("req-optional-null",
                List.of(LlmMessage.of(LlmMessageRole.USER, "find gold")),
                List.of(search), PromptCacheProfile.COMMANDER);
        JsonObject arguments = new JsonObject();
        arguments.addProperty("key", "gold");
        arguments.add("max_distance", null);

        LlmResult result = run(new ScriptedAdapter(ok("search", arguments)), request);

        assertTrue(result.isValid());
        assertEquals(1, sends.get(), "a harmless optional null must not spend the repair attempt");
        assertFalse(result.toolInvocations().get(0).arguments().has("max_distance"));
    }

    @Test
    void settlingOnlyContinuationRejectsClassificationWithAnExtraSettlingCall() throws Exception {
        LlmResult result = run(new ScriptedAdapter(ok("speak"), ok("classify_turn", "speak")),
                requestOffering("speak", "classify_turn"));

        assertFalse(result.isValid());
        assertEquals(LlmResult.Status.INVALID_RESPONSE, result.status());
        assertEquals(2, sends.get());
    }

    @Test
    void classifyingTurnRequiresExactlyOneSettlingCall() throws Exception {
        LlmResult result = run(new ScriptedAdapter(ok("classify_turn", "speak", "speak"),
                        ok("classify_turn", "speak")),
                requestOffering("speak", "classify_turn"));

        assertTrue(result.isValid());
        assertEquals(List.of("classify_turn", "speak"),
                result.toolInvocations().stream().map(LlmToolInvocation::name).toList());
        assertEquals(2, sends.get());
    }

    @Test
    void classifyOnlyContinuesWithPendingToolResult() throws Exception {
        ScriptedAdapter adapter = new ScriptedAdapter(ok("classify_turn"), ok("speak"));
        LlmRequest request = requestWithMessages(
                List.of(
                        LlmMessage.of(LlmMessageRole.SYSTEM, "rules"),
                        LlmMessage.of(LlmMessageRole.USER, "go")),
                "speak", "classify_turn");
        LlmResult result = run(adapter, request);

        assertTrue(result.isValid());
        assertEquals(2, sends.get());
        assertEquals(List.of("classify_turn", "speak"),
                result.toolInvocations().stream().map(LlmToolInvocation::name).toList());
        assertPendingClassificationContinuation(request, adapter.lastRequest, "c1");
    }

    @Test
    void lmStudioClassifyThenNavigationResponsesCompleteOneLogicalRequest() throws Exception {
        JsonObject classifyResponse = JsonParser.parseString("""
                {
                  "id": "chatcmpl-48y8ierfdmbzx6ftg8lc6e",
                  "object": "chat.completion",
                  "created": 1783786749,
                  "model": "google/gemma-4-e4b",
                  "choices": [{
                    "index": 0,
                    "message": {
                      "role": "assistant",
                      "content": "",
                      "tool_calls": [{
                        "type": "function",
                        "id": "681212727",
                        "function": {
                          "name": "classify_turn",
                          "arguments": "{\\\"canonical_fact\\\":\\\"\\\",\\\"importance\\\":\\\"normal\\\",\\\"is_question\\\":false,\\\"topic\\\":\\\"navigation\\\"}"
                        }
                      }]
                    },
                    "logprobs": null,
                    "finish_reason": "tool_calls"
                  }],
                  "usage": {
                    "prompt_tokens": 1966,
                    "completion_tokens": 34,
                    "total_tokens": 2000,
                    "completion_tokens_details": {"reasoning_tokens": 0}
                  },
                  "stats": {},
                  "system_fingerprint": "google/gemma-4-e4b"
                }
                """).getAsJsonObject();
        JsonObject navigationResponse = JsonParser.parseString("""
                {
                  "id": "chatcmpl-9c6emb9u6hbltt3ckzd1i",
                  "object": "chat.completion",
                  "created": 1783786751,
                  "model": "google/gemma-4-e4b",
                  "choices": [{
                    "index": 0,
                    "message": {
                      "role": "assistant",
                      "content": "",
                      "tool_calls": [{
                        "type": "function",
                        "id": "318357260",
                        "function": {
                          "name": "navigate_to_squadron_carrier",
                          "arguments": "{}"
                        }
                      }]
                    },
                    "logprobs": null,
                    "finish_reason": "tool_calls"
                  }],
                  "usage": {
                    "prompt_tokens": 2025,
                    "completion_tokens": 15,
                    "total_tokens": 2040,
                    "completion_tokens_details": {"reasoning_tokens": 0}
                  },
                  "stats": {},
                  "system_fingerprint": "google/gemma-4-e4b"
                }
                """).getAsJsonObject();
        Deque<JsonObject> providerResponses = new ArrayDeque<>(List.of(classifyResponse, navigationResponse));
        List<JsonObject> renderedRequests = new ArrayList<>();
        LlmTransport transport = requestBody -> {
            renderedRequests.add(JsonParser.parseString(requestBody).getAsJsonObject());
            return providerResponses.removeFirst();
        };
        LlmRequest request = new LlmRequest("req-lm-studio",
                List.of(
                        LlmMessage.of(LlmMessageRole.SYSTEM, "rules"),
                        LlmMessage.of(LlmMessageRole.USER, "navigate to the squadron carrier")),
                List.of(
                        new LlmToolDefinition(ClassifyTurnFunction.ID, "Classify", "",
                                new ClassifyTurnFunction().parameters()),
                        new LlmToolDefinition("navigate_to_squadron_carrier", "Navigate", "", List.of())),
                PromptCacheProfile.COMMANDER);

        LlmResult result = new CompanionLlmGateway(
                new LmStudioLlmAdapter("google/gemma-4-e4b"), transport, Runnable::run)
                .submit(request)
                .get();

        assertTrue(result.isValid());
        assertEquals(List.of("classify_turn", "navigate_to_squadron_carrier"),
                result.toolInvocations().stream().map(LlmToolInvocation::name).toList());
        assertEquals(List.of("681212727", "318357260"),
                result.toolInvocations().stream().map(LlmToolInvocation::id).toList());
        assertEquals("navigation", result.toolInvocations().get(0).arguments().get("topic").getAsString());
        assertTrue(result.toolInvocations().get(1).arguments().isEmpty());
        assertEquals(2, renderedRequests.size());

        JsonArray continuationMessages = renderedRequests.get(1).getAsJsonArray("messages");
        JsonObject replayedClassify = continuationMessages.get(2).getAsJsonObject()
                .getAsJsonArray("tool_calls").get(0).getAsJsonObject();
        JsonObject pendingToolResult = continuationMessages.get(3).getAsJsonObject();
        assertEquals("681212727", replayedClassify.get("id").getAsString());
        assertEquals("classify_turn", replayedClassify.getAsJsonObject("function").get("name").getAsString());
        assertEquals("tool", pendingToolResult.get("role").getAsString());
        assertEquals("681212727", pendingToolResult.get("tool_call_id").getAsString());
        assertTrue(pendingToolResult.get("content").getAsString().contains("\"execution\":\"pending\""));
    }

    @Test
    void classifyOnlyContinuationSynthesizesAnIdWhenProviderOmitsOne() throws Exception {
        LlmResult classify = new LlmResult(LlmResult.Status.OK,
                List.of(new LlmToolInvocation(null, "classify_turn", new JsonObject())));
        ScriptedAdapter adapter = new ScriptedAdapter(classify, ok("speak"));
        LlmRequest request = requestOffering("speak", "classify_turn");

        LlmResult result = run(adapter, request);

        assertTrue(result.isValid());
        assertEquals(List.of("classify_turn", "speak"),
                result.toolInvocations().stream().map(LlmToolInvocation::name).toList());
        assertEquals("gateway-classify-1", result.toolInvocations().get(0).id());
        assertEquals(2, sends.get());
        assertPendingClassificationContinuation(request, adapter.lastRequest, "gateway-classify-1");
    }

    @Test
    void repairCanContinueThroughAClassifyOnlyRound() throws Exception {
        ScriptedAdapter adapter = new ScriptedAdapter(
                ok("speak", "interrupt"), ok("classify_turn"), ok("speak"));
        LlmRequest request = requestOffering("speak", "interrupt", "classify_turn");

        LlmResult result = run(adapter, request);

        assertTrue(result.isValid());
        assertEquals(List.of("classify_turn", "speak"),
                result.toolInvocations().stream().map(LlmToolInvocation::name).toList());
        assertEquals("gateway-classify-1", result.toolInvocations().get(0).id());
        assertEquals(3, sends.get());
        List<LlmMessage> continuation = adapter.lastRequest.messages();
        assertEquals(List.of("c1", "c2", "gateway-classify-1"), continuation.stream()
                .flatMap(message -> message.toolCalls().stream())
                .map(LlmToolInvocation::id)
                .toList(), "every assistant tool call in the local flow must have a distinct id");
        assertEquals(List.of("c1", "c2", "gateway-classify-1"), continuation.stream()
                .map(LlmMessage::toolCallId)
                .filter(java.util.Objects::nonNull)
                .toList(), "each tool result must link to its corresponding unique call");
    }

    @Test
    void classifyOnlyContinuationRejectsMultipleSettlingCalls() throws Exception {
        LlmResult result = run(new ScriptedAdapter(ok("classify_turn"), ok("speak", "speak")),
                requestOffering("speak", "classify_turn"));

        assertFalse(result.isValid());
        assertEquals(LlmResult.Status.INVALID_RESPONSE, result.status());
        assertEquals(2, sends.get());
    }

    @Test
    void classifyOnlyContinuationRejectsAnotherClassifyTurn() throws Exception {
        LlmResult result = run(new ScriptedAdapter(ok("classify_turn"), ok("classify_turn")),
                requestOffering("speak", "classify_turn"));

        assertFalse(result.isValid());
        assertEquals(LlmResult.Status.INVALID_RESPONSE, result.status());
        assertEquals(2, sends.get());
    }

    @Test
    void settlingOnlyContinuationSynthesizesAnIdWhenProviderOmitsOne() throws Exception {
        LlmResult first = new LlmResult(LlmResult.Status.OK,
                List.of(new LlmToolInvocation(null, "speak", new JsonObject())));
        ScriptedAdapter adapter = new ScriptedAdapter(first, ok("classify_turn"));
        LlmRequest request = requestWithMessages(
                List.of(
                        LlmMessage.of(LlmMessageRole.SYSTEM, "rules"),
                        LlmMessage.of(LlmMessageRole.USER, "go")),
                "speak", "classify_turn");

        LlmResult result = run(adapter, request);

        assertTrue(result.isValid());
        assertEquals(List.of("classify_turn", "speak"),
                result.toolInvocations().stream().map(LlmToolInvocation::name).toList());
        assertEquals("gateway-settling-1", result.toolInvocations().get(1).id());
        assertPendingSettlingContinuation(request, adapter.lastRequest, "speak", "gateway-settling-1");
    }

    @Test
    void continuationAvoidsCollidingWithProviderIssuedIds() throws Exception {
        LlmResult first = new LlmResult(LlmResult.Status.OK, List.of(
                new LlmToolInvocation("repair-rejected-call-1", "speak", new JsonObject()),
                new LlmToolInvocation(null, "interrupt", new JsonObject())));
        ScriptedAdapter adapter = new ScriptedAdapter(first, ok("classify_turn", "speak"));
        LlmRequest request = requestWithMessages(
                List.of(
                        LlmMessage.of(LlmMessageRole.SYSTEM, "rules"),
                        LlmMessage.of(LlmMessageRole.USER, "go")),
                "speak", "interrupt", "classify_turn");

        LlmResult result = run(adapter, request);

        assertTrue(result.isValid());
        List<LlmMessage> messages = adapter.lastRequest.messages();
        assertEquals(List.of("repair-rejected-call-1", "repair-rejected-call-2"),
                messages.get(2).toolCalls().stream().map(LlmToolInvocation::id).toList());
        assertEquals(List.of("repair-rejected-call-1", "repair-rejected-call-2"),
                messages.subList(3, 5).stream().map(LlmMessage::toolCallId).toList());
    }

    @Test
    void turnWithoutClassifyOfferedNeverRequiresIt() throws Exception {
        // A narration-style turn does not offer classify_turn, so a speak-only response is fine as-is.
        LlmResult result = run(new ScriptedAdapter(ok("speak")), requestOffering("speak"));

        assertTrue(result.isValid());
        assertEquals(1, sends.get());
    }

    @Test
    void malformedResponseRetriesWithoutChangingThePrompt() throws Exception {
        ScriptedAdapter adapter = new ScriptedAdapter(invalid(), invalid());
        LlmRequest request = requestWithMessages(
                List.of(
                        LlmMessage.of(LlmMessageRole.SYSTEM, "rules"),
                        LlmMessage.of(LlmMessageRole.USER, "go")),
                "speak");

        run(adapter, request);

        List<LlmMessage> retried = adapter.lastRequest.messages();
        assertEquals(request.messages(), retried);
    }

    /** Verifies the genuine classify-only continuation, which is pending rather than rejected or executed. */
    private static void assertPendingClassificationContinuation(
            LlmRequest original,
            LlmRequest continuation,
            String classifyCallId
    ) {
        int continuationIndex = original.messages().size();
        List<LlmMessage> messages = continuation.messages();
        assertEquals(continuationIndex + 2, messages.size());
        assertEquals(original.messages(), messages.subList(0, continuationIndex),
                "continuation must preserve the durable prompt prefix byte-for-byte");
        assertEquals(LlmMessageRole.ASSISTANT, messages.get(continuationIndex).role());
        assertEquals(1, messages.get(continuationIndex).toolCalls().size());
        assertEquals("classify_turn", messages.get(continuationIndex).toolCalls().get(0).name());
        assertEquals(classifyCallId, messages.get(continuationIndex).toolCalls().get(0).id());
        LlmMessage tool = messages.get(continuationIndex + 1);
        assertEquals(LlmMessageRole.TOOL, tool.role());
        assertEquals(classifyCallId, tool.toolCallId());
        assertTrue(tool.content().contains("\"status\":\"received\""));
        assertTrue(tool.content().contains("\"execution\":\"pending\""));
        assertTrue(tool.content().contains("call exactly one settling function"));
        assertFalse(tool.content().contains("rejected"));
    }

    /** Verifies a settling-only continuation, which keeps the action pending while requesting classification. */
    private static void assertPendingSettlingContinuation(
            LlmRequest original,
            LlmRequest continuation,
            String settlingToolName,
            String settlingCallId
    ) {
        int continuationIndex = original.messages().size();
        List<LlmMessage> messages = continuation.messages();
        assertEquals(continuationIndex + 2, messages.size());
        assertEquals(original.messages(), messages.subList(0, continuationIndex),
                "continuation must preserve the durable prompt prefix byte-for-byte");
        assertEquals(LlmMessageRole.ASSISTANT, messages.get(continuationIndex).role());
        assertEquals(1, messages.get(continuationIndex).toolCalls().size());
        assertEquals(settlingToolName, messages.get(continuationIndex).toolCalls().get(0).name());
        assertEquals(settlingCallId, messages.get(continuationIndex).toolCalls().get(0).id());
        LlmMessage tool = messages.get(continuationIndex + 1);
        assertEquals(LlmMessageRole.TOOL, tool.role());
        assertEquals(settlingCallId, tool.toolCallId());
        assertTrue(tool.content().contains("\"status\":\"received\""));
        assertTrue(tool.content().contains("\"execution\":\"pending\""));
        assertTrue(tool.content().contains("call classify_turn only"));
        assertFalse(tool.content().contains("rejected"));
    }
}
