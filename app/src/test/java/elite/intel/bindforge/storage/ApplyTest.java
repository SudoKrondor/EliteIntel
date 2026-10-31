package elite.intel.bindforge.storage;

import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.preseteditor.BindNameEntry;
import elite.intel.bindforge.preseteditor.BindNameList;
import elite.intel.bindforge.preseteditor.BindsFileOrigin;
import elite.intel.bindforge.storage.BindingsComparison.Side;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static elite.intel.bindforge.storage.BindingsComparisonTest.binds;
import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static elite.intel.bindforge.storage.WorkingCopyTest.text;
import static org.junit.jupiter.api.Assertions.*;

/**
 * APPLY (storage.md rules 7-8; Storage S4a): the edits and the default labels, through the write path with a
 * before-APPLY backup, leaving alone an installation whose device files differ from the working copy - and, first of
 * all, refusing a preset file that names a binds file nobody can find (Storage S6). Every APPLY runs the check
 * first, and the edits go on top of what it settled (Storage S8b).
 */
class ApplyTest {

    private static final String LVWAP = "<Root><LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP></Root>";
    private static final Map<String, byte[]> LVWAP_DEFAULTS =
            Map.of("LVWAP.buttonMap", bytes("<Root><Joy_1>Button 1</Joy_1></Root>"));
    private static final String CUSTOM_BINDS = "Custom.4.2.binds";
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
    /** The working copy each backup held as well - {@code null} where the check settled nothing. */
    private final List<ConfigurationFiles> heldWorkingCopies = new ArrayList<>();
    private final List<Set<String>> removed = new ArrayList<>();
    /** The bindings folder the check reads; {@code null} for the working copy's own files. */
    private Map<String, byte[]> liveBindings;
    /** Thrown when the check reads the bindings folder, when set. */
    private IOException unreadable;

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
        Run apply = new Run(new Apply(edits, (current, applied) -> Map.of(), (files, backUp, leaveAlone, remove) -> {
            backUp.take(sources());
            return null;
        }, (sources, held) -> {
            throw new IOException("the backup folder cannot be written");
        }, installation -> live(LVWAP), OFFERED, check()));

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

        new Run(new Apply(edits, (current, applied) -> Map.of(), this::write, this::backUp,
                installation -> live(LVWAP), files -> {
                    seen.add(text(files.bindings().get("Custom.4.2.binds")));
                    return OFFERED.of(files);
                }, check())).apply();

        assertEquals(List.of("<edited/>", "<edited/>"), seen,
                "checked before the warning, and again on the set written (Storage S8b)");
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

        PresetRefusedException refused = assertThrows(PresetRefusedException.class, new Run(new Apply(edits,
                (current, applied) -> Map.of(), this::write, this::backUp, installation -> live(LVWAP),
                files -> new Apply.Offered(new BindNameList(List.of(CUSTOM), List.of()), List.of(unplugged)),
                check()))::apply);

