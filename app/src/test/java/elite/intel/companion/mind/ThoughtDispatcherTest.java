package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.AIConstants;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.memory.MemorySnapshot;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.llm.*;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.IntelActionAccessPolicy;
import elite.intel.companion.prompt.PromptComposer;
import elite.intel.companion.prompt.ReflexResolver;
import elite.intel.companion.prompt.SemanticReflexResolver;
import elite.intel.companion.speech.SpeechGateway;
import elite.intel.companion.tools.ClassifyTurnFunction;
import elite.intel.companion.tools.IntelActionTypeResolver;
import elite.intel.companion.tools.SpeakFunction;
import elite.intel.companion.tools.SystemFunctionProvider;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lane scheduling: a submitted input runs a thought to a memory write, the two sources use separate lanes,
 * blank input and input racing lifecycle (before start / after stop) are ignored, an urgent thought
 * preempts a live one, barge-in ({@code interruptLiveThoughts}) interrupts it, and the watchdog
 * force-interrupts a stuck thought. The fake LLM settles a turn with a bare {@code classify_turn} - which
 * records the input and, being single-round by design, ends the turn (or blocks, to be preempted);
 * {@code stop()} drains the lanes, making the assertions deterministic.
 */
class ThoughtDispatcherTest {

    private final FakeMemory memory = new FakeMemory();

    private ThoughtDispatcher dispatcher() {
        return new ThoughtDispatcher(dependenciesWith(new TerminatingLlm()));
    }

    private ThoughtDependencies dependenciesWith(LlmGateway llm) {
        return new ThoughtDependencies(
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

        // The bare classify_turn settles with no speak and no action, so the turn records the commander input
        // and then a <no_reply/> boundary as the companion's omitted reply (see CommanderThought.recordTurnBoundary).
        assertEquals(2, memory.writes.size());
        assertEquals(MemorySource.COMMANDER, memory.writes.get(0).source());
        assertEquals(MemorySource.COMPANION, memory.writes.get(1).source());
    }

    @Test
    void reflexInputExecutesTheCommandWithoutEngagingLlm() {
        // The reflex resolver matches the input to one safe parameterless command: it runs directly and the
        // LLM is never engaged. This command returns no spoken outcome, so it is a pure side effect and files
        // nothing (neither the imperative nor the call echo); a command that DOES return an outcome files the
        // pair (see reflexCommandRecordsThePairAndVoicesTheOutcomeItReturns).
        LlmGateway failIfCalled = new LlmGateway() {
            @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
                throw new AssertionError("a reflex must not engage the LLM");
            }
            @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        };
        List<String> executed = new CopyOnWriteArrayList<>();
        ExecutionGateway tracking = request -> {
            executed.add(request.toolName());
            return CompletableFuture.completedFuture(new JsonObject());
        };
        ThoughtDependencies dependencies = new ThoughtDependencies(
                failIfCalled, new FakeSpeech(), tracking, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> List.of(), new CompanionState(),
                invocation -> false, new ConfirmationCoordinator(),
                new IntelActionTypeResolver(id -> IntelActionTypeResolver.IntelActionType.COMMAND));
        ReflexResolver reflex = new ReflexResolver(
                () -> List.of(new ReflexResolver.CommandPhrase("open_nav", "navigation", true)),
                invocation -> false);
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependencies, UrgencyPolicy.normalOnly(), reflex);
        dispatcher.start();
        dispatcher.submitCommanderInput("navigation");
        dispatcher.stop();

