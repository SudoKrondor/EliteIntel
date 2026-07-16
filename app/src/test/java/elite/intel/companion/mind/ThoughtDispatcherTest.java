package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.AIConstants;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.clarify.ClarificationCoordinator;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.memory.MemorySearchResult;
import elite.intel.companion.memory.MemorySnapshot;
import elite.intel.companion.model.GameStateSnapshot;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.llm.*;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.memory.MemorySearchMatch;
import elite.intel.companion.model.memory.MemoryRecord;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.IntelActionAccessPolicy;
import elite.intel.companion.prompt.PromptComposer;
import elite.intel.companion.prompt.ReflexResolver;
import elite.intel.companion.prompt.SemanticReflexResolver;
import elite.intel.companion.speech.SpeechGateway;
import elite.intel.companion.tools.IntelActionTypeResolver;
import elite.intel.companion.tools.SpeakFunction;
import elite.intel.companion.tools.SystemFunctionProvider;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lane scheduling: a submitted input runs a thought to settlement, the two sources use separate lanes,
 * blank input and input racing lifecycle (before start / after stop) are ignored, an urgent thought
 * preempts a live one, barge-in ({@code interruptLiveThoughts}) interrupts it, and the watchdog
 * force-interrupts a stuck thought. The fake LLM settles a turn with {@code speak}, producing a complete
 * commander/companion memory pair and ending the turn (or blocks, to be preempted);
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

        // The fake speak settles the turn as one complete commander/companion pair.
        assertEquals(1, memory.writes.size());
        assertEquals(MemoryKind.DIALOGUE, memory.writes.get(0).kind());
        assertEquals(List.of(MemorySource.COMMANDER, MemorySource.COMPANION),
                memory.writes.get(0).entries().stream().map(MemoryEntry::source).toList());
    }

    @Test
    void reflexInputExecutesTheCommandWithoutEngagingLlm() {
        // The reflex resolver matches the input to one safe parameterless command: it runs directly and the
        // LLM is never engaged. This command returns no spoken outcome, so it is a pure side effect and files
        // nothing (neither the imperative nor the call echo). A non-blank handler outcome is voiced but remains
        // execution feedback, so it is memory-silent too.
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
    void confidentNewReflexClaimsAndSupersedesPendingClarification() throws InterruptedException {
        ClarificationCoordinator clarification = new ClarificationCoordinator();
        clarification.open("set_speed", "amount", "increase speed", "By how much?");
        List<String> executed = new CopyOnWriteArrayList<>();
        LlmGateway failIfCalled = new LlmGateway() {
            @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
                throw new AssertionError("a new exact reflex must supersede pending state before the LLM");
            }
            @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        };
        ThoughtDependencies dependencies = new ThoughtDependencies(
                failIfCalled, new FakeSpeech(), request -> {
                    executed.add(request.toolName());
                    return CompletableFuture.completedFuture(new JsonObject());
                }, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> List.of(), new CompanionState(),
                invocation -> false, new ConfirmationCoordinator(), clarification,
                new IntelActionTypeResolver(id -> IntelActionTypeResolver.IntelActionType.COMMAND));
        ReflexResolver reflex = new ReflexResolver(
                () -> List.of(new ReflexResolver.CommandPhrase("open_nav", "navigation", true)),
                invocation -> false);
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependencies, UrgencyPolicy.normalOnly(), reflex);
        dispatcher.start();

        dispatcher.submitCommanderInput("navigation");
        waitUntil(() -> !executed.isEmpty());

        assertTrue(clarification.peek().isEmpty(), "the new command owns the reply and abandons old pending state");
        assertEquals(List.of("open_nav"), executed);
        dispatcher.stop();
    }

    @Test
    void reflexCommandVoicesItsOutcomeWithoutMemory() {
        // A parameterless command that computes an answer (e.g. calculate_fleet_carrier_route returning its route
        // summary) is reflex-eligible, so it never reaches the LLM. Its handler result is voiced, but command
        // execution is not an LLM dialogue pair and therefore contributes no memory.
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
        assertTrue(memory.writes.isEmpty(), "the reflex command and its spoken handler outcome stay out of memory");
    }

    @Test
    void phoneticNormalizerCanonicalizesBeforeTheReflexGate() {
        // A known Parakeet acoustic confusion ("career") is corrected to the intended command term ("carrier")
        // before the reflex gate, so it reflexes without the LLM. This command returns no spoken outcome, so nothing is filed.
        Language previousLanguage = SystemSession.getInstance().getLanguage();
        SystemSession.getInstance().setLanguage(Language.EN);
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
            List<GameStateSnapshot> observedStates = new CopyOnWriteArrayList<>();
            ReflexResolver reflex = new ReflexResolver(snapshot -> {
                observedStates.add(snapshot);
                return List.of(new ReflexResolver.CommandPhrase(
                        "display_fleet_carrier_management_panel", "open fleet carrier management panel", true));
            }, invocation -> false);
            ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependencies, reflex);
            dispatcher.start();
            dispatcher.submitCommanderInput("open fleet career management panel");
            dispatcher.stop();

            assertEquals(List.of("display_fleet_carrier_management_panel"), executed,
                    "the corrected acoustic term reflexes to the resolved command without the LLM");
            assertEquals(2, observedStates.size(), "raw and normalized exact attempts both consult visibility");
            assertSame(observedStates.get(0), observedStates.get(1),
                    "raw and normalized exact attempts must share one immutable turn state");
            assertTrue(memory.writes.isEmpty(), "a silent reflex command (blank outcome) files nothing to memory");
        } finally {
            SystemSession.getInstance().setLanguage(previousLanguage);
        }
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
        assertTrue(memory.entries().stream().anyMatch(e -> e.source() == MemorySource.COMMANDER));
    }

    @Test
    void aNonCommandTurnRecordsCanonicalWordsNotTheRawSttForm() {
        // The normalizer corrects an acoustic STT error. The reflex matches nothing, so the turn takes the LLM
        // path and settles with speak; the committed pair must retain the same canonical wording the LLM saw.
        CapturingLlm llm = new CapturingLlm();
        ReflexResolver noReflex = new ReflexResolver(() -> List.of(), invocation -> false);
        Function<String, String> normalizer = s -> "open fleet career management panel".equals(s)
                ? "open fleet carrier management panel" : s;
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependenciesWith(llm), noReflex, normalizer);
        dispatcher.setSemanticReflexResolver(SemanticReflexResolver.disabled()); // exercise the LLM path, not the embedder reflex
        dispatcher.start();
        dispatcher.submitCommanderInput("open fleet career management panel");
        dispatcher.stop();

        assertTrue(memory.entries().stream().anyMatch(
                        e -> e.source() == MemorySource.COMMANDER
                                && "open fleet carrier management panel".equals(e.content())),
                "memory keeps the canonical form used for matching and prompting");
        assertTrue(memory.entries().stream().noneMatch(
                        e -> e.source() == MemorySource.COMMANDER
                                && "open fleet career management panel".equals(e.content())),
                "memory must not retain the broken STT wording");
    }

    @Test
    void eventReactionStoresOnlyTheFinalNarrationAndOffersOnlySpeak() {
        // Source data is transient: it reaches the isolated EVENT prompt, while only the valid final line is stored.
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
        dispatcher.submitEventReaction("surface scan: alexandrite", "Report it briefly.", Urgency.NORMAL);
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
        assertEquals(1, memory.writes.size(), "the completed event is one atomic record");
        MemoryRecord event = memory.writes.get(0);
        assertEquals(MemoryKind.EVENT, event.kind());
        assertEquals(1, event.entryCount());
        assertEquals(MemorySource.EVENT, event.entries().get(0).source());
        assertEquals("Signals detected on the ring, Commander.", event.entries().get(0).content());
    }

    @Test
    void interruptBeforeDialogueCommitFilesNoPartialPair() throws InterruptedException {
        // An interrupt landing after the LLM round but during speak execution must prevent the pending input/reply
        // from being committed as a partial or stale dialogue pair.
        // The turn is awaited BEFORE stop(): stop() nulls the lanes first, which would make the barge-in a no-op.
        ThoughtDispatcher[] holder = new ThoughtDispatcher[1];
        AtomicInteger executionCalls = new AtomicInteger();
        ExecutionGateway interruptingExecution = request -> {
            executionCalls.incrementAndGet();
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
        waitUntil(() -> executionCalls.get() >= 1);
        waitUntil(dispatcher::isIdle);
        dispatcher.stop();

        assertTrue(memory.writes.isEmpty(), "interruption before commit leaves no input or boundary marker");
    }

    @Test
    void blankOrNullInputIsIgnored() {
        ThoughtDispatcher dispatcher = dispatcher();
        dispatcher.start();
        dispatcher.submitCommanderInput("   ");
        dispatcher.submitCommanderInput(null);
        dispatcher.submitEventReaction(null, null, Urgency.NORMAL);
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
        waitUntil(() -> llm.calls.get() >= 2);
        dispatcher.stop();

        assertTrue(llm.calls.get() >= 2, "the urgent thought ran after preempting the normal one");
        assertTrue(memory.entries().stream().noneMatch(e -> "slow task".equals(e.content())),
                "the preempted thought leaves no partial memory turn");
        assertTrue(memory.entries().stream().anyMatch(e -> "urgent stop".equals(e.content())),
                "the completed urgent turn is committed as dialogue");
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
        waitUntil(dispatcher::isIdle);
        dispatcher.stop();

        assertTrue(memory.writes.isEmpty(), "barge-in discards the incomplete turn without a memory marker");
    }

    @Test
    void watchdogInterruptsAStuckThought() throws InterruptedException {
        BlockFirstLlm llm = new BlockFirstLlm();
        // Tiny watchdog: 50ms timeout, checked every 10ms.
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependenciesWith(llm), UrgencyPolicy.normalOnly(), 50, 10);
        dispatcher.start();

        dispatcher.submitCommanderInput("stuck task"); // blocks on the LLM forever
        waitUntil(() -> llm.calls.get() >= 1);
        waitUntil(dispatcher::isIdle);
        dispatcher.stop();

        assertTrue(memory.writes.isEmpty(), "watchdog discards the incomplete turn without a memory marker");
    }

    @Test
    void watchdogRollsBackAStuckDetachedQueryAndDiscardsItsLateResult() throws InterruptedException {
        AtomicInteger querySubmissions = new AtomicInteger();
        AtomicInteger cancelAttempts = new AtomicInteger();
        CompletableFuture<JsonObject> startedQuery = new CompletableFuture<>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                cancelAttempts.incrementAndGet();
                return false;
            }
        };
        LlmGateway querySelectingLlm = new LlmGateway() {
            @Override
            public CompletableFuture<LlmResult> submit(LlmRequest request) {
                LlmToolInvocation query = new LlmToolInvocation(
                        UUID.randomUUID().toString(), "slow_query", new JsonObject());
                return CompletableFuture.completedFuture(new LlmResult(LlmResult.Status.OK, List.of(query)));
            }

            @Override
            public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        };
        ExecutionGateway execution = request -> {
            if ("slow_query".equals(request.toolName())) {
                querySubmissions.incrementAndGet();
                return startedQuery;
            }
            return CompletableFuture.completedFuture(new JsonObject());
        };
        FakeSpeech speech = new FakeSpeech();
        IntelActionTypeResolver actionTypes = new IntelActionTypeResolver(id ->
                "slow_query".equals(id)
                        ? IntelActionTypeResolver.IntelActionType.QUERY
                        : IntelActionTypeResolver.IntelActionType.SYSTEM);
        ThoughtDependencies dependencies = new ThoughtDependencies(
                querySelectingLlm, speech, execution, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> List.of(), new CompanionState(),
                invocation -> false, new ConfirmationCoordinator(), actionTypes);
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(
                dependencies, UrgencyPolicy.normalOnly(), new ReflexResolver(() -> List.of(), invocation -> false),
                Function.identity(), 50, 10);
        dispatcher.setSemanticReflexResolver(SemanticReflexResolver.disabled());
        dispatcher.start();

        dispatcher.submitCommanderInput("stuck query");
        waitUntil(() -> querySubmissions.get() == 1);
        assertTrue(memory.writes.isEmpty(), "pending QUERY must not publish a partial record");
        waitUntil(() -> cancelAttempts.get() > 0);
        JsonObject late = new JsonObject();
        late.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, "late answer");
        startedQuery.complete(late);
        waitUntil(dispatcher::isIdle);
        dispatcher.stop();

        assertTrue(memory.writes.isEmpty(), "watchdog interruption leaves no QUERY contract in memory");
        assertTrue(speech.requests.stream().noneMatch(request -> "late answer".equals(request.text())),
                "the watchdog-discarded QUERY cannot voice its late result");
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
                LlmToolInvocation settling;
                if (callNumber == 1) {
                    settling = new LlmToolInvocation(UUID.randomUUID().toString(), "slow_command", new JsonObject());
                } else {
                    JsonObject speakArgs = new JsonObject();
                    speakArgs.addProperty(SpeakFunction.PARAM_TEXT, "quick reply");
                    settling = new LlmToolInvocation(UUID.randomUUID().toString(), SpeakFunction.ID, speakArgs);
                }
                return CompletableFuture.completedFuture(new LlmResult(LlmResult.Status.OK, List.of(settling)));
            }

            @Override
            public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
                return CompletableFuture.completedFuture(null);
            }
        };
        ExecutionGateway execution = request -> {
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
        waitUntil(() -> memory.entries().stream().anyMatch(e -> "quick one".equals(e.content())));

        List<MemoryEntry> commanderInputs = memory.entries().stream()
                .filter(e -> e.source() == MemorySource.COMMANDER)
                .toList();
        assertEquals(List.of("quick one"),
                commanderInputs.stream().map(MemoryEntry::content).toList(),
                "only the completed LLM dialogue is committed");
        assertFalse(dispatcher.isIdle(), "the detached slow command remains owned by the dispatcher");

        JsonObject completed = new JsonObject();
        completed.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, "slow complete");
        slowResult.complete(completed);
        waitUntil(dispatcher::isIdle);
        assertTrue(memory.entries().stream().noneMatch(e -> "slow complete".equals(e.content())),
                "the late command result is voiced execution feedback, not memory");
        dispatcher.stop();
    }

    @Test
    void aFailingThoughtDoesNotKillTheLane() {
        // A reducer that always throws makes every thought fail during prompt assembly.
        AtomicInteger reducerCalls = new AtomicInteger();
        ThoughtDependencies dependencies = new ThoughtDependencies(
                new TerminatingLlm(), new FakeSpeech(), new FakeExecution(), memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(),
                (categories, currentInput) -> {
                    reducerCalls.incrementAndGet();
                    throw new RuntimeException("boom");
                }, new CompanionState(),
                invocation -> false, new ConfirmationCoordinator());
        ThoughtDispatcher dispatcher = new ThoughtDispatcher(dependencies);
        dispatcher.start();

        dispatcher.submitCommanderInput("first");
        dispatcher.submitCommanderInput("second");
        dispatcher.stop();

        assertEquals(2, reducerCalls.get(), "the lane survives the first failure and attempts the second turn");
        assertTrue(memory.writes.isEmpty(), "failed turns form no dialogue pairs");
    }

    // --- helpers ---

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }
    }

    // --- fakes ---

    /** Settles every turn immediately with speak, so a thought commits one dialogue pair and stops. */
    private static final class TerminatingLlm implements LlmGateway {
        @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
            JsonObject args = new JsonObject();
            args.addProperty(SpeakFunction.PARAM_TEXT, "done");
            LlmToolInvocation terminator = new LlmToolInvocation(
                    UUID.randomUUID().toString(), SpeakFunction.ID, args);
            return CompletableFuture.completedFuture(new LlmResult(LlmResult.Status.OK, List.of(terminator)));
        }

        @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            return CompletableFuture.completedFuture(null);
        }
    }

    /** Blocks the first turn forever (the preempted thought) and settles later turns with speak. */
    private static final class BlockFirstLlm implements LlmGateway {
        final AtomicInteger calls = new AtomicInteger();

        @Override public CompletableFuture<LlmResult> submit(LlmRequest request) {
            if (calls.incrementAndGet() == 1) {
                return new CompletableFuture<>(); // never completes; interrupt (cancel) unblocks it
            }
            JsonObject args = new JsonObject();
            args.addProperty(SpeakFunction.PARAM_TEXT, "done");
            LlmToolInvocation terminator = new LlmToolInvocation(
                    UUID.randomUUID().toString(), SpeakFunction.ID, args);
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
            JsonObject args = new JsonObject();
            args.addProperty(SpeakFunction.PARAM_TEXT, "done");
            LlmToolInvocation terminator = new LlmToolInvocation(
                    UUID.randomUUID().toString(), SpeakFunction.ID, args);
            return CompletableFuture.completedFuture(new LlmResult(LlmResult.Status.OK, List.of(terminator)));
        }

        @Override public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class FakeMemory implements MemoryGateway {
        final List<MemoryRecord> writes = new CopyOnWriteArrayList<>();

        @Override public void write(MemoryRecord record) { writes.add(record); }
        @Override public List<MemoryRecord> readRecentHistory() { return List.of(); }
        @Override public MemorySearchResult recallMatching(String query, int limit) {
            return MemorySearchResult.empty();
        }
        @Override public List<MemorySearchMatch> recallFactCandidates(String query, int limit) { return List.of(); }
        @Override public Map<MemoryKind, String> longTermSummaries() { return Map.of(); }
        @Override public void commitConsolidation(
                MemoryKind kind, List<MemoryRecord> batch, String summary
        ) { }
        @Override public List<MemoryRecord> savedTextRecords() { return List.of(); }
        @Override public MemorySnapshot snapshot() {
            return new MemorySnapshot(List.of(), Map.of(), Map.of(), Map.of(), List.of());
        }

        List<MemoryEntry> entries() {
            return writes.stream().flatMap(record -> record.entries().stream()).toList();
        }
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
