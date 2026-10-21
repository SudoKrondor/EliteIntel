package elite.intel.gameapi.population;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SystemPortTest {

    @Test
    void theTiersAreTheOwnersOnes() {
        assertEquals(Optional.of(PortSize.HUGE), PortSize.of("Orbis"));
        assertEquals(Optional.of(PortSize.LARGE), PortSize.of("Ocellus"));
        assertEquals(Optional.of(PortSize.LARGE), PortSize.of("Coriolis"));
        assertEquals(Optional.of(PortSize.LARGE), PortSize.of("Dodec"));
        assertEquals(Optional.of(PortSize.MEDIUM), PortSize.of("Outpost"));
        assertEquals(Optional.of(PortSize.SMALL), PortSize.of("OnFootSettlement"));
    }

    @Test
    void theFssSpellingsSizeTheSameAsTheDockingOnes() {
        assertEquals(PortSize.of("Orbis"), PortSize.of("StationONeilOrbis"));
        assertEquals(PortSize.of("Coriolis"), PortSize.of("StationCoriolis"));
        assertEquals(PortSize.of("Ocellus"), PortSize.of("StationBernalSphere"));
    }

    @Test
    void aCrewIsNotAPopulation() {
        assertTrue(new SystemPort("X7Z-12K", "FleetCarrier", null, null).size().isEmpty());
        assertTrue(new SystemPort("X7Z-12K", null, "FleetCarrier", null).size().isEmpty());
        assertTrue(new SystemPort("Depot", "SpaceConstructionDepot", null, null).size().isEmpty());
        assertTrue(new SystemPort("Navigation Beacon", null, "NavBeacon", null).size().isEmpty());
    }

    @Test
    void theDockingTellsASettlementFromTheOutpostTheFssCallsIt() {
        SystemPort settlement = new SystemPort("Pashin Settlement", "OnFootSettlement", "Outpost", "Gende 1 a");

        assertEquals(Optional.of(PortSize.SMALL), settlement.size());
    }

    @Test
    void aColonyStationIsSizedByItsSignal() {
        // Witt Hub docks as a SurfaceStation and is reported by the FSS as a Coriolis.
        assertEquals(Optional.of(PortSize.LARGE), new SystemPort("Witt Hub", "SurfaceStation", "StationCoriolis", null).size());
        assertEquals(Optional.of(PortSize.HUGE), new SystemPort("Big One", "SurfaceStation", "StationONeilOrbis", null).size());
        assertEquals(Optional.of(PortSize.LARGE), new SystemPort("Unknown Shape", "SurfaceStation", null, null).size());
    }

    @Test
    void aSettlementWeOnlyFlewPastIsSmall() {
        SystemPort approached = new SystemPort("Jara Terminus", null, null, "Hyades Sector SI-T c3-13 1 d");

        assertEquals(Optional.of(PortSize.SMALL), approached.size());
        assertTrue(approached.isSettlementOn("hyades sector si-t c3-13 1 d"));
        assertFalse(approached.isSettlementOn("Hyades Sector SI-T c3-13 1 c"));
    }

    @Test
    void anOrbitalPortStandsOnNoBody() {
        assertFalse(new SystemPort("Witt Hub", "Coriolis", null, null).isSettlementOn("Witt Hub"));
    }
}