        assertEquals(List.of("open_nav"), executed, "the reflex ran the resolved command directly");
        assertTrue(memory.writes.isEmpty(), "a silent reflex command (blank outcome) files nothing to memory");
    }

    @Test
    void reflexCommandRecordsThePairAndVoicesTheOutcomeItReturns() {
        // A parameterless command that computes an answer (e.g. calculate_fleet_carrier_route returning its route
        // summary) is reflex-eligible, so it never reaches the LLM. Because it returns a spoken outcome, it is a
        // real exchange: the imperative is filed as the user turn and the outcome voiced and remembered as the
        // companion reply - a clean pair - otherwise the summary is computed and silently dropped.
        LlmGateway failIfCalled = new LlmGateway() {
            @Override
            public CompletableFuture<LlmResult> submit(LlmRequest request) {
                throw new AssertionError("a reflex must not engage the LLM");
            }

            @Override
            public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        };
        String summary = "Route to Colonia calculated. 8 jumps, 4800 tons of tritium required.";
        ExecutionGateway summarizing = request -> {
            JsonObject result = new JsonObject();
            result.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, summary);
            return CompletableFuture.completedFuture(result);
        };
        FakeSpeech speech = new FakeSpeech();
        ThoughtDependencies dependencies = new ThoughtDependencies(
                failIfCalled, speech, summarizing, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> List.of(), new CompanionState(),
                invocation -> false, new ConfirmationCoordinator(),
                new IntelActionTypeResolver(id -> IntelActionTypeResolver.IntelActionType.COMMAND));
        ReflexResolver reflex = new ReflexResolver(
                () -> List.of(new ReflexResolver.CommandPhrase(
                        "calculate_fleet_carrier_route", "calculate fleet carrier route", true)),
                invocation -> false);
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependencies, UrgencyPolicy.normalOnly(), reflex);
        dispatcher.start();
        dispatcher.submitCommanderInput("calculate fleet carrier route");
        dispatcher.stop();

        assertEquals(List.of(summary), speech.requests.stream().map(SpeechRequest::text).toList(),
                "the reflex voiced the summary the command returned");
        // The imperative is filed as the user turn and the spoken outcome as the companion reply - a clean pair.
        // The call echo is still not filed (a command records no CALL to pair a result with).
        assertEquals(2, memory.writes.size(), "the imperative and the spoken outcome are recorded as a pair");
        assertEquals(MemorySource.COMMANDER, memory.writes.get(0).source());
        assertEquals("calculate fleet carrier route", memory.writes.get(0).content());
        assertEquals(MemorySource.COMPANION, memory.writes.get(1).source());
        assertEquals(summary, memory.writes.get(1).content());
    }

    @Test
    void inputNormalizerCanonicalizesBeforeTheReflexGate() {
        // A synonym ("combat mode") is canonicalized to its training phrase ("switch to combat mode") before the
        // reflex gate, so it reflexes without the LLM. This command returns no spoken outcome, so nothing is filed.
        LlmGateway failIfCalled = new LlmGateway() {
            @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
                throw new AssertionError("a reflex must not engage the LLM");
            }
            @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        };
        List<String> executed = new CopyOnWriteArrayList<>();
        ExecutionGateway tracking = request -> {
            executed.add(request.toolName());
            return CompletableFuture.completedFuture(new JsonObject());
        };
        ThoughtDependencies dependencies = new ThoughtDependencies(
                failIfCalled, new FakeSpeech(), tracking, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> List.of(), new CompanionState(),
                invocation -> false, new ConfirmationCoordinator(),
                new IntelActionTypeResolver(id -> IntelActionTypeResolver.IntelActionType.COMMAND));
        ReflexResolver reflex = new ReflexResolver(
                () -> List.of(new ReflexResolver.CommandPhrase("switch_combat", "switch to combat mode", true)),
                invocation -> false);
        Function<String, String> normalizer = s -> "combat mode".equals(s) ? "switch to combat mode" : s;
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependencies, reflex, normalizer);
        dispatcher.start();
        dispatcher.submitCommanderInput("combat mode");
        dispatcher.stop();

        assertEquals(List.of("switch_combat"), executed,
                "the normalized synonym reflexes to the resolved command without the LLM");
        assertTrue(memory.writes.isEmpty(), "a silent reflex command (blank outcome) files nothing to memory");
    }

    @Test
    void aLeadingCompanionNameIsStrippedForTheReflexGate() {
        // Addressing the companion by name ("Vega, all stop") still takes the reflex fast-path: the leading
        // vocative name is stripped before reflex matching. This command returns no spoken outcome, so nothing is filed.
        LlmGateway failIfCalled = new LlmGateway() {
            @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
                throw new AssertionError("a reflex must not engage the LLM");
            }
            @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        };
        List<String> executed = new CopyOnWriteArrayList<>();
        ExecutionGateway tracking = request -> {
            executed.add(request.toolName());
            return CompletableFuture.completedFuture(new JsonObject());
        };
        ThoughtDependencies dependencies = new ThoughtDependencies(
                failIfCalled, new FakeSpeech(), tracking, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> List.of(), new CompanionState(),
                invocation -> false, new ConfirmationCoordinator(),
                new IntelActionTypeResolver(id -> IntelActionTypeResolver.IntelActionType.COMMAND));
        ReflexResolver reflex = new ReflexResolver(
                () -> List.of(new ReflexResolver.CommandPhrase("stop_ship", "all stop", true)),
                invocation -> false);
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependencies, reflex, s -> s); // identity normalizer
        dispatcher.start();
        String input = CompanionConfig.companionName() + ", all stop";
        dispatcher.submitCommanderInput(input);
        dispatcher.stop();

        assertEquals(List.of("stop_ship"), executed, "the name-addressed short command still reflexes");
        assertTrue(memory.writes.isEmpty(), "a silent reflex command (blank outcome) files nothing to memory");
    }

    @Test
    void aLeadingTransliteratedCompanionNameIsAlsoStrippedForTheReflexGate() {
        // Russian STT returns the name in Cyrillic ("Вега"), not the canonical Latin "Vega". The Cyrillic form
        // is the localized spoken form for the RU session language, so it is recognized as a leading vocative
        // and a name-addressed short command still reflexes (no LLM).
        Language previousLanguage = SystemSession.getInstance().getLanguage();
        SystemSession.getInstance().setLanguage(Language.RU);
        try {
            LlmGateway failIfCalled = new LlmGateway() {
                @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
                    throw new AssertionError("a reflex must not engage the LLM");
                }
                @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
                    return CompletableFuture.completedFuture(null);
                }
            };
            List<String> executed = new CopyOnWriteArrayList<>();
            ExecutionGateway tracking = request -> {
                executed.add(request.toolName());
                return CompletableFuture.completedFuture(new JsonObject());
            };
            ThoughtDependencies dependencies = new ThoughtDependencies(
                    failIfCalled, new FakeSpeech(), tracking, memory,
                    new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                    (categories, currentInput) -> List.of(), new CompanionState(),
                    invocation -> false, new ConfirmationCoordinator(),
                    new IntelActionTypeResolver(id -> IntelActionTypeResolver.IntelActionType.COMMAND));
            ReflexResolver reflex = new ReflexResolver(
                    () -> List.of(new ReflexResolver.CommandPhrase("stop_ship", "all stop", true)),
                    invocation -> false);
            ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependencies, reflex, s -> s); // identity normalizer
            dispatcher.start();
            dispatcher.submitCommanderInput("Вега, all stop"); // Cyrillic vocative + the reflex phrase
            dispatcher.stop();

            assertEquals(List.of("stop_ship"), executed, "the Cyrillic name-addressed short command still reflexes");
            assertTrue(memory.writes.isEmpty(), "a silent reflex command (blank outcome) files nothing to memory");
        } finally {
            SystemSession.getInstance().setLanguage(previousLanguage);
        }
    }

    @Test
    void nonReflexInputFallsThroughToTheCommanderLlmPath() {
        // The resolver matches nothing, so the input takes the normal CommanderThought path - the LLM is engaged.
        CapturingLlm llm = new CapturingLlm();
        ReflexResolver noReflex = new ReflexResolver(() -> List.of(), invocation -> false);
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependenciesWith(llm), UrgencyPolicy.normalOnly(), noReflex);
        dispatcher.setSemanticReflexResolver(SemanticReflexResolver.disabled()); // exercise the LLM path, not the embedder reflex
        dispatcher.start();
        dispatcher.submitCommanderInput("how is the ship");
        dispatcher.stop();

        assertTrue(llm.requests.size() >= 1, "a non-reflex commander input engages the LLM");
        assertTrue(memory.writes.stream().anyMatch(e -> e.source() == MemorySource.COMMANDER));
    }

    @Test
    void aNonCommandTurnRecordsRawWordsNotTheCanonicalForm() {
        // The normalizer canonicalizes the input for matching/tool selection, but a recorded (non-command) turn
        // keeps the raw words the commander actually said. The reflex matches nothing, so the turn takes the LLM
        // path and settles as a bare classify_turn - a conversational turn that files its input.
        CapturingLlm llm = new CapturingLlm();
        ReflexResolver noReflex = new ReflexResolver(() -> List.of(), invocation -> false);
        Function<String, String> normalizer = s -> "combat mode".equals(s) ? "switch to combat mode" : s;
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependenciesWith(llm), noReflex, normalizer);
        dispatcher.setSemanticReflexResolver(SemanticReflexResolver.disabled()); // exercise the LLM path, not the embedder reflex
        dispatcher.start();
        dispatcher.submitCommanderInput("combat mode");
        dispatcher.stop();

        assertTrue(memory.writes.stream().anyMatch(
                        e -> e.source() == MemorySource.COMMANDER && "combat mode".equals(e.content())),
                "memory keeps the raw words, not the canonical form used for matching");
    }

    @Test
    void eventReactionRecordsStimulusThenReplyAndOffersOnlySpeak() {
        // A subscriber-driven reaction: one short round offering only speak; the stimulus is recorded as an
        // EVENT (user) turn and the spoken reply as the companion's words, keeping a clean two-party dialogue.
        List<LlmRequest> requests = new CopyOnWriteArrayList<>();
        LlmGateway llm = new LlmGateway() {
            @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
                requests.add(request);
                JsonObject speakArgs = new JsonObject();
                speakArgs.addProperty(SpeakFunction.PARAM_TEXT, "Signals detected on the ring, Commander.");
                LlmToolInvocation speak = new LlmToolInvocation(UUID.randomUUID().toString(), SpeakFunction.ID, speakArgs);
                return CompletableFuture.completedFuture(new LlmResult(LlmResult.Status.OK, List.of(speak)));
            }
            @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        };
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependenciesWith(llm));
        dispatcher.start();
        dispatcher.submitEventReaction("surface scan: alexandrite", "Report it briefly.",
                "EXPLORATION", Urgency.NORMAL);
        dispatcher.stop();

        assertEquals(1, requests.size(), "an event reaction is a single short round");
        List<LlmMessage> promptMessages = requests.get(0).messages();
        LlmMessage promptInput = promptMessages.get(promptMessages.size() - 1);
        assertEquals(LlmMessageRole.USER, promptInput.role());
        assertTrue(promptInput.content().contains("<event_data>\nsurface scan: alexandrite\n</event_data>"));
        assertTrue(promptInput.content().contains(
                "<narration_instructions>\nReport it briefly.\n</narration_instructions>"));
        assertEquals(Set.of(SpeakFunction.ID),
                requests.get(0).tools().stream().map(tool -> tool.name()).collect(java.util.stream.Collectors.toSet()),
                "an event reaction offers only speak");
        assertEquals(2, memory.writes.size(), "the stimulus and the reply are both recorded, in order");
        assertEquals(MemorySource.EVENT, memory.writes.get(0).source());
        assertEquals("surface scan: alexandrite", memory.writes.get(0).content());
        assertEquals(MemorySource.COMPANION, memory.writes.get(1).source());
        assertEquals("Signals detected on the ring, Commander.", memory.writes.get(1).content());
        assertEquals(ConversationTopic.EXPLORATION, memory.writes.get(1).topic());
    }

    @Test
    void interruptAfterInputRecordedWritesACutOffMarker() throws InterruptedException {
        // An interrupt landing after the LLM round but before the turn replies (raised here while the
        // classify_turn pre-execution runs on the lane) must leave the recorded input followed by a
        // <cut_off/> boundary as the companion's omitted reply, not end silently (see CommanderThought.safeFlush).
        // The turn is awaited BEFORE stop(): stop() nulls the lanes first, which would make the barge-in a no-op.
        ThoughtDispatcher[] holder = new ThoughtDispatcher[1];
        ExecutionGateway interruptingExecution = request -> {
            holder[0].interruptLiveThoughts();
            return CompletableFuture.completedFuture(new JsonObject());
        };
        ThoughtDependencies dependencies = new ThoughtDependencies(
                new TerminatingLlm(), new FakeSpeech(), interruptingExecution, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> List.of(), new CompanionState(),
                invocation -> false, new ConfirmationCoordinator());
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependencies);
        holder[0] = dispatcher;
        dispatcher.start();
        dispatcher.submitCommanderInput("set speed to 50");
        waitUntil(() -> memory.writes.size() >= 2);
        dispatcher.stop();

        assertEquals(2, memory.writes.size());
        assertEquals(MemorySource.COMMANDER, memory.writes.get(0).source());
        MemoryEntry marker = memory.writes.get(1);
        assertEquals(MemorySource.COMPANION, marker.source());
        assertEquals("<cut_off/>", marker.content());
    }

    @Test
    void blankOrNullInputIsIgnored() {
        ThoughtDispatcher dispatcher = dispatcher();
        dispatcher.start();
        dispatcher.submitCommanderInput("   ");
        dispatcher.submitCommanderInput(null);
        dispatcher.submitEventReaction(null, null, "SYSTEM", Urgency.NORMAL);
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
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependenciesWith(llm), policy);
        dispatcher.setSemanticReflexResolver(SemanticReflexResolver.disabled()); // exercise the LLM/preemption path, not reflex
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
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependenciesWith(llm));
        dispatcher.setSemanticReflexResolver(SemanticReflexResolver.disabled()); // exercise the LLM/barge-in path, not reflex
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
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependenciesWith(llm), UrgencyPolicy.normalOnly(), 50, 10);
        dispatcher.start();

        dispatcher.submitCommanderInput("stuck task"); // blocks on the LLM forever
        waitUntil(() -> hasUnresolvedInput());
        dispatcher.stop();

        assertTrue(hasUnresolvedInput(), "watchdog force-interrupts a stuck thought");
    }

    @Test
    void aSlowCommandDoesNotBlockTheNextCommanderCognitiveTurn() throws InterruptedException {
        CompletableFuture<JsonObject> slowResult = new CompletableFuture<>();
        AtomicInteger llmCalls = new AtomicInteger();
        CompanionState state = new CompanionState();
        LlmGateway llm = new LlmGateway() {
            @Override
            public CompletableFuture<LlmResult> submit(LlmRequest request) {
                int callNumber = llmCalls.incrementAndGet();
                String topic = callNumber == 1 ? "navigation" : "ship_status";
                JsonObject classifyArgs = new JsonObject();
                classifyArgs.addProperty(ClassifyTurnFunction.PARAM_TOPIC, topic);
                classifyArgs.addProperty(ClassifyTurnFunction.PARAM_IMPORTANCE, "normal");
                classifyArgs.addProperty(ClassifyTurnFunction.PARAM_IS_QUESTION, false);
                classifyArgs.addProperty(ClassifyTurnFunction.PARAM_CANONICAL_FACT, "");
                LlmToolInvocation classify = new LlmToolInvocation(UUID.randomUUID().toString(),
                        ClassifyTurnFunction.ID, classifyArgs);
                LlmToolInvocation settling;
                if (callNumber == 1) {
                    settling = new LlmToolInvocation(UUID.randomUUID().toString(), "slow_command", new JsonObject());
                } else {
                    JsonObject speakArgs = new JsonObject();
                    speakArgs.addProperty(SpeakFunction.PARAM_TEXT, "quick reply");
                    settling = new LlmToolInvocation(UUID.randomUUID().toString(), SpeakFunction.ID, speakArgs);
                }
                return CompletableFuture.completedFuture(new LlmResult(LlmResult.Status.OK,
                        List.of(classify, settling)));
            }

            @Override
            public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        };
        ExecutionGateway execution = request -> {
            if (ClassifyTurnFunction.ID.equals(request.toolName())) {
                ConversationTopic topic = ConversationTopic.fromSelectableId(
                        request.arguments().get(ClassifyTurnFunction.PARAM_TOPIC).getAsString());
                state.setGlobalTopic(topic);
                return CompletableFuture.completedFuture(new JsonObject());
            }
            if ("slow_command".equals(request.toolName())) {
                return slowResult;
            }
            return CompletableFuture.completedFuture(new JsonObject());
        };
        IntelActionTypeResolver actionTypes = new IntelActionTypeResolver(id ->
                "slow_command".equals(id)
                        ? IntelActionTypeResolver.IntelActionType.COMMAND
                        : IntelActionTypeResolver.IntelActionType.SYSTEM);
        ThoughtDependencies dependencies = new ThoughtDependencies(
                llm, new FakeSpeech(), execution, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> List.of(), state,
                invocation -> false, new ConfirmationCoordinator(), actionTypes);
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(
                dependencies, UrgencyPolicy.normalOnly(), new ReflexResolver(() -> List.of(), invocation -> false));
        dispatcher.setSemanticReflexResolver(SemanticReflexResolver.disabled());
        dispatcher.start();

        dispatcher.submitCommanderInput("slow one");
        waitUntil(() -> llmCalls.get() == 1 && !dispatcher.isIdle());
        dispatcher.submitCommanderInput("quick one");
        waitUntil(() -> memory.writes.stream().anyMatch(e -> "quick one".equals(e.content())));

        List<MemoryEntry> commanderInputs = memory.writes.stream()
                .filter(e -> e.source() == MemorySource.COMMANDER)
                .toList();
        assertEquals(List.of("slow one", "quick one"),
                commanderInputs.stream().map(MemoryEntry::content).toList(),
                "commander cognition and input commits retain intake order");
        assertEquals(ConversationTopic.NAVIGATION, commanderInputs.get(0).topic());
        assertEquals(ConversationTopic.SHIP_STATUS, commanderInputs.get(1).topic());
        assertFalse(dispatcher.isIdle(), "the detached slow command remains owned by the dispatcher");

        JsonObject completed = new JsonObject();
        completed.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, "slow complete");
        slowResult.complete(completed);
        waitUntil(dispatcher::isIdle);
        assertTrue(memory.writes.stream().anyMatch(e -> "slow complete".equals(e.content())
                        && e.topic() == ConversationTopic.NAVIGATION),
                "the late command result retains its own frozen topic");
        dispatcher.stop();
    }

    @Test
    void aFailingThoughtDoesNotKillTheLane() {
        // A reducer that always throws makes every thought fail during prompt assembly.
        ThoughtDependencies dependencies = new ThoughtDependencies(
                new TerminatingLlm(), new FakeSpeech(), new FakeExecution(), memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> { throw new RuntimeException("boom"); }, new CompanionState(),
                invocation -> false, new ConfirmationCoordinator());
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependencies);
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

    /** Settles every turn immediately with a bare classify_turn, so a thought records its input and stops. */
    private static final class TerminatingLlm implements LlmGateway {
        @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
            LlmToolInvocation terminator = new LlmToolInvocation(UUID.randomUUID().toString(),
                    ClassifyTurnFunction.ID, new JsonObject());
            return CompletableFuture.completedFuture(new LlmResult(LlmResult.Status.OK, List.of(terminator)));
        }

        @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            return CompletableFuture.completedFuture(null);
        }
    }

    /** Blocks the first turn forever (the preempted thought) and settles later turns with classify_turn. */
    private static final class BlockFirstLlm implements LlmGateway {
        final AtomicInteger calls = new AtomicInteger();

        @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
            if (calls.incrementAndGet() == 1) {
                return new CompletableFuture<>(); // never completes; interrupt (cancel) unblocks it
            }
            LlmToolInvocation terminator = new LlmToolInvocation(UUID.randomUUID().toString(),
                    ClassifyTurnFunction.ID, new JsonObject());
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
                    ClassifyTurnFunction.ID, new JsonObject());
            return CompletableFuture.completedFuture(new LlmResult(LlmResult.Status.OK, List.of(terminator)));
        }

        @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class FakeMemory implements MemoryGateway {
        final List<MemoryEntry> writes = new CopyOnWriteArrayList<>();

        @Override public void write(MemoryEntry entry) { writes.add(entry); }
        @Override public MemorySnapshot snapshot() { throw new UnsupportedOperationException(); }
        @Override public List<MemoryEntry> readShortTermTimeline() { return List.of(); }
        @Override public List<MemoryEntry> recallTopicMemory(ConversationTopic topic, String query, int limit) { return List.of(); }
        @Override public List<String> recallMatching(String query, int limit) { return List.of(); }
        @Override public List<MemoryEntry> recallCandidates(String query, int limit) { return List.of(); }
        @Override public String longTermSummary() { return ""; }
        @Override public void replaceLongTermSummary(String summary) { }
        @Override public List<MemoryEntry> longTermPinnedFacts() { return List.of(); }
        @Override public void addLongTermPinned(MemoryEntry fact) { }
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

}
