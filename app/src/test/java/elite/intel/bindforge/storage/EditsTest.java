package elite.intel.bindforge.storage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static elite.intel.bindforge.storage.WorkingCopyTest.text;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The edits not applied (storage.md rules 6-7; Storage S4a): held in memory over the working copy, lit only by the
 * user's edits, thrown away by DISCARD, and dropped once a write puts its set in the working copy.
 */
class EditsTest {

    private final AtomicReference<ConfigurationFiles> workingCopy = new AtomicReference<>(set("<applied/>"));
    private final List<String> announced = new ArrayList<>();
    private final Edits edits = new Edits(workingCopy::get, () -> announced.add("changed"));

    @Test
    void withNoEditsTheWorkingCopyIsReadAgainEachTime() throws IOException {
        assertEquals("<applied/>", binds(edits.snapshot().current()));
        workingCopy.set(set("<written elsewhere/>"));

        assertEquals("<written elsewhere/>", binds(edits.snapshot().current()), "never older than the last write");
        assertFalse(edits.hasEdits());
    }

    @Test
    void anEditLightsTheBarAndHoldsItsSetOverTheWorkingCopy() throws IOException {
        edits.change(current -> set("<edited/>"));
        workingCopy.set(set("<written elsewhere/>"));

        assertTrue(edits.hasEdits());
        assertEquals("<edited/>", binds(edits.snapshot().current()), "the edits are not read over");
        assertEquals("<applied/>", binds(edits.snapshot().applied()), "what the edits started from");
        assertEquals(List.of("changed"), announced);
    }

    /** An edit that puts every file back as it was leaves nothing to apply. */
    @Test
    void anEditBackToTheWorkingCopyIsNoEdit() throws IOException {
        edits.change(current -> set("<edited/>"));
        edits.change(current -> set("<applied/>"));

        assertFalse(edits.hasEdits());
    }

    @Test
    void discardThrowsEveryEditAway() throws IOException {
        edits.change(current -> set("<edited/>"));

        edits.discard();

        assertFalse(edits.hasEdits());
        assertEquals("<applied/>", binds(edits.snapshot().current()));
    }

    /** The edits and the working copy come from one read, never two reads of the disk a change apart. */
    @Test
    void aSnapshotReadsTheWorkingCopyOnce() throws IOException {
        int[] reads = {0};
        Edits counted = new Edits(() -> {
            reads[0]++;
            return workingCopy.get();
        }, () -> {
        });

        Edits.Snapshot snapshot = counted.snapshot();

        assertEquals(1, reads[0]);
        assertSame(snapshot.current(), snapshot.applied(), "no edits: the edits are the working copy");
    }

    /** An undo is handed the working copy with the edits, so it never reaches back into the Edits running it. */
    @Test
    void anEditFromAppliedIsHandedTheWorkingCopy() throws IOException {
        edits.change(current -> set("<edited/>"));

        edits.change((current, applied) -> {
            assertEquals("<edited/>", binds(current));
            return applied;
        });

        assertFalse(edits.hasEdits());
    }

    /** DISCARD is refused while APPLY writes, as a change is - not left to the bar alone. */
    @Test
    void discardIsRefusedWhileAWriteRuns() throws IOException {
        edits.change(current -> set("<edited/>"));

        edits.whileWriting(() -> {
            assertThrows(Edits.WriteRunningException.class, edits::discard);
            return null;
        });

        assertTrue(edits.hasEdits());
    }

    /** A write puts its set in the working copy: the edits start from it, and are gone. */
    @Test
    void aWriteLeavesNoEditsAndStartsFromItsSet() throws IOException {
        edits.change(current -> set("<edited/>"));

        // WHY: as the write path does - the working copy is saved first, then the set is handed on.
        workingCopy.set(set("<edited/>"));
        edits.written(set("<edited/>"));

        assertFalse(edits.hasEdits());
        assertEquals("<edited/>", binds(edits.snapshot().applied()));
    }

    /** While APPLY writes, no change is let in - its set would drop it. */
    @Test
    void noChangeIsLetInWhileAWriteRuns() throws IOException {
        edits.whileWriting(() -> {
            assertTrue(edits.writing());
            assertThrows(Edits.WriteRunningException.class, () -> edits.change(current -> set("<late/>")));
            return null;
        });

        assertFalse(edits.writing());
    }

    @Test
    void noWorkingCopyYetIsSaid() {
        Edits none = new Edits(() -> {
            throw new NoSuchFileException("working copy");
        }, () -> {
        });

        assertThrows(NoSuchFileException.class, none::snapshot);
        assertFalse(none.hasEdits());
    }

    @Test
    void anEditThatFailsChangesNothing() {
        assertThrows(IOException.class, () -> edits.change(current -> {
            throw new IOException("cannot be made");
        }));

        assertFalse(edits.hasEdits());
        assertEquals(List.of(), announced);
    }

    private static ConfigurationFiles set(String binds) {
        return new ConfigurationFiles(Map.of("Custom.4.2.binds", bytes(binds)), bytes("<Root/>"), Map.of());
    }

    private static String binds(ConfigurationFiles files) {
        return text(files.bindings().get("Custom.4.2.binds"));
    }
}
