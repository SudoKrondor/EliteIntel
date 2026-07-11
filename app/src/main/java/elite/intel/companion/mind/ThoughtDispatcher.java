package elite.intel.companion.mind;

import elite.intel.ai.brain.InputNormalizer;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.diag.CompanionDiagnostics;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.prompt.ReflexResolver;
import elite.intel.companion.prompt.SemanticReflexResolver;
import elite.intel.eventbus.UiBus;
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
 * The accounting/scheduling node of the consciousness. Owns one ordered {@link ThoughtLane} per
 * {@link ThoughtSource}; a lane's deque is that source's cognitive queue. COMMANDER cognition is serialized, but
 * completed cognitive stages may leave detached handlers live while the worker accepts later turns. It assigns
 * urgency at thought birth and drives preemption without interpreting a thought's meaning (§2.3).
 * <p>
 * The lanes are held in one source-keyed map, published as a single volatile reference: cross-cutting
 * operations (start/stop, interrupt, watchdog, idle) iterate the lanes, while a submit targets the lane of
 * its source. The EVENT lane carries subscriber-driven reactions (a reaction to a game event); it is kept off
 * the commander lane so a slow reaction never blocks live commander input, and vice versa.
 * <p>
 * Urgency (§1.1.4/§1.7.29): a normal thought queues at its lane's tail; an urgent thought interrupts every
 * live thought (regardless of its origin) and jumps to its lane's head. The urgent-phrase /
 * urgent-event-type matchers are a tunable concern (§7.1).
 * <p>
 * A watchdog periodically force-interrupts a thought that overruns the timeout (§2.3). Barge-in reaches
 * the live thoughts via {@link #interruptLiveThoughts()}.
 */
public final class ThoughtDispatcher implements ManagedService {

    private static final Logger log = LogManager.getLogger(ThoughtDispatcher.class);

    /** Grace period for a lane to drain on stop before its live thoughts are force-interrupted. */
    private static final long SHUTDOWN_WAIT_MILLIS = 5000;
    /** A thought running longer than this is force-interrupted by the watchdog (§2.3 / §7.2 setting). */
    private static final long WATCHDOG_TIMEOUT_MILLIS = 60_000;
    /** How often the watchdog checks the live thoughts. */
    private static final long WATCHDOG_INTERVAL_MILLIS = 5_000;

    /** Production input canonicalizer: the legacy synonym map ("combat mode" -> "switch to combat mode"). */
    private static final Function<String, String> DEFAULT_NORMALIZER = InputNormalizer.getInstance()::normalize;

    private final ThoughtDependencies dependencies;
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

    public ThoughtDispatcher(ThoughtDependencies dependencies) {
        this(dependencies, UrgencyPolicy.normalOnly(), new ReflexResolver(), DEFAULT_NORMALIZER,
                WATCHDOG_TIMEOUT_MILLIS, WATCHDOG_INTERVAL_MILLIS);
    }

    /** Wires the dispatcher with an explicit reflex resolver (production wiring, or a test pinning the gate). */
    public ThoughtDispatcher(ThoughtDependencies dependencies, ReflexResolver reflexResolver) {
        this(dependencies, UrgencyPolicy.normalOnly(), reflexResolver, DEFAULT_NORMALIZER,
                WATCHDOG_TIMEOUT_MILLIS, WATCHDOG_INTERVAL_MILLIS);
    }

    /** Test seam: inject the reflex resolver and the input normalizer to exercise canonicalization routing. */
    ThoughtDispatcher(ThoughtDependencies dependencies, ReflexResolver reflexResolver, Function<String, String> inputNormalizer) {
        this(dependencies, UrgencyPolicy.normalOnly(), reflexResolver, inputNormalizer,
                WATCHDOG_TIMEOUT_MILLIS, WATCHDOG_INTERVAL_MILLIS);
    }

    /** Test seam: inject the urgency policy to exercise preemption. */
    ThoughtDispatcher(ThoughtDependencies dependencies, UrgencyPolicy urgencyPolicy) {
        this(dependencies, urgencyPolicy, new ReflexResolver(), DEFAULT_NORMALIZER,
                WATCHDOG_TIMEOUT_MILLIS, WATCHDOG_INTERVAL_MILLIS);
    }

    /** Test seam: inject the reflex resolver to exercise reflex-vs-commander routing. */
    ThoughtDispatcher(ThoughtDependencies dependencies, UrgencyPolicy urgencyPolicy, ReflexResolver reflexResolver) {
        this(dependencies, urgencyPolicy, reflexResolver, DEFAULT_NORMALIZER,
                WATCHDOG_TIMEOUT_MILLIS, WATCHDOG_INTERVAL_MILLIS);
    }

    /** Test seam: inject the urgency policy and watchdog timing. */
    ThoughtDispatcher(ThoughtDependencies dependencies, UrgencyPolicy urgencyPolicy,
                      long watchdogTimeoutMillis, long watchdogIntervalMillis) {
        this(dependencies, urgencyPolicy, new ReflexResolver(), DEFAULT_NORMALIZER,
                watchdogTimeoutMillis, watchdogIntervalMillis);
    }

    /** Canonical constructor: all collaborators, the input normalizer, and watchdog timing explicit. */
    ThoughtDispatcher(ThoughtDependencies dependencies, UrgencyPolicy urgencyPolicy, ReflexResolver reflexResolver,
                      Function<String, String> inputNormalizer,
                      long watchdogTimeoutMillis, long watchdogIntervalMillis) {
        this.dependencies = dependencies;
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
        long acceptedAtNanos = System.nanoTime();
        Urgency urgency = urgencyPolicy.forCommander(input);
        // Strip a leading vocative address by the companion's own name ("Vega, all stop", or - as STT usually
        // returns it, with no comma - "Vega all stop" / "Вега все стоп") before normalizing, for BOTH paths: the
        // reflex fast-path and the LLM path (the reducer and the prompt's current-input). The name carries no
        // routing signal, and a leading "Vega," in the current-input was throwing the model off - a command that
        // routed fine without it fell to a bare classify_turn with it. Memory still keeps the raw words: they are
        // recorded from `input`, never from this normalized match text.
        String rawStripped = stripLeadingCompanionName(input);
        String matchInput = inputNormalizer.apply(rawStripped);
        ThoughtContext context = ThoughtContext.commander(urgency, input, matchInput, acceptedAtNanos);
        dependencies.state().setLastCommanderMatchInput(matchInput); // observer snapshot only; this turn owns context
        UiBus.publish(new CommanderMatchInputChangedEvent(matchInput));
        // Exact-alias reflex: try the commander's actual words FIRST, then the normalized form. The synonym
        // normalizer canonicalizes for the reducer/LLM but can rewrite an exact alias into a phrase that is not
        // itself an alias ("where are we" -> "what is our current location"), which would otherwise defeat the
        // deterministic reflex for a phrase the commander said verbatim.
        Optional<String> reflexCommand = reflexResolver.resolve(rawStripped);
        if (reflexCommand.isEmpty()) {
            reflexCommand = reflexResolver.resolve(matchInput);
        }
        // Which reflex mechanism fired, for the intake log: the verbatim exact-alias reflex, or - failing that -
        // the semantic embedding shortcut. Both dispatch a known action without the LLM (a ReflexThought); the log
        // spells out which one so an exact-phrase reflex is never confused with a semantic-similarity match.
        String reflexKind = reflexCommand.isPresent() ? "exact" : null;
        long semanticReflexMillis = -1;
        if (reflexCommand.isEmpty()) {
            // No verbatim exact-alias match: try the semantic reflex (a confident, unambiguous embedding match
            // dispatches without the LLM - the weak model is not asked to pick a tool the embedder already found).
            long semanticReflexStartedNanos = System.nanoTime();
            SemanticReflexResolver.Resolution semanticResolution =
                    semanticReflexResolver.resolveWithSemanticQuery(matchInput);
            semanticReflexMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - semanticReflexStartedNanos);
            reflexCommand = semanticResolution.actionId();
            context = context.withSemanticQuery(semanticResolution.semanticQuery());
            if (reflexCommand.isPresent()) {
                reflexKind = "semantic";
            }
        }
        ThoughtContext finalContext = context;
        Thought thought = reflexCommand
                .map(actionId -> Thought.reflex(finalContext, actionId, dependencies))
                .orElseGet(() -> Thought.commander(finalContext, dependencies));
        String route = reflexCommand.isPresent()
                ? "reflex " + reflexCommand.get() + " (" + reflexKind + ")"
                : "think";
        CompanionDiagnostics.info(thought.trace(), "intake",
                "\"" + CompanionDiagnostics.truncate(input) + "\" -> " + route);
        if (semanticReflexMillis >= 0) {
            CompanionDiagnostics.debug(thought.trace(), "semantic-reflex", semanticReflexMillis + " ms");
        }
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
     * Accepts a gameplay subscriber's request to <b>react out loud</b> (a {@code CompanionReactionEvent}) and
     * queues a reactive {@link EventThought} on the EVENT lane. The subscriber pre-digested the data, so the
     * thought only phrases {@code stimulus} and speaks it; the stimulus is recorded as a {@code user} turn and
     * the reply as the companion's own words, keeping a clean two-party dialogue. {@code instructions} steer only
     * this turn's phrasing and are not remembered.
     */
    public void submitEventReaction(String stimulus, String instructions, String topic, Urgency urgency) {
        if (stimulus == null || stimulus.isBlank()) {
            return;
        }
        ConversationTopic conversationTopic = topicFrom(topic);
        Thought thought = Thought.eventReaction(urgency, stimulus, instructions, conversationTopic, dependencies);
        CompanionDiagnostics.debug(thought.trace(), "event", "reaction topic=" + conversationTopic);
        enqueue(ThoughtSource.EVENT, thought, urgency);
    }

    /**
     * Accepts a gameplay subscriber's <b>finished phrase</b> to voice verbatim (no LLM) and queues a verbatim
     * {@link EventThought} on the EVENT lane. The phrase is recorded as the companion's reply, paired with the
     * short {@code sourceId} as the {@code user} turn (never the raw data), so the timeline keeps a clean
     * two-party dialogue. A blank phrase is ignored.
     */
    public void submitEventVerbatim(String sourceId, String phrase, String topic, Urgency urgency) {
        if (phrase == null || phrase.isBlank()) {
            return;
        }
        ConversationTopic conversationTopic = topicFrom(topic);
        Thought thought = Thought.eventVerbatim(urgency, sourceId, phrase, conversationTopic, dependencies);
        CompanionDiagnostics.debug(thought.trace(), "event", "verbatim topic=" + conversationTopic);
        enqueue(ThoughtSource.EVENT, thought, urgency);
    }

    @Override
    public void start() {
        if (lanes == null) {
            Map<ThoughtSource, ThoughtLane> built = new EnumMap<>(ThoughtSource.class);
            // Commander cognition is one ordered stream: prompt, classification, topic change and input commit
            // follow intake order. Slow game handlers detach while the lane keeps their lifecycle live,
            // so this worker accepts the next turn immediately after dispatch. EVENT remains single-worker too.
            built.put(ThoughtSource.COMMANDER, new ThoughtLane("companion-commander", 1));
            built.put(ThoughtSource.EVENT, new ThoughtLane("companion-event", 1));
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

    /** Maps a neutral topic tag (a {@code ConversationTopic} name) to the enum, falling back to SYSTEM when unknown/blank. */
    private static ConversationTopic topicFrom(String topic) {
        try {
            return ConversationTopic.valueOf(topic.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException invalidTopic) {
            return ConversationTopic.SYSTEM;
        }
    }
}
