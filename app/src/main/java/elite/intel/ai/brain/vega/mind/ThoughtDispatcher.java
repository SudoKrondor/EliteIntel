package elite.intel.ai.brain.vega.mind;

import elite.intel.ai.brain.i18n.PhoneticInputNormalizer;
import elite.intel.ai.brain.vega.CompanionAddressing;
import elite.intel.ai.brain.vega.CompanionConfig;
import elite.intel.ai.brain.vega.clarify.PendingClarification;
import elite.intel.ai.brain.vega.diag.CompanionDiagnostics;
import elite.intel.ai.brain.vega.model.GameStateSnapshot;
import elite.intel.ai.brain.vega.model.ThoughtSource;
import elite.intel.ai.brain.vega.model.Urgency;
import elite.intel.ai.brain.vega.prompt.ReflexResolver;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.controller.ManagedService;
import elite.intel.ui.event.CommanderMatchInputChangedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
    private static final long WATCHDOG_TIMEOUT_MILLIS = CompanionConfig.thoughtWatchdogTimeout().toMillis();
    /** How often the watchdog checks the live thoughts. */
    private static final long WATCHDOG_INTERVAL_MILLIS = 5_000;

    /**
     * Production input canonicalizer: only per-language acoustic STT corrections. The {@link #inputNormalizer}
     * seam is retained so a test can still inject a canonicalizing function.
     */
    private static final Function<String, String> DEFAULT_NORMALIZER = PhoneticInputNormalizer::normalize;

    private final ThoughtDependencies dependencies;
    private final UrgencyPolicy urgencyPolicy;
    private final ReflexResolver reflexResolver;
    /**
     * Canonicalizes commander input for command matching, LLM prompting, and any memory settlement this turn earns.
     * Production applies only per-language acoustic STT corrections; the seam remains injectable for tests.
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
    public ThoughtDispatcher(ThoughtDependencies dependencies, UrgencyPolicy urgencyPolicy, ReflexResolver reflexResolver) {
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
     * {@link #inputNormalizer} (acoustic corrections only) - this is the form used for the reflex gate and, on
     * the LLM path, the reducer, prompt current-input, and conversational memory. The raw words are retained for
     * execution and intake diagnostics. The reflex gate runs first ({@link ReflexResolver}): a canonicalized input that
     * matches a training phrase verbatim and resolves to exactly one safe command, with every argument the alias
     * supplies, becomes a deterministic {@code ReflexThought} (no LLM); everything else becomes a full
     * {@code CommanderThought}.
     */
    public void submitCommanderInput(String input) {
        if (input == null || input.isBlank()) {
            return;
        }
        long acceptedAtNanos = System.nanoTime();
        PendingClarification pendingClarification = dependencies.clarificationCoordinator()
                .claim().orElse(null);
        // One coherent visibility context owns the whole turn. Exact reflex and the reducer must never re-read a
        // changing player_status row independently.
        GameStateSnapshot gameStateSnapshot = GameStateSnapshot.capture();
        Urgency urgency = urgencyPolicy.forCommander(input);
        // Strip a leading vocative address by the companion's own name ("Vega, all stop", or - as STT usually
        // returns it, with no comma - "Vega all stop" / "Вега все стоп") before normalizing, for BOTH paths: the
        // reflex fast-path and the LLM path (the reducer, prompt current-input, and eligible memory). The name carries no
        // routing signal and can distract the model from the command. Raw STT stays only in intake diagnostics
        // and the execution request; a completed dialogue/query remembers the normalized match text.
        String rawStripped = CompanionAddressing.stripLeadingName(input);
        String matchInput = inputNormalizer.apply(rawStripped);
        ThoughtContext context = ThoughtContext.commander(
                urgency, input, matchInput, acceptedAtNanos, gameStateSnapshot)
                .withPendingClarification(pendingClarification);
        dependencies.state().setLastCommanderMatchInput(matchInput); // observer snapshot only; this turn owns context
        UiBus.publish(new CommanderMatchInputChangedEvent(matchInput, gameStateSnapshot));
        // Exact-alias reflex: try the commander's actual words FIRST, then the acoustically corrected form. The raw
        // phrase keeps a deliberately authored alias authoritative if a correction ever overlaps with it.
        Optional<ReflexResolver.Reflex> reflexCommand = reflexResolver.resolve(rawStripped, gameStateSnapshot);
        if (reflexCommand.isEmpty() && !matchInput.equals(rawStripped)) {
            reflexCommand = reflexResolver.resolve(matchInput, gameStateSnapshot);
        }
        Thought thought = reflexCommand
                .map(reflex -> Thought.reflex(context, reflex.actionId(), reflex.argumentsJson(), dependencies))
                .orElseGet(() -> Thought.commander(context, dependencies));
        String route = reflexCommand.isPresent()
                ? "reflex " + reflexCommand.get().actionId() + reflexCommand.get().arguments() + " (exact)"
                : "think";
        CompanionDiagnostics.info(thought.trace(), "intake",
                "\"" + CompanionDiagnostics.truncate(input) + "\" -> " + route);
        if (pendingClarification != null) {
            String disposition = reflexCommand.isPresent() ? "superseded by reflex" : "claimed for continuation";
            CompanionDiagnostics.debug(thought.trace(), "clarify",
                    pendingClarification.actionId() + "." + pendingClarification.parameterName()
                            + " " + disposition);
        }
        if (!matchInput.equals(input)) {
            // The normalized/name-stripped form actually used for tool matching and the LLM current-input.
            CompanionDiagnostics.debug(thought.trace(), "intake", "match text: \"" + CompanionDiagnostics.truncate(matchInput) + "\"");
        }
        enqueue(ThoughtSource.COMMANDER, thought, urgency);
    }

    /**
     * Accepts a gameplay subscriber's request to <b>react out loud</b> (a {@code CompanionReactionEvent}) and
     * queues a reactive {@link EventThought} on the EVENT lane. Event data and instructions exist only in the
     * bounded narration request; after success, only the model's final spoken line becomes an EVENT fact.
     */
    public void submitEventReaction(String stimulus, String instructions, Urgency urgency) {
        if (stimulus == null || stimulus.isBlank()) {
            return;
        }
        Thought thought = Thought.eventReaction(urgency, stimulus, instructions, dependencies);
        CompanionDiagnostics.debug(thought.trace(), "event", "reaction");
        enqueue(ThoughtSource.EVENT, thought, urgency);
    }

    /**
     * Accepts a gameplay subscriber's <b>finished phrase</b> to voice verbatim (no LLM) and queues a verbatim
     * {@link EventThought} on the EVENT lane. The finished phrase itself becomes the single EVENT fact.
     */
    public void submitEventVerbatim(String phrase, Urgency urgency) {
        if (phrase == null || phrase.isBlank()) {
            return;
        }
        Thought thought = Thought.eventVerbatim(urgency, phrase, dependencies);
        CompanionDiagnostics.debug(thought.trace(), "event", "verbatim");
        enqueue(ThoughtSource.EVENT, thought, urgency);
    }

    @Override
    public synchronized void start() {
        Map<ThoughtSource, ThoughtLane> newlyStartedLanes = null;
        if (lanes == null) {
            Map<ThoughtSource, ThoughtLane> built = new EnumMap<>(ThoughtSource.class);
            try {
                // Commander cognition is one ordered stream: prompt and tool selection follow intake order. Slow
                // game handlers detach while the lane keeps their lifecycle live,
                // so this worker accepts the next turn immediately after dispatch. EVENT remains single-worker too.
                built.put(ThoughtSource.COMMANDER, new ThoughtLane("companion-commander", 1));
                built.put(ThoughtSource.EVENT, new ThoughtLane("companion-event", 1));
                lanes = built; // single volatile publish of the fully-built lane set
                newlyStartedLanes = built;
            } catch (RuntimeException | Error startupFailure) {
                built.values().forEach(lane -> lane.shutdown(0));
                throw startupFailure;
            }
        }
        if (watchdog == null) {
            ScheduledExecutorService newlyStartedWatchdog = null;
            try {
                newlyStartedWatchdog = Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "companion-watchdog");
                    thread.setDaemon(true);
                    return thread;
                });
                newlyStartedWatchdog.scheduleAtFixedRate(this::checkWatchdog,
                        watchdogIntervalMillis, watchdogIntervalMillis, TimeUnit.MILLISECONDS);
                watchdog = newlyStartedWatchdog;
            } catch (RuntimeException | Error startupFailure) {
                if (newlyStartedWatchdog != null) {
                    newlyStartedWatchdog.shutdownNow();
                }
                if (newlyStartedLanes != null && lanes == newlyStartedLanes) {
                    lanes = null;
                    newlyStartedLanes.values().forEach(lane -> lane.shutdown(0));
                }
                throw startupFailure;
            }
        }
    }

    @Override
    public synchronized void stop() {
        dependencies.clarificationCoordinator().cancel();
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

}
