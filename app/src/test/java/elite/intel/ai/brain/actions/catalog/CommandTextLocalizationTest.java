package elite.intel.ai.brain.actions.catalog;

import elite.intel.ai.brain.actions.IntelActionContext;
import elite.intel.ai.brain.actions.handlers.commands.CommandI18nKeys;
import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.QueryI18nKeys;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.ui.i18n.MultiLingualTextProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every built-in command and query has a display name and a description of its own in every GUI bundle.
 * <p>
 * Without them, {@link CommandCatalog} quietly falls back: the name to the humanized id -
 * {@code navigate_to_fleet_carrier} becomes "Navigate To Fleet Carrier" - and the description to
 * "Built-in command action: navigate_to_fleet_carrier". Neither fails anything; the Built-in Commands tab
 * simply shows one English row among translated ones, and its details dialog an id instead of an explanation. The bundles are read as files, not through
 * {@link MultiLingualTextProvider}, because the provider's English fallback would hide exactly that gap.
 */
class CommandTextLocalizationTest {

    private static final Pattern TEXT_KEY = Pattern.compile("^(command|query)\\.[^.]+\\.(name|description)$");

    @BeforeAll
    static void loadRegistries() {
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
    }

    @AfterEach
    void restoreLanguage() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @Test
    void everyCommandAndQueryHasANameAndDescriptionInEveryLanguage() {
        Set<String> expected = expectedTextKeys();
        assertFalse(expected.isEmpty(), "the registries found no commands or queries");

        List<String> gaps = new ArrayList<>();
        for (Language language : Language.values()) {
            Properties bundle = guiBundle(language);
            for (String key : expected) {
                String text = bundle.getProperty(key);
                if (text == null || text.isBlank()) {
                    gaps.add(language + ": " + key);
                }
            }
        }
        assertTrue(gaps.isEmpty(), "missing names or descriptions:\n" + String.join("\n", gaps));
    }

    /**
     * Text keyed by an id no command or query has any more - left behind by a rename - is never read,
     * and the renamed action falls back to its humanized id in every language at once.
     */
    @Test
    void noTextIsLeftForAnIdThatNoLongerExists() {
        Set<String> expected = expectedTextKeys();
        List<String> orphans = new ArrayList<>();
        for (Language language : Language.values()) {
            for (String key : guiBundle(language).stringPropertyNames()) {
                if (TEXT_KEY.matcher(key).matches() && !expected.contains(key)) {
                    orphans.add(language + ": " + key);
                }
            }
        }
        assertTrue(orphans.isEmpty(), "text for unknown ids:\n" + String.join("\n", orphans));
    }

    /** Two rows reading the same in one list cannot be told apart without opening each one. */
    @Test
    void noTwoActionsShareANameInOneLanguage() {
        Set<String> expected = expectedNameKeys();
        List<String> clashes = new ArrayList<>();
        for (Language language : Language.values()) {
            Properties bundle = guiBundle(language);
            Map<String, String> keyByName = new HashMap<>();
            for (String key : expected) {
                String name = bundle.getProperty(key);
                if (name == null) {
                    continue; // reported by everyCommandAndQueryHasANameAndDescriptionInEveryLanguage
                }
                String previous = keyByName.putIfAbsent(name.strip().toLowerCase(Locale.ROOT), key);
                if (previous != null) {
                    clashes.add(language + ": \"" + name + "\" is both " + previous + " and " + key);
                }
            }
        }
        assertTrue(clashes.isEmpty(), "duplicate display names:\n" + String.join("\n", clashes));
    }

    @Test
    void namesSortByTheAlphabetOfTheAppLanguage() {
        SystemSession.getInstance().setLanguage(Language.UK);
        List<String> ukrainian = new ArrayList<>(List.of("Ж", "Ї", "Є", "Е", "І", "И"));
        ukrainian.sort(MultiLingualTextProvider.collator());
        assertEquals(List.of("Е", "Є", "Ж", "И", "І", "Ї"), ukrainian);

        SystemSession.getInstance().setLanguage(Language.FR);
        List<String> french = new ArrayList<>(List.of("Zone", "fermer", "Éteindre", "Annuler"));
        french.sort(MultiLingualTextProvider.collator());
        assertEquals(List.of("Annuler", "Éteindre", "fermer", "Zone"), french);
    }

    private static Set<String> expectedTextKeys() {
        Set<String> keys = new TreeSet<>();
        CommandRegistry.getInstance().byId().values().stream()
                .filter(command -> command.isAvailableIn(IntelActionContext.GUI))
                .forEach(command -> {
                    keys.add(CommandI18nKeys.nameKey(command.id()));
                    keys.add(CommandI18nKeys.descriptionKey(command.id()));
                });
        QueryRegistry.getInstance().byId().keySet().forEach(id -> {
            keys.add(QueryI18nKeys.nameKey(id));
            keys.add(QueryI18nKeys.descriptionKey(id));
        });
        return keys;
    }

    private static Set<String> expectedNameKeys() {
        Set<String> keys = new TreeSet<>();
        CommandRegistry.getInstance().byId().values().stream()
                .filter(command -> command.isAvailableIn(IntelActionContext.GUI))
                .forEach(command -> keys.add(CommandI18nKeys.nameKey(command.id())));
        QueryRegistry.getInstance().byId().keySet()
                .forEach(id -> keys.add(QueryI18nKeys.nameKey(id)));
        return keys;
    }

    private static Properties guiBundle(Language language) {
        String resource = language == Language.EN
                ? "/i18n/gui.properties"
                : "/i18n/gui_" + language.name().toLowerCase(Locale.ROOT) + ".properties";
        try (InputStream in = CommandTextLocalizationTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "no bundle at " + resource);
            Properties bundle = new Properties();
            bundle.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return bundle;
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + resource, e);
        }
    }
}
