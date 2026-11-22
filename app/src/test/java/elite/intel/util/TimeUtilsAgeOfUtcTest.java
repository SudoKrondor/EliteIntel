package elite.intel.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A Spansh stamp is UTC. Measured against the PC's local clock it came out an hour old for a commander in
 * Paris before it was a minute old, so the age is taken against an {@link Instant}.
 */
class TimeUtilsAgeOfUtcTest {

    private static final Instant NOW = Instant.parse("2026-11-19T21:44:00Z");

    @Test
    void theIsoShapeIsReadAsUtc() {
        assertEquals(Duration.ofHours(2), TimeUtils.ageOfUtc("2026-11-19T19:44:00Z", NOW));
    }

    @Test
    void theSpaceSeparatedShapeIsReadAsUtc() {
        assertEquals(Duration.ofDays(1), TimeUtils.ageOfUtc("2026-11-18 21:44:00+00", NOW));
    }

    @Test
    void aStampFromTheFutureIsNoAgeAtAll() {
        assertEquals(Duration.ZERO, TimeUtils.ageOfUtc("2026-11-19T21:45:00Z", NOW));
    }

    @Test
    void anUnreadableStampHasNoAge() {
        assertNull(TimeUtils.ageOfUtc(null, NOW));
        assertNull(TimeUtils.ageOfUtc("yesterday", NOW));
        assertNull(TimeUtils.ageOfUtc("2026-13-45T99:00:00Z", NOW));
    }
}
