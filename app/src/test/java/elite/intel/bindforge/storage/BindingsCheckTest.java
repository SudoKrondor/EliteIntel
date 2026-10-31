package elite.intel.bindforge.storage;

import elite.intel.bindforge.storage.BindingsComparison.Factory;
import elite.intel.bindforge.storage.BindingsComparison.Side;
import elite.intel.bindforge.storage.DeviceCheck.State;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static elite.intel.bindforge.storage.BindingsComparisonTest.binds;
import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static elite.intel.bindforge.storage.WorkingCopyTest.text;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The check for the binds and preset files (storage.md rule 14; Storage S8a): it reads and compares, then writes what
 * the user settled on through the write path - backed up first with the working copy inside - and writes nothing when
 * the files changed meanwhile or edits are on screen. At the first click it judges the device files too, and writes
 * what the user settled for them in the same write (Storage S9a).
 */
class BindingsCheckTest {

    private static final String CUSTOM = "Custom.4.2.binds";
    private static final String SPARE = "Spare.4.2.binds";

    @TempDir
    Path temp;

    private Path bindings;
    private final ConfigurationFiles workingCopy = new ConfigurationFiles(
            Map.of(CUSTOM, bytes(binds("Custom", "Key_A", "Joy_XAxis", ""))), null, Map.of());
    private final Edits edits = new Edits(() -> workingCopy, () -> {
    });
    private final List<ConfigurationFiles> written = new ArrayList<>();
    private final List<Set<String>> removed = new ArrayList<>();
    private final List<ConfigurationFiles> backedUpWorkingCopies = new ArrayList<>();
    private final AtomicInteger factoryReads = new AtomicInteger();
    /** What the device files' check finds - every installation matching, until a test says otherwise. */
    private DeviceCheck.Found devices = new DeviceCheck.Found(true, List.of());

    @BeforeEach
    void aBindingsFolder() throws IOException {
        bindings = temp.resolve("Bindings");
        Files.createDirectories(bindings);
        Files.writeString(bindings.resolve("notes.txt"), "not a control file");
    }

