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
        assertEquals("<applied/>", binds(edits.current()));
        workingCopy.set(set("<written elsewhere/>"));

        assertEquals("<written elsewhere/>", binds(edits.current()), "never older than the last write");
        assertFalse(edits.hasEdits());
    }

    @Test
    void anEditLightsTheBarAndHoldsItsSetOverTheWorkingCopy() throws IOException {
        edits.change(current -> set("<edited/>"));
        workingCopy.set(set("<written elsewhere/>"));

        assertTrue(edits.hasEdits());
        assertEquals("<edited/>", binds(edits.current()), "the edits are not read over");
        assertEquals("<applied/>", binds(edits.applied()), "what the edits started from");
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
        assertEquals("<applied/>", binds(edits.current()));
    }

    /** Default labels are written at the next APPLY, but are not the user's edits, so they never light the bar. */
    @Test
    void defaultLabelsGoOutWithApplyWithoutLightingTheBar() throws IOException {
        edits.defaultLabels(Map.of("LVWAP.buttonMap", bytes("<Root><Joy_1>Button 1</Joy_1></Root>")));

        assertFalse(edits.hasEdits());
        assertTrue(edits.toApply().buttonMaps().containsKey("LVWAP.buttonMap"));
    }

    /** A map the edits hold under the device's name, in any spelling, wins over its default labels. */
    @Test
    void theUsersOwnMapWinsOverItsDefaults() throws IOException {
        edits.change(current -> current.withButtonMaps(Map.of("lvwap.buttonMap", bytes("<Root><Joy_1>FIRE</Joy_1></Root>"))));
        edits.defaultLabels(Map.of("LVWAP.buttonMap", bytes("<Root><Joy_1>Button 1</Joy_1></Root>")));

        Map<String, byte[]> maps = edits.toApply().buttonMaps();
        assertEquals(1, maps.size());
        assertEquals("<Root><Joy_1>FIRE</Joy_1></Root>", text(maps.get("lvwap.buttonMap")));
    }

    /** A write puts its set in the working copy: the edits start from it, and are gone. */
    @Test
    void aWriteLeavesNoEditsAndStartsFromItsSet() throws IOException {
        edits.change(current -> set("<edited/>"));

        // WHY: as the write path does - the working copy is saved first, then the set is handed on.
        workingCopy.set(set("<edited/>"));
        edits.written(set("<edited/>"));

        assertFalse(edits.hasEdits());
        assertEquals("<edited/>", binds(edits.applied()));
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

        assertThrows(NoSuchFileException.class, none::current);
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
