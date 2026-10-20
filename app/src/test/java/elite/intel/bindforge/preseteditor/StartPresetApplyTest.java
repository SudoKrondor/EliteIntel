package elite.intel.bindforge.preseteditor;

import elite.intel.bindforge.io.BindingsLoader;
import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.io.TimestampedBackups;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Apply for the preset file: the per-line merge, the refusals, the game's form, Edit History, and the promote.
 * The game's file starts as the specimen, {@code KeyboardMouseOnly / Custom / Custom / Custom}.
 */
class StartPresetApplyTest {

    private static final String GAME_FILE = "StartPreset.4.start";

    @TempDir
    Path bindingsDir;

    @TempDir
    Path workingDir;

    @TempDir
    Path controlSchemes;

    @TempDir
    Path historyDir;

    private StartPresetStore store;

    @BeforeEach
    void setUp() throws IOException {
        copySpecimen(GAME_FILE);
        binds(bindingsDir, "Custom.4.2.binds", "Custom");
        binds(bindingsDir, "Testing.4.2.binds", "Testing");
        binds(controlSchemes, "KeyboardMouseOnly.binds", "KeyboardMouseOnly");
        store = new StartPresetStore(workingDir, new BindingsLoader());
        store.masterOrSeedFrom(bindingsDir);
    }

    private StartPresetApply apply(int retention) {
        BindingsLoader loader = new BindingsLoader();
        return new StartPresetApply(store, loader, new BindNameCatalog(loader), new TimestampedBackups(), historyDir,
                () -> retention);
    }

    private StartPresetApply.Outcome applyDraft() throws IOException {
        return apply(10).apply(bindingsDir, List.of(controlSchemes));
    }

    @Test
    void theDraftIsWrittenInTheGamesForm() throws IOException {
        store.save(sections("Custom", "Testing", "Custom", "Custom"));

        StartPresetApply.Outcome outcome = applyDraft();

        StartPresetApply.Applied applied = assertInstanceOf(StartPresetApply.Applied.class, outcome);
        assertEquals(bindingsDir.resolve(GAME_FILE), applied.gameFile());
        assertEquals("Custom\r\nTesting\r\nCustom\r\nCustom", gameText());
    }

    @Test
    void fourMatchingLinesAreStillFourLines() throws IOException {
        store.save(sections("Custom", "Custom", "Custom", "Custom"));

        applyDraft();

        assertEquals("Custom\r\nCustom\r\nCustom\r\nCustom", gameText());
    }

    @Test
    void theSingleLineFormIsWrittenAsFourLines() throws IOException {
        Files.delete(bindingsDir.resolve(GAME_FILE));
        copySpecimen("StartPreset.start");
        store = new StartPresetStore(workingDir.resolve("single-line"), new BindingsLoader());
        store.masterOrSeedFrom(bindingsDir);
        store.save(sections("Testing", "Custom", "Custom", "Custom"));

        applyDraft();

        assertEquals("Testing\r\nCustom\r\nCustom\r\nCustom",
                Files.readString(bindingsDir.resolve("StartPreset.start"), StandardCharsets.UTF_8));
    }

    @Test
    void theWrittenFileBecomesTheMasterAndTheDraftIsDropped() throws IOException {
        store.save(sections("Custom", "Testing", "Custom", "Custom"));

        applyDraft();

        assertArrayEquals(Files.readAllBytes(bindingsDir.resolve(GAME_FILE)),
                Files.readAllBytes(workingDir.resolve("master").resolve(GAME_FILE)));
        assertTrue(store.draft().isEmpty());
        assertFalse(store.hasDraftChanges());
    }

    @Test
    void theReplacedFileIsKeptInEditHistory() throws IOException {
        byte[] before = Files.readAllBytes(bindingsDir.resolve(GAME_FILE));
        store.save(sections("Custom", "Testing", "Custom", "Custom"));

        StartPresetApply.Applied applied = assertInstanceOf(StartPresetApply.Applied.class, applyDraft());

        assertEquals(historyDir, applied.kept().getParent());
        assertTrue(applied.kept().getFileName().toString().startsWith(GAME_FILE + "."));
        assertArrayEquals(before, Files.readAllBytes(applied.kept()));
    }

