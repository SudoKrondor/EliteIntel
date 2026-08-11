package elite.intel.gameapi.hge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Arrival is the whole trigger. What a system's emissions sites drop is fixed by its allegiance,
 * population and faction states, so the advice is owed as soon as the commander jumps in.
 *
 * <p>These tests replace an earlier set built around a rendezvous between the jump event and an
 * {@code FSSSignalDiscovered} for the emissions source. That pairing was removed because the signal
 * often never arrives: emissions sites spawn over time rather than on arrival, and a real session
 * recorded a commander farming seven Imperial Shielding out of a site in a system whose only reported
 * signal was its Nav Beacon. The advice stayed silent for want of an event the game never wrote.
 */
class HighGradeEmissionsAdvisorTest {

    private final List<List<String>> announcements = new ArrayList<>();
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final HighGradeEmissionsAdvisor advisor =
            new HighGradeEmissionsAdvisor(enabled::get, announcements::add);

    @Test
    @DisplayName("arriving in a qualifying system advises straight away")
    void arrivalAdvisesImmediately() {
        advisor.onSystemEntered("Independent", 5_000_000L, List.of("Boom"));

        assertEquals(1, announcements.size(), "no second event is needed to earn the advice");
        assertEquals(List.of("protoheatradiators", "protolightalloys", "protoradiolicalloys"),
                announcements.getFirst());
    }

    /**
     * The exact system from the session that exposed the bug: Empire allegiance with factions in
     * Expansion. It was silent before, and its emissions site really did drop Imperial Shielding.
     */
    @Test
    @DisplayName("an Empire system in Expansion offers its allegiance and its state materials")
    void allegianceAndStateMaterialsAreCombined() {
        advisor.onSystemEntered("Empire", 15_727L, List.of("Expansion", "Expansion"));

        assertEquals(List.of("imperialshielding", "protoheatradiators", "protolightalloys",
                "protoradiolicalloys"), announcements.getFirst());
    }

    /**
     * NLTT 4671 from the same session: Empire with two factions at War. Imperial Shielding comes from
     * the allegiance and the military pair from the war, and the commander is owed all three. The
     * combination was only ever covered a piece at a time, so this pins it whole.
     */
    @Test
    @DisplayName("an Empire system at war offers Imperial Shielding and the military pair")
    void allegianceAndWarMaterialsAreCombined() {
        advisor.onSystemEntered("Empire", 70_707L, List.of("War", "War"));

        assertEquals(List.of("imperialshielding", "militarygradealloys", "militarysupercapacitors"),
                announcements.getFirst());
    }

    /**
     * Insoess, same session: one system running Boom, CivilLiberty, Election, Expansion and War at
     * once, because a faction's {@code FactionState} names only its dominant state while
     * {@code ActiveStates} lists the rest. Every rule that matches has to contribute.
     */
    @Test
    @DisplayName("a system running several states at once offers every material they earn")
    void concurrentStatesAllContribute() {
        advisor.onSystemEntered("Empire", 2_534_651L,
                List.of("Boom", "CivilLiberty", "Election", "Expansion", "War"));

        assertEquals(List.of("imperialshielding", "militarygradealloys", "militarysupercapacitors",
                        "protoheatradiators", "protolightalloys", "protoradiolicalloys"),
                announcements.getFirst());
    }

    @Test
    @DisplayName("each system arrived in gets its own advice")
    void eachArrivalAdvisesAgain() {
        advisor.onSystemEntered("Empire", 15_727L, List.of());
        advisor.onSystemEntered("Federation", 236_907L, List.of());

        assertEquals(2, announcements.size());
        assertEquals(List.of("imperialshielding"), announcements.getFirst());
        assertEquals(List.of("fedcorecomposites", "fedproprietarycomposites"), announcements.get(1));
    }

    /**
     * Most systems on any route qualify for nothing, and this is what keeps the advice worth hearing.
     * Independent with no relevant state is the common case, and it was the correct silence in the
     * one system of that session whose emissions signal the journal actually did report.
     */
    @Test
    @DisplayName("a system that qualifies for nothing says nothing")
    void quietWhenNothingQualifies() {
        advisor.onSystemEntered("Independent", 401_749L, List.of());
        advisor.onSystemEntered("Alliance", 136_236L, List.of("Lockdown"));

        assertTrue(announcements.isEmpty());
    }

    @Test
    @DisplayName("the ship's setting decides whether it is spoken")
    void respectsTheShipSetting() {
        enabled.set(false);
        advisor.onSystemEntered("Independent", 5_000_000L, List.of("Boom"));
        assertTrue(announcements.isEmpty());

        enabled.set(true);
        advisor.onSystemEntered("Independent", 5_000_000L, List.of("Boom"));
        assertEquals(1, announcements.size());
    }

    @Test
    @DisplayName("a system with no reported factions is handled, not thrown at")
    void nullFactionStatesAreTolerated() {
        advisor.onSystemEntered("Empire", 15_727L, null);

        assertEquals(List.of("imperialshielding"), announcements.getFirst());
    }
}
