package elite.intel.bindforge.storage;

import elite.intel.bindforge.preseteditor.BindNameEntry;
import elite.intel.bindforge.preseteditor.BindsFileOrigin;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.ui.i18n.MultiLingualTextProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The notice when Elite-Intel starts (Storage S14c): BindForge's lines in a fixed order - read only, changed outside,
 * a binds file named like a factory one, the files the check will move to Leftover User Files (Storage S13b, S13d) -
 * with <em>changed outside</em> and the files to move not said while read only, and missing installations named by
 * their store alone.
 */
class StartupNoticeTest {

    private static final InstallationRow STEAM = new InstallationRow(1, "STEAM", "C:\\Steam", false, false);
    private static final InstallationRow MANUAL = new InstallationRow(2, "MANUAL", "E:\\Games\\Elite", true, false);

    @BeforeEach
    void english() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @Test
    void nothingToSaySaysNothing() {
        assertEquals(List.of(), notice(List.of(), false, List.of()).lines());
    }

    @Test
    void theLinesComeInAFixedOrder() {
        assertEquals(List.of(new StartupNotice.Line.ChangedOutside(),
                        new StartupNotice.Line.NamedLikeFactory(List.of("KeyboardMouseOnly"))),
                notice(List.of(), true, List.of("KeyboardMouseOnly")).lines());
    }

    /** Alan, 2026-11-06: the read-only warning covers it, and the check it points to is off. */
    @Test
    void changedOutsideIsNotSaidWhileReadOnly() {
        StartupNotice notice = new StartupNotice(() -> List.of(STEAM), () -> {
            throw new AssertionError("the files are not compared while read only");
        }, List::of, List::of);

        assertEquals(List.of(new StartupNotice.Line.ReadOnly(List.of(STEAM))), notice.lines());
    }

    @Test
    void aFileNamedLikeAFactoryOneIsSaidWhileReadOnlyToo() {
        assertEquals(List.of(new StartupNotice.Line.ReadOnly(List.of(STEAM)),
                        new StartupNotice.Line.NamedLikeFactory(List.of("KeyboardMouseOnly"))),
                notice(List.of(STEAM), false, List.of("KeyboardMouseOnly")).lines());
    }

    /** A hint only: a fault working out the factory names is logged, and the other lines are still said. */
    @Test
    void aFaultInOneLineLeavesTheOthers() {
        StartupNotice notice = new StartupNotice(List::of, () -> true, () -> {
            throw new IOException("working copy locked");
        }, () -> {
            throw new IOException("bindings folder locked");
        });

        assertEquals(List.of(new StartupNotice.Line.ChangedOutside()), notice.lines());
    }

    // ---- files the check will move (Storage S13b, S13d)

    /** The fourth line, after the factory-name line. */
    @Test
    void filesToMoveAreTheLastLine() {
        StartupNotice notice = new StartupNotice(List::of, () -> true, () -> List.of("KeyboardMouseOnly"),
                () -> List.of("Custom.binds"));

        assertEquals(List.of(new StartupNotice.Line.ChangedOutside(),
                new StartupNotice.Line.NamedLikeFactory(List.of("KeyboardMouseOnly")),
                new StartupNotice.Line.ToMove(List.of("Custom.binds"))), notice.lines());
    }

    /** Alan, 2026-11-07, Q5: the check that moves them is off while read only, so they are not said then. */
    @Test
    void filesToMoveAreNotSaidWhileReadOnly() {
        StartupNotice notice = new StartupNotice(() -> List.of(STEAM), () -> false, List::of, () -> {
            throw new AssertionError("the bindings folder is not looked at while read only");
        });

        assertEquals(List.of(new StartupNotice.Line.ReadOnly(List.of(STEAM))), notice.lines());
    }

    @Test
    void oneFileToMoveAndSeveralAreSaidApart() {
        assertEquals(english(StartupNotice.TO_MOVE_KEY, "Custom.binds"),
                StartupNotice.text(new StartupNotice.Line.ToMove(List.of("Custom.binds"))));
        assertEquals(english(StartupNotice.TO_MOVE_MANY_KEY, "Custom.binds, notes.txt"),
                StartupNotice.text(new StartupNotice.Line.ToMove(List.of("Custom.binds", "notes.txt"))));
    }