    @Test
    void editHistoryIsPrunedToTheRetention() throws IOException {
        Clock past = Clock.fixed(Instant.parse("2020-01-01T12:00:00Z"), ZoneOffset.UTC);
        TimestampedBackups oldCopies = new TimestampedBackups(past);
        for (int i = 0; i < 3; i++) {
            oldCopies.create(bindingsDir.resolve(GAME_FILE), historyDir);
        }
        store.save(sections("Custom", "Testing", "Custom", "Custom"));

        StartPresetApply.Applied applied = assertInstanceOf(StartPresetApply.Applied.class,
                apply(2).apply(bindingsDir, List.of(controlSchemes)));

        List<Path> left = history();
        assertEquals(2, left.size());
        assertTrue(left.contains(applied.kept()), "the copy just taken is the newest and is kept");
    }

    @Test
    void aGameChangeToAnotherLineIsKept() throws IOException {
        store.save(sections("KeyboardMouseOnly", "Testing", "Custom", "Custom"));
        writeGame("KeyboardMouseOnly\r\nCustom\r\nCustom\r\nTesting");

        StartPresetApply.Applied applied = assertInstanceOf(StartPresetApply.Applied.class, applyDraft());

        assertEquals(sections("KeyboardMouseOnly", "Testing", "Custom", "Testing"), applied.written());
        assertEquals("KeyboardMouseOnly\r\nTesting\r\nCustom\r\nTesting", gameText());
    }

    @Test
    void theSameLineChangedDifferentlyIsAConflictAndNothingIsWritten() throws IOException {
        store.save(sections("KeyboardMouseOnly", "Testing", "Custom", "Custom"));
        writeGame("KeyboardMouseOnly\r\nKeyboardMouseOnly\r\nCustom\r\nCustom");

        StartPresetApply.Conflicts conflicts = assertInstanceOf(StartPresetApply.Conflicts.class, applyDraft());

        assertEquals(List.of(BindingSection.SHIP), conflicts.lines().stream().map(StartPresetComparison.Line::section).toList());
        assertEquals("KeyboardMouseOnly\r\nKeyboardMouseOnly\r\nCustom\r\nCustom", gameText());
        assertTrue(store.draft().isPresent(), "a refused Apply keeps the draft");
        assertTrue(history().isEmpty());
    }

    @Test
    void aBindNameNoFileAnswersToIsRefused() throws IOException {
        store.save(sections("KeyboardMouseOnly", "Missing", "Custom", "Custom"));
        byte[] before = Files.readAllBytes(bindingsDir.resolve(GAME_FILE));

        StartPresetApply.UnknownBindNames unknown =
                assertInstanceOf(StartPresetApply.UnknownBindNames.class, applyDraft());

        assertEquals(List.of(BindingSection.SHIP), unknown.sections());
        assertEquals(List.of("Missing"), unknown.values());
        assertArrayEquals(before, Files.readAllBytes(bindingsDir.resolve(GAME_FILE)));
        assertTrue(store.draft().isPresent());
        assertTrue(history().isEmpty());
    }

    @Test
    void anEmptyLineIsRefused() throws IOException {
        store.save(sections("Custom", "", "Custom", "Custom"));

        StartPresetApply.UnknownBindNames unknown =
                assertInstanceOf(StartPresetApply.UnknownBindNames.class, applyDraft());

        assertEquals(List.of(BindingSection.SHIP), unknown.sections());
        assertEquals(List.of(""), unknown.values());
    }

    @Test
    void aLineTheDraftDidNotChangeIsCheckedToo() throws IOException {
        Files.delete(bindingsDir.resolve("Custom.4.2.binds"));
        store.save(sections("Testing", "Custom", "Custom", "Custom"));

        StartPresetApply.UnknownBindNames unknown =
                assertInstanceOf(StartPresetApply.UnknownBindNames.class, applyDraft());

        assertEquals(List.of(BindingSection.SHIP, BindingSection.SRV, BindingSection.ON_FOOT), unknown.sections());
    }

    @Test
    void aFileWithABindsNameThatIsNotABindsFileAnswersToNothing() throws IOException {
        Files.writeString(bindingsDir.resolve("Fake.binds"), "Custom\r\nCustom", StandardCharsets.UTF_8);
        store.save(sections("Fake", "Custom", "Custom", "Custom"));

        assertInstanceOf(StartPresetApply.UnknownBindNames.class, applyDraft());
    }

