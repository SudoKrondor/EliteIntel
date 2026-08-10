package elite.intel.gameapi.hge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The arrival/signal rendezvous. The two facts arrive on separate threads in an order the journal
 * does not guarantee, so the advice has to come out exactly once however they interleave.
 */
class HighGradeEmissionsAdvisorTest {

    private static final long SYSTEM = 1_000L;
    private static final long OTHER_SYSTEM = 2_000L;

    private final List<List<String>> announcements = new ArrayList<>();
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final HighGradeEmissionsAdvisor advisor =
            new HighGradeEmissionsAdvisor(enabled::get, announcements::add);

    private void enterBoomSystem(long systemAddress) {
        advisor.onSystemEntered(systemAddress, "Independent", 5_000_000L, List.of("Boom"));
    }

    @Test
    @DisplayName("arrival first, then the signal")
    void arrivalThenSignal() {
        enterBoomSystem(SYSTEM);
        assertTrue(announcements.isEmpty(), "nothing to say until emissions actually turn up");

        advisor.onHighGradeEmissions(SYSTEM);
        assertEquals(1, announcements.size());
        assertEquals(List.of("protoheatradiators", "protolightalloys", "protoradiolicalloys"),
                announcements.getFirst());
    }

    @Test
    @DisplayName("signal first, then arrival — the other half completes the pair")
    void signalThenArrival() {
        advisor.onHighGradeEmissions(SYSTEM);
        assertTrue(announcements.isEmpty(), "system state is not known yet");

        enterBoomSystem(SYSTEM);
        assertEquals(1, announcements.size());
    }

    @Test
    @DisplayName("a system full of emissions is still one announcement")
    void announcesOncePerSystem() {
        enterBoomSystem(SYSTEM);
        advisor.onHighGradeEmissions(SYSTEM);
        advisor.onHighGradeEmissions(SYSTEM);
        advisor.onHighGradeEmissions(SYSTEM);

        assertEquals(1, announcements.size());
    }

    @Test
    @DisplayName("the next system gets its own announcement")
    void newSystemAnnouncesAgain() {
        enterBoomSystem(SYSTEM);
        advisor.onHighGradeEmissions(SYSTEM);
        enterBoomSystem(OTHER_SYSTEM);
        advisor.onHighGradeEmissions(OTHER_SYSTEM);

        assertEquals(2, announcements.size());
    }

    @Test
    @DisplayName("a signal left over from the system behind us cannot trigger this one")
    void staleSignalFromPreviousSystemIsDiscarded() {
        enterBoomSystem(SYSTEM);
        advisor.onHighGradeEmissions(OTHER_SYSTEM);

        assertTrue(announcements.isEmpty());
    }

    @Test
    @DisplayName("a late signal from the old system does not wipe the new system's state")
    void lateSignalFromOldSystemDoesNotClobberTheNewOne() {
        // Signals run on their own virtual threads, so one belonging to the system we are leaving can
        // land after the jump into this one has been handled. It must not cost us this system.
        enterBoomSystem(OTHER_SYSTEM);
        enterBoomSystem(SYSTEM);
        advisor.onHighGradeEmissions(OTHER_SYSTEM);
        advisor.onHighGradeEmissions(SYSTEM);

        assertEquals(1, announcements.size());
    }

    @Test
    @DisplayName("a signal seen before the jump event was processed is not lost")
    void signalArrivingAheadOfTheJumpEventIsKept() {
        enterBoomSystem(OTHER_SYSTEM);
        advisor.onHighGradeEmissions(SYSTEM);
        assertTrue(announcements.isEmpty(), "not the current system yet");

        enterBoomSystem(SYSTEM);
        assertEquals(1, announcements.size(), "arrival completes the pair the signal already half-made");
    }

    @Test
    @DisplayName("a system that qualifies for nothing says nothing")
    void quietWhenNothingQualifies() {
        advisor.onSystemEntered(SYSTEM, "Independent", 5_000_000L, List.of("Lockdown"));
        advisor.onHighGradeEmissions(SYSTEM);

        assertTrue(announcements.isEmpty());
    }

    @Test
    @DisplayName("the ship's setting decides whether it is spoken")
    void respectsTheShipSetting() {
        enabled.set(false);
        enterBoomSystem(SYSTEM);
        advisor.onHighGradeEmissions(SYSTEM);
        assertTrue(announcements.isEmpty());

        // Turning it on mid-system still works: the pair was never announced, so it is not used up.
        enabled.set(true);
        advisor.onHighGradeEmissions(SYSTEM);
        assertEquals(1, announcements.size());
    }
}
