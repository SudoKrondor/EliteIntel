package elite.intel.bindforge.storage;

import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.preseteditor.BindNameEntry;
import elite.intel.bindforge.preseteditor.BindNameList;
import elite.intel.bindforge.preseteditor.BindsFileOrigin;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
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
 * before-APPLY backup, leaving alone an installation whose device files differ from the working copy - and, first of
 * all, refusing a preset file that names a binds file nobody can find (Storage S6).
 */
class ApplyTest {

    private static final String LVWAP = "<Root><LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP></Root>";
    private static final Map<String, byte[]> LVWAP_DEFAULTS =
            Map.of("LVWAP.buttonMap", bytes("<Root><Joy_1>Button 1</Joy_1></Root>"));
    private static final String ALL_CUSTOM = "Custom\r\nCustom\r\nCustom\r\nCustom";
    private static final BindNameEntry CUSTOM =
            new BindNameEntry("Custom", BindsFileOrigin.USER, List.of(Path.of("Custom.4.2.binds")));
    /** The BindName list: the player's Custom, and the factory KeyboardMouseOnly. Every installation reachable. */
    private static final Apply.BindNames OFFERED = files -> new Apply.Offered(new BindNameList(List.of(CUSTOM,
            new BindNameEntry("KeyboardMouseOnly", BindsFileOrigin.FACTORY,
                    List.of(Path.of("Steam/ControlSchemes/KeyboardMouseOnly.binds")))), List.of()), List.of());