    @Test
    void aFactoryBindNameIsAccepted() throws IOException {
        store.save(sections("Custom", "KeyboardMouseOnly", "Custom", "Custom"));

        assertInstanceOf(StartPresetApply.Applied.class, applyDraft());
    }

    @Test
    void valuesTheGameAlreadyHoldsAreNotWrittenButBecomeTheMaster() throws IOException {
        store.save(sections("Custom", "Testing", "Custom", "Custom"));
        writeGame("Custom\nTesting\nCustom\nCustom\n");

        StartPresetApply.Outcome outcome = applyDraft();

        assertInstanceOf(StartPresetApply.AlreadyInGame.class, outcome);
        assertEquals("Custom\nTesting\nCustom\nCustom\n", gameText(), "the game's own bytes are left alone");
        assertArrayEquals(Files.readAllBytes(bindingsDir.resolve(GAME_FILE)),
                Files.readAllBytes(workingDir.resolve("master").resolve(GAME_FILE)));
        assertTrue(store.draft().isEmpty());
        assertTrue(history().isEmpty());
    }

    @Test
    void noDraftAppliesNothingEvenWhenTheGameChanged() throws IOException {
        writeGame("Custom\r\nCustom\r\nCustom\r\nCustom");
        byte[] master = Files.readAllBytes(workingDir.resolve("master").resolve(GAME_FILE));

        assertInstanceOf(StartPresetApply.NothingToApply.class, applyDraft());

        assertEquals("Custom\r\nCustom\r\nCustom\r\nCustom", gameText());
        assertArrayEquals(master, Files.readAllBytes(workingDir.resolve("master").resolve(GAME_FILE)),
                "a game change alone is adopted by P5, never by Apply");
    }

    @Test
    void aDraftWithNoMasterIsRefused() throws IOException {
        store.save(sections("Custom", "Testing", "Custom", "Custom"));
        Files.delete(workingDir.resolve("master").resolve(GAME_FILE));

        assertInstanceOf(StartPresetApply.NoMaster.class, applyDraft());
        assertEquals(Optional.of(sections("Custom", "Testing", "Custom", "Custom")), store.draft());
    }

    @Test
    void noGameFileIsRefusedAndNothingIsCreated() throws IOException {
        store.save(sections("Custom", "Testing", "Custom", "Custom"));
        Files.delete(bindingsDir.resolve(GAME_FILE));

        assertInstanceOf(StartPresetApply.NoGameFile.class, applyDraft());
        assertFalse(Files.exists(bindingsDir.resolve(GAME_FILE)));
    }

    @Test
    void theGamesFormReadsBackAsTheSameValues() {
        StartPresetSections split = sections("KeyboardMouseOnly", "Custom", "Custom", "Custom");

        assertEquals(split, StartPresetSections.parse(StartPresetApply.gameForm(split)));
    }

    private static StartPresetSections sections(String general, String ship, String srv, String onFoot) {
        return new StartPresetSections(general, ship, srv, onFoot, false);
    }

    private String gameText() throws IOException {
        return Files.readString(bindingsDir.resolve(GAME_FILE), StandardCharsets.UTF_8);
    }

    private void writeGame(String text) throws IOException {
        Files.writeString(bindingsDir.resolve(GAME_FILE), text, StandardCharsets.UTF_8);
    }

    private List<Path> history() throws IOException {
        try (Stream<Path> files = Files.list(historyDir)) {
            return files.toList();
        }
    }

    private static void binds(Path folder, String fileName, String bindName) throws IOException {
        Files.writeString(folder.resolve(fileName), "<Root PresetName=\"" + bindName + "\" MajorVersion=\"4\" MinorVersion=\"2\">"
                + "<YawLeftButton><Primary Device=\"Keyboard\" Key=\"Key_A\" /></YawLeftButton></Root>", StandardCharsets.UTF_8);
    }

    private void copySpecimen(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/bindforge/startpreset/" + name)) {
            assertNotNull(in, "missing test resource " + name);
            Files.write(bindingsDir.resolve(name), in.readAllBytes());
        }
    }
}
