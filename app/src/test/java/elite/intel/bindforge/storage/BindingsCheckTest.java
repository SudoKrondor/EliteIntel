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
import static elite.intel.bindforge.storage.BindingsComparisonTest.binds43;
import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static elite.intel.bindforge.storage.WorkingCopyTest.text;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The check for the binds and preset files (storage.md rule 14; Storage S8a): it reads and compares, then writes what
 * the user settled on through the write path - backed up first with the working copy inside - and writes nothing when
 * the files changed meanwhile or edits are on screen. It judges the device files too, and writes what the user settled
 * for them in the same write (Storage S9a); the reconcile reads the binds files the answers settle (Storage S9b).
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
    private DeviceCheck.Found devices = new DeviceCheck.Found(List.of());
    /** The factory binds files a plain file is judged against, by BindName - none until a test says otherwise. */
    private Map<String, byte[]> factoryByName = Map.of();

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

    /**
     * A newer version approved (Storage S10): the game's file becomes the working copy's under its name, the older
     * name removed from the game's folder after the backup, which holds the working copy.
     */
    @Test
    void anApprovedNewerVersionIsWrittenUnderItsNameAndTheOlderOneRemoved() throws IOException {
        String newer = "Custom.4.3.binds";
        live(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""));
        live(newer, binds43("Custom", "Key_A", "Joy_XAxis", "\t<NewThing Value=\"1\" />\r\n"));
        BindingsCheck check = check();
        BindingsCheck.Found found = check.find();
        assertTrue(found.anythingToAsk());
        assertEquals(List.of(), found.differences().files());

        check.settle(found, Map.of(), DeviceCheck.Answer.PUT_MINE_BACK);

        assertEquals(Set.of(newer), written.getFirst().bindings().keySet());
        assertEquals(List.of(Set.of(CUSTOM)), removed);
        assertSame(workingCopy, backedUpWorkingCopies.getFirst());
    }

    /** A newer version the game changes again after its question was answered stops the write, as a row does. */
    @Test
    void aNewerVersionChangedMeanwhileStopsTheWrite() throws IOException {
        String newer = "Custom.4.3.binds";
        live(newer, binds43("Custom", "Key_A", "Joy_XAxis", ""));
        BindingsCheck check = check();
        BindingsCheck.Found found = check.find();
        live(newer, binds43("Custom", "Key_A", "Joy_XAxis", "\t<NewThing Value=\"1\" />\r\n"));

        assertThrows(BindingsCheck.ChangedMeanwhileException.class,
                () -> check.settle(found, Map.of(), DeviceCheck.Answer.PUT_MINE_BACK));
        assertEquals(List.of(), written);
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

    // ---- the device files (Storage S9a, S9b)

    /** The one check judges the device files - at the first click and, since Storage S9b, at APPLY. */
    @Test
    void theCheckJudgesTheDeviceFiles() throws IOException {
        live(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""));
        devices = new DeviceCheck.Found(List.of(judged(State.RESET_MISSING)));

        BindingsCheck.Found found = check().find();

        assertTrue(found.differences().none());
        assertFalse(found.none(), "an installation that differs is something to say");
        assertTrue(found.anythingToAsk());
    }

    /** One the check could not read at all is said, but there is nothing to ask about it (Alan, 2026-10-31). */
    @Test
    void anInstallationThatCannotBeReadIsNothingToAsk() throws IOException {
        live(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""));
        devices = new DeviceCheck.Found(List.of(judged(State.UNREADABLE)));

        BindingsCheck.Found found = check().find();

        assertFalse(found.none());
        assertFalse(found.anythingToAsk());
    }

    /** One backup and one write: the settled binds files and the reconciled device files together (principle 1). */
    @Test
    void theReconciledDeviceFilesGoInTheSameWriteAsTheBindsFiles() throws IOException {
        live(CUSTOM, binds("Custom", "Key_Q", "Joy_XAxis", ""));
        devices = new DeviceCheck.Found(List.of(judged(State.DIFFERS)));
        BindingsCheck check = check();
        BindingsCheck.Found found = check.find();
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
        devices = new DeviceCheck.Found(List.of(judged(State.RESET_MISSING)));
        BindingsCheck check = check();
        BindingsCheck.Found found = check.find();
        devices = new DeviceCheck.Found(List.of(judged(State.DIFFERS)));

        BindingsCheck.ChangedMeanwhileException changed = assertThrows(BindingsCheck.ChangedMeanwhileException.class,
                () -> check.settle(found, Map.of(), DeviceCheck.Answer.PUT_MINE_BACK));

        assertEquals(List.of(), written);
        assertEquals(State.DIFFERS, changed.now().devices().installations().getFirst().state());
    }

    // ---- the reconcile's reading (Storage S9b, from the S9a review)

    /**
     * The reconcile reads the binds files the answers settle, the edits on top - so a binding only the game's side holds
     * counts - and BindForge's side is the working copy with the edits, under the name given.
     */
    @Test
    void theReconcileReadsTheBindsFilesTheAnswersSettleWithTheEditsOnTop() throws IOException {
        live(CUSTOM, binds("Custom", "Key_Q", "Joy_XAxis", ""));
        devices = new DeviceCheck.Found(List.of(judged(State.DIFFERS)));
        edits.change(current -> current.withBindingsFile(CUSTOM, bytes(binds("Custom", "Key_A", "Joy_YAxis", ""))));
        BindingsCheck check = check();
        BindingsCheck.Found found = check.find();

        FirstSetupReading reading = check.reading(found, every(found, Side.LIVE), "Yours").orElseThrow();

        assertEquals(1, found.differences().items().size(), "the game's line only - the edit's is not asked");
        assertEquals(binds("Custom", "Key_Q", "Joy_YAxis", ""), text(reading.bindings().get(CUSTOM)),
                "the game's line, and the edit on top");
        assertEquals(List.of(DeviceCheck.WORKING_COPY_SIDE, 1L),
                reading.installations().stream().map(InstallationFiles::installId).toList());
        assertEquals("Yours", reading.installations().getFirst().storefront());
        assertEquals(temp.resolve("working-copy"), reading.installations().getFirst().root());
        assertEquals(List.of(), written, "the reading writes nothing");
    }

    /** Putting the working copy's side back keeps the working copy's lines in the reading. */
    @Test
    void keepingMineReadsTheWorkingCopysBindsFiles() throws IOException {
        live(CUSTOM, binds("Custom", "Key_Q", "Joy_XAxis", ""));
        devices = new DeviceCheck.Found(List.of(judged(State.DIFFERS)));
        BindingsCheck check = check();
        BindingsCheck.Found found = check.find();

        FirstSetupReading reading = check.reading(found, every(found, Side.WORKING_COPY), "Yours").orElseThrow();

        assertEquals(text(workingCopy.bindings().get(CUSTOM)), text(reading.bindings().get(CUSTOM)));
    }

    /** A missing or unparseable file is never a side, so there is nothing to reconcile. */
    @Test
    void noInstallationThatCanBeASideHasNoReading() throws IOException {
        live(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""));
        devices = new DeviceCheck.Found(List.of(judged(State.RESET_MISSING)));
        BindingsCheck check = check();

        assertTrue(check.reading(check.find(), Map.of(), "Yours").isEmpty());
    }

    // ---- plain binds files (Storage S13b)

    @Test
    void aPlainBindsFileIsFoundAndIsSomethingToAskOnItsOwn() throws IOException {
        live(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""));
        live("KeyboardMouseOnly.binds", binds("KeyboardMouseOnly", "Key_Z", "Joy_XAxis", ""));

        BindingsCheck.Found found = check().find();

        assertTrue(found.differences().none());
        assertFalse(found.live().containsKey("KeyboardMouseOnly.binds"), "never compared as a binds file");
        assertEquals(List.of("KeyboardMouseOnly.binds"), found.plain().toTidy());
        assertFalse(found.none());
        assertTrue(found.anythingToAsk());
    }

    /** No twin and no factory file of its name: converted into the working copy, and the plain file removed. */
    @Test
    void tidyingUpConvertsAPlainFileIntoTheWorkingCopyAndRemovesIt() throws IOException {
        live(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""));
        live("KeyboardMouseOnly.binds", binds("Frontier", "Key_Z", "Joy_XAxis", ""));
        BindingsCheck check = check();
        BindingsCheck.Found found = check.find();

        Written result = check.settle(found, Map.of(), DeviceCheck.Answer.PUT_MINE_BACK);

        ConfigurationFiles files = written.getFirst();
        assertEquals(Set.of(CUSTOM, "KeyboardMouseOnly.4.2.binds"), files.bindings().keySet());
        assertEquals(binds("KeyboardMouseOnly", "Key_Z", "Joy_XAxis", ""),
                text(files.bindings().get("KeyboardMouseOnly.4.2.binds")), "every byte kept but PresetName");
        assertEquals(List.of(Set.of("KeyboardMouseOnly.binds")), removed);
        assertSame(workingCopy, backedUpWorkingCopies.getFirst(), "the working copy before it took the file");
        assertEquals(List.of(new PlainBinds.Tidied("KeyboardMouseOnly.binds", PlainBinds.Fate.CONVERTED,
                "KeyboardMouseOnly.4.2.binds")), result.plain());
    }

    /** A versioned twin in another case: the plain file goes, and nothing is added (Storage S13b D3). */
    @Test
    void aPlainFileWithAVersionedTwinIsRemoved() throws IOException {
        live(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""));
        live("custom.binds", binds("Custom", "Key_Z", "Joy_XAxis", ""));
        BindingsCheck check = check();

        Written result = check.settle(check.find(), Map.of(), DeviceCheck.Answer.PUT_MINE_BACK);

        assertEquals(Set.of(CUSTOM), written.getFirst().bindings().keySet());
        assertEquals(List.of(Set.of("custom.binds")), removed);
        assertEquals(PlainBinds.Fate.TWIN, result.plain().getFirst().fate());
        assertEquals(CUSTOM, result.plain().getFirst().to());
    }

    /** The factory file's values, but its own PresetName: removed, and the preset line loads the factory file. */
    @Test
    void aPlainFileEqualToTheFactoryFileOfItsNameIsRemoved() throws IOException {
        live(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""));
        live("KeyboardMouseOnly.binds", binds("Copied", "Key_Z", "Joy_XAxis", ""));
        factoryByName = Map.of("keyboardmouseonly", bytes(binds("KeyboardMouseOnly", "Key_Z", "Joy_XAxis", "")));
        BindingsCheck check = check();

        Written result = check.settle(check.find(), Map.of(), DeviceCheck.Answer.PUT_MINE_BACK);

        assertEquals(Set.of(CUSTOM), written.getFirst().bindings().keySet());
        assertEquals(List.of(Set.of("KeyboardMouseOnly.binds")), removed);
        assertEquals(PlainBinds.Fate.FACTORY, result.plain().getFirst().fate());
    }

    /** Judged against the settled set: taking the game's live-only file makes it the plain file's twin. */
    @Test
    void aPlainFileIsJudgedAgainstTheFilesTheAnswersSettle() throws IOException {
        live(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""));
        live(SPARE, binds("Spare", "Key_A", "Joy_XAxis", ""));
        live("Spare.binds", binds("Spare", "Key_Z", "Joy_XAxis", ""));
        BindingsCheck check = check();
        BindingsCheck.Found found = check.find();

        Written result = check.settle(found, every(found, Side.LIVE), DeviceCheck.Answer.PUT_MINE_BACK);

        assertEquals(PlainBinds.Fate.TWIN, result.plain().getFirst().fate());
        assertEquals(Set.of(CUSTOM, SPARE), written.getFirst().bindings().keySet());
    }

    @Test
    void aPlainFileChangedWhileTheWarningWasOpenWritesNothing() throws IOException {
        live(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""));
        live("KeyboardMouseOnly.binds", binds("KeyboardMouseOnly", "Key_Z", "Joy_XAxis", ""));
        BindingsCheck check = check();
        BindingsCheck.Found found = check.find();
        live("KeyboardMouseOnly.binds", binds("KeyboardMouseOnly", "Key_Y", "Joy_XAxis", ""));

        BindingsCheck.ChangedMeanwhileException changed = assertThrows(BindingsCheck.ChangedMeanwhileException.class,
                () -> check.settle(found, Map.of(), DeviceCheck.Answer.PUT_MINE_BACK));

        assertTrue(written.isEmpty());
        assertEquals(List.of("KeyboardMouseOnly.binds"), changed.now().plain().toTidy());
    }

    /** One that does not read as a binds file is named, never asked about, and left in the folder. */
    @Test
    void aPlainFileThatDoesNotReadAsABindsFileIsLeftAlone() throws IOException {
        live(CUSTOM, binds("Custom", "Key_A", "Joy_XAxis", ""));
        live("Broken.binds", "not a binds file");
        BindingsCheck check = check();
        BindingsCheck.Found found = check.find();

        assertEquals(List.of("Broken.binds"), found.plain().unreadable());
        assertFalse(found.none());
        assertFalse(found.anythingToAsk());
        Written result = check.settle(found, Map.of(), DeviceCheck.Answer.PUT_MINE_BACK);
        assertEquals(List.of(Set.of()), removed);
        assertEquals(PlainBinds.Fate.UNREADABLE, result.plain().getFirst().fate());
    }

    // ---- helpers

    /** Steam, judged {@code state}, with files to match. */
    private DeviceCheck.Judged judged(State state) {
        InstallationRow row = new InstallationRow(1, "STEAM", temp.resolve("Steam").toString(), false, false);
        if (state == State.UNREADABLE) return new DeviceCheck.Judged(row, state, null, "locked");
        return new DeviceCheck.Judged(row, state, new DeviceFiles(null, Map.of()), null);
    }

    private BindingsCheck check() {
        return new BindingsCheck(edits, () -> bindings, () -> temp.resolve("working-copy"),
                BindingsCheck::readBindingsFolder,
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
                files -> devices,
                PlainBinds::read, files -> factoryByName);
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
