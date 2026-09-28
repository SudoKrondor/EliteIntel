package elite.intel.ai.brain.health;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * What the wire has seen of the AI service this session: whether the last exchange got an answer, since when
 * it has been failing, and how long answers take. Fed by {@code BaseAiClient}, the one place every provider
 * exchange passes through, and read by {@link AiServiceCheck}.
 * <p>
 * It records outcomes, never causes. A provider is free to answer a status code that means something else
 * entirely - Mistral answered {@code 429 rate_limited} for a week while a whole cluster of its models was
 * switched off for everyone - so the only split this record trusts is whether a request reached the provider at
 * all. Beyond that it keeps the status code only for the one refusal a commander can act on (a rejected key).
 * <p>
 * One record for whichever provider is active: the app talks to one at a time, and {@link #reset()} runs when
 * the LLM service restarts, which is how a provider change takes effect. In memory only - it describes this
 * session and nothing else.
 */
public final class AiServiceHealth {

    /**
     * How many recent answers the typical reply time is taken over.
     */
    private static final int REPLY_TIME_SAMPLES = 5;

    private static final AiServiceHealth INSTANCE = new AiServiceHealth(Clock.systemUTC());

    private final Clock clock;
    private final Deque<Duration> replyTimes = new ArrayDeque<>();
    private Instant lastExchange;
    private Outcome lastOutcome;
    private Instant failingSince;
    private Integer lastRefusalCode;

    AiServiceHealth(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public static AiServiceHealth getInstance() {
        return INSTANCE;
    }

    /**
     * How one exchange ended, from our side of the wire.
     */
    public enum Outcome {
        /**
         * The provider answered with a 2xx, whatever the body held.
         */
        ANSWERED,
        /**
         * The request never got an HTTP answer: DNS, connection refused, timeout.
         */
        UNREACHABLE,
        /**
         * The provider answered with a non-2xx status.
         */
        REFUSED
    }

    /**
     * The provider answered.
     *
     * @param replyTime from sending the request to receiving the whole answer
     */
    public synchronized void recordAnswered(Duration replyTime) {
        Objects.requireNonNull(replyTime, "replyTime");
        record(Outcome.ANSWERED);
        failingSince = null;
        lastRefusalCode = null;
        replyTimes.addLast(replyTime);
        if (replyTimes.size() > REPLY_TIME_SAMPLES) {
            replyTimes.removeFirst();
        }
    }

    /**
     * The request got no HTTP answer at all.
     */
    public synchronized void recordUnreachable() {
        recordFailure(Outcome.UNREACHABLE);
        lastRefusalCode = null;
    }

    /**
     * The provider answered with a non-2xx status.
     */
    public synchronized void recordRefused(int statusCode) {
        recordFailure(Outcome.REFUSED);
        lastRefusalCode = statusCode;
    }

    /**
     * Forgets everything; the next exchange starts a fresh record.
     */
    public synchronized void reset() {
        replyTimes.clear();
        lastExchange = null;
        lastOutcome = null;
        failingSince = null;
        lastRefusalCode = null;
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(lastExchange, lastOutcome, failingSince, lastRefusalCode, typicalReplyTime());
    }

    private void recordFailure(Outcome outcome) {
        record(outcome);
        if (failingSince == null) {
            failingSince = lastExchange;
        }
    }

    private void record(Outcome outcome) {
        lastExchange = clock.instant();
        lastOutcome = outcome;
    }

    /**
     * The median of the recent answers, which a single slow narration cannot drag up on its own.
     */
    private Optional<Duration> typicalReplyTime() {
        if (replyTimes.isEmpty()) {
            return Optional.empty();
        }
        List<Duration> sorted = replyTimes.stream().sorted().toList();
        return Optional.of(sorted.get(sorted.size() / 2));
    }

    /**
     * The record at one moment.
     *
     * @param lastExchange     when the last exchange ended, or null before any
     * @param lastOutcome      how it ended, or null before any
     * @param failingSince     when the current run of failures began, or null while the last exchange succeeded
     * @param lastRefusalCode  the status of the last refusal, or null unless the last exchange was one
     * @param typicalReplyTime median of the recent answers, empty before the first
     */
    public record Snapshot(Instant lastExchange, Outcome lastOutcome, Instant failingSince, Integer lastRefusalCode,
                           Optional<Duration> typicalReplyTime) {

        /**
         * True when an exchange ended at or after {@code moment}.
         */
        public boolean exchangedSince(Instant moment) {
            return lastExchange != null && !lastExchange.isBefore(moment);
        }
    }
}
