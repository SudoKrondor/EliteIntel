package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.memory.MemoryAvailabilitySnapshot;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.Verbosity;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.IntelActionAccessPolicy;
import elite.intel.companion.prompt.PromptComposer;
import elite.intel.companion.prompt.ReflexResolver;
import elite.intel.companion.speech.SpeechGateway;
import elite.intel.companion.tools.IntelActionTypeResolver;
import elite.intel.companion.tools.NothingToDoFunction;
import elite.intel.companion.tools.SpeakFunction;
import elite.intel.companion.tools.SystemFunctionProvider;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.journal.events.BaseEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lane scheduling: a submitted input runs a thought to a memory write, the two sources use separate lanes,
 * blank input and input racing lifecycle (before start / after stop) are ignored, an urgent thought
 * preempts a live one, barge-in ({@code interruptLiveThoughts}) interrupts it, and the watchdog
 * force-interrupts a stuck thought. The fake LLM ends a turn with {@code nothing_to_do} (or blocks, to be
 * preempted); {@code stop()} drains the lanes, making the assertions deterministic.
 */
class ThoughtDispatcherTest {

    private final FakeMemory memory = new FakeMemory();

    private ThoughtDispatcher dispatcher() {
        return new ThoughtDispatcher(ctxWith(new NothingToDoLlm()));
    }

    private ThoughtContext ctxWith(LlmGateway llm) {
        return new ThoughtContext(
                llm, new FakeSpeech(), new FakeExecution(), memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> List.of(), new CompanionState(),
                invocation -> false, new ConfirmationCoordinator());
    }

    @Test
    void commanderInputRunsAThought() {
        ThoughtDispatcher dispatcher = dispatcher();
        dispatcher.start();
        dispatcher.submitCommanderInput("set speed to 50");
        dispatcher.stop();

        assertEquals(1, memory.writes.size());
        assertEquals(MemorySource.COMMANDER, memory.writes.get(0).source());
    }

    @Test
    void reflexInputExecutesTheCommandWithoutEngagingLlm() {
        // The reflex resolver matches the input to one safe parameterless command: it runs directly and the
        // LLM is never engaged (the dispatcher routes to a ReflexThought, not a CommanderThought).
        LlmGateway failIfCalled = new LlmGateway() {
            @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
                throw new AssertionError("a reflex must not engage the LLM");
            }
            @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        };
        ThoughtContext ctx = new ThoughtContext(
                failIfCalled, new FakeSpeech(), new FakeExecution(), memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> List.of(), new CompanionState(),
                invocation -> false, new ConfirmationCoordinator(),
                new IntelActionTypeResolver(id -> IntelActionTypeResolver.IntelActionType.COMMAND));
        ReflexResolver reflex = new ReflexResolver(
                () -> List.of(new ReflexResolver.CommandPhrase("open_nav", "navigation", true)),
                invocation -> false);
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(ctx, UrgencyPolicy.normalOnly(), reflex);
        dispatcher.start();
        dispatcher.submitCommanderInput("navigation");
        dispatcher.stop();

