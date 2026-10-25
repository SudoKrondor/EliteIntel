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
 * The installation list persists across detections, which is the whole reason it is a table rather than
 * whatever detection last returned - and a removed installation stays removed (Storage S2b, 2026-10-25).
 */
class BindForgeInstallationsManagerTest {

    private static final Path STEAM = Path.of("E:\\SteamLibrary\\Elite Dangerous");
    private static final Path EPIC = Path.of("C:\\EpicLibrary\\Elite");

    private final BindForgeInstallationsManager installations = BindForgeInstallationsManager.getInstance();

    // WHY: the suite shares one database and JUnit fixes no method order, so each test starts from an empty
    // list rather than inheriting rows a sibling left behind.
    @BeforeEach
    void emptyTheList() {
        InstallationsTable.empty();
    }

    @Test
    void recordsADetectedInstallation() {
        installations.recordDetected("STEAM", STEAM);

        InstallationRow row = installations.findAll().getFirst();
        assertEquals("STEAM", row.storefront());
        assertEquals(STEAM.toString(), row.rootPath());
        assertFalse(row.addedByHand());
        assertFalse(row.missing());
        assertEquals(1, installations.findAll().size());
    }

    /** Detection re-reports what it found last time. The row has to survive that unchanged. */
    @Test
    void detectingTheSameFolderTwiceKeepsTheOriginalRow() {
        installations.recordDetected("STEAM", STEAM);
        long id = installations.findAll().getFirst().id();

        installations.recordDetected("STEAM", STEAM);

        assertEquals(1, installations.findAll().size());
        assertEquals(id, installations.findAll().getFirst().id(), "the same folder is the same installation");
    }

    /**
     * Nothing records where a Frontier install went, so the user adds it by hand. If detection later happens
     * to find that folder, the row must not be demoted to auto-detected.
     */
    @Test
    void detectionDoesNotDemoteAHandAddedInstallation() {
        installations.addByHand("MANUAL", STEAM);
        installations.recordDetected("STEAM", STEAM);

        assertTrue(installations.findAll().getFirst().addedByHand(), "it is still the user's row");
    }

    @Test
    void aMissingInstallationIsMarkedRatherThanDropped() {
        installations.recordDetected("EPIC", EPIC);
        InstallationRow row = installations.findAll().getFirst();

        installations.setMissing(row.id(), true);

        List<InstallationRow> all = installations.findAll();
        assertEquals(1, all.size(), "an unmounted drive and an uninstall look the same, so the row stays");
        assertTrue(all.getFirst().missing());
        assertEquals(row.id(), all.getFirst().id());
    }

    @Test
    void aMissingInstallationThatComesBackIsMarkedPresentAgain() {
        installations.recordDetected("EPIC", EPIC);
        InstallationRow row = installations.findAll().getFirst();
        installations.setMissing(row.id(), true);

        installations.setMissing(row.id(), false);

        assertFalse(installations.findAll().getFirst().missing());
    }

    /** Migration 12006 (A14): a new row has missed nothing; the flag is kept until a write clears it. */
    @Test
    void aMissedWriteIsKeptUntilCleared() {
        installations.recordDetected("EPIC", EPIC);
        InstallationRow row = installations.findAll().getFirst();
        assertFalse(row.missedWrite());

        installations.setMissedWrite(row.id(), true);
        assertTrue(installations.findAll().getFirst().missedWrite());

        installations.setMissedWrite(row.id(), false);
        assertFalse(installations.findAll().getFirst().missedWrite());
    }

    @Test
    void twoStorefrontsAreTwoInstallations() {
        installations.recordDetected("STEAM", STEAM);
        installations.recordDetected("EPIC", EPIC);

        assertEquals(2, installations.findAll().size());
    }

    @Test
    void aRemovedInstallationLeavesTheList() {
        installations.recordDetected("STEAM", STEAM);

        installations.remove(installations.findAll().getFirst().id());

        assertTrue(installations.findAll().isEmpty());
    }

    /** Detection at the next start would otherwise put it straight back (file-manager.md, Game Install Locations). */
    @Test
    void aRemovedInstallationStaysRemovedWhenDetectionFindsItAgain() {
        installations.recordDetected("STEAM", STEAM);
        installations.remove(installations.findAll().getFirst().id());

        installations.recordDetected("STEAM", STEAM);

        assertTrue(installations.findAll().isEmpty());
    }

    @Test
    void addMissingOnARemovedInstallationsFolderBringsItBack() {
        installations.recordDetected("STEAM", STEAM);
        long id = installations.findAll().getFirst().id();
        installations.remove(id);

        InstallationRow back = installations.addByHand("MANUAL", STEAM);

        assertEquals(id, back.id(), "the same row, back in the list");
        assertEquals(List.of(back), installations.findAll());
    }

    @Test
    void addMissingRecordsANewFolderAsAddedByHand() {
        InstallationRow row = installations.addByHand("MANUAL", EPIC);

        assertTrue(row.addedByHand());
        assertEquals(List.of(row), installations.findAll());
    }
}
