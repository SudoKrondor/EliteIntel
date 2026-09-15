package elite.intel.ui.overlay;

import elite.intel.db.managers.CombatBondManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.signals.ConflictZoneIntensity;
import elite.intel.session.ConflictZone;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static elite.intel.ui.overlay.HudCards.valueOf;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The card for fighting a faction war for the bonds.
 * <p>
 * The same shape as the bounty-hunting card and guarded the same way: it has to survive the loop
 * the commander actually flies - drop in, fight, boost out, drop into the next zone, fly to a
 * station and cash in - and the side row has to say who is paying, because a commander who drops
 * into the wrong instance is fighting for the wrong faction and wants to know.
 */
class ConflictZoneCardTest {

    private static final long HERE = 2278152997195L;
    private static final long SOMEWHERE_ELSE = 3137146456387L;

    private final CombatBondManager bonds = CombatBondManager.getInstance();
    private final ConflictZone conflictZone = ConflictZone.getInstance();

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @BeforeEach
    void clean() {
        bonds.cashedIn();
        conflictZone.cashedIn();
    }

    @Test
    void droppingIntoAConflictZonePutsTheFightOnScreen() {
        conflictZone.droppedIn(HERE, ConflictZoneIntensity.HIGH);
        bond(1, 23_684, "Sirius Corporation");
        bond(2, 129_331, "Sirius Corporation");

        HudObjective card = cardHere().orElseThrow();

        assertEquals("conflict-zone", card.id());
        assertEquals(HudObjective.PRIORITY_AMBIENT, card.priority(),
                "the commander never accepted this the way they accept a contract");
        assertEquals("CEOS - HIGH CZ", card.subtitle());
        assertEquals(HudText.credits(153_015), valueOf(card, HudText.get("overlay.card.row.bonds")));
        assertEquals("2", valueOf(card, HudText.get("overlay.card.row.kills")));
        assertEquals("Sirius Corporation", valueOf(card, HudText.get("overlay.card.row.side")));
    }

    @Test
    void anArrivalWithNoKillsYetShowsTheCardWithoutASide() {
        conflictZone.droppedIn(HERE, ConflictZoneIntensity.LOW);

        HudObjective card = cardHere().orElseThrow();

        assertEquals("0", valueOf(card, HudText.get("overlay.card.row.kills")));
        assertEquals(List.of(HudText.get("overlay.card.row.bonds"), HudText.get("overlay.card.row.kills")),
                HudCards.labels(card), "nobody has paid yet, so there is no side to name");
    }

    @Test
    void aResourceSiteIsNotAWar() {
        assertNull(ConflictZoneIntensity.fromSymbol("$MULTIPLAYER_SCENARIO79_TITLE;"));

        conflictZone.droppedIn(HERE, ConflictZoneIntensity.fromSymbol("$MULTIPLAYER_SCENARIO79_TITLE;"));

        assertTrue(cardHere().isEmpty(), "only a $Warzone_ scenario starts a fight");
    }

    @Test
    void theFightSurvivesTheTripBetweenZones() {
        conflictZone.droppedIn(HERE, ConflictZoneIntensity.MEDIUM);
        bond(1, 27_238, "Sirius Corporation");

        conflictZone.droppedIn(HERE, ConflictZoneIntensity.HIGH);

        HudObjective card = cardHere().orElseThrow();
        assertEquals("CEOS - HIGH CZ", card.subtitle(), "the card follows the commander to the next zone");
        assertEquals(HudText.credits(27_238), valueOf(card, HudText.get("overlay.card.row.bonds")),
                "and the bonds already earned are still theirs");
    }

    @Test
    void leavingTheSystemTakesTheCardWithIt() {
        conflictZone.droppedIn(HERE, ConflictZoneIntensity.HIGH);
        bond(1, 27_238, "Sirius Corporation");

        assertTrue(card(SOMEWHERE_ELSE).isEmpty(), "the fight was in Ceos, and the ship is in Sothis");
    }

    @Test
    void cashingInEndsTheFight() {
        conflictZone.droppedIn(HERE, ConflictZoneIntensity.HIGH);
        bond(1, 27_238, "Sirius Corporation");

        conflictZone.cashedIn();
        bonds.cashedIn();

        assertTrue(cardHere().isEmpty(), "handing the bonds over is the commander saying they are done");
    }

    @Test
    void theCardIsForTheCockpitOnly() {
        conflictZone.droppedIn(HERE, ConflictZoneIntensity.HIGH);

        ConflictZoneObjectiveSource onFoot = new ConflictZoneObjectiveSource(
                conflictZone, bonds, () -> HERE, () -> "Ceos", () -> false);

        assertTrue(onFoot.currentObjective().isEmpty());
    }

    private void bond(int n, long reward, String side) {
        bonds.record(HERE, side, "Kumo Council", reward, "2026-08-06T11:1" + n + ":00Z");
    }

    private Optional<HudObjective> cardHere() {
        return card(HERE);
    }

    private Optional<HudObjective> card(long systemAddress) {
        return new ConflictZoneObjectiveSource(conflictZone, bonds, () -> systemAddress, () -> "Ceos", () -> true)
                .currentObjective();
    }
}