        assertEquals(List.of(BindingSection.GENERAL), refused.sections());
        assertEquals(List.of(unplugged), refused.unreachable());
        assertEquals(List.of(), backedUp);
        assertEquals(List.of(), written);
    }

    /** No drive explains an empty line, so the refusal blames the preset file alone (Storage S6R2, 2026-10-30). */
    @Test
    void anEmptyLineIsRefusedWithoutNamingTheInstallationsThatCouldNotBeReached() throws IOException {
        edits.change(current -> current.withBindingsFile("StartPreset.4.start",
                bytes("\r\nCustom\r\nCustom\r\nCustom")));
        InstallationRow unplugged = new InstallationRow(2, "EPIC", root.resolve("Epic").toString(), false, false);

        PresetRefusedException refused = assertThrows(PresetRefusedException.class, new Run(new Apply(edits,
                (current, applied) -> Map.of(), this::write, this::backUp, installation -> live(LVWAP),
                files -> new Apply.Offered(new BindNameList(List.of(CUSTOM), List.of()), List.of(unplugged)),
                check()))::apply);

        assertEquals(List.of(BindingSection.GENERAL), refused.sections());
        assertEquals(List.of(), refused.unreachable());
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

    // ---- the check at APPLY (Storage S8b)

    /**
     * The check settles the working copy first - here the game's change kept - and the edits go on top, in one backup
     * and one write; the backup holds the working copy too, since the check changed it (Alan, 2026-10-30).
     */
    @Test
    void theGamesChangeIsSettledAndTheEditsGoOnTopInOneWrite() throws IOException {
        workingCopy.set(bindsSet(binds("Custom", "Key_A", "Joy_XAxis", "")));
        edits.change(current -> current.withBindingsFile(CUSTOM_BINDS,
                bytes(binds("Custom", "Key_A", "Joy_YAxis", ""))));
        liveBindings = bindsSet(binds("Custom", "Key_Q", "Joy_XAxis", "")).bindings();
        Apply apply = applying((current, applied) -> Map.of(), installation -> live(LVWAP));

        BindingsCheck.Found found = apply.check();
        Written result = apply.apply(found, Map.of(found.differences().items().getFirst().id(), Side.LIVE));

        assertEquals(1, found.differences().items().size(), "the game's line only - the edit's is not asked");
        assertEquals(1, written.size());
        assertEquals(binds("Custom", "Key_Q", "Joy_YAxis", ""), text(written.getFirst().bindings().get(CUSTOM_BINDS)));
        assertTrue(heldWorkingCopies.getFirst().sameAs(workingCopy.get()), "the working copy, before the check");
        assertEquals(Backups.Before.APPLY, result.backup().before());
    }

    /**
     * A line the edits change is not asked about, and the edit wins on it. With nothing else to ask the check settled
     * nothing, so the backup holds no working copy.
     */
    @Test
    void aLineTheEditsChangeIsNotAskedAndTheEditWins() throws IOException {
        workingCopy.set(bindsSet(binds("Custom", "Key_A", "Joy_XAxis", "")));
        edits.change(current -> current.withBindingsFile(CUSTOM_BINDS,
                bytes(binds("Custom", "Key_W", "Joy_XAxis", ""))));
        liveBindings = bindsSet(binds("Custom", "Key_Q", "Joy_XAxis", "")).bindings();
        Apply apply = applying((current, applied) -> Map.of(), installation -> live(LVWAP));

        BindingsCheck.Found found = apply.check();
        apply.apply(found, Map.of());

        assertTrue(found.differences().none());
        assertEquals(1, found.edited());
        assertEquals(binds("Custom", "Key_W", "Joy_XAxis", ""), text(written.getFirst().bindings().get(CUSTOM_BINDS)));
        assertNull(heldWorkingCopies.getFirst());
    }

    /** Putting mine back at APPLY removes a live-only binds file, after the backup, as the check does. */
    @Test
    void keepingMineAtApplyRemovesTheLiveOnlyFile() throws IOException {
        workingCopy.set(bindsSet(binds("Custom", "Key_A", "Joy_XAxis", "")));
        edits.change(current -> current.withBindingsFile(CUSTOM_BINDS,
                bytes(binds("Custom", "Key_A", "Joy_YAxis", ""))));
        Map<String, byte[]> live = new HashMap<>(workingCopy.get().bindings());
        live.put("Spare.4.2.binds", bytes(binds("Spare", "Key_A", "Joy_XAxis", "")));
        liveBindings = live;
        Apply apply = applying((current, applied) -> Map.of(), installation -> live(LVWAP));

        BindingsCheck.Found found = apply.check();
        apply.apply(found, Map.of(found.differences().items().getFirst().id(), Side.WORKING_COPY));

        assertEquals(List.of(Set.of("Spare.4.2.binds")), removed);
        assertFalse(written.getFirst().bindings().containsKey("Spare.4.2.binds"));
        assertEquals(binds("Custom", "Key_A", "Joy_YAxis", ""), text(written.getFirst().bindings().get(CUSTOM_BINDS)));
    }

    /** Nothing watches the files: a change after the warning opened stops APPLY, nothing written, the edits kept. */
    @Test
    void aFileChangedAfterTheCheckStopsApplyWithNothingWritten() throws IOException {
        workingCopy.set(bindsSet(binds("Custom", "Key_A", "Joy_XAxis", "")));
        edits.change(current -> current.withBindingsFile(CUSTOM_BINDS,
                bytes(binds("Custom", "Key_A", "Joy_YAxis", ""))));
        liveBindings = bindsSet(binds("Custom", "Key_Q", "Joy_XAxis", "")).bindings();
        Apply apply = applying((current, applied) -> Map.of(), installation -> live(LVWAP));
        BindingsCheck.Found found = apply.check();
        liveBindings = bindsSet(binds("Custom", "Key_E", "Joy_XAxis", "")).bindings();

        BindingsCheck.ChangedMeanwhileException changed = assertThrows(BindingsCheck.ChangedMeanwhileException.class,
                () -> apply.apply(found, Map.of(found.differences().items().getFirst().id(), Side.LIVE)));

        assertEquals("Key_E", changed.now().differences().items().getFirst().live());
        assertEquals(List.of(), backedUp);
        assertEquals(List.of(), written);
        assertTrue(edits.hasEdits());
        assertFalse(edits.writing());
    }

    /** The preset file is checked again on the set written: a merge can take the game's line, naming nothing. */
    @Test
    void aPresetLineTakenFromTheGameIsCheckedAgainBeforeTheBackup() throws IOException {
        workingCopy.set(bindsSet(binds("Custom", "Key_A", "Joy_XAxis", "")));
        edits.change(current -> current.withBindingsFile(CUSTOM_BINDS,
                bytes(binds("Custom", "Key_A", "Joy_YAxis", ""))));
        liveBindings = workingCopy.get().withBindingsFile("StartPreset.4.start",
                bytes("Custom\r\nGone\r\nCustom\r\nCustom")).bindings();
        Apply apply = applying((current, applied) -> Map.of(), installation -> live(LVWAP));

        BindingsCheck.Found found = apply.check();
        PresetRefusedException refused = assertThrows(PresetRefusedException.class,
                () -> apply.apply(found, Map.of(found.differences().items().getFirst().id(), Side.LIVE)));

        assertEquals(List.of(BindingSection.SHIP), refused.sections());
        assertEquals(List.of(), backedUp);
        assertTrue(edits.hasEdits());
    }

    /** A game file the check cannot read could hold the user's work: APPLY stops before the backup (Alan, 2026-10-30). */
    @Test
    void theCheckThatCannotReadTheGamesFilesStopsApply() throws IOException {
        edits.change(current -> set("<edited/>", LVWAP));
        unreadable = new IOException("locked");

        assertThrows(IOException.class, apply(installation -> live(LVWAP))::apply);

        assertEquals(List.of(), backedUp);
        assertEquals(List.of(), written);
        assertTrue(edits.hasEdits());
    }

    /** APPLY as the bar runs it: the check, then the write - with nothing to answer unless a test answers. */
    private record Run(Apply applying) {
        Written apply() throws IOException {
            return applying.apply(applying.check(), Map.of());
        }
    }

    private Run apply(Apply.Reader reader) {
        return apply((current, applied) -> Map.of(), reader);
    }

    private Run apply(Apply.DefaultLabels defaults, Apply.Reader reader) {
        return new Run(applying(defaults, reader));
    }

    private Apply applying(Apply.DefaultLabels defaults, Apply.Reader reader) {
        return new Apply(edits, defaults, this::write, this::backUp, reader, OFFERED, check());
    }

    /** The check, reading {@link #liveBindings} as the bindings folder - the working copy's files until a test sets it. */
    private BindingsCheck check() {
        return new BindingsCheck(edits, () -> Path.of("Bindings"), folder -> {
            if (unreadable != null) throw unreadable;
            return liveBindings != null ? liveBindings : workingCopy.get().bindings();
        }, files -> BindingsComparison.Factory.NONE, (files, backUp, leaveAlone, remove) -> {
            throw new AssertionError("APPLY writes through its own writer");
        }, (sources, files) -> {
            throw new AssertionError("APPLY takes its own backup");
        }, installation -> {
            throw new AssertionError("APPLY reads the installations through its own reader");
        });
    }

    private WriteReport write(ConfigurationFiles files, WritePath.BackUpFirst backUp, LiveCopies.LeaveAlone leaveAlone,
                              Set<String> remove) throws IOException {
        BackupSources sources = sources();
        backUp.take(sources);
        written.add(files);
        removed.add(remove);
        for (BackupSources.Installation installation : sources.installations()) {
            if (leaveAlone.leaveAlone(installation)) leftAlone.add(installation.storefront());
        }
        return new WriteReport(new WriteReport.Place(WriteReport.Kind.BINDINGS_FOLDER, null, Path.of("Bindings"),
                List.of(), List.of(), List.of(), List.of()), List.of());
    }

    private Backup backUp(BackupSources sources, ConfigurationFiles held) {
        backedUp.add("APPLY");
        heldWorkingCopies.add(held);
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

    /** A real binds file as {@code Custom}, the preset file naming it on all four lines, and LVWAP's device file. */
    private static ConfigurationFiles bindsSet(String custom) {
        return new ConfigurationFiles(Map.of(CUSTOM_BINDS, bytes(custom), "StartPreset.4.start", bytes(ALL_CUSTOM)),
                bytes(LVWAP), Map.of());
    }

    /** A binds file, a preset file naming it on all four lines, and a {@code DeviceMappings.xml}. */
    private static ConfigurationFiles set(String binds, String deviceMappings) {
        return new ConfigurationFiles(Map.of("Custom.4.2.binds", bytes(binds), "StartPreset.4.start", bytes(ALL_CUSTOM)),
                bytes(deviceMappings), Map.of());
    }
}
