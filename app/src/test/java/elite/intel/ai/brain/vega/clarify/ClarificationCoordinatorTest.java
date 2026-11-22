package elite.intel.ai.brain.vega.clarify;

import elite.intel.ai.brain.vega.clarify.ClarificationCoordinator;
import elite.intel.ai.brain.vega.clarify.PendingClarification;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClarificationCoordinatorTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-07-12T12:00:00Z"));
    private final ClarificationCoordinator coordinator =
            new ClarificationCoordinator(clock, Duration.ofSeconds(30));

    @Test
    void claimTransfersPendingStateExactlyOnce() {
        coordinator.open("set_speed", "amount", "increase speed", "By how much?");

        PendingClarification claimed = coordinator.claim().orElseThrow();

        assertEquals("set_speed", claimed.actionId());
        assertEquals("amount", claimed.parameterName());
        assertTrue(coordinator.claim().isEmpty());
    }

    @Test
    void laterRequestReplacesEarlierPendingState() {
        coordinator.open("set_speed", "amount", "increase speed", "By how much?");
        coordinator.open("plot_route", "destination", "plot a route", "Where to?");

        assertEquals("plot_route", coordinator.claim().orElseThrow().actionId());
    }

    @Test
    void expiredStateCannotClaimAReply() {
        coordinator.open("set_speed", "amount", "increase speed", "By how much?");
        clock.advance(Duration.ofSeconds(31));

        assertTrue(coordinator.claim().isEmpty());
        assertTrue(coordinator.peek().isEmpty());
    }

    @Test
    void aClarificationGivenBackCanBeClaimedAgainUntilItExpires() {
        coordinator.open("set_speed", "amount", "increase speed", "By how much?");
        PendingClarification claimed = coordinator.claim().orElseThrow();

        coordinator.giveBack(claimed);

        assertEquals(claimed, coordinator.claim().orElseThrow());
        coordinator.giveBack(claimed);
        clock.advance(Duration.ofSeconds(31));
        assertTrue(coordinator.claim().isEmpty(), "a give-back keeps the original expiry");
    }

    @Test
    void aGiveBackNeverReplacesAClarificationOpenedSince() {
        coordinator.open("set_speed", "amount", "increase speed", "By how much?");
        PendingClarification old = coordinator.claim().orElseThrow();
        coordinator.open("target_subsystem", "subsystem", "target the", "Which subsystem?");

        coordinator.giveBack(old);

        assertEquals("target_subsystem", coordinator.claim().orElseThrow().actionId());
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