        assertEquals(2, memory.writes.size(), "the reflex records the input and the command outcome");
        assertEquals(MemorySource.COMMANDER, memory.writes.get(0).source());
        assertEquals("navigation", memory.writes.get(0).content());
        MemoryEntry outcome = memory.writes.get(1);
        assertEquals(MemorySource.TOOL_RESULT, outcome.source());
        assertEquals("command open_nav executed", outcome.content());
    }

    @Test
    void inputNormalizerCanonicalizesBeforeTheReflexGateButMemoryKeepsRawWords() {
        // A synonym ("combat mode") is canonicalized to its training phrase ("switch to combat mode") before the
        // reflex gate, so it reflexes without the LLM; the raw words are still what memory records.
        LlmGateway failIfCalled = new LlmGateway() {
            @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
                throw new AssertionError("a reflex must not engage the LLM");
            }
            @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        };
        ThoughtContext ctx = new ThoughtContext(
                failIfCalled, new FakeSpeech(), new FakeExecution(), memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> List.of(), new CompanionState(),
                invocation -> false, new ConfirmationCoordinator(),
                new IntelActionTypeResolver(id -> IntelActionTypeResolver.IntelActionType.COMMAND));
        ReflexResolver reflex = new ReflexResolver(
                () -> List.of(new ReflexResolver.CommandPhrase("switch_combat", "switch to combat mode", true)),
                invocation -> false);
        Function<String, String> normalizer = s -> "combat mode".equals(s) ? "switch to combat mode" : s;
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(ctx, reflex, normalizer);
        dispatcher.start();
        dispatcher.submitCommanderInput("combat mode");
        dispatcher.stop();

        assertEquals(2, memory.writes.size(), "the reflex records the input and the command outcome");
        assertEquals(MemorySource.COMMANDER, memory.writes.get(0).source());
        assertEquals("combat mode", memory.writes.get(0).content(), "memory keeps the raw words, not the canonical form");
        assertEquals("command switch_combat executed", memory.writes.get(1).content());
    }

    @Test
    void nonReflexInputFallsThroughToTheCommanderLlmPath() {
        // The resolver matches nothing, so the input takes the normal CommanderThought path - the LLM is engaged.
        CapturingLlm llm = new CapturingLlm();
        ReflexResolver noReflex = new ReflexResolver(() -> List.of(), invocation -> false);
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(ctxWith(llm), UrgencyPolicy.normalOnly(), noReflex);
        dispatcher.start();
        dispatcher.submitCommanderInput("how is the ship");
        dispatcher.stop();

        assertTrue(llm.requests.size() >= 1, "a non-reflex commander input engages the LLM");
        assertTrue(memory.writes.stream().anyMatch(e -> e.source() == MemorySource.COMMANDER));
    }

    @Test
    void eventInputRunsAThoughtTaggedFromEventTopic() {
        ThoughtDispatcher dispatcher = dispatcher();
        dispatcher.start();
        dispatcher.submitEvent(new FakeEvent("FSDJump"));
        dispatcher.stop();

        assertEquals(1, memory.writes.size());
        MemoryEntry entry = memory.writes.get(0);
        assertEquals(MemorySource.EVENT, entry.source());
        assertEquals(ConversationTopic.NAVIGATION, entry.topic(), "event memory tag comes from the event-type map");
    }

    @Test
    void highEventCurrentInputUsesLlmDescriptionInMemory() {
        CapturingLlm llm = new CapturingLlm();
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(ctxWith(llm));
        dispatcher.start();
        dispatcher.submitEvent(new FakeEvent("FSDJump", BaseEvent.Importance.HIGH,
                "The ship completed a hyperspace jump.", "arrived in Sol"));
        dispatcher.stop();

        assertTrue(llm.requests.isEmpty(), "a HIGH event is memory-only and never engages the LLM");
        assertEquals(1, memory.writes.size());

        JsonObject input = JsonParser.parseString(memory.writes.get(0).content()).getAsJsonObject();
        assertEquals("FSDJump", input.get("event_type").getAsString());
        assertEquals("The ship completed a hyperspace jump.", input.get("description").getAsString());
        assertEquals("arrived in Sol", input.getAsJsonObject("payload").get("detail").getAsString());
    }

    @Test
    void normalEventIsDroppedWithoutEngagingLlm() {
        // A NORMAL event is dropped inside the thought: nothing is recorded and the LLM is never called.
        LlmGateway failIfCalled = new LlmGateway() {
            @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
                throw new AssertionError("NORMAL event must not engage the LLM");
            }
            @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        };
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(ctxWith(failIfCalled));
        dispatcher.start();
        dispatcher.submitEvent(new FakeEvent("MarketSell", BaseEvent.Importance.NORMAL));
        dispatcher.stop();

        assertTrue(memory.writes.isEmpty(), "a NORMAL event is not retained in memory");
    }

    @Test
    void sensorNarrationOffersOnlyNarrationToolsEvenWhenQuietAndRecordsTheSpokenLine() {
        List<LlmRequest> requests = new CopyOnWriteArrayList<>();
        LlmGateway llm = new LlmGateway() {
            @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
                requests.add(request);
                JsonObject speakArgs = new JsonObject();
                speakArgs.addProperty(SpeakFunction.PARAM_TEXT, "Plotting a route to Sol, Commander.");
                LlmToolInvocation speak = new LlmToolInvocation(UUID.randomUUID().toString(), SpeakFunction.ID, speakArgs);
                LlmToolInvocation done = new LlmToolInvocation(UUID.randomUUID().toString(),
                        NothingToDoFunction.ID, new JsonObject());
                return CompletableFuture.completedFuture(new LlmResult(LlmResult.Status.OK, List.of(speak, done)));
            }
            @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        };
        CompanionState state = new CompanionState();
        state.setVerbosity(Verbosity.QUIET);
        ThoughtContext ctx = new ThoughtContext(
                llm, new FakeSpeech(), new FakeExecution(), memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> {
                    assertTrue(categories.isEmpty(), "sensor narration is offered no game-tool categories");
                    return List.of();
                },
                state, invocation -> false, new ConfirmationCoordinator());
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(ctx);
        dispatcher.start();

        dispatcher.submitSensorData(new SensorDataEvent(
                "In route to Sol, G class star.",
                "Announce this route information.",
                SensorDataEvent.TOPIC_NAVIGATION));
        dispatcher.stop();

        assertEquals(1, requests.size(), "narration is a single short round");
        assertEquals(Set.of(SpeakFunction.ID, NothingToDoFunction.ID),
                requests.get(0).tools().stream().map(tool -> tool.name()).collect(java.util.stream.Collectors.toSet()),
                "narration offers only speak + nothing_to_do, even when QUIET");

        // Only the spoken line is recorded, under the provided topic - the raw sensor data is not persisted.
        assertEquals(1, memory.writes.size());
        MemoryEntry spoken = memory.writes.get(0);
        assertEquals(MemorySource.COMPANION, spoken.source());
        assertEquals(ConversationTopic.NAVIGATION, spoken.topic());
        assertEquals("Plotting a route to Sol, Commander.", spoken.content());
    }

    @Test
    void aBlockedNarrationDoesNotDelayEventRecording() throws InterruptedException {
        // Narration blocks on the LLM (live on its own lane); a HIGH event must still record immediately,
        // because it runs on the separate event lane - not queued behind the slow narration.
        BlockFirstLlm llm = new BlockFirstLlm();
        ThoughtContext ctx = new ThoughtContext(
                llm, new FakeSpeech(), new FakeExecution(), memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> List.of(), new CompanionState(),
                invocation -> false, new ConfirmationCoordinator());
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(ctx);
        dispatcher.start();

        dispatcher.submitSensorData(new SensorDataEvent(
                "In route to Sol.", "Announce it.", SensorDataEvent.TOPIC_NAVIGATION));
        waitUntil(() -> llm.calls.get() >= 1);     // the narration is live and blocked on the LLM
        dispatcher.submitEvent(new FakeEvent("FSDJump", BaseEvent.Importance.HIGH));
        waitUntil(() -> !memory.writes.isEmpty()); // the event records without waiting for narration

        assertTrue(memory.writes.stream().anyMatch(e -> e.source() == MemorySource.EVENT),
                "a HIGH event records immediately on its own lane, not behind the blocked narration");
        dispatcher.stop();
    }

    @Test
    void verbatimNarrationIsRecordedAndVoicedWithoutEngagingLlm() {
        FakeSpeech speech = new FakeSpeech();
        LlmGateway failIfCalled = new LlmGateway() {
            @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
                throw new AssertionError("verbatim narration must not engage the LLM");
            }
            @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        };
        ThoughtContext ctx = new ThoughtContext(
                failIfCalled, speech, new FakeExecution(), memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> List.of(), new CompanionState(),
                invocation -> false, new ConfirmationCoordinator());
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(ctx);
        dispatcher.start();
        dispatcher.submitVerbatimNarration("Target material detected.", ConversationTopic.MINING);
        dispatcher.stop();

        assertEquals(1, memory.writes.size());
        MemoryEntry spoken = memory.writes.get(0);
        assertEquals(MemorySource.COMPANION, spoken.source());
        assertEquals(ConversationTopic.MINING, spoken.topic());
        assertEquals("Target material detected.", spoken.content());
        assertEquals(1, speech.requests.size(), "the line is voiced verbatim");
        assertEquals("Target material detected.", speech.requests.get(0).text());
    }

    @Test
    void commanderAndEventUseSeparateLanes() {
        ThoughtDispatcher dispatcher = dispatcher();
        dispatcher.start();
        dispatcher.submitCommanderInput("how is the ship");
        dispatcher.submitEvent(new FakeEvent("MarketSell"));
        dispatcher.stop();

        assertEquals(2, memory.writes.size());
        assertTrue(memory.writes.stream().anyMatch(e -> e.source() == MemorySource.COMMANDER));
        assertTrue(memory.writes.stream().anyMatch(e -> e.source() == MemorySource.EVENT));
    }

    @Test
    void blankOrNullInputIsIgnored() {
        ThoughtDispatcher dispatcher = dispatcher();
        dispatcher.start();
        dispatcher.submitCommanderInput("   ");
        dispatcher.submitCommanderInput(null);
        dispatcher.submitEvent(null);
        dispatcher.stop();

        assertTrue(memory.writes.isEmpty());
    }

    @Test
    void inputBeforeStartIsIgnored() {
        ThoughtDispatcher dispatcher = dispatcher();
        dispatcher.submitCommanderInput("too early");
        assertTrue(memory.writes.isEmpty());
    }

    @Test
    void inputAfterStopIsIgnored() {
        ThoughtDispatcher dispatcher = dispatcher();
        dispatcher.start();
        dispatcher.stop();
        dispatcher.submitCommanderInput("too late");

        assertTrue(memory.writes.isEmpty());
    }

    @Test
    void urgentThoughtPreemptsTheLiveThought() throws InterruptedException {
        BlockFirstLlm llm = new BlockFirstLlm();
        UrgencyPolicy policy = new UrgencyPolicy() {
            @Override public Urgency forCommander(String input) {
                return input.contains("urgent") ? Urgency.URGENT : Urgency.NORMAL;
            }
            @Override public Urgency forEvent(BaseEvent event) {
                return Urgency.NORMAL;
            }
        };
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(ctxWith(llm), policy);
        dispatcher.start();

        dispatcher.submitCommanderInput("slow task");   // runs, blocks on the LLM
        waitUntil(() -> llm.calls.get() >= 1);           // the normal thought is live and blocked
        dispatcher.submitCommanderInput("urgent stop");  // urgent: interrupts the live thought, jumps the head
        waitUntil(() -> hasUnresolvedInput());
        dispatcher.stop();

        assertTrue(hasUnresolvedInput(), "preempted thought safe-flushes as INTERRUPTED");
        assertTrue(llm.calls.get() >= 2, "the urgent thought ran after preempting the normal one");
    }

    @Test
    void interruptLiveThoughtsPreemptsTheLiveThought() throws InterruptedException {
        BlockFirstLlm llm = new BlockFirstLlm();
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(ctxWith(llm));
        dispatcher.start();

        dispatcher.submitCommanderInput("slow task");   // blocks on the LLM
        waitUntil(() -> llm.calls.get() >= 1);
        dispatcher.interruptLiveThoughts();              // barge-in path
        waitUntil(() -> hasUnresolvedInput());
        dispatcher.stop();

        assertTrue(hasUnresolvedInput(), "barge-in interrupts the live thought");
    }

    @Test
    void watchdogInterruptsAStuckThought() throws InterruptedException {
        BlockFirstLlm llm = new BlockFirstLlm();
        // Tiny watchdog: 50ms timeout, checked every 10ms.
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(ctxWith(llm), UrgencyPolicy.normalOnly(), 50, 10);
        dispatcher.start();

        dispatcher.submitCommanderInput("stuck task"); // blocks on the LLM forever
        waitUntil(() -> hasUnresolvedInput());
        dispatcher.stop();

        assertTrue(hasUnresolvedInput(), "watchdog force-interrupts a stuck thought");
    }

    @Test
    void aBlockedCommanderThoughtDoesNotBlockNewCommanderInput() throws InterruptedException {
        // A long command occupies a worker; with the bounded commander pool a new commander thought runs on a
        // free worker instead of queuing behind the blocked one.
        BlockFirstLlm llm = new BlockFirstLlm(); // first thought blocks on the LLM forever; later ones end
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(ctxWith(llm));
        dispatcher.start();

        dispatcher.submitCommanderInput("slow one");   // takes the first LLM call and blocks (worker A)
        waitUntil(() -> llm.calls.get() >= 1);
        dispatcher.submitCommanderInput("quick one");   // must run concurrently, not wait for the blocked one
        waitUntil(() -> memory.writes.stream().anyMatch(e -> "quick one".equals(e.content())));

        assertTrue(memory.writes.stream().anyMatch(
                        e -> e.source() == MemorySource.COMMANDER && "quick one".equals(e.content())),
                "a second commander thought runs while the first is blocked");
        dispatcher.stop();
    }

    @Test
    void aFailingThoughtDoesNotKillTheLane() {
        // A reducer that always throws makes every thought fail during prompt assembly.
        ThoughtContext ctx = new ThoughtContext(
                new NothingToDoLlm(), new FakeSpeech(), new FakeExecution(), memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> { throw new RuntimeException("boom"); }, new CompanionState(),
                invocation -> false, new ConfirmationCoordinator());
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(ctx);
        dispatcher.start();

        dispatcher.submitCommanderInput("first");
        dispatcher.submitCommanderInput("second");
        dispatcher.stop();

        // The lane survived the first failure to process the second, and neither left a memory hole.
        assertEquals(2, memory.writes.size());
        assertTrue(memory.writes.stream()
                .allMatch(e -> e.topic() == ConversationTopic.UNRESOLVED_COMMANDER_INPUT));
    }

    // --- helpers ---

    /** A safe-flushed/interrupted thought records its input under the unresolved-commander-input topic. */
    private boolean hasUnresolvedInput() {
        return memory.writes.stream().anyMatch(e -> e.topic() == ConversationTopic.UNRESOLVED_COMMANDER_INPUT);
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }
    }

    // --- fakes ---

    /** Ends every turn immediately with nothing_to_do, so a thought records its input and stops. */
    private static final class NothingToDoLlm implements LlmGateway {
        @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
            LlmToolInvocation terminator = new LlmToolInvocation(UUID.randomUUID().toString(),
                    NothingToDoFunction.ID, new JsonObject());
            return CompletableFuture.completedFuture(new LlmResult(LlmResult.Status.OK, List.of(terminator)));
        }

        @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            return CompletableFuture.completedFuture(null);
        }
    }

    /** Blocks the first turn forever (the preempted thought) and ends later turns with nothing_to_do. */
    private static final class BlockFirstLlm implements LlmGateway {
        final AtomicInteger calls = new AtomicInteger();

        @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
            if (calls.incrementAndGet() == 1) {
                return new CompletableFuture<>(); // never completes; interrupt (cancel) unblocks it
            }
            LlmToolInvocation terminator = new LlmToolInvocation(UUID.randomUUID().toString(),
                    NothingToDoFunction.ID, new JsonObject());
            return CompletableFuture.completedFuture(new LlmResult(LlmResult.Status.OK, List.of(terminator)));
        }

        @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class CapturingLlm implements LlmGateway {
        final List<LlmRequest> requests = new CopyOnWriteArrayList<>();

        @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
            requests.add(request);
            LlmToolInvocation terminator = new LlmToolInvocation(UUID.randomUUID().toString(),
                    NothingToDoFunction.ID, new JsonObject());
            return CompletableFuture.completedFuture(new LlmResult(LlmResult.Status.OK, List.of(terminator)));
        }

        @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class FakeMemory implements MemoryGateway {
        final List<MemoryEntry> writes = new CopyOnWriteArrayList<>();

        @Override public void write(MemoryEntry entry) { writes.add(entry); }
        @Override public List<MemoryEntry> readShortTermTimeline() { return List.of(); }
        @Override public List<MemoryEntry> recallTopicMemory(ConversationTopic topic, String query, int limit) { return List.of(); }
        @Override public List<String> recallMatching(String query, int limit) { return List.of(); }
        @Override public List<String> readLlmMemory() { return List.of(); }
        @Override public void writeLlmMemory(String content) { }
        @Override public MemoryAvailabilitySnapshot indexes() { return new MemoryAvailabilitySnapshot(0, 15, List.of()); }
        @Override public String longTermSummary() { return ""; }
        @Override public void replaceLongTermSummary(String summary) { }
    }

    private static final class FakeSpeech implements SpeechGateway {
        final List<SpeechRequest> requests = new CopyOnWriteArrayList<>();

        @Override public CompletableFuture<Void> submit(SpeechRequest request) {
            requests.add(request);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class FakeExecution implements ExecutionGateway {
        @Override public CompletableFuture<JsonObject> submit(ExecutionRequest request) {
            return CompletableFuture.completedFuture(new JsonObject());
        }
    }

    private static final class FakeEvent extends BaseEvent {
        private final String type;
        private final Importance importance;
        private final String description;
        private final String detail;

        FakeEvent(String type) {
            this(type, Importance.HIGH); // default: exercise the full thinking loop
        }

        FakeEvent(String type, Importance importance) {
            this(type, importance, "Description for " + type, "detail for " + type);
        }

        FakeEvent(String type, Importance importance, String description, String detail) {
            super(Instant.now().toString(), Duration.ofMinutes(1), type);
            this.type = type;
            this.importance = importance;
            this.description = description;
            this.detail = detail;
        }

        @Override public String getEventType() { return type; }
        @Override public Importance importance() { return importance; }
        @Override public String llmDescription() { return description; }
        @Override public String toJson() { return toJsonObject().toString(); }
        @Override public JsonObject toJsonObject() {
            JsonObject object = new JsonObject();
            object.addProperty("event", type);
            object.addProperty("detail", detail);
            return object;
        }
    }
}
