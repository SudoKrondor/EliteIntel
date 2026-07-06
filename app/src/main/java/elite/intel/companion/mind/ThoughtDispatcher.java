package elite.intel.companion.mind;

import elite.intel.ai.brain.InputNormalizer;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.diag.CompanionDiagnostics;
import elite.intel.companion.input.EventTopicMap;
import elite.intel.companion.input.SensorInputFormatter;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.prompt.ReflexResolver;
import elite.intel.companion.prompt.SemanticReflexResolver;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.ui.controller.ManagedService;
import elite.intel.ui.event.CommanderMatchInputChangedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The accounting/scheduling node of the consciousness. Owns one serialized {@link ThoughtLane} per
 * {@link ThoughtSource}, so at most one COMMANDER, one EVENT, and one NARRATION thought are live at a time
 * (they may run concurrently); a lane's deque is that source's thought queue. It assigns urgency at thought
 * birth and drives preemption, but does not interpret meaning or know a thought's internal state (§2.3).
 * <p>
 * The lanes are held in one source-keyed map, published as a single volatile reference: cross-cutting
 * operations (start/stop, interrupt, watchdog, idle) iterate the lanes, while a submit targets the lane of
 * its source. A separate NARRATION lane keeps the slow LLM narration round off the EVENT lane, so the
 * memory-only event "knowing" channel records without queuing behind it.
 * <p>
 * Urgency (§1.1.4/§1.7.29): a normal thought queues at its lane's tail; an urgent thought interrupts every
 * live thought (regardless of its origin) and jumps to its lane's head. The urgent-phrase /
 * urgent-event-type matchers are a tunable concern (§7.1); by the default policy nothing is urgent yet,
 * except subscriber narration, which is born urgent.
 * <p>
 * A watchdog periodically force-interrupts a thought that overruns the timeout (§2.3). Barge-in reaches
 * the live thoughts via {@link #interruptLiveThoughts()}.
 */
public final class ThoughtDispatcher implements ManagedService, VerbatimNarrationSink {

    private static final Logger log = LogManager.getLogger(ThoughtDispatcher.class);

    /** Grace period for a lane to drain on stop before its live thoughts are force-interrupted. */
    private static final long SHUTDOWN_WAIT_MILLIS = 5000;
    /** A thought running longer than this is force-interrupted by the watchdog (§2.3 / §7.2 setting). */
    private static final long WATCHDOG_TIMEOUT_MILLIS = 60_000;
    /** How often the watchdog checks the live thoughts. */
    private static final long WATCHDOG_INTERVAL_MILLIS = 5_000;

    /** Production input canonicalizer: the legacy synonym map ("combat mode" -> "switch to combat mode"). */
    private static final Function<String, String> DEFAULT_NORMALIZER = InputNormalizer.getInstance()::normalize;

    private final ThoughtContext ctx;
    private final UrgencyPolicy urgencyPolicy;
    private final ReflexResolver reflexResolver;
    /**
     * The semantic reflex gate, tried after the exact-alias {@link #reflexResolver}: a confident, unambiguous
     * embedding match dispatches directly (command or query), so a weak command model is never asked to pick a
     * tool the embedder already identified. A no-op when the embedding model is unavailable. Package-private and
     * replaceable so a test exercising the LLM/preemption path can pin it to {@link SemanticReflexResolver#disabled()}.
     */
    private volatile SemanticReflexResolver semanticReflexResolver = new SemanticReflexResolver();

    /**
     * Test seam: disable (or pin) the semantic reflex so a preemption/LLM-path test is not intercepted by it.
     */
    void setSemanticReflexResolver(SemanticReflexResolver resolver) {
        this.semanticReflexResolver = resolver;
    }

    /**
     * Canonicalizes commander input for command matching only (the reflex gate and the reducer/LLM prompt);
     * memory keeps the raw words. Reuses the legacy {@link InputNormalizer} owner so a synonym phrase is
     * recognized the same way the legacy router recognizes it.
     */
    private final Function<String, String> inputNormalizer;
    private final long watchdogTimeoutMillis;
    private final long watchdogIntervalMillis;

    /** One serialized lane per source; null until {@link #start()}, published as a single volatile reference. */
    private volatile Map<ThoughtSource, ThoughtLane> lanes;
    private volatile ScheduledExecutorService watchdog;

    public ThoughtDispatcher(ThoughtContext ctx) {
        this(ctx, UrgencyPolicy.normalOnly(), new ReflexResolver(), DEFAULT_NORMALIZER,
                WATCHDOG_TIMEOUT_MILLIS, WATCHDOG_INTERVAL_MILLIS);
    }

    /** Wires the dispatcher with an explicit reflex resolver (production wiring, or a test pinning the gate). */
    public ThoughtDispatcher(ThoughtContext ctx, ReflexResolver reflexResolver) {
        this(ctx, UrgencyPolicy.normalOnly(), reflexResolver, DEFAULT_NORMALIZER,
                WATCHDOG_TIMEOUT_MILLIS, WATCHDOG_INTERVAL_MILLIS);
    }

    /** Test seam: inject the reflex resolver and the input normalizer to exercise canonicalization routing. */
    ThoughtDispatcher(ThoughtContext ctx, ReflexResolver reflexResolver, Function<String, String> inputNormalizer) {
        this(ctx, UrgencyPolicy.normalOnly(), reflexResolver, inputNormalizer,
                WATCHDOG_TIMEOUT_MILLIS, WATCHDOG_INTERVAL_MILLIS);
    }

    /** Test seam: inject the urgency policy to exercise preemption. */
    ThoughtDispatcher(ThoughtContext ctx, UrgencyPolicy urgencyPolicy) {
        this(ctx, urgencyPolicy, new ReflexResolver(), DEFAULT_NORMALIZER,
                WATCHDOG_TIMEOUT_MILLIS, WATCHDOG_INTERVAL_MILLIS);
    }

    /** Test seam: inject the reflex resolver to exercise reflex-vs-commander routing. */
    ThoughtDispatcher(ThoughtContext ctx, UrgencyPolicy urgencyPolicy, ReflexResolver reflexResolver) {
        this(ctx, urgencyPolicy, reflexResolver, DEFAULT_NORMALIZER,
                WATCHDOG_TIMEOUT_MILLIS, WATCHDOG_INTERVAL_MILLIS);
    }

    /** Test seam: inject the urgency policy and watchdog timing. */
    ThoughtDispatcher(ThoughtContext ctx, UrgencyPolicy urgencyPolicy,
                      long watchdogTimeoutMillis, long watchdogIntervalMillis) {
        this(ctx, urgencyPolicy, new ReflexResolver(), DEFAULT_NORMALIZER,
                watchdogTimeoutMillis, watchdogIntervalMillis);
    }

    /** Canonical constructor: all collaborators, the input normalizer, and watchdog timing explicit. */
    ThoughtDispatcher(ThoughtContext ctx, UrgencyPolicy urgencyPolicy, ReflexResolver reflexResolver,
                      Function<String, String> inputNormalizer,
                      long watchdogTimeoutMillis, long watchdogIntervalMillis) {
        this.ctx = ctx;
        this.urgencyPolicy = urgencyPolicy;
        this.reflexResolver = reflexResolver;
        this.inputNormalizer = inputNormalizer;
        this.watchdogTimeoutMillis = watchdogTimeoutMillis;
        this.watchdogIntervalMillis = watchdogIntervalMillis;
    }

    /**
     * Accepts a commander reply and queues it on the commander lane. The input is first canonicalized by the
     * {@link #inputNormalizer} (synonym map) - this is the form used for command matching only: the reflex
     * gate and, on the LLM path, the reducer and the prompt's current-input. The raw words are passed through
     * unchanged for memory. The reflex gate runs first ({@link ReflexResolver}): a canonicalized input that
     * matches a training phrase verbatim and resolves to exactly one safe, parameterless command becomes a
     * deterministic {@code ReflexThought} (no LLM); everything else becomes a full {@code CommanderThought}.
     */
    public void submitCommanderInput(String input) {
        if (input == null || input.isBlank()) {
            return;
        }
        Urgency urgency = urgencyPolicy.forCommander(input);
        // Strip a leading vocative address by the companion's own name ("Vega, all stop", or - as STT usually
        // returns it, with no comma - "Vega all stop" / "Вега все стоп") before normalizing, for BOTH paths: the
        // reflex fast-path and the LLM path (the reducer and the prompt's current-input). The name carries no
        // routing signal, and a leading "Vega," in the current-input was throwing the model off - a command that
        // routed fine without it fell to a bare classify_turn with it. Memory still keeps the raw words: they are
        // recorded from `input`, never from this normalized match text.
        String rawStripped = stripLeadingCompanionName(input);
        String matchInput = inputNormalizer.apply(rawStripped);
        ctx.state().setLastCommanderMatchInput(matchInput);
        UiBus.publish(new CommanderMatchInputChangedEvent(matchInput));
        // Exact-alias reflex: try the commander's actual words FIRST, then the normalized form. The synonym
        // normalizer canonicalizes for the reducer/LLM but can rewrite an exact alias into a phrase that is not
        // itself an alias ("where are we" -> "what is our current location"), which would otherwise defeat the
        // deterministic reflex for a phrase the commander said verbatim.
        Optional<String> reflexCommand = reflexResolver.resolve(rawStripped);
        if (reflexCommand.isEmpty()) {
            reflexCommand = reflexResolver.resolve(matchInput);
        }
        if (reflexCommand.isEmpty()) {
            // No verbatim exact-alias match: try the semantic reflex (a confident, unambiguous embedding match
            // dispatches without the LLM - the weak model is not asked to pick a tool the embedder already found).
            reflexCommand = semanticReflexResolver.resolve(matchInput);
        }
        Thought thought = reflexCommand
                .map(actionId -> Thought.reflex(urgency, input, actionId, ctx))
                .orElseGet(() -> Thought.commander(urgency, input, matchInput, ctx));
        CompanionDiagnostics.info(thought.trace(), "intake",
                "\"" + CompanionDiagnostics.truncate(input) + "\" -> " + reflexCommand.map(id -> "reflex " + id).orElse("think"));
        if (!matchInput.equals(input)) {
            // The normalized/name-stripped form actually used for tool matching and the LLM current-input.
            CompanionDiagnostics.debug(thought.trace(), "intake", "match text: \"" + CompanionDiagnostics.truncate(matchInput) + "\"");
        }
        enqueue(ThoughtSource.COMMANDER, thought, urgency);
    }

    /**
     * Removes a single leading vocative use of the companion's name (e.g. "Vega, ..." or, as STT usually returns
     * it without a comma, "Vega ..." / "Вега ...") from the input, used for both the reflex fast-path and the LLM
     * match text (reducer + prompt current-input). Any recognized name form
     * ({@link CompanionConfig#companionNameForms()}: the canonical name plus transliterations) is matched as a
     * whole leading word - a Unicode-aware {@code \b}, so a Cyrillic form matches too - so it is an address, not
     * part of a longer word; any following separators/spaces are consumed (all optional, so a comma-less STT
     * address still strips). If only the name remains (a bare address), the original input is returned unchanged.
     */
    private static String stripLeadingCompanionName(String input) {
        String alternation = CompanionConfig.companionNameForms().stream()
                .filter(form -> form != null && !form.isBlank())
                .map(form -> Pattern.quote(form.trim()))
                .collect(Collectors.joining("|"));
        if (alternation.isEmpty()) {
            return input;
        }
        Pattern leadingName = Pattern.compile(
                "^\\s*(?:" + alternation + ")\\b[\\s,.:;!?-]*",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
        String stripped = leadingName.matcher(input).replaceFirst("");
        return stripped.isBlank() ? input : stripped;
    }

    /**
     * Accepts a filtered game event, creates an EVENT thought, and queues it on the event lane. The EVENT
     * thought is memory-only: it records the event's readable {@link BaseEvent#memorySummary()} when non-blank
     * (an event with no summary writes nothing), and the LLM is never engaged (see {@code EventThought}).
     */
    public void submitEvent(BaseEvent event) {
        if (event == null) {
            return;
        }
        Urgency urgency = urgencyPolicy.forEvent(event);
        ConversationTopic topic = EventTopicMap.topicFor(event);
        Thought thought = Thought.event(urgency, event.memorySummary(), topic, ctx);
        CompanionDiagnostics.debug(thought.trace(), "event",
                "\"" + CompanionDiagnostics.truncate(event.memorySummary()) + "\" type=" + event.getEventType()
                        + " topic=" + topic);
        enqueue(ThoughtSource.EVENT, thought, urgency);
    }

    /**
     * Accepts subscriber-prepared sensor narration, creates a NARRATION thought, and queues it on the
     * narration lane. SensorDataEvent is trusted output from gameplay subscribers: they already applied
     * settings, filtering, and calculations. The thought is queued at normal urgency under the topic
     * provided by that layer, so a burst of sensor callouts plays in order instead of each one cutting off
     * the line before it.
     */
    public void submitSensorData(SensorDataEvent event) {
        if (event == null) {
            return;
        }
        Urgency urgency = Urgency.NORMAL;
        ConversationTopic topic = sensorTopic(event);
        Thought thought = Thought.sensorNarration(urgency, SensorInputFormatter.format(event), topic, ctx);
        CompanionDiagnostics.debug(thought.trace(), "narration", "sensor topic=" + topic);
        enqueue(ThoughtSource.NARRATION, thought, urgency);
    }

    /**
     * Accepts a curated announcement that already carries finished text (mining/discovery/route/
     * navigation), creates a verbatim NARRATION thought, and queues it on the narration lane at normal
     * urgency, so a burst of announcements plays in order instead of each one cutting off the line before
     * it. A caller that must preempt current speech (radar contact, mission-critical) uses the explicit
     * urgency overload. The line is remembered and voiced verbatim in the companion's voice - no LLM
     * phrasing.
     */
    @Override
    public void submitVerbatimNarration(String text, ConversationTopic topic) {
        submitVerbatimNarration(text, topic, Urgency.NORMAL, null);
    }

    /**
     * Verbatim narration with an explicit urgency and an optional {@code spokenSignal} completed when playback
     * ends - used to bridge a command/macro's own narration ({@code AiVoxResponseEvent}/
     * {@code MissionCriticalAnnouncementEvent}) so a synchronous caller waits the same as on the legacy path.
     */
    @Override
    public void submitVerbatimNarration(String text, ConversationTopic topic, Urgency urgency,
                                        java.util.concurrent.CompletableFuture<Void> spokenSignal) {
        submitVerbatimNarration(text, topic, urgency, spokenSignal, null);
    }

    /**
     * As above, but attributes the line to a model tool-call ({@code toolCallId}), so it is remembered as that
     * call's tool result rather than free-standing companion speech (see {@code VerbatimNarrationThought}).
     */
    @Override
    public void submitVerbatimNarration(String text, ConversationTopic topic, Urgency urgency,
                                        java.util.concurrent.CompletableFuture<Void> spokenSignal, String toolCallId) {
        if (text == null || text.isBlank()) {
            if (spokenSignal != null) {
                spokenSignal.complete(null); // never strand a caller blocked on an empty line
            }
            return;
        }
        Thought thought = Thought.verbatimNarration(urgency, text, topic, ctx, spokenSignal, toolCallId);
        CompanionDiagnostics.debug(thought.trace(), "narration",
                "verbatim topic=" + topic + ": \"" + CompanionDiagnostics.truncate(text) + "\"");
        enqueue(ThoughtSource.NARRATION, thought, urgency);
    }

    @Override
    public void start() {
        if (lanes == null) {
            Map<ThoughtSource, ThoughtLane> built = new EnumMap<>(ThoughtSource.class);
            // Commander lane is a bounded pool: a long synchronous command/query occupies a worker, so several
            // let new commander input run meanwhile instead of blocking; the rest queue (§1.2). EVENT/NARRATION
            // stay single-worker (no slow handlers there).
            built.put(ThoughtSource.COMMANDER,
                    new ThoughtLane("companion-commander", CompanionConfig.maxParallelCommanderThoughts()));
            built.put(ThoughtSource.EVENT, new ThoughtLane("companion-event", 1));
            built.put(ThoughtSource.NARRATION, new ThoughtLane("companion-narration", 1));
            lanes = built; // single volatile publish of the fully-built lane set
        }
        if (watchdog == null) {
            watchdog = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "companion-watchdog");
                thread.setDaemon(true);
                return thread;
            });
            watchdog.scheduleAtFixedRate(this::checkWatchdog,
                    watchdogIntervalMillis, watchdogIntervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stop() {
        ScheduledExecutorService currentWatchdog = watchdog;
        watchdog = null;
        if (currentWatchdog != null) {
            currentWatchdog.shutdownNow();
        }
        Map<ThoughtSource, ThoughtLane> current = lanes;
        lanes = null; // stop accepting new work before draining
        if (current != null) {
            current.values().forEach(lane -> lane.shutdown(SHUTDOWN_WAIT_MILLIS));
        }
    }

    /** Interrupts every live thought on barge-in (§2.15); the dispatcher owns the thought lifecycle, not speech. */
    public void interruptLiveThoughts() {
        CompanionDiagnostics.debug(CompanionDiagnostics.SYSTEM, "barge-in", "interrupting live thoughts");
        interruptLive();
    }

    /** Whether all lanes are idle (no live thought, empty queues) - a turn-boundary signal for harnesses. */
    public boolean isIdle() {
        Map<ThoughtSource, ThoughtLane> snapshot = lanes;
        return snapshot == null || snapshot.values().stream().allMatch(ThoughtLane::isIdle);
    }

    /** Queues a thought on its source's lane; an urgent one interrupts every live thought and jumps its head. */
    private void enqueue(ThoughtSource source, Thought thought, Urgency urgency) {
        Map<ThoughtSource, ThoughtLane> snapshot = lanes;
        ThoughtLane lane = snapshot == null ? null : snapshot.get(source);
        if (lane == null) {
            return; // subsystem not running (input racing lifecycle)
        }
        if (urgency == Urgency.URGENT) {
            interruptLive();
            lane.submitFirst(thought);
        } else {
            lane.submit(thought);
        }
    }

    /** Interrupts every live thought, regardless of the urgent thought's origin. */
    private void interruptLive() {
        Map<ThoughtSource, ThoughtLane> snapshot = lanes;
        if (snapshot != null) {
            snapshot.values().forEach(ThoughtLane::interruptLive);
        }
    }

    /** Watchdog tick: force-interrupt any thought that has been running past the timeout (§2.3). */
    private void checkWatchdog() {
        try {
            Map<ThoughtSource, ThoughtLane> snapshot = lanes;
            if (snapshot != null) {
                snapshot.values().forEach(this::interruptIfStuck);
            }
        } catch (RuntimeException unexpected) {
            // Never let a tick failure cancel the periodic schedule (scheduleAtFixedRate stops on throw).
            log.error("Companion watchdog tick failed", unexpected);
        }
    }

    private void interruptIfStuck(ThoughtLane lane) {
        lane.interruptStuck(watchdogTimeoutMillis);
    }

    private static ConversationTopic sensorTopic(SensorDataEvent event) {
        try {
            return ConversationTopic.valueOf(event.getTopic().trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException invalidTopic) {
            return ConversationTopic.SYSTEM;
        }
    }
}
