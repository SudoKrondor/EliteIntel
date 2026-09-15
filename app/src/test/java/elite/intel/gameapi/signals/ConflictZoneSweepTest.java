package elite.intel.gameapi.signals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Counting a system's conflict zones out of a burst of signals.
 * <p>
 * The trap that is specific to conflict zones: the journal lists the same zone twice in one sweep
 * ({@code #index=3} arrived twice at one timestamp in a real Ceos sweep), so counting events reports
 * a system as holding more zones than it does. Zones are counted by identity instead.
 */
class ConflictZoneSweepTest {

    private static final String LOW_3 = "$Warzone_PointRace_Low:#index=3;";
    private static final String LOW_5 = "$Warzone_PointRace_Low:#index=5;";
    private static final String MED_1 = "$Warzone_PointRace_Med:#index=1;";
    private static final String HIGH_2 = "$Warzone_PointRace_High:#index=2;";
    private static final String POWER_1 = "$Warzone_Powerplay_Med:#index=1;";

    @Test
    void everyIntensityIsRecognisedByItsSymbolAndCarriesItsIndex() {
        assertEquals(new ConflictZoneSignal(ConflictZoneIntensity.LOW, 3), ConflictZoneSignal.fromSymbol(LOW_3));
        assertEquals(new ConflictZoneSignal(ConflictZoneIntensity.MEDIUM, 1), ConflictZoneSignal.fromSymbol(MED_1));
        assertEquals(new ConflictZoneSignal(ConflictZoneIntensity.HIGH, 2), ConflictZoneSignal.fromSymbol(HIGH_2));
        assertEquals(new ConflictZoneSignal(ConflictZoneIntensity.POWERPLAY, 1), ConflictZoneSignal.fromSymbol(POWER_1));
    }

    @Test
    void aSignalThatIsNotAConflictZoneIsNotOne() {
        assertNull(ConflictZoneSignal.fromSymbol("$MULTIPLAYER_SCENARIO79_TITLE;"), "a resource site is the other ledger");
        assertNull(ConflictZoneSignal.fromSymbol("Conflict Zone [Low Intensity]"),
                "the localised name is the game client's language, not ours - only the symbol counts");
        assertNull(ConflictZoneSignal.fromSymbol("$Warzone_TG_High:#index=1;"),
                "a war zone family this app has not met is not silently counted as a faction war");
        assertNull(ConflictZoneSignal.fromSymbol(null));
    }

    @Test
    void theSameZoneListedTwiceInOneSweepCountsOnce() {
        ConflictZoneSweep sweep = new ConflictZoneSweep();
        String key = "2278152997195@2026-08-06T09:24:54Z";

        sweep.add(key, ConflictZoneSignal.fromSymbol(LOW_3));
        sweep.add(key, ConflictZoneSignal.fromSymbol(LOW_5));
        ConflictZoneProfile profile = sweep.add(key, ConflictZoneSignal.fromSymbol(LOW_3));

        assertEquals(2, profile.low(), "index 3 arrived twice, and there is still one zone with that index");
    }

    @Test
    void theTallyRestartsWithTheNextSweep() {
        ConflictZoneSweep sweep = new ConflictZoneSweep();
        sweep.add("A@t1", ConflictZoneSignal.fromSymbol(HIGH_2));
        sweep.add("A@t1", ConflictZoneSignal.fromSymbol(MED_1));

        ConflictZoneProfile next = sweep.add("A@t2", ConflictZoneSignal.fromSymbol(LOW_3));

        assertEquals(new ConflictZoneProfile(1, 0, 0, 0), next,
                "every arrival re-announces the whole set - counting across sweeps would double it");
    }

    @Test
    void powerplayZonesAreCountedButAreNotAFactionWar() {
        ConflictZoneSweep sweep = new ConflictZoneSweep();
        ConflictZoneProfile profile = sweep.add("B@t", ConflictZoneSignal.fromSymbol(POWER_1));

        assertEquals(1, profile.powerplay());
        assertEquals(0, profile.factionWarZones(), "a power's war is not somewhere to earn faction combat bonds");
        assertTrue(profile.present().isEmpty(), "and it is not read out as one either");
    }

    @Test
    void presentListsTheHardestFightFirst() {
        ConflictZoneProfile profile = new ConflictZoneProfile(4, 0, 2, 0);

        assertEquals("[HIGH, LOW]", profile.present().keySet().toString());
    }
}
