package elite.intel.ai.brain.health;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AiServiceHealthTest {

    private static final Instant START = Instant.parse("2026-09-27T20:00:00Z");

    private final MutableClock clock = new MutableClock(START);
    private final AiServiceHealth health = new AiServiceHealth(clock);

    @Test
    void aFailureRunStartsAtItsFirstFailureAndLastsUntilAnAnswer() {
        health.recordRefused(429);
        clock.advance(Duration.ofHours(2));
        health.recordUnreachable();

        AiServiceHealth.Snapshot failing = health.snapshot();
        assertEquals(START, failing.failingSince(), "later failures extend the run, they do not restart it");
        assertEquals(AiServiceHealth.Outcome.UNREACHABLE, failing.lastOutcome());
        assertNull(failing.lastRefusalCode(), "an unreachable provider sent no status");

        health.recordAnswered(Duration.ofMillis(800));
        assertNull(health.snapshot().failingSince());
    }

    @Test
    void theTypicalReplyTimeIsTheMedianOfTheRecentAnswers() {
        for (long millis : new long[]{9_000, 1_000, 2_000, 1_500, 1_200, 1_100}) {
            health.recordAnswered(Duration.ofMillis(millis));
        }
        // The 9 s answer has aged out of the last five: 1000, 1100, 1200, 1500, 2000.
        assertEquals(Optional.of(Duration.ofMillis(1_200)), health.snapshot().typicalReplyTime());
    }

    @Test
    void exchangedSinceComparesTheLastExchangeWithAMoment() {
        assertFalse(health.snapshot().exchangedSince(START), "nothing exchanged yet");
        health.recordRefused(401);
        assertTrue(health.snapshot().exchangedSince(START));
        assertFalse(health.snapshot().exchangedSince(START.plusSeconds(1)));
    }

    @Test
    void resetForgetsEverything() {
        health.recordAnswered(Duration.ofSeconds(1));
        health.recordRefused(500);
        health.reset();

        AiServiceHealth.Snapshot empty = health.snapshot();
        assertNull(empty.lastExchange());
        assertNull(empty.lastOutcome());
        assertNull(empty.failingSince());
        assertEquals(Optional.empty(), empty.typicalReplyTime());
    }
}
