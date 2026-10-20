package elite.intel.bindforge.preseteditor;

import elite.intel.bindforge.io.BindingsLoader;
import elite.intel.bindforge.io.StartPresetSections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code StartPreset.4.start} master and draft: where they live, when the master is made, what Save writes, and that a
 * draft which exists always holds a change.
 */
class StartPresetStoreTest {

    private static final StartPresetSections SPECIMEN =
            new StartPresetSections("KeyboardMouseOnly", "Custom", "Custom", "Custom", false);

    @TempDir
    Path bindingsDir;

    @TempDir
    Path workingDir;

    private StartPresetStore store() {
        return new StartPresetStore(workingDir, new BindingsLoader());
    }

    @Test
    void noMasterUntilOneIsMade() throws IOException {
        copySpecimen("StartPreset.4.start");

        assertTrue(store().master().isEmpty());
        assertTrue(store().draft().isEmpty());
        assertTrue(store().draftOrMaster().isEmpty());
    }

    @Test
    void masterIsMadeFromTheGameFileAsAByteCopy() throws IOException {
        copySpecimen("StartPreset.4.start");

        assertEquals(Optional.of(SPECIMEN), store().masterOrSeedFrom(bindingsDir));

        Path master = workingDir.resolve("master").resolve("StartPreset.4.start");
        assertArrayEquals(Files.readAllBytes(bindingsDir.resolve("StartPreset.4.start")), Files.readAllBytes(master));
    }

    @Test
    void masterIsMadeFromTheLiveFileWhenTwoGenerationsAreThere() throws IOException {
        copySpecimen("StartPreset.start");
        copySpecimen("StartPreset.4.start");

        assertEquals(Optional.of(SPECIMEN), store().masterOrSeedFrom(bindingsDir));
    }

    @Test
    void anExistingMasterIsNotReplacedByTheGameFile() throws IOException {
        copySpecimen("StartPreset.4.start");
        store().masterOrSeedFrom(bindingsDir);
        Files.writeString(bindingsDir.resolve("StartPreset.4.start"), "A\nB\nC\nD", StandardCharsets.UTF_8);

        assertEquals(Optional.of(SPECIMEN), store().masterOrSeedFrom(bindingsDir));
    }

    @Test
    void noGameFileMakesNoMaster() throws IOException {
        assertTrue(store().masterOrSeedFrom(bindingsDir).isEmpty());
        assertFalse(Files.exists(workingDir.resolve("master")));
    }

    @Test
    void saveWritesFourLinesAndReadsBack() throws IOException {
        copySpecimen("StartPreset.4.start");
        StartPresetStore store = store();
        store.masterOrSeedFrom(bindingsDir);
        StartPresetSections edited = new StartPresetSections("Custom", "Custom", "Custom", "Custom", false);

        store.save(edited);

        assertEquals(Optional.of(edited), store.draft());
        assertEquals(Optional.of(edited), store.draftOrMaster());
        assertEquals("Custom\nCustom\nCustom\nCustom\n",
                Files.readString(workingDir.resolve("draft").resolve("StartPreset.4.start"), StandardCharsets.UTF_8));
        assertTrue(store.hasDraftChanges());
        assertEquals(Optional.of(SPECIMEN), store.master(), "Save never touches the master");
    }

    @Test
    void saveNeverTouchesTheGameFile() throws IOException {
        copySpecimen("StartPreset.4.start");
        byte[] before = Files.readAllBytes(bindingsDir.resolve("StartPreset.4.start"));
        StartPresetStore store = store();
        store.masterOrSeedFrom(bindingsDir);

        store.save(new StartPresetSections("A", "B", "C", "D", false));

        assertArrayEquals(before, Files.readAllBytes(bindingsDir.resolve("StartPreset.4.start")));
    }

    @Test
    void savingTheMastersValuesLeavesNoDraft() throws IOException {
        copySpecimen("StartPreset.4.start");
        StartPresetStore store = store();
        store.masterOrSeedFrom(bindingsDir);
        store.save(new StartPresetSections("A", "B", "C", "D", false));

        store.save(SPECIMEN);

        assertTrue(store.draft().isEmpty());
        assertFalse(store.hasDraftChanges());
    }

    @Test
    void singleLineMasterAndFourMatchingLinesAreTheSame() throws IOException {
        copySpecimen("StartPreset.start");
        StartPresetStore store = store();
        store.masterOrSeedFrom(bindingsDir);

        store.save(new StartPresetSections("Custom", "Custom", "Custom", "Custom", false));

        assertTrue(store.draft().isEmpty());
    }

    @Test
    void anEmptyLineSurvivesTheRoundTrip() throws IOException {
        copySpecimen("StartPreset.4.start");
        StartPresetStore store = store();
        store.masterOrSeedFrom(bindingsDir);
        StartPresetSections generalOnly = new StartPresetSections("Custom", "", "", "", false);

        store.save(generalOnly);

        assertEquals(Optional.of(generalOnly), store.draft());
    }

    @Test
    void saveWithNoMasterIsRefused() {
        assertThrows(IllegalStateException.class,
                () -> store().save(new StartPresetSections("A", "B", "C", "D", false)));
    }

    @Test
    void aValueThatWouldNotReadBackIsRefused() throws IOException {
        copySpecimen("StartPreset.4.start");
        StartPresetStore store = store();
        store.masterOrSeedFrom(bindingsDir);

        assertThrows(IllegalArgumentException.class,
                () -> store.save(new StartPresetSections("A\nB", "B", "C", "D", false)));
        assertThrows(IllegalArgumentException.class,
                () -> store.save(new StartPresetSections(" A", "B", "C", "D", false)));
        assertThrows(IllegalArgumentException.class,
                () -> store.save(new StartPresetSections("A\rB", "B", "C", "D", false)));
        assertThrows(IllegalArgumentException.class,
                () -> store.save(new StartPresetSections("\uFEFFA", "B", "C", "D", false)));
        assertTrue(store.draft().isEmpty());
    }

    /** Only something outside BindForge leaves a draft with no master; the draft still counts, so it is not hidden. */
    @Test
    void aDraftWithNoMasterCountsAsAChange() throws IOException {
        Path draft = workingDir.resolve("draft").resolve("StartPreset.4.start");
        Files.createDirectories(draft.getParent());
        Files.writeString(draft, "A\nB\nC\nD\n", StandardCharsets.UTF_8);

        assertTrue(store().master().isEmpty());
        assertTrue(store().hasDraftChanges());
    }

    @Test
    void droppingTheDraftReturnsToTheMaster() throws IOException {
        copySpecimen("StartPreset.4.start");
        StartPresetStore store = store();
        store.masterOrSeedFrom(bindingsDir);
        store.save(new StartPresetSections("A", "B", "C", "D", false));

        store.dropDraft();

        assertTrue(store.draft().isEmpty());
        assertEquals(Optional.of(SPECIMEN), store.draftOrMaster());
        assertFalse(store.hasDraftChanges());
    }

    @Test
    void droppingNoDraftIsNotAnError() {
        assertDoesNotThrow(() -> store().dropDraft());
    }

    private void copySpecimen(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/bindforge/startpreset/" + name)) {
            assertNotNull(in, "missing test resource " + name);
            Files.write(bindingsDir.resolve(name), in.readAllBytes());
        }
    }
}
