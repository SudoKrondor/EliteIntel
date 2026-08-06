package elite.intel.ai.brain.vega.llm;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.AiTransportResult;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.vega.model.llm.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CompanionLlmGatewayTest {

    private static final LlmTransport SUCCESS_TRANSPORT = body -> new JsonObject();

    private static LlmRequest request(LlmToolDefinition... tools) {
        return new LlmRequest(
                "req-1",
                List.of(
                        LlmMessage.of(LlmMessageRole.SYSTEM, "rules"),
                        LlmMessage.of(LlmMessageRole.USER, "do it")),
                List.of(tools),
                PromptCacheProfile.COMMANDER);
    }

    private static LlmToolDefinition tool(String name) {
        return new LlmToolDefinition(name, "description", "", List.of());
    }

    private static LlmResult calls(String... names) {
        List<LlmToolInvocation> invocations = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            invocations.add(new LlmToolInvocation("call-" + i, names[i], new JsonObject()));
        }
        return new LlmResult(LlmResult.Status.OK, invocations);
    }

    private static LlmResult call(String name, JsonObject arguments) {
        return new LlmResult(LlmResult.Status.OK,
                List.of(new LlmToolInvocation("call-1", name, arguments)));
    }

    private static LlmResult malformed() {
        return new LlmResult(LlmResult.Status.INVALID_RESPONSE, List.of());
    }

    private static LlmResult run(ScriptedAdapter adapter, LlmRequest request) throws Exception {
        return new CompanionLlmGateway(adapter, SUCCESS_TRANSPORT, Runnable::run).submit(request).get();
    }

    @Test
    void acceptsExactlyOneOfferedCallOnFirstAttempt() throws Exception {
        ScriptedAdapter adapter = new ScriptedAdapter(calls("speak"));

        LlmResult result = run(adapter, request(tool("speak")));

        assertTrue(result.isValid());
        assertEquals("speak", result.toolInvocations().get(0).name());
        assertEquals(1, adapter.requests.size());
    }

    @Test
    void rejectsMultipleCallsAndAcceptsSingleCallRepair() throws Exception {
        ScriptedAdapter adapter = new ScriptedAdapter(
                calls("remember", "speak"), calls("remember"));
        LlmRequest original = request(tool("remember"), tool("speak"));

        LlmResult result = run(adapter, original);

        assertTrue(result.isValid());
        assertEquals(List.of("remember"), result.toolInvocations().stream()
                .map(LlmToolInvocation::name).toList());
        assertEquals(original.messages(), adapter.requests.get(1).messages(),
                "multi-call output is malformed and must not be replayed as history");
    }

    @Test
    void twoInvalidResponsesReturnInvalidResult() throws Exception {
        LlmResult result = run(
                new ScriptedAdapter(calls("speak", "remember"), malformed()),
                request(tool("speak"), tool("remember")));

        assertEquals(LlmResult.Status.INVALID_RESPONSE, result.status());
        assertTrue(result.toolInvocations().isEmpty());
    }

    @Test
    void unofferedCallGetsTruthfulRejectedResultBeforeRepair() throws Exception {
        ScriptedAdapter adapter = new ScriptedAdapter(calls("invented"), calls("speak"));
        LlmRequest original = request(tool("speak"));

        LlmResult result = run(adapter, original);

        assertTrue(result.isValid());
        List<LlmMessage> repaired = adapter.requests.get(1).messages();
        assertEquals(original.messages(), repaired.subList(0, original.messages().size()));
        LlmMessage replay = repaired.get(original.messages().size());
        LlmMessage rejection = repaired.get(original.messages().size() + 1);
        assertEquals("invented", replay.toolCalls().get(0).name());
        assertEquals(replay.toolCalls().get(0).id(), rejection.toolCallId());
        assertTrue(rejection.content().contains("not listed"));
        assertTrue(rejection.content().contains("exactly one listed function"));
    }

    @Test
    void schemaInvalidCallGetsActionableRepair() throws Exception {
        LlmToolDefinition navigate = new LlmToolDefinition(
                "navigate", "description", "", List.of(
                new ActionParameterSpec("target", "string", true, "destination", List.of("Sol"), null)));
        JsonObject invalid = new JsonObject();
        invalid.addProperty("destination", "Sol");
        JsonObject valid = new JsonObject();
        valid.addProperty("target", "Sol");
        ScriptedAdapter adapter = new ScriptedAdapter(call("navigate", invalid), call("navigate", valid));

        LlmResult result = run(adapter, request(navigate, tool("speak")));

        assertEquals("Sol", result.toolInvocations().get(0).arguments().get("target").getAsString());
        String rejection = adapter.requests.get(1).messages().get(3).content();
        assertTrue(rejection.contains("accepts only these argument fields: target"));
        assertEquals(List.of("navigate"), adapter.requests.get(1).tools().stream()
                .map(LlmToolDefinition::name).toList());
    }

    /**
     * Asking for input on an action that declares no required parameter is provably wrong - nothing can be
     * missing - and the model already named its choice in {@code action_id}. Observed with a small local model:
     * "enter next fleet carrier destination" produced a mis-shaped request_input (which is not even offered when
     * nothing takes an argument), and the free retry answered the order with sarcasm because speak was still on
     * the table. The repair now aims at the action itself.
     */
    @Test
    void inputRequestForAParameterlessActionIsRepairedIntoThatAction() throws Exception {
        JsonObject invalid = new JsonObject();
        invalid.addProperty("action_id", "enter_fleet_carrier_destination");
        invalid.addProperty("missing_parameter_name", "system name");
        ScriptedAdapter adapter = new ScriptedAdapter(
                call("request_input", invalid), calls("enter_fleet_carrier_destination"));

        LlmResult result = run(adapter, request(tool("enter_fleet_carrier_destination"), tool("speak")));

        assertEquals("enter_fleet_carrier_destination", result.toolInvocations().get(0).name());
        LlmRequest repair = adapter.requests.get(1);
        assertEquals(List.of("enter_fleet_carrier_destination"),
                repair.tools().stream().map(LlmToolDefinition::name).toList(),
                "speak must not remain available to abandon the order");
        assertTrue(repair.messages().get(3).content().contains("declares no required parameter"));
    }

    @Test
    void inputRequestForAnActionThatDoesNeedAnArgumentIsRepairedAsItself() throws Exception {
        LlmToolDefinition navigate = new LlmToolDefinition(
                "navigate", "description", "", List.of(
                new ActionParameterSpec("target", "string", true, "destination", List.of(), null)));
        LlmToolDefinition requestInput = new LlmToolDefinition(
                "request_input", "ask for one missing parameter", "", List.of(
                new ActionParameterSpec("action_id", "string", true, "action", List.of(), null),
                new ActionParameterSpec("parameter_name", "string", true, "parameter", List.of(), null),
                new ActionParameterSpec("question", "string", true, "question", List.of(), null)));
        JsonObject invalid = new JsonObject();
        invalid.addProperty("action_id", "navigate");
        invalid.addProperty("missing_parameter_name", "target");
        JsonObject valid = new JsonObject();
        valid.addProperty("action_id", "navigate");
        valid.addProperty("parameter_name", "target");
        valid.addProperty("question", "Which system?");
        ScriptedAdapter adapter = new ScriptedAdapter(
                call("request_input", invalid), call("request_input", valid));

        LlmResult result = run(adapter, request(navigate, requestInput, tool("speak")));

        assertEquals("request_input", result.toolInvocations().get(0).name());
        assertEquals(List.of("request_input"), adapter.requests.get(1).tools().stream()
                        .map(LlmToolDefinition::name).toList(),
                "an action with a genuinely missing argument must still be able to ask");
    }

    @Test
    void optionalNullIsNormalizedToOmissionWithoutRepair() throws Exception {
        LlmToolDefinition navigate = new LlmToolDefinition(
                "navigate", "description", "", List.of(
                new ActionParameterSpec("max_distance", "number", false, "limit", List.of(), null)));
        JsonObject arguments = new JsonObject();
        arguments.add("max_distance", null);
        ScriptedAdapter adapter = new ScriptedAdapter(call("navigate", arguments));

        LlmResult result = run(adapter, request(navigate));

        assertTrue(result.isValid());
        assertFalse(result.toolInvocations().get(0).arguments().has("max_distance"));
        assertEquals(1, adapter.requests.size());
    }

    @Test
    void malformedParseRetriesWithoutChangingPrompt() throws Exception {
        ScriptedAdapter adapter = new ScriptedAdapter(malformed(), malformed());
        LlmRequest original = request(tool("speak"));

        run(adapter, original);

        assertEquals(original.messages(), adapter.requests.get(1).messages());
    }

    @Test
    void permanentTransportFailureSkipsProtocolRepair() throws Exception {
        AtomicInteger sends = new AtomicInteger();
        LlmTransport transport = outcomeTransport(sends,
                AiTransportResult.failure(AiTransportResult.FailureKind.PERMANENT, 401, "invalid key"));
        CompanionLlmGateway gateway = new CompanionLlmGateway(
                new ScriptedAdapter(calls("speak")), transport, Runnable::run);

        LlmResult result = gateway.submit(request(tool("speak"))).get();

        assertEquals(LlmResult.Status.INVALID_RESPONSE, result.status());
        assertEquals(1, sends.get());
    }

    @Test
    void transientTransportFailureRetriesPhysicalSendBeforeParsing() throws Exception {
        AtomicInteger sends = new AtomicInteger();
        LlmTransport transport = outcomeTransport(sends,
                AiTransportResult.failure(AiTransportResult.FailureKind.TRANSIENT, 429, "rate limited"),
                AiTransportResult.success(new JsonObject()));
        CompanionLlmGateway gateway = new CompanionLlmGateway(
                new ScriptedAdapter(calls("speak")), transport, Runnable::run);

        LlmResult result = gateway.submit(request(tool("speak"))).get();

        assertTrue(result.isValid());
        assertEquals(2, sends.get());
    }

    @Test
    void cancellationInterruptsPhysicalTransport() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        LlmTransport blocking = body -> {
            started.countDown();
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
            } catch (InterruptedException failure) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
                throw new CancellationException("interrupted");
            }
            return new JsonObject();
        };
        ExecutorService worker = Executors.newSingleThreadExecutor();
        CompanionLlmGateway gateway = new CompanionLlmGateway(
                new ScriptedAdapter(calls("speak")), blocking, worker, Duration.ofSeconds(5));
        CompletableFuture<LlmResult> future = gateway.submit(request(tool("speak")));
        assertTrue(started.await(1, TimeUnit.SECONDS));

        future.cancel(true);
        worker.shutdown();
        assertTrue(worker.awaitTermination(1, TimeUnit.SECONDS));

        assertTrue(interrupted.get());
        gateway.close();
    }

    @Test
    void logicalDeadlineInterruptsStalledTransport() throws Exception {
        LlmTransport blocking = body -> {
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new CancellationException("interrupted");
            }
            return new JsonObject();
        };
        ExecutorService worker = Executors.newSingleThreadExecutor();
        CompanionLlmGateway gateway = new CompanionLlmGateway(
                new ScriptedAdapter(calls("speak")), blocking, worker, Duration.ofMillis(50));

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> gateway.submit(request(tool("speak"))).get(1, TimeUnit.SECONDS));

        assertInstanceOf(TimeoutException.class, failure.getCause());
        gateway.close();
    }

    private static LlmTransport outcomeTransport(
            AtomicInteger calls,
            AiTransportResult... scriptedOutcomes
    ) {
        Deque<AiTransportResult> outcomes = new ArrayDeque<>(List.of(scriptedOutcomes));
        return new LlmTransport() {
            @Override
            public JsonObject send(String requestBody) {
                throw new UnsupportedOperationException();
            }

            @Override
            public AiTransportResult sendOutcome(String requestBody) {
                calls.incrementAndGet();
                AiTransportResult outcome = outcomes.pollFirst();
                if (outcome == null) {
                    throw new AssertionError("Unexpected transport send");
                }
                return outcome;
            }
        };
    }

    private static final class ScriptedAdapter implements LlmProviderAdapter {
        private final Deque<LlmResult> results = new ArrayDeque<>();
        private final List<LlmRequest> requests = new ArrayList<>();

        private ScriptedAdapter(LlmResult... results) {
            this.results.addAll(List.of(results));
        }

        @Override
        public String buildRequestBody(LlmRequest request) {
            requests.add(request);
            return "{}";
        }

        @Override
        public LlmResult parse(JsonObject response) {
            return results.removeFirst();
        }

        @Override
        public String parseText(JsonObject response) {
            return null;
        }
    }
}