    @Test
    void filesAsBindForgeLastWroteThemAreNoDifferenceAndTheFactoryFilesAreNotRead() throws IOException {
        live(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", "").replace("\r\n", "\n"));

        BindingsCheck.Found found = check().find();

        assertTrue(found.differences().none());
        assertEquals(0, factoryReads.get(), "most first clicks find nothing - the factory files are read only then");
        assertFalse(found.live().containsKey("notes.txt"), "only the binds and preset files are read");
    }

    @Test
    void aChangeInTheGameIsFound() throws IOException {
        live(CUSTOM, binds("Custom", "Key_Q", "Joy_XAxis", ""));

        BindingsCheck.Found found = check().find();

        assertEquals(1, found.differences().items().size());
        assertEquals(1, factoryReads.get());
    }

    @Test
    void noBindingsFolderIsNotCompared() throws IOException {
        Files.delete(bindings.resolve("notes.txt"));
        Files.delete(bindings);

        assertThrows(NoSuchFileException.class, () -> check().find());
    }

    /** Putting mine back: the working copy's files, the live-only one removed, the working copy inside the backup. */
    @Test
    void keepingMineWritesTheWorkingCopyAndRemovesTheLiveOnlyFile() throws IOException {
        live(CUSTOM, binds("Custom", "Key_Q", "Joy_XAxis", ""));
        live(SPARE, binds("Spare", "Key_A", "Joy_XAxis", ""));
        BindingsCheck check = check();
        BindingsCheck.Found found = check.find();

        Written result = check.settle(found, every(found, Side.WORKING_COPY), DeviceCheck.Answer.PUT_MINE_BACK);

        assertEquals(text(workingCopy.bindings().get(CUSTOM)), text(written.getFirst().bindings().get(CUSTOM)));
        assertEquals(Set.of(CUSTOM), written.getFirst().bindings().keySet());
        assertEquals(List.of(Set.of(SPARE)), removed);
        assertSame(workingCopy, backedUpWorkingCopies.getFirst(), "the working copy, before the check changes it");
        assertEquals(Backups.Before.CHECK, result.backup().before());
    }

    @Test
    void takingTheGamesWritesTheGamesFilesAsTheWorkingCopy() throws IOException {
        live(CUSTOM, binds("Custom", "Key_Q", "Joy_XAxis", ""));
        live(SPARE, binds("Spare", "Key_A", "Joy_XAxis", ""));
        BindingsCheck check = check();
        BindingsCheck.Found found = check.find();

        check.settle(found, every(found, Side.LIVE), DeviceCheck.Answer.PUT_MINE_BACK);

        ConfigurationFiles files = written.getFirst();
        assertEquals(binds("Custom", "Key_Q", "Joy_XAxis", ""), text(files.bindings().get(CUSTOM)));
        assertTrue(files.bindings().containsKey(SPARE));
        assertEquals(List.of(Set.of()), removed);
    }

    /** Nothing watches the files: a row that changed after the warning opened stops the write (Alan, 2026-10-30). */
    @Test
    void aFileChangedAfterTheWarningOpenedStopsTheWriteAndSaysWhatIsThereNow() throws IOException {
        live(CUSTOM, binds("Custom", "Key_Q", "Joy_XAxis", ""));
        BindingsCheck check = check();
        BindingsCheck.Found found = check.find();
        live(CUSTOM, binds("Custom", "Key_W", "Joy_XAxis", ""));

        BindingsCheck.ChangedMeanwhileException changed = assertThrows(BindingsCheck.ChangedMeanwhileException.class,
                () -> check.settle(found, every(found, Side.LIVE), DeviceCheck.Answer.PUT_MINE_BACK));

        assertEquals(List.of(), written);
        assertEquals("Key_W", changed.now().differences().items().getFirst().live());
    }

    /** A change in how the file is written, not what it says, is not a change - the write goes ahead. */
    @Test
    void aFileRewrittenWithTheSameValuesStillSettles() throws IOException {
        live(CUSTOM, binds("Custom", "Key_Q", "Joy_XAxis", ""));
        BindingsCheck check = check();
        BindingsCheck.Found found = check.find();
        live(CUSTOM, binds("Custom", "Key_Q", "Joy_XAxis", "").replace("\r\n", "\n"));

        check.settle(found, every(found, Side.LIVE), DeviceCheck.Answer.PUT_MINE_BACK);

        assertEquals(binds("Custom", "Key_Q", "Joy_XAxis", "").replace("\r\n", "\n"),
                text(written.getFirst().bindings().get(CUSTOM)), "the game's file as it is now");
    }

    /** The write hands its set to the edits, which would drop edits on screen unseen - so nothing is written. */
    @Test
    void editsOnScreenStopTheWrite() throws IOException {
        live(CUSTOM, binds("Custom", "Key_Q", "Joy_XAxis", ""));
        BindingsCheck check = check();
        BindingsCheck.Found found = check.find();
        edits.change(current -> current.withBindingsFile(SPARE, bytes("<edited/>")));

        assertThrows(BindingsCheck.EditsNotAppliedException.class,
                () -> check.settle(found, every(found, Side.WORKING_COPY), DeviceCheck.Answer.PUT_MINE_BACK));
        assertEquals(List.of(), written);
    }

    // ---- the device files, at the first click (Storage S9a)

    @Test
    void theFirstClickJudgesTheDeviceFilesAndAPPLYsCheckDoesNot() throws IOException {
        live(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""));
        devices = new DeviceCheck.Found(true, List.of(judged(State.RESET_MISSING)));

        BindingsCheck.Found atFirstClick = check().findWithDeviceFiles();
        BindingsCheck.Found atApply = check().find();

        assertTrue(atFirstClick.differences().none());
        assertFalse(atFirstClick.none(), "an installation that differs is something to say");
        assertTrue(atFirstClick.anythingToAsk());
        assertFalse(atApply.devices().checked(), "APPLY's check judges no device files until Storage S9b");
        assertTrue(atApply.none());
    }

    /** One the check could not read at all is said, but there is nothing to ask about it (Alan, 2026-10-31). */
    @Test
    void anInstallationThatCannotBeReadIsNothingToAsk() throws IOException {
        live(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""));
        devices = new DeviceCheck.Found(true, List.of(judged(State.UNREADABLE)));

        BindingsCheck.Found found = check().findWithDeviceFiles();

