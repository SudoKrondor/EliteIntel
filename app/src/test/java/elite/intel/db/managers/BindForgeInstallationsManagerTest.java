package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The installation list persists across rescans, which is the whole reason it is a table rather than
 * whatever detection last returned.
 */
class BindForgeInstallationsManagerTest {

    private final BindForgeInstallationsManager installations = BindForgeInstallationsManager.getInstance();

    // WHY: the suite shares one database and JUnit fixes no method order, so each test starts from an empty
    // list rather than inheriting rows a sibling left behind.
    @BeforeEach
    void emptyTheList() {
        installations.findAll().forEach(row -> installations.remove(row.id()));
    }

    @Test
    void recordsADetectedInstallation() {
        InstallationRow row = installations.record("STEAM", Path.of("E:\\SteamLibrary\\Elite Dangerous"), false);

        assertEquals("STEAM", row.storefront());
        assertEquals("E:\\SteamLibrary\\Elite Dangerous", row.rootPath());
        assertFalse(row.addedByHand());
        assertFalse(row.missing());
        assertEquals(1, installations.findAll().size());
    }

    /**
     * A rescan re-reports what it found last time. The row has to survive that unchanged, because device
     * records are keyed to its id.
     */
    @Test
    void recordingTheSameFolderTwiceKeepsTheOriginalRow() {
        InstallationRow first = installations.record("STEAM", Path.of("E:\\Elite"), false);
        InstallationRow second = installations.record("STEAM", Path.of("E:\\Elite"), false);

        assertEquals(first.id(), second.id(), "the same folder is the same installation");
        assertEquals(1, installations.findAll().size());
    }

    /**
     * Nothing records where a Frontier install went, so the user adds it by hand. If detection later happens
     * to find that folder, the row must not be demoted to auto-detected - it is still the only reason
     * BindForge knows about it.
     */
    @Test
    void detectionDoesNotDemoteAHandAddedInstallation() {
        installations.record("FRONTIER", Path.of("D:\\Games\\Frontier"), true);
        installations.record("FRONTIER", Path.of("D:\\Games\\Frontier"), false);

        assertTrue(installations.findAll().get(0).addedByHand(), "it is still the user's row");
    }

    @Test
    void aMissingInstallationIsMarkedRatherThanDropped() {
        InstallationRow row = installations.record("EPIC", Path.of("C:\\EpicLibrary\\Elite"), false);

        installations.setMissing(row.id(), true);

        List<InstallationRow> all = installations.findAll();
        assertEquals(1, all.size(), "an unmounted drive and an uninstall look the same, so the row stays");
        assertTrue(all.get(0).missing());
        assertEquals(row.id(), all.get(0).id(), "and it keeps its id, so its device records survive");
    }

    @Test
    void aMissingInstallationThatComesBackIsMarkedPresentAgain() {
        InstallationRow row = installations.record("EPIC", Path.of("C:\\EpicLibrary\\Elite"), false);
        installations.setMissing(row.id(), true);

        installations.setMissing(row.id(), false);

        assertFalse(installations.findAll().get(0).missing());
    }

    /**
     * The reinstalled-to-another-drive case: the row follows the game rather than being replaced, so
     * everything keyed to it comes along.
     */
    @Test
    void relocateMovesTheRowAndClearsMissing() {
        InstallationRow row = installations.record("STEAM", Path.of("E:\\Elite"), false);
        installations.setMissing(row.id(), true);

        installations.relocate(row.id(), Path.of("F:\\Elite"));

        InstallationRow moved = installations.findAll().get(0);
        assertEquals(row.id(), moved.id(), "same row, new folder");
        assertEquals("F:\\Elite", moved.rootPath());
        assertFalse(moved.missing(), "it is where the user says it is");
    }

    @Test
    void twoStorefrontsAreTwoInstallations() {
        installations.record("STEAM", Path.of("E:\\SteamLibrary\\Elite Dangerous"), false);
        installations.record("EPIC", Path.of("C:\\EpicLibrary\\EliteDangerous"), false);

        assertEquals(2, installations.findAll().size());
    }

    @Test
    void removeDropsTheRow() {
        InstallationRow row = installations.record("STEAM", Path.of("E:\\Elite"), false);

        installations.remove(row.id());

        assertTrue(installations.findAll().isEmpty());
    }
}
