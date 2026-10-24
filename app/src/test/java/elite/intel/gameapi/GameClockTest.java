package elite.intel.gameapi;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.GameClockDisagreementEvent;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A game stamping UTC away from the PC clock (seen after a Windows Update, fixed by changing the time zone) made
 * every live event look outdated. The offset is the smallest age of fresh stamps, and only a real one is corrected.
 */
class GameClockTest {

    private static final Instant PC_NOW = Instant.parse("2026-10-23T03:18:22Z");

    private final GameClock clock = new GameClock(Clock.fixed(PC_NOW, ZoneOffset.UTC));

    @Test
    void theSmallestAgeIsTheOffset() {
        observeAges(3605, 3601, 3620, 3602, 3610);

        Instant stamp = Instant.parse("2026-10-23T02:18:21Z");
        assertEquals(stamp.plusSeconds(3601), clock.toPcTime(stamp));
        String described = clock.describe();
        assertTrue(described.contains("3601 s behind"), described);
        assertTrue(described.contains("1 whole hour"), described);
    }

    @Test
    void nothingIsCorrectedUntilEnoughStampsAreSeen() {
        observeAges(3600, 3600, 3600, 3600);

        Instant stamp = Instant.parse("2026-10-23T02:18:22Z");
        assertEquals(stamp, clock.toPcTime(stamp));
        assertTrue(clock.describe().startsWith("not measured yet (4 of 5"), clock.describe());
    }

    @Test
    void roundingAndPollDelayAreNotCorrected() {
        observeAges(0, 1, 3, 2, 1);

        Instant stamp = PC_NOW.minusSeconds(4);
        assertEquals(stamp, clock.toPcTime(stamp));
        assertTrue(clock.describe().contains("level with the PC clock"), clock.describe());
        assertTrue(clock.describe().endsWith("no correction"), clock.describe());
    }

    @Test
    void aGameAheadOfThePcIsCorrectedTheOtherWay() {
        observeAges(-30, -29, -25, -30, -28);

        Instant stamp = PC_NOW.plusSeconds(30);
        assertEquals(PC_NOW, clock.toPcTime(stamp));
        assertTrue(clock.describe().contains("30 s ahead of"), clock.describe());
        assertFalse(clock.describe().contains("whole hour"), clock.describe());
    }

    @Test
    void aFreshStampLevelWithThePcEndsTheCorrection() {
        observeAges(3600, 3600, 3600, 3600, 3600);
        observeAges(0);

        Instant stamp = PC_NOW.minusSeconds(3600);
        assertEquals(stamp, clock.toPcTime(stamp));
    }

    @Test
    void anUnreadableStampIsNotASample() {
        observeAges(3600, 3600, 3600, 3600);
        clock.observe("not a time");
        clock.observe((String) null);

        assertTrue(clock.describe().startsWith("not measured yet (4 of 5"), clock.describe());
    }

    @Test
    void theCommanderIsWarnedOncePerRun() {
        List<GameClockDisagreementEvent> warnings = new ArrayList<>();
        List<VocalisationRequestEvent> spoken = new ArrayList<>();
        Object listener = new Object() {
            @Subscribe
            public void on(GameClockDisagreementEvent event) {
                warnings.add(event);
            }

            @Subscribe
            public void on(VocalisationRequestEvent event) {
                spoken.add(event);
            }
        };
        UiBus.register(listener);
        GameEventBus.register(listener);
        try {
            observeAges(3601, 3602, 3601, 3603, 3601);
            observeAges(0);
            observeAges(7200, 7200, 7200, 7200, 7200, 7200);
        } finally {
            UiBus.unregister(listener);
            GameEventBus.unregister(listener);
        }

        assertEquals(1, warnings.size());
        assertEquals(1, spoken.size());
        assertEquals(Duration.ofSeconds(3601), warnings.getFirst().offset());
        assertEquals(1, warnings.getFirst().wholeHours());
    }

    @Test
    void aHealthyClockIsNeverWarnedAbout() {
        List<GameClockDisagreementEvent> warnings = new ArrayList<>();
        Object listener = new Object() {
            @Subscribe
            public void on(GameClockDisagreementEvent event) {
                warnings.add(event);
            }
        };
        UiBus.register(listener);
        try {
            observeAges(0, 1, 3, 2, 1, 4, 0);
        } finally {
            UiBus.unregister(listener);
        }

        assertTrue(warnings.isEmpty());
    }

    @Test
    void onlyAGapNearWholeHoursCountsAsHours() {
        assertEquals(1, GameClock.wholeHours(Duration.ofSeconds(3650)));
        assertEquals(2, GameClock.wholeHours(Duration.ofSeconds(-7180)));
        assertEquals(0, GameClock.wholeHours(Duration.ofMinutes(10)));
        assertEquals(0, GameClock.wholeHours(Duration.ofMinutes(90)));
    }

    private void observeAges(long... secondsOld) {
        for (long age : secondsOld) {
            clock.observe(PC_NOW.minusSeconds(age).toString());
        }
    }
}