        assertFalse(found.none());
        assertFalse(found.anythingToAsk());
    }

    /** One backup and one write: the settled binds files and the reconciled device files together (principle 1). */
    @Test
    void theReconciledDeviceFilesGoInTheSameWriteAsTheBindsFiles() throws IOException {
        live(CUSTOM, binds("Custom", "Key_Q", "Joy_XAxis", ""));
        devices = new DeviceCheck.Found(true, List.of(judged(State.DIFFERS)));
        BindingsCheck check = check();
        BindingsCheck.Found found = check.findWithDeviceFiles();
        ConfigurationFiles reconciled = new ConfigurationFiles(Map.of(), bytes("<Root><LVWAP/></Root>"),
                Map.of("LVWAP.buttonMap", bytes("<Root/>")));

        check.settle(found, every(found, Side.LIVE), DeviceCheck.Answer.reconciled(reconciled, Map.of()));

        assertEquals(1, written.size());
        assertEquals(1, backedUpWorkingCopies.size());
        ConfigurationFiles files = written.getFirst();
        assertEquals(binds("Custom", "Key_Q", "Joy_XAxis", ""), text(files.bindings().get(CUSTOM)));
        assertEquals("<Root><LVWAP/></Root>", text(files.deviceMappings()));
        assertEquals(Set.of("LVWAP.buttonMap"), files.buttonMaps().keySet());
    }

    /** Nothing watches the installations either: one whose device files changed meanwhile stops the write. */
    @Test
    void anInstallationChangedAfterTheWarningOpenedStopsTheWrite() throws IOException {
        live(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""));
        devices = new DeviceCheck.Found(true, List.of(judged(State.RESET_MISSING)));
        BindingsCheck check = check();
        BindingsCheck.Found found = check.findWithDeviceFiles();
        devices = new DeviceCheck.Found(true, List.of(judged(State.DIFFERS)));

        BindingsCheck.ChangedMeanwhileException changed = assertThrows(BindingsCheck.ChangedMeanwhileException.class,
                () -> check.settle(found, Map.of(), DeviceCheck.Answer.PUT_MINE_BACK));

        assertEquals(List.of(), written);
        assertEquals(State.DIFFERS, changed.now().devices().installations().getFirst().state());
    }

    // ---- helpers

    /** Steam, judged {@code state}, with files to match. */
    private DeviceCheck.Judged judged(State state) {
        InstallationRow row = new InstallationRow(1, "STEAM", temp.resolve("Steam").toString(), false, false);
        if (state == State.UNREADABLE) return new DeviceCheck.Judged(row, state, null, "locked");
        return new DeviceCheck.Judged(row, state, new DeviceFiles(null, Map.of()), null);
    }

    private BindingsCheck check() {
        return new BindingsCheck(edits, () -> bindings, BindingsCheck::readBindingsFolder,
                files -> {
                    factoryReads.incrementAndGet();
                    return Factory.NONE;
                },
                (files, backUp, leaveAlone, remove) -> {
                    // WHY: taken as the write path takes it, so the backup step records the working copy.
                    backUp.take(new BackupSources(bindings, List.of()));
                    written.add(files);
                    removed.add(remove);
                    WriteReport.Place place = new WriteReport.Place(WriteReport.Kind.BINDINGS_FOLDER, null, bindings,
                            List.of(), List.copyOf(remove), List.of(), List.of());
                    return new WriteReport(place, List.of());
                },
                (sources, files) -> {
                    backedUpWorkingCopies.add(files);
                    return new Backup(temp.resolve("backup.zip"), LocalDateTime.of(2026, 10, 30, 18, 0),
                            Backups.Kind.BEFORE_CHANGES, Backups.Before.CHECK, true, List.of());
                },
                installation -> {
                    throw new AssertionError("no installation is read: the fake write asks none");
                },
                files -> devices);
    }

    private void live(String name, String content) throws IOException {
        Files.writeString(bindings.resolve(name), content);
    }

    private static Map<String, Side> every(BindingsCheck.Found found, Side side) {
        Map<String, Side> answers = new HashMap<>();
        found.differences().items().forEach(item -> answers.put(item.id(), side));
        return answers;
    }
}
