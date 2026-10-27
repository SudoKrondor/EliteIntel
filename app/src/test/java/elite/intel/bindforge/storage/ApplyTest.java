package elite.intel.bindforge.storage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static elite.intel.bindforge.storage.WorkingCopyTest.text;
import static org.junit.jupiter.api.Assertions.*;

/**
 * APPLY (storage.md rules 7-8; Storage S4a): the edits and the default labels, through the write path with a
 * before-APPLY backup, leaving alone an installation whose device files differ from the working copy.
 */
class ApplyTest {

    private static final String LVWAP = "<Root><LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP></Root>";

    private final AtomicReference<ConfigurationFiles> workingCopy = new AtomicReference<>(set("<applied/>", LVWAP));
    private final Edits edits = new Edits(workingCopy::get, () -> {
    });
    private final List<ConfigurationFiles> written = new ArrayList<>();
    private final List<String> leftAlone = new ArrayList<>();

    @Test
    void theEditsAndTheDefaultLabelsAreWrittenAfterTheBackup() throws IOException {
        edits.change(current -> set("<edited/>", LVWAP));
        edits.defaultLabels(Map.of("LVWAP.buttonMap", bytes("<Root><Joy_1>Button 1</Joy_1></Root>")));

        Written result = apply(installation -> live(LVWAP)).apply();

        assertEquals("<edited/>", text(written.getFirst().bindings().get("Custom.4.2.binds")));
        assertTrue(written.getFirst().buttonMaps().containsKey("LVWAP.buttonMap"));
        assertEquals(Backups.Before.APPLY, result.backup().before());
        assertEquals(List.of(), leftAlone);
    }

    /** Compared against the working copy as it was before the edits - not the edits, which always differ. */
    @Test
    void anInstallationThatDiffersFromTheWorkingCopyIsLeftAlone() throws IOException {
        edits.change(current -> set("<edited/>", "<Root><Renamed><PID>83F4</PID><VID>3344</VID></Renamed></Root>"));

        apply(installation -> live("<Root><Changed><PID>0001</PID><VID>1234</VID></Changed></Root>")).apply();

        assertEquals(List.of("STEAM"), leftAlone);
    }

    @Test
    void anInstallationMatchingTheWorkingCopyIsWritten() throws IOException {
        edits.change(current -> set("<edited/>", "<Root><Renamed><PID>83F4</PID><VID>3344</VID></Renamed></Root>"));

        apply(installation -> live(LVWAP)).apply();

        assertEquals(List.of(), leftAlone);
    }

    /** An installation file that cannot be read could hold anything of the user's, so it is left alone too. */
    @Test
    void anInstallationThatCannotBeReadIsLeftAlone() throws IOException {
        edits.change(current -> set("<edited/>", LVWAP));

        apply(installation -> {
            throw new IOException("locked");
        }).apply();

        assertEquals(List.of("STEAM"), leftAlone);
    }

    @Test
    void aBackupThatFailsStopsApplyWithTheEditsStillOnScreen() throws IOException {
        edits.change(current -> set("<edited/>", LVWAP));
        Apply apply = new Apply(edits, (files, backUp, leaveAlone) -> {
            backUp.take(sources());
            return null;
        }, sources -> {
            throw new IOException("the backup folder cannot be written");
        }, installation -> live(LVWAP));

        assertThrows(IOException.class, apply::apply);
        assertTrue(edits.hasEdits());
        assertFalse(edits.writing());
    }

    private Apply apply(Apply.Reader reader) {
        return new Apply(edits, (files, backUp, leaveAlone) -> {
            BackupSources sources = sources();
            backUp.take(sources);
            written.add(files);
            for (BackupSources.Installation installation : sources.installations()) {
                if (leaveAlone.leaveAlone(installation)) leftAlone.add(installation.storefront());
            }
            return new WriteReport(new WriteReport.Place(WriteReport.Kind.BINDINGS_FOLDER, null, Path.of("Bindings"),
                    List.of(), List.of(), List.of(), List.of()), List.of());
        }, sources -> new Backup(Path.of("BeforeChanges/2026-10-26_10-00-00.zip"), LocalDateTime.now(),
                Backups.Kind.BEFORE_CHANGES, Backups.Before.APPLY, true, List.of()), reader);
    }

    private static BackupSources sources() {
        return new BackupSources(Path.of("Bindings"),
                List.of(new BackupSources.Installation("STEAM", Path.of("Steam"), false, null)));
    }

    private static InstallationFiles live(String deviceMappings) {
        return new InstallationFiles(-1, "STEAM", Path.of("Steam"), bytes(deviceMappings), Map.of());
    }

    private static ConfigurationFiles set(String binds, String deviceMappings) {
        return new ConfigurationFiles(Map.of("Custom.4.2.binds", bytes(binds)), bytes(deviceMappings), Map.of());
    }
}
