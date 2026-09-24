package elite.intel.db.util;

import elite.intel.db.dao.HuntingGroundDao;
import elite.intel.db.dao.MaterialNameDao;
import elite.intel.db.dao.PlayerDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Switching the open database from one commander to another, on the test JVM's own database.
 * <p>
 * The whole point of the split: once the game loads a second commander, nothing the first one left behind may be
 * read as theirs, and nothing of the user's may be lost with the switch.
 */
class CommanderSwitchDatabaseTest {

    private static final String ALPHA = "FTESTALPHA";
    private static final String BRAVO = "FTESTBRAVO";

    private String original;
    // Under test each commander's file is a named in-memory database, which SQLite discards when its last
    // connection closes. A real file survives the retired pool, so these keepers stand in for the disk.
    private Handle alphaKeeper;
    private Handle bravoKeeper;

    @BeforeEach
    void rememberTheOpenCommander() {
        Database.init().close();
        original = Database.currentCommander();
        alphaKeeper = Jdbi.create("jdbc:sqlite:" + Database.commanderLocation(ALPHA)).open();
        bravoKeeper = Jdbi.create("jdbc:sqlite:" + Database.commanderLocation(BRAVO)).open();
    }

    @AfterEach
    void putTheOriginalCommanderBack() {
        Database.switchCommander(original.equals(Database.PENDING_COMMANDER) ? ALPHA : original);
        alphaKeeper.close();
        bravoKeeper.close();
    }

    @Test
    void eachCommanderSeesOnlyTheirOwnRows() {
        assertTrue(Database.switchCommander(ALPHA));
        setPlayerName("Alpha");
        setMiningAnnouncements(false);

        assertTrue(Database.switchCommander(BRAVO));
        assertEquals(BRAVO, Database.currentCommander());
        assertEquals("", player().getPlayerName(), "Bravo starts with a player row of their own");
        assertFalse(player().isMiningAnnouncementOn(), "the user's preferences are the same for every commander");

        assertTrue(Database.switchCommander(ALPHA));
        assertEquals("Alpha", player().getPlayerName(), "Alpha's data is where Alpha left it");
        setMiningAnnouncements(true);
    }

    @Test
    void theSharedTablesKeepEachCommandersOwnHalfApart() {
        Database.switchCommander(ALPHA);
        Database.withDao(MaterialNameDao.class, dao -> {
            dao.setAmount("iron", 12);
            return null;
        });
        Database.withDao(HuntingGroundDao.class, dao -> {
            dao.touch("Split Test Ground", null, null, null, null, "2026-09-23T00:00:00Z");
            return dao.forget("Split Test Ground");
        });
        LocationDto place = new LocationDto(77L, 424242L);
        place.setStarName("Split Test Star");
        place.setPlanetName("Split Test Star 1");
        place.setOurDiscovery(true);
        LocationManager.getInstance().save(place);

        Database.switchCommander(BRAVO);
        assertEquals(0, iron(), "Bravo holds none of Alpha's iron");
        assertFalse(forgotten("Split Test Ground"), "Bravo still hunts at the ground Alpha forgot");
        LocationDto seenByBravo = LocationManager.getInstance().findBySystemAddress(424242L, 77L);
        assertEquals("Split Test Star", seenByBravo.getStarName(), "the place itself is shared");
        assertFalse(seenByBravo.isOurDiscovery(), "but Bravo did not discover it");

        Database.switchCommander(ALPHA);
        assertEquals(12, iron());
        assertTrue(forgotten("Split Test Ground"));
        assertTrue(LocationManager.getInstance().findBySystemAddress(424242L, 77L).isOurDiscovery());
    }

    private static int iron() {
        return Database.withDao(MaterialNameDao.class, dao -> dao.findBySymbol("iron").getAmount());
    }

    private static boolean forgotten(String starSystem) {
        return Database.withDao(HuntingGroundDao.class, dao -> dao.findByName(starSystem).forgotten());
    }

    @Test
    void switchingToTheOpenCommanderChangesNothing() {
        Database.switchCommander(ALPHA);
        assertFalse(Database.switchCommander(ALPHA));
        assertEquals(ALPHA, Database.currentCommander());
    }

    @Test
    void anIdThatCannotNameAFileIsRefused() {
        String before = Database.currentCommander();
        assertFalse(Database.switchCommander("../escape"));
        assertFalse(Database.switchCommander(Database.PENDING_COMMANDER));
        assertEquals(before, Database.currentCommander());
    }

    private static PlayerDao.Player player() {
        return Database.withDao(PlayerDao.class, PlayerDao::get);
    }

    private static void setPlayerName(String name) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player p = dao.get();
            p.setPlayerName(name);
            dao.save(p);
            return null;
        });
    }

    private static void setMiningAnnouncements(boolean on) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player p = dao.get();
            p.setMiningAnnouncementOn(on);
            dao.save(p);
            return null;
        });
    }
}
