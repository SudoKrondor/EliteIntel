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

    /**
     * A request from a caller that settles several calls per round, as a commander turn does.
     */
    private static LlmRequest request(int maxToolCalls, LlmToolDefinition... tools) {
        LlmRequest single = request(tools);
        return new LlmRequest(single.requestId(), single.messages(), single.tools(), single.profile(),
                null, maxToolCalls);
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

    /**
     * Several calls at once is an arity error, not a malformed response: the model parsed, chose offered
     * functions and only asked for too many. Observed with mistral-small answering "check the loadout, what is
     * our cargo capacity" with two valid queries - repairing against the functions it named turns a lost turn
     * into an answer.
     */
    @Test
    void rejectsMultipleCallsAndAcceptsSingleCallRepair() throws Exception {
        ScriptedAdapter adapter = new ScriptedAdapter(
                calls("remember", "speak"), calls("remember"));
        LlmRequest original = request(tool("remember"), tool("speak"), tool("navigate"));

        LlmResult result = run(adapter, original);

        assertTrue(result.isValid());
        assertEquals(List.of("remember"), result.toolInvocations().stream()
                .map(LlmToolInvocation::name).toList());
        List<LlmMessage> repaired = adapter.requests.get(1).messages();
        assertEquals(original.messages(), repaired.subList(0, original.messages().size()));
        LlmMessage replay = repaired.get(original.messages().size());
        assertEquals(List.of("remember", "speak"), replay.toolCalls().stream()
                .map(LlmToolInvocation::name).toList(), "both calls are replayed so each can be rejected");
        for (int i = 0; i < replay.toolCalls().size(); i++) {
            LlmMessage rejection = repaired.get(original.messages().size() + 1 + i);
            assertEquals(replay.toolCalls().get(i).id(), rejection.toolCallId());
            assertTrue(rejection.content().contains("No call was executed"));
            assertTrue(rejection.content().contains("exactly one function"));
        }
        assertEquals(List.of("remember", "speak"), adapter.requests.get(1).tools().stream()
                        .map(LlmToolDefinition::name).toList(),
                "the repair only has to pick between the functions the model itself named");
    }

    /**
     * One intent stated twice is not two actions, so it needs no round trip to resolve.
     */
    @Test
    void repeatedIdenticalCallCollapsesToThatCall() throws Exception {
        ScriptedAdapter adapter = new ScriptedAdapter(calls("remember", "remember"));

        LlmResult result = run(adapter, request(tool("remember"), tool("speak")));

        assertTrue(result.isValid());
        assertEquals(List.of("remember"), result.toolInvocations().stream()
                .map(LlmToolInvocation::name).toList());
        assertEquals(1, adapter.requests.size(), "an identical repeat is collapsed, not repaired");
    }

    /**
     * A model that will not narrow down still gets its first choice carried out rather than losing the turn.
     */
    @Test
    void persistentMultiCallExecutesFirstNamedCall() throws Exception {
        ScriptedAdapter adapter = new ScriptedAdapter(
                calls("remember", "speak"), calls("remember", "speak"));

        LlmResult result = run(adapter, request(tool("remember"), tool("speak")));

        assertTrue(result.isValid());
        assertEquals(List.of("remember"), result.toolInvocations().stream()
                .map(LlmToolInvocation::name).toList());
        assertEquals(2, adapter.requests.size());
    }

    /**
     * A caller that settles a batch gets the batch: the arity is the request's, not the gateway's.
     */
    @Test
    void severalCallsAreAcceptedWhenTheCallerSettlesThem() throws Exception {
        ScriptedAdapter adapter = new ScriptedAdapter(calls("remember", "speak"));

        LlmResult result = run(adapter, request(3, tool("remember"), tool("speak")));

        assertTrue(result.isValid());
        assertEquals(List.of("remember", "speak"), result.toolInvocations().stream()
                .map(LlmToolInvocation::name).toList());
        assertEquals(1, adapter.requests.size(), "a response within the allowance needs no repair");
    }

    /**
     * The allowance is a ceiling, so overshooting it is still repaired - down to the ceiling, not to one.
     */
    @Test
    void callsBeyondTheAllowanceAreRepairedDownToIt() throws Exception {
        ScriptedAdapter adapter = new ScriptedAdapter(
                calls("remember", "speak", "navigate"), calls("remember", "navigate"));

        LlmResult result = run(adapter, request(2, tool("remember"), tool("speak"), tool("navigate")));

        assertTrue(result.isValid());
        assertEquals(List.of("remember", "navigate"), result.toolInvocations().stream()
                .map(LlmToolInvocation::name).toList());
        assertTrue(adapter.requests.get(1).messages().get(3).content().contains("at most 2 functions"));
    }

    /**
     * Overshooting twice keeps the calls the model itself put first, up to what the caller can settle.
     */
    @Test
    void persistentOvershootKeepsTheFirstCallsWithinTheAllowance() throws Exception {
        ScriptedAdapter adapter = new ScriptedAdapter(
                calls("remember", "speak", "navigate"), calls("remember", "speak", "navigate"));

        LlmResult result = run(adapter, request(2, tool("remember"), tool("speak"), tool("navigate")));

        assertTrue(result.isValid());
        assertEquals(List.of("remember", "speak"), result.toolInvocations().stream()
                .map(LlmToolInvocation::name).toList());
    }

    /**
     * Repeats are dropped, but the same function asked about two different things is two answers.
     */
    @Test
    void deduplicationKeepsDistinctArgumentsOfTheSameFunction() throws Exception {
        LlmToolDefinition navigate = new LlmToolDefinition(
                "navigate", "description", "", List.of(
                new ActionParameterSpec("target", "string", false, "destination", List.of(), null)));
        JsonObject sol = new JsonObject();
        sol.addProperty("target", "Sol");
        JsonObject colonia = new JsonObject();
        colonia.addProperty("target", "Colonia");
        LlmResult twoSystemsAndARepeat = new LlmResult(LlmResult.Status.OK, List.of(
                new LlmToolInvocation("call-1", "navigate", sol),
                new LlmToolInvocation("call-2", "navigate", colonia),
                new LlmToolInvocation("call-3", "navigate", sol)));
        ScriptedAdapter adapter = new ScriptedAdapter(twoSystemsAndARepeat);

        LlmResult result = run(adapter, request(2, navigate));

        assertTrue(result.isValid());
        assertEquals(2, result.toolInvocations().size());
        assertEquals(List.of("Sol", "Colonia"), result.toolInvocations().stream()
                .map(call -> call.arguments().get("target").getAsString()).toList());
        assertEquals(1, adapter.requests.size(), "dropping the repeat brought it within the allowance");
    }

    /**
     * Naming a function that was never offered is a bad choice, not a bad count; the whole list stays open.
     */
    @Test
    void multiCallNamingUnofferedFunctionKeepsEveryOfferedFunction() throws Exception {
        ScriptedAdapter adapter = new ScriptedAdapter(calls("remember", "invented"), calls("speak"));

        LlmResult result = run(adapter, request(tool("remember"), tool("speak")));

        assertTrue(result.isValid());
        assertEquals(List.of("remember", "speak"), adapter.requests.get(1).tools().stream()
                .map(LlmToolDefinition::name).toList());
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

        assertEquals(LlmResult.Status.SERVICE_UNAVAILABLE, result.status());
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

    /**
     * The blip that motivated the ladder: a provider shedding load answers 503 to the first sends and
     * recovers a moment later, so the turn must survive rather than end on the first failed resend.
     */
    @Test
    void transientTransportFailureKeepsResendingAlongTheBackoffLadder() throws Exception {
        AtomicInteger sends = new AtomicInteger();
        LlmTransport transport = outcomeTransport(sends,
                AiTransportResult.failure(AiTransportResult.FailureKind.TRANSIENT, 503, "overloaded"),
                AiTransportResult.failure(AiTransportResult.FailureKind.TRANSIENT, 503, "overloaded"),
                AiTransportResult.failure(AiTransportResult.FailureKind.TRANSIENT, 503, "overloaded"),
                AiTransportResult.success(new JsonObject()));
        CompanionLlmGateway gateway = new CompanionLlmGateway(
                new ScriptedAdapter(calls("speak")), transport, Runnable::run);

        LlmResult result = gateway.submit(request(tool("speak"))).get();

        assertTrue(result.isValid());
        assertEquals(4, sends.get());
    }

    /**
     * The ladder is bounded: an outage that outlives it ends the turn as an unreachable service, not as an
     * unusable response, so the commander is told the provider is down rather than that the request is impossible.
     */
    @Test
    void transientTransportFailureOutlivingTheLadderReportsServiceUnavailable() throws Exception {
        AtomicInteger sends = new AtomicInteger();
        AiTransportResult overloaded =
                AiTransportResult.failure(AiTransportResult.FailureKind.TRANSIENT, 503, "overloaded");
        LlmTransport transport = outcomeTransport(sends,
                overloaded, overloaded, overloaded, overloaded);
        CompanionLlmGateway gateway = new CompanionLlmGateway(
                new ScriptedAdapter(calls("speak")), transport, Runnable::run);

        LlmResult result = gateway.submit(request(tool("speak"))).get();

        assertEquals(LlmResult.Status.SERVICE_UNAVAILABLE, result.status());
        assertFalse(result.isValid());
        assertEquals(4, sends.get()); // the initial send plus the whole ladder, and no protocol repair
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

    /**
     * A rate limiter that names its own cooldown is obeyed rather than guessed at - but only up to the ladder's
     * ceiling, because a companion that goes silent for the ten seconds a saturated provider might ask for is
     * worse company than one that admits the service is unreachable.
     */
    @Test
    void retryAfterAdviceIsHonouredUpToTheLaddersCeiling() {
        long[] shortestRung = {250, 750};

        assertEquals(3_000, CompanionLlmGateway.retryDelayMillis(rateLimited(10_000L), shortestRung),
                "advice longer than the ceiling must be clamped, not obeyed");
        assertEquals(1_500, CompanionLlmGateway.retryDelayMillis(rateLimited(1_500L), shortestRung),
                "advice inside the ceiling must be taken over the jittered rung");

        long ignoredAdvice = CompanionLlmGateway.retryDelayMillis(rateLimited(10L), shortestRung);
        assertTrue(ignoredAdvice >= 250 && ignoredAdvice <= 750,
                "advice shorter than the rung must not shorten the wait, was " + ignoredAdvice);

        long unadvised = CompanionLlmGateway.retryDelayMillis(rateLimited(null), shortestRung);
        assertTrue(unadvised >= 250 && unadvised <= 750,
                "a provider that advises nothing must leave the rung alone, was " + unadvised);
    }

    private static AiTransportResult.Failure rateLimited(Long retryAfterMillis) {
        return new AiTransportResult.Failure(
                AiTransportResult.FailureKind.TRANSIENT, 429, "rate limited", retryAfterMillis);
    }
}
