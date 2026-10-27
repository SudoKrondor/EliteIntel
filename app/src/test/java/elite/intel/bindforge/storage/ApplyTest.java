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
    private static final Map<String, byte[]> LVWAP_DEFAULTS =
            Map.of("LVWAP.buttonMap", bytes("<Root><Joy_1>Button 1</Joy_1></Root>"));

    private final AtomicReference<ConfigurationFiles> workingCopy = new AtomicReference<>(set("<applied/>", LVWAP));
    private final Edits edits = new Edits(workingCopy::get, () -> {
    });
    private final List<ConfigurationFiles> written = new ArrayList<>();
    private final List<String> leftAlone = new ArrayList<>();
    private final List<String> backedUp = new ArrayList<>();

    @Test
    void theEditsAndTheDefaultLabelsAreWrittenAfterTheBackup() throws IOException {
        edits.change(current -> set("<edited/>", LVWAP));

        Written result = apply((current, applied) -> LVWAP_DEFAULTS, installation -> live(LVWAP)).apply();

        assertEquals("<edited/>", text(written.getFirst().bindings().get("Custom.4.2.binds")));
        assertTrue(written.getFirst().buttonMaps().containsKey("LVWAP.buttonMap"));
        assertEquals(Backups.Before.APPLY, result.backup().before());
        assertEquals(List.of(), leftAlone);
    }

    /**
     * The default labels are worked out as APPLY runs, from the edits it writes - never left over from an earlier
     * screen refresh (review, 2026-10-27).
     */
    @Test
    void theDefaultLabelsAreWorkedOutFromTheEditsApplyWrites() throws IOException {
        edits.change(current -> set("<edited/>", LVWAP));
        List<String> seen = new ArrayList<>();

        apply((current, applied) -> {
            seen.add(text(current.bindings().get("Custom.4.2.binds")));
            seen.add(text(applied.bindings().get("Custom.4.2.binds")));
            return Map.of();
        }, installation -> live(LVWAP)).apply();

        assertEquals(List.of("<edited/>", "<applied/>"), seen);
    }

    /** A map the edits hold under the device's name, in any spelling, wins over its default labels. */
    @Test
    void theUsersOwnMapWinsOverItsDefaults() throws IOException {
        edits.change(current -> current.withButtonMaps(
                Map.of("lvwap.buttonMap", bytes("<Root><Joy_1>FIRE</Joy_1></Root>"))));

        apply((current, applied) -> LVWAP_DEFAULTS, installation -> live(LVWAP)).apply();

        Map<String, byte[]> maps = written.getFirst().buttonMaps();
        assertEquals(1, maps.size());
        assertEquals("<Root><Joy_1>FIRE</Joy_1></Root>", text(maps.get("lvwap.buttonMap")));
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

    /** One that cannot be parsed, likewise. */
    @Test
    void anInstallationThatCannotBeParsedIsLeftAlone() throws IOException {
        edits.change(current -> set("<edited/>", LVWAP));

        apply(installation -> live("not xml at all")).apply();

        assertEquals(List.of("STEAM"), leftAlone);
    }

    /**
     * The working copy is BindForge's own: one it cannot parse stops APPLY before the backup, rather than leaving every
     * installation alone and blaming them (review, 2026-10-27).
     */
    @Test
    void aWorkingCopyThatCannotBeParsedStopsApplyBeforeTheBackup() throws IOException {
        workingCopy.set(set("<applied/>", "not xml at all"));
        edits.change(current -> set("<edited/>", LVWAP));

        assertThrows(IOException.class, apply(installation -> live(LVWAP))::apply);

        assertEquals(List.of(), backedUp);
        assertEquals(List.of(), written);
        assertTrue(edits.hasEdits());
    }

    @Test
    void aBackupThatFailsStopsApplyWithTheEditsStillOnScreen() throws IOException {
        edits.change(current -> set("<edited/>", LVWAP));
        Apply apply = new Apply(edits, (current, applied) -> Map.of(), (files, backUp, leaveAlone) -> {
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
        return apply((current, applied) -> Map.of(), reader);
    }

    private Apply apply(Apply.DefaultLabels defaults, Apply.Reader reader) {
        return new Apply(edits, defaults, (files, backUp, leaveAlone) -> {
            BackupSources sources = sources();
            backUp.take(sources);
            written.add(files);
            for (BackupSources.Installation installation : sources.installations()) {
                if (leaveAlone.leaveAlone(installation)) leftAlone.add(installation.storefront());
            }
            return new WriteReport(new WriteReport.Place(WriteReport.Kind.BINDINGS_FOLDER, null, Path.of("Bindings"),
                    List.of(), List.of(), List.of(), List.of()), List.of());
        }, sources -> {
            backedUp.add("APPLY");
            return new Backup(Path.of("BeforeChanges/2026-10-26_10-00-00.zip"), LocalDateTime.now(),
                    Backups.Kind.BEFORE_CHANGES, Backups.Before.APPLY, true, List.of());
        }, reader);
    }

    private static BackupSources sources() {
        return new BackupSources(Path.of("Bindings"),
                List.of(new BackupSources.Installation("STEAM", Path.of("Steam"), false, null)));
    }

    private static DeviceFiles live(String deviceMappings) {
        return new DeviceFiles(bytes(deviceMappings), Map.of());
    }

    private static ConfigurationFiles set(String binds, String deviceMappings) {
        return new ConfigurationFiles(Map.of("Custom.4.2.binds", bytes(binds)), bytes(deviceMappings), Map.of());
    }
}
