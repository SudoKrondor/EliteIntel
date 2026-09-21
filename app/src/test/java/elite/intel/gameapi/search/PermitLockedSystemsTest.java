package elite.intel.gameapi.search;

import elite.intel.util.Ranks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Who may be sent to a permit-locked system. A search that plots a route into Sol for a commander with no
 * Federation rank ends at a jump the game refuses, so the rule is pinned per kind of lock: navy rank,
 * Elite rank, faction standing (unprovable, so withheld) - and a visit, which proves any of them.
 */
class PermitLockedSystemsTest {

    private static final Predicate<String> NEVER_VISITED = system -> false;
    private static final Predicate<String> ALWAYS_VISITED = system -> true;
    private static final int UNKNOWN = -1;

    private static final int PETTY_OFFICER = Ranks.federationRankNumber("Petty Officer");
    private static final int SQUIRE = Ranks.imperialRankNumber("Squire");
    private static final int EARL = Ranks.imperialRankNumber("Earl");
    private static final int LORD = Ranks.imperialRankNumber("Lord");
    private static final int BARON = Ranks.imperialRankNumber("Baron");
    private static final int POST_COMMANDER = Ranks.federationRankNumber("Post Commander");

    @Test
    @DisplayName("an ordinary system is reachable whatever the ranks")
    void ordinarySystemIsReachable() {
        assertTrue(PermitLockedSystems.isReachable("Deciat", UNKNOWN, UNKNOWN, false, NEVER_VISITED));
        assertFalse(PermitLockedSystems.isLocked("Deciat"));
    }

    @Test
    @DisplayName("Sol opens at Federation Petty Officer and not one rank below")
    void solNeedsPettyOfficer() {
        assertTrue(PermitLockedSystems.isReachable("Sol", PETTY_OFFICER, UNKNOWN, false, NEVER_VISITED));
        assertTrue(PermitLockedSystems.isReachable("Sol", PETTY_OFFICER + 3, UNKNOWN, false, NEVER_VISITED));
        assertFalse(PermitLockedSystems.isReachable("Sol", PETTY_OFFICER - 1, UNKNOWN, false, NEVER_VISITED));
        assertFalse(PermitLockedSystems.isReachable("Sol", UNKNOWN, UNKNOWN, false, NEVER_VISITED));
    }

    @Test
    @DisplayName("an Imperial rank does not open a Federation system, nor the reverse")
    void navyRanksDoNotCross() {
        assertFalse(PermitLockedSystems.isReachable("Sol", UNKNOWN, EARL, true, NEVER_VISITED));
        assertFalse(PermitLockedSystems.isReachable("Achenar", PETTY_OFFICER + 10, UNKNOWN, true, NEVER_VISITED));
        assertTrue(PermitLockedSystems.isReachable("Achenar", UNKNOWN, SQUIRE, false, NEVER_VISITED));
        assertFalse(PermitLockedSystems.isReachable("Facece", UNKNOWN, SQUIRE, false, NEVER_VISITED));
        assertTrue(PermitLockedSystems.isReachable("Facece", UNKNOWN, EARL, false, NEVER_VISITED));
    }

    @Test
    @DisplayName("Summerland is Baron, not the Lord older lists claim, and Hors is Post Commander")
    void wikiCorrections() {
        assertFalse(PermitLockedSystems.isReachable("Summerland", UNKNOWN, LORD, false, NEVER_VISITED));
        assertTrue(PermitLockedSystems.isReachable("Summerland", UNKNOWN, BARON, false, NEVER_VISITED));
        assertFalse(PermitLockedSystems.isReachable("Hors", POST_COMMANDER - 1, UNKNOWN, false, NEVER_VISITED));
        assertTrue(PermitLockedSystems.isReachable("Hors", POST_COMMANDER, UNKNOWN, false, NEVER_VISITED));
    }

    @Test
    @DisplayName("a closed region is matched by its sector prefix, whole words only")
    void closedRegionsByPrefix() {
        assertTrue(PermitLockedSystems.isLocked("Col 70 Sector FY-N c21-3"));
        assertTrue(PermitLockedSystems.isLocked("Praei3 AB-C d1-4"));
        assertTrue(PermitLockedSystems.isLocked("Horsehead Dark Region XY-Z a1-0"));
        assertFalse(PermitLockedSystems.isReachable("Col 70 Sector FY-N c21-3", PETTY_OFFICER + 10, EARL, true, NEVER_VISITED));
        assertFalse(PermitLockedSystems.isLocked("Col 7 Sector AB-C d1-1"));
        assertFalse(PermitLockedSystems.isLocked("Drymanni"));
        assertFalse(PermitLockedSystems.isLocked("Praei"));
    }

