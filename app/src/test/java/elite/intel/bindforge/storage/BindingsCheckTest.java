package elite.intel.bindforge.storage;

import elite.intel.bindforge.storage.BindingsComparison.Factory;
import elite.intel.bindforge.storage.BindingsComparison.Side;
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
 * the files changed meanwhile or edits are on screen.
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

        Written result = check.settle(found, every(found, Side.WORKING_COPY));

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

        check.settle(found, every(found, Side.LIVE));

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
                () -> check.settle(found, every(found, Side.LIVE)));

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

        check.settle(found, every(found, Side.LIVE));

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
                () -> check.settle(found, every(found, Side.WORKING_COPY)));
        assertEquals(List.of(), written);
    }

    // ---- helpers

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
                    throw new AssertionError("no installation is read: the fake write leaves none alone");
                });
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