    /**
     * Only the files the check will move are named: a read-only one stays, and is named once in the check's report, not
     * at every start (Alan, 2026-11-08, from the S13aR-S13b review's D1).
     */
    @Test
    void aReadOnlyFileIsNotNamed(@TempDir Path folder) throws IOException {
        Files.writeString(folder.resolve("notes.txt"), "a");
        Path kept = folder.resolve("Spare.binds");
        Files.writeString(kept, "b");
        ReadOnlyFlag.set(kept, true);
        try {
            assertEquals(List.of("notes.txt"), StartupNotice.movable(folder, List.of("Spare.binds", "notes.txt")));
        } finally {
            ReadOnlyFlag.set(kept, false);
        }
    }

    @Test
    void eachLineIsHandedOnInOrder() {
        List<String> said = new ArrayList<>();

        notice(List.of(STEAM), false, List.of("KeyboardMouseOnly")).sayOn(said::add, Runnable::run);

        assertEquals(List.of(
                english(StartupNotice.READ_ONLY_KEY, "Steam"),
                english(StartupNotice.NAMED_LIKE_FACTORY_KEY, "KeyboardMouseOnly")), said);
    }

    /** Alan, 2026-11-06: the store alone, never the path - no use spoken. */
    @Test
    void aMissingInstallationIsNamedByItsStoreAlone() {
        String said = StartupNotice.text(new StartupNotice.Line.ReadOnly(List.of(STEAM, MANUAL)));

        assertEquals(english(StartupNotice.READ_ONLY_KEY,
                "Steam, " + english(StartupNotice.MANUAL_INSTALLATION_KEY)), said);
        assertFalse(said.contains("C:\\Steam"), said);
        assertFalse(said.contains("E:\\Games"), said);
    }

    @Test
    void storesAreSaidAsNames() {
        assertEquals("Steam", StartupNotice.store("STEAM"));
        assertEquals("Epic", StartupNotice.store("EPIC"));
        assertEquals("Frontier", StartupNotice.store("FRONTIER"));
        assertEquals("Oculus", StartupNotice.store("OCULUS"));
        assertEquals(english(StartupNotice.MANUAL_INSTALLATION_KEY), StartupNotice.store("MANUAL"));
        assertEquals("SOMETHING_NEW", StartupNotice.store("SOMETHING_NEW"));
    }

    @Test
    void severalFilesNamedLikeFactoryOnesAreOneLine() {
        assertEquals(english(StartupNotice.NAMED_LIKE_FACTORY_MANY_KEY, "Gamepad, KeyboardMouseOnly"),
                StartupNotice.text(new StartupNotice.Line.NamedLikeFactory(List.of("Gamepad", "KeyboardMouseOnly"))));
    }

    @Test
    void onlyTheUsersFilesShadowingAFactoryOneAreNamed() {
        List<BindNameEntry> entries = List.of(
                new BindNameEntry("Custom", BindsFileOrigin.USER, List.of(Path.of("Custom.4.2.binds"))),
                new BindNameEntry("KeyboardMouseOnly", BindsFileOrigin.USER,
                        List.of(Path.of("KeyboardMouseOnly.4.2.binds")), true),
                new BindNameEntry("Gamepad", BindsFileOrigin.FACTORY, List.of(Path.of("C:\\ControlSchemes\\Gamepad.binds"))));

        assertEquals(List.of("KeyboardMouseOnly"), StartupNotice.factoryNamed(entries));
    }

    // ---- helpers

    private static StartupNotice notice(List<InstallationRow> readOnly, boolean changedOutside, List<String> named) {
        return new StartupNotice(() -> readOnly, () -> changedOutside, () -> named, List::of);
    }

    private static String english(String key, Object... args) {
        return MultiLingualTextProvider.getText(Language.EN, key, args);
    }
}