    @Test
    @DisplayName("a lock the journal cannot speak to is taboo whatever the ranks")
    void unprovableIsTaboo() {
        assertFalse(PermitLockedSystems.isReachable("CD-43 11917", PETTY_OFFICER + 10, EARL, true, NEVER_VISITED));
        assertFalse(PermitLockedSystems.isReachable("Polaris", PETTY_OFFICER + 10, EARL, true, NEVER_VISITED));
        assertFalse(PermitLockedSystems.isReachable("van Maanen's Star", PETTY_OFFICER + 10, EARL, true, NEVER_VISITED));
    }

    @Test
    @DisplayName("a null rank reads as unranked rather than throwing")
    void nullRankIsUnranked() {
        assertFalse(PermitLockedSystems.isReachable("Sol", null, null, false, NEVER_VISITED));
    }

    @Test
    @DisplayName("Shinrarta Dezhra opens at Elite in any career and no navy rank helps")
    void foundersWorldNeedsElite() {
        assertTrue(PermitLockedSystems.isReachable("Shinrarta Dezhra", UNKNOWN, UNKNOWN, true, NEVER_VISITED));
        assertFalse(PermitLockedSystems.isReachable("Shinrarta Dezhra", PETTY_OFFICER + 10, EARL, false, NEVER_VISITED));
    }

    @Test
    @DisplayName("a faction-locked system is withheld whatever the ranks, since standing is not in the journal")
    void factionLockedIsTaboo() {
        assertFalse(PermitLockedSystems.isReachable("Alioth", PETTY_OFFICER + 10, EARL, true, NEVER_VISITED));
        assertFalse(PermitLockedSystems.isReachable("Sirius", PETTY_OFFICER + 10, EARL, true, NEVER_VISITED));
        assertFalse(PermitLockedSystems.isReachable("LTT 198", PETTY_OFFICER + 10, EARL, true, NEVER_VISITED));
    }

    @Test
    @DisplayName("a visit proves the permit for every kind of lock")
    void visitProvesPermit() {
        assertTrue(PermitLockedSystems.isReachable("Alioth", UNKNOWN, UNKNOWN, false, ALWAYS_VISITED));
        assertTrue(PermitLockedSystems.isReachable("Sol", UNKNOWN, UNKNOWN, false, ALWAYS_VISITED));
        assertTrue(PermitLockedSystems.isReachable("Shinrarta Dezhra", UNKNOWN, UNKNOWN, false, ALWAYS_VISITED));
    }

    @Test
    @DisplayName("the ledger is only consulted for a locked system the ranks do not open")
    void ledgerAskedOnlyWhenNeeded() {
        Predicate<String> mustNotBeAsked = system -> fail("ledger consulted for " + system);
        assertTrue(PermitLockedSystems.isReachable("Deciat", UNKNOWN, UNKNOWN, false, mustNotBeAsked));
        assertTrue(PermitLockedSystems.isReachable("Sol", PETTY_OFFICER, UNKNOWN, false, mustNotBeAsked));
        assertTrue(PermitLockedSystems.isReachable("Shinrarta Dezhra", UNKNOWN, UNKNOWN, true, mustNotBeAsked));
    }

    @Test
    @DisplayName("names match however Spansh or the journal cases and pads them")
    void nameMatchingIsForgiving() {
        assertTrue(PermitLockedSystems.isLocked("SOL"));
        assertTrue(PermitLockedSystems.isLocked(" Beta Hydri "));
        assertTrue(PermitLockedSystems.isLocked("shinrarta dezhra"));
        assertFalse(PermitLockedSystems.isLocked("Solati"));
    }

    @Test
    @DisplayName("the list filter keeps order, drops nothing it cannot judge, and never returns null")
    void listFilterShape() {
        record Hit(String system) {
        }
        List<Hit> hits = List.of(new Hit("Deciat"), new Hit(null), new Hit("Eravate"));
        assertEquals(hits, PermitLockedSystems.reachable(hits, Hit::system));
        assertEquals(List.of(), PermitLockedSystems.reachable(null, Hit::system));
    }
}
