package elite.intel.companion.prompt;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.command.builtin.IgnoreNonsensicalInputCommand;
import elite.intel.ai.brain.actions.customcommand.CustomCommandDefinition;
import elite.intel.ai.brain.actions.handlers.query.GeneralConversationQueryCommand;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.i18n.Language;
import elite.intel.session.Status;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies candidate building (step 1): category gating (EVENT gets only QUERY), {@code isVisibleForLLM}
 * filtering, legacy-fallback-id exclusion, and the localized-phrase hard rule (phrases embedded into the
 * description and kept in the neutral field). Uses fake actions so it needs no registry scan, game status,
 * or DB; the language is EN, under which fake ids have no alias bundle entry.
 */
class GameToolCandidatesTest {

    /** Fake action that ignores status and reports a fixed visibility. */
    private static IntelAction action(String id, boolean visible) {
        return new IntelAction() {
            @Override public String id() { return id; }
            @Override public boolean isVisibleForLLM(Status status) { return visible; }
            @Override public List<ActionParameterSpec> parameters() { return List.of(); }
            @Override public JsonObject handle(String a, JsonObject p, String t) { return null; }
        };
    }

    private static CustomCommandDefinition macro(String actionKey, String phrases) {
        return new CustomCommandDefinition("uuid-" + actionKey, actionKey, actionKey, "", phrases, List.of(), List.of());
    }

    private static GameToolCandidates candidates(Map<String, IntelAction> commands,
                                                 Map<String, IntelAction> queries,
                                                 List<CustomCommandDefinition> macros) {
        return new GameToolCandidates(commands, queries, macros, /*status*/ null, Language.EN);
    }

    private static List<String> ids(List<GameToolCandidates.Candidate> list) {
        return list.stream().map(GameToolCandidates.Candidate::id).toList();
    }

    @Test
    void commanderGetsAllCategoriesEventGetsOnlyQuery() {
        GameToolCandidates c = candidates(
                Map.of("nav", action("nav", true)),
                Map.of("scan", action("scan", true)),
                List.of(macro("my_macro", "do the thing")));

        assertEquals(List.of("nav", "scan", "my_macro"),
                ids(c.collect(EnumSet.allOf(IntelActionCategory.class))));

        List<String> eventTools = ids(c.collect(EnumSet.of(IntelActionCategory.QUERY)));
        assertEquals(List.of("scan"), eventTools, "EVENT thought must receive only QUERY tools");
    }

    @Test
    void hiddenActionsAreExcluded() {
        GameToolCandidates c = candidates(
                Map.of("visible", action("visible", true), "hidden", action("hidden", false)),
                Map.of(), List.of());

        assertEquals(List.of("visible"), ids(c.collect(EnumSet.of(IntelActionCategory.ACTION))));
    }

    @Test
    void legacyFallbackIdsAreNeverOffered() {
        GameToolCandidates c = candidates(
                Map.of(IgnoreNonsensicalInputCommand.ID, action(IgnoreNonsensicalInputCommand.ID, true)),
                Map.of(GeneralConversationQueryCommand.ID, action(GeneralConversationQueryCommand.ID, true),
                        "real_query", action("real_query", true)),
                List.of());

        Set<String> all = Set.copyOf(ids(c.collect(EnumSet.allOf(IntelActionCategory.class))));
        assertEquals(Set.of("real_query"), all);
    }

    @Test
    void macroEmbedsPhrasesIntoDescriptionAndKeepsNeutralField() {
        GameToolCandidates c = candidates(Map.of(), Map.of(), List.of(macro("dock_routine", "dock us, take us in")));

        GameToolCandidates.Candidate macro = c.collect(EnumSet.of(IntelActionCategory.MACRO)).get(0);
        assertTrue(macro.tool().description().contains("dock us, take us in"),
                "localized phrases must be embedded in the description");
        assertEquals("dock us, take us in", macro.tool().localizedTrainingPhrases());
        assertEquals("dock us, take us in", macro.phraseKey());
    }

    @Test
    void actionWithoutLocalizedPhraseStillIncludedButPlain() {
        // EN bundle has no alias for this made-up id: included, no example phrases, phraseKey falls back to id.
        GameToolCandidates c = candidates(Map.of("made_up_action", action("made_up_action", true)), Map.of(), List.of());

        GameToolCandidates.Candidate only = c.collect(EnumSet.of(IntelActionCategory.ACTION)).get(0);
        assertEquals("made_up_action", only.phraseKey());
        assertTrue(only.tool().localizedTrainingPhrases().isEmpty());
        assertFalse(only.tool().description().contains("Example commander phrases"));
    }
}