    @TempDir
    Path root;

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
        }, installation -> live(LVWAP), OFFERED);

        assertThrows(IOException.class, apply::apply);
        assertTrue(edits.hasEdits());
        assertFalse(edits.writing());
    }

    /**
     * A line naming a binds file nobody can find is refused before anything is backed up or written, naming the
     * section (preset-editor.md, What APPLY checks and writes for the preset file).
     */
    @Test
    void aPresetLineNamingNothingStopsApplyBeforeTheBackup() throws IOException {
        edits.change(current -> current.withBindingsFile("StartPreset.4.start",
                bytes("Custom\r\nGone\r\nCustom\r\nCustom")));

        PresetRefusedException refused = assertThrows(PresetRefusedException.class,
                apply(installation -> live(LVWAP))::apply);

        assertEquals(List.of(BindingSection.SHIP), refused.sections());
        assertEquals(List.of(), refused.unreachable(), "every installation was reachable");
        assertEquals(List.of(), backedUp);
        assertEquals(List.of(), written);
        assertTrue(edits.hasEdits(), "the edits are still on screen");
        assertFalse(edits.writing());
    }

    /** Every APPLY checks the preset file, whatever the edits are - a device edit alone included (Alan, 2026-10-28). */
    @Test
    void thePresetFileIsCheckedWhenOnlyADeviceIsEdited() throws IOException {
        workingCopy.set(set("<applied/>", LVWAP)
                .withBindingsFile("StartPreset.4.start", bytes("Custom\r\n\r\nGone\r\n")));
        edits.change(current -> current.withButtonMaps(Map.of("LVWAP.buttonMap", bytes("<Root/>"))));

        PresetRefusedException refused = assertThrows(PresetRefusedException.class,
                apply(installation -> live(LVWAP))::apply);

        assertEquals(List.of(BindingSection.SHIP, BindingSection.SRV, BindingSection.ON_FOOT), refused.sections(),
                "an empty line is refused too, and a missing one is empty");
        assertEquals(List.of(), backedUp);
    }

    @Test
    void aSetWithNoPresetFileIsRefusedForAllFourSections() throws IOException {
        workingCopy.set(new ConfigurationFiles(Map.of("Custom.4.2.binds", bytes("<applied/>")), bytes(LVWAP), Map.of()));
        edits.change(current -> current.withBindingsFile("Custom.4.2.binds", bytes("<edited/>")));

        PresetRefusedException refused = assertThrows(PresetRefusedException.class,
                apply(installation -> live(LVWAP))::apply);

        assertEquals(PresetEdits.SECTIONS, refused.sections());
        assertEquals(List.of(), backedUp);
    }

    /** The list is built from the set APPLY writes - the edits, not the working copy. */
    @Test
    void theBindNameListIsBuiltFromTheEdits() throws IOException {
        edits.change(current -> set("<edited/>", LVWAP));
        List<String> seen = new ArrayList<>();

        new Apply(edits, (current, applied) -> Map.of(), this::write, this::backUp, installation -> live(LVWAP),
                files -> {
                    seen.add(text(files.bindings().get("Custom.4.2.binds")));
                    return OFFERED.of(files);
                }).apply();

        assertEquals(List.of("<edited/>"), seen);
    }

    /**
     * A refusal while an installation could not be reached names it: its factory binds files may be what the line
     * names, so the preset file is not blamed alone (Storage S6R, 2026-10-29). Still refused.
     */
    @Test
    void aRefusalNamesTheInstallationsThatCouldNotBeReached() throws IOException {
        edits.change(current -> current.withBindingsFile("StartPreset.4.start",
                bytes("KeyboardMouseOnly\r\nCustom\r\nCustom\r\nCustom")));
        InstallationRow unplugged = new InstallationRow(2, "EPIC", root.resolve("Epic").toString(), false, false);

        PresetRefusedException refused = assertThrows(PresetRefusedException.class, new Apply(edits,
                (current, applied) -> Map.of(), this::write, this::backUp, installation -> live(LVWAP),
                files -> new Apply.Offered(new BindNameList(List.of(CUSTOM), List.of()), List.of(unplugged)))::apply);

        assertEquals(List.of(BindingSection.GENERAL), refused.sections());
        assertEquals(List.of(unplugged), refused.unreachable());
        assertEquals(List.of(), backedUp);
        assertEquals(List.of(), written);
    }

    /**
     * The factory binds files come only from installations that pass the one reachable test - not marked missing, and
     * their ControlSchemes folder there; the others are returned as unreachable (Storage S6R, 2026-10-29).
     */
    @Test
    void theBindNameListReadsOnlyTheReachableInstallations() throws IOException {
        Path steamSchemes = Files.createDirectories(root.resolve("Steam/Products/elite-dangerous-odyssey-64/ControlSchemes"));
        Files.writeString(steamSchemes.resolve("KeyboardMouseOnly.binds"), "<Root PresetName=\"KeyboardMouseOnly\">"
                + "<YawLeftButton><Primary Device=\"Keyboard\" Key=\"Key_A\" /></YawLeftButton></Root>");
        Files.createDirectories(root.resolve("Epic"));
        InstallationRow steam = new InstallationRow(1, "STEAM", root.resolve("Steam").toString(), false, false);
        InstallationRow noControlSchemes = new InstallationRow(2, "EPIC", root.resolve("Epic").toString(), false, false);
        InstallationRow missing = new InstallationRow(3, "FRONTIER", root.resolve("Steam").toString(), false, true);

        Apply.Offered offered = Apply.offered(set("<applied/>", LVWAP), List.of(steam, noControlSchemes, missing));

        assertEquals(List.of(new BindNameEntry("KeyboardMouseOnly", BindsFileOrigin.FACTORY,
                        List.of(steamSchemes.resolve("KeyboardMouseOnly.binds")))),
                offered.list().entries().stream().filter(entry -> entry.origin() == BindsFileOrigin.FACTORY).toList());
        assertEquals(List.of(noControlSchemes, missing), offered.unreachable());
    }

    /** A split preset file - General on a factory binds file - is applied like any other. */
    @Test
    void aPresetLineOnAFactoryBindsFileIsApplied() throws IOException {
        edits.change(current -> current.withBindingsFile("StartPreset.4.start",
                bytes("KeyboardMouseOnly\r\nCustom\r\nCustom\r\nCustom")));

        apply(installation -> live(LVWAP)).apply();

        assertEquals(1, written.size());
        assertEquals(List.of("APPLY"), backedUp);
    }

    private Apply apply(Apply.Reader reader) {
        return apply((current, applied) -> Map.of(), reader);
    }

    private Apply apply(Apply.DefaultLabels defaults, Apply.Reader reader) {
        return new Apply(edits, defaults, this::write, this::backUp, reader, OFFERED);
    }

    private WriteReport write(ConfigurationFiles files, WritePath.BackUpFirst backUp, LiveCopies.LeaveAlone leaveAlone)
            throws IOException {
        BackupSources sources = sources();
        backUp.take(sources);
        written.add(files);
        for (BackupSources.Installation installation : sources.installations()) {
            if (leaveAlone.leaveAlone(installation)) leftAlone.add(installation.storefront());
        }
        return new WriteReport(new WriteReport.Place(WriteReport.Kind.BINDINGS_FOLDER, null, Path.of("Bindings"),
                List.of(), List.of(), List.of(), List.of()), List.of());
    }

    private Backup backUp(BackupSources sources) {
        backedUp.add("APPLY");
        return new Backup(Path.of("BeforeChanges/2026-10-26_10-00-00.zip"), LocalDateTime.now(),
                Backups.Kind.BEFORE_CHANGES, Backups.Before.APPLY, true, List.of());
    }

    private static BackupSources sources() {
        return new BackupSources(Path.of("Bindings"),
                List.of(new BackupSources.Installation("STEAM", Path.of("Steam"), false, null)));
    }

    private static DeviceFiles live(String deviceMappings) {
        return new DeviceFiles(bytes(deviceMappings), Map.of());
    }

    /** A binds file, a preset file naming it on all four lines, and a {@code DeviceMappings.xml}. */
    private static ConfigurationFiles set(String binds, String deviceMappings) {
        return new ConfigurationFiles(Map.of("Custom.4.2.binds", bytes(binds), "StartPreset.4.start", bytes(ALL_CUSTOM)),
                bytes(deviceMappings), Map.of());
    }
}
