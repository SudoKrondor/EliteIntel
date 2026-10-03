package elite.intel.db.util;

import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The one-time move of a v1.1.0019 database into the shared file plus the commander's own.
 * <p>
 * An upgrading commander must find everything where they left it: their ships and the voices set on them, their
 * player row, and their preferences. Nothing may be left behind in the shared file, where the next commander
 * would read it as their own.
 */
class CommanderSplitTest {

    @Test
    void anUpgradedDatabaseKeepsItsCommanderDataInTheCommandersFile() throws Exception {
        try (MigrationLayoutTest.Built built = MigrationLayoutTest.Built.fresh()) {
            Handle h = built.handle;
            // The state a v1.1.0019 install is in: commander tables still in the shared file, holding data.
            h.execute("UPDATE main.player SET player_name = 'Jameson', current_wealth = 42 WHERE id = 1");
            h.execute("INSERT INTO main.ship (shipName, shipId, voice, personality, commanderName) "
                    + "VALUES ('Lucky Dip', 7, 'NOVA', 'ROGUE', 'Jameson')");
            h.execute("INSERT INTO main.ship_settings (shipId, honkOnJump) VALUES (7, 1)");

            CommanderSplit.runIfNeeded(h);

            assertEquals("Jameson", h.createQuery("SELECT player_name FROM cmdr.player WHERE id = 1")
                    .mapTo(String.class).one());
            assertEquals(42L, h.createQuery("SELECT current_wealth FROM cmdr.player WHERE id = 1")
                    .mapTo(Long.class).one());
            assertEquals("NOVA", h.createQuery("SELECT voice FROM cmdr.ship WHERE shipId = 7")
                    .mapTo(String.class).one(), "the voice set on a ship moves with it");
            assertEquals(1, h.createQuery("SELECT COUNT(*) FROM cmdr.ship_settings WHERE shipId = 7")
                    .mapTo(Integer.class).one());
            assertFalse(CommanderSplit.existsIn(h, "main", "player"), "nothing is left in the shared file");
            assertFalse(CommanderSplit.existsIn(h, "main", "ship"));
        }
    }

    @Test
    void theUsersPreferencesStayShared() throws Exception {
        try (MigrationLayoutTest.Built built = MigrationLayoutTest.Built.fresh()) {
            Handle h = built.handle;
            h.execute("UPDATE main.user_preferences SET journal_dir = '/journals', is_mining_announcement_on = 0");

            CommanderSplit.runIfNeeded(h);

            assertEquals("/journals", h.createQuery("SELECT journal_dir FROM main.user_preferences")
                    .mapTo(String.class).one());
            assertFalse(h.createQuery("SELECT is_mining_announcement_on FROM main.user_preferences")
                    .mapTo(Boolean.class).one());
            assertFalse(h.createQuery("SELECT COUNT(*) FROM pragma_table_info('player', 'cmdr') WHERE name = 'journal_dir'")
                    .mapTo(Integer.class).one() > 0, "the commander's player row does not carry the user's half");
        }
    }

    @Test
    void aFreshInstallStillEndsWithTheSeededRows() throws Exception {
        try (MigrationLayoutTest.Built built = MigrationLayoutTest.Built.fresh()) {
            Handle h = built.handle;
            h.execute("DELETE FROM main.player");
            h.execute("DELETE FROM main.player_status");

            CommanderSplit.runIfNeeded(h);

            assertEquals(1, h.createQuery("SELECT COUNT(*) FROM cmdr.player WHERE id = 1").mapTo(Integer.class).one());
            assertEquals(1, h.createQuery("SELECT COUNT(*) FROM cmdr.player_status WHERE id = 1")
                    .mapTo(Integer.class).one());
        }
    }

    @Test
    void anInterruptedSplitFinishesOnTheNextStartWithoutDuplicatingRows() throws Exception {
        try (MigrationLayoutTest.Built built = MigrationLayoutTest.Built.fresh()) {
            Handle h = built.handle;
            h.execute("INSERT INTO main.ship (shipName, shipId) VALUES ('Lucky Dip', 7)");
            // A crash after the copy but before the drop: the row is already in the commander file.
            h.execute("INSERT INTO cmdr.ship (shipName, shipId) VALUES ('Lucky Dip', 7)");

            CommanderSplit.runIfNeeded(h);

            assertEquals(1, h.createQuery("SELECT COUNT(*) FROM cmdr.ship").mapTo(Integer.class).one());
            assertFalse(CommanderSplit.existsIn(h, "main", "ship"));
        }
    }

    @Test
    void aSplitDatabaseIsLeftAlone() throws Exception {
        try (MigrationLayoutTest.Built built = MigrationLayoutTest.Built.freshAndSplit()) {
            Handle h = built.handle;
            h.execute("UPDATE cmdr.player SET player_name = 'Jameson'");

            CommanderSplit.runIfNeeded(h);

            assertEquals("Jameson", h.createQuery("SELECT player_name FROM cmdr.player").mapTo(String.class).one());
            assertTrue(CommanderSplit.existsIn(h, "cmdr", "player"));
        }
    }

    @Test
    void anFidIsOnlyTrustedAsAFileNameWhenItLooksLikeOne() {
        assertTrue(Database.isUsableFid("F1234567"));
        assertFalse(Database.isUsableFid("../../etc/passwd"));
        assertFalse(Database.isUsableFid(""));
        assertFalse(Database.isUsableFid(null));
    }
}
