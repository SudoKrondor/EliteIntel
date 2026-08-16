package elite.intel.ai.brain.actions.handlers.commands.custom;

import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the custom-command files shipped in {@code custom-commands/} - definitions a commander imports
 * through the Custom Commands tab rather than built-ins the app registers itself.
 *
 * <p>A shipped file is authored by hand, so it gets none of the editor's live validation. It is also the one
 * kind of custom command whose phrases span every locale at once: a single {@code phrases} list is all a
 * custom command has, so the nine languages share it. That makes two mistakes easy and invisible until a
 * commander imports the file - a phrase the import dialog rejects as colliding with a built-in alias in one
 * language, and two locales spelling a phrase identically (the validator counts that as a duplicate).
 *
 * <p>Runs the same validation the import flow runs ({@link CustomCommandValidator}), once per locale,
 * because built-in phrase collisions are resolved against the <em>session</em> language.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShippedCustomCommandFilesTest {

    /**
     * The repo's shipped-definition folder, relative to the {@code app} module the tests run in.
     */
    private static final Path SHIPPED_DIR = Path.of("..", "custom-commands");

    @BeforeAll
    void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @Test
    void theShippedFolderIsNotEmpty() throws IOException {
        assertFalse(shippedFiles().isEmpty(), () -> "no .json files found in " + SHIPPED_DIR.toAbsolutePath());
    }

    /**
     * Every shipped definition must import cleanly in every language the app ships.
     */
    @ParameterizedTest(name = "{0}: every shipped definition imports without errors")
    @EnumSource(Language.class)
    void everyShippedDefinitionValidatesInEveryLocale(Language language) throws IOException {
        SystemSession.getInstance().setLanguage(language);
        try {
            for (Path file : shippedFiles()) {
                List<CustomCommandExportImportService.ImportCandidate> candidates =
                        CustomCommandExportImportService.parseImport(read(file), List.of());
                assertFalse(candidates.isEmpty(), () -> file.getFileName() + " parsed to no entries");
                for (CustomCommandExportImportService.ImportCandidate candidate : candidates) {
                    assertTrue(candidate.isValid(),
                            () -> file.getFileName() + " is not importable: " + candidate.validationErrors());
                    List<String> errors = CustomCommandValidator.validate(candidate.definition(), List.of(), null);
                    assertTrue(errors.isEmpty(),
                            () -> language + " rejects " + file.getFileName() + ": " + errors);
                }
            }
        } finally {
            SystemSession.getInstance().setLanguage(Language.EN);
        }
    }

    /**
     * The nine locales share one phrase list, so two of them spelling a phrase the same way silently costs
     * one language its trigger. {@link CustomCommandValidator} reports it, but only as one error among many;
     * this names the offending phrase directly.
     */
    @Test
    void noShippedDefinitionRepeatsAPhrase() throws IOException {
        for (Path file : shippedFiles()) {
            for (var candidate : CustomCommandExportImportService.parseImport(read(file), List.of())) {
                Set<String> seen = new HashSet<>();
                for (String phrase : AiActionLocalizations.splitPhraseGroup(candidate.definition().getPhrases())) {
                    String normalized = phrase.trim().toLowerCase(Locale.ROOT);
                    assertTrue(seen.add(normalized),
                            () -> file.getFileName() + " repeats the phrase \"" + phrase + "\"");
                }
            }
        }
    }

    /**
     * A step naming a binding Elite Dangerous does not have would fail only at execution time, announced as
     * a missing binding, which reads to the commander exactly like an unbound key.
     */
    @Test
    void everyStepNamesARealGameBinding() throws IOException {
        Set<String> known = new HashSet<>();
        for (elite.intel.ai.hands.Bindings.GameCommand command : elite.intel.ai.hands.Bindings.GameCommand.values()) {
            known.add(command.getGameBinding());
        }
        for (Path file : shippedFiles()) {
            for (var candidate : CustomCommandExportImportService.parseImport(read(file), List.of())) {
                for (String bindingId : candidate.definition().distinctBindingIds()) {
                    assertTrue(known.contains(bindingId),
                            () -> file.getFileName() + " names unknown binding \"" + bindingId + "\"");
                }
            }
        }
    }

    /**
     * The external-camera definition is the reported case: "deploy external camera" answered with "I don't
     * recognize that command", because no action - built-in or custom - owned any camera phrase at all.
     */
    @Test
    void theExternalCameraDefinitionCoversTheReportedUtterances() throws IOException {
        CustomCommandDefinition camera = CustomCommandExportImportService
                .parseImport(read(SHIPPED_DIR.resolve("external-camera.json")), List.of())
                .get(0).definition();

        assertEquals("external_camera", camera.getActionKey());
        assertEquals(List.of("PhotoCameraToggle"), camera.distinctBindingIds());

        Set<String> phrases = new HashSet<>(
                AiActionLocalizations.splitPhraseGroup(camera.getPhrases()).stream()
                        .map(phrase -> phrase.trim().toLowerCase(Locale.ROOT))
                        .toList());
        // The English wording, and the Cyrillic transliteration of it the RU commander actually said.
        assertTrue(phrases.contains("deploy external camera"));
        assertTrue(phrases.contains("экстернал камера"));
        // One phrase per shipped language, so no commander is left without a trigger.
        for (String perLocale : List.of(
                "external camera", "externe kamera", "cámara externa", "caméra externe",
                "telecamera esterna", "câmara externa", "câmera externa",
                "внешняя камера", "зовнішня камера")) {
            assertTrue(phrases.contains(perLocale), () -> "missing localized phrase: " + perLocale);
        }
    }

    private static List<Path> shippedFiles() throws IOException {
        try (var stream = Files.list(SHIPPED_DIR)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".json")).sorted().toList();
        }
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
