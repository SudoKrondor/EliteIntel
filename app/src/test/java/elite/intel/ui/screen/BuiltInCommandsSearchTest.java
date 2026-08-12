package elite.intel.ui.screen;

import elite.intel.session.PlayerSituation;
import elite.intel.ui.screen.BuiltInCommandsTabPanel.ActionRow;
import elite.intel.ui.screen.BuiltInCommandsTabPanel.Scope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The search box is a plain text filter, and it is the whole point that it behaves like one: the previous
 * field ran the semantic reducer, so typing "find" could leave "close" highlighted with no word in common and
 * nothing on screen to explain why. What is typed is what is looked for.
 */
class BuiltInCommandsSearchTest {

    private static final List<ActionRow> ROWS = List.of(
            ActionRow.of("find_interstellar_factors", "Find Interstellar Factors", "find interstellar factors"),
            ActionRow.of("close_all_panels", "Close Panels", "close panels, close all panels"),
            ActionRow.of("deploy_hardpoints", "Deploy Hardpoints", "hardpoints, weapons out"),
            ActionRow.of("query_biome_analysis", "Biome Analysis", "analyse the biome"));

    @Test
    void aSearchMatchesTheDisplayName() {
        assertEquals(List.of("Find Interstellar Factors"),
                names(BuiltInCommandsTabPanel.matching(ROWS, "interstellar")));
    }

    @Test
    void aSearchMatchesTheActionKey() {
        // The id is what the details dialog and the logs call it, so it has to be findable.
        assertEquals(List.of("Biome Analysis"), names(BuiltInCommandsTabPanel.matching(ROWS, "query_biome")));
    }

    @Test
    void aSearchMatchesTheSpokenPhrases() {
        assertEquals(List.of("Deploy Hardpoints"), names(BuiltInCommandsTabPanel.matching(ROWS, "weapons out")));
    }

    @Test
    void aSearchIsCaseInsensitiveAndIgnoresSurroundingSpace() {
        assertEquals(List.of("Close Panels"), names(BuiltInCommandsTabPanel.matching(ROWS, "  CLOSE  ")));
    }

    /**
     * The complaint this replaced: "find" returned commands with no "find" in them, including "close".
     */
    @Test
    void aSearchNeverMatchesSomethingWithoutTheTypedText() {
        List<String> hits = names(BuiltInCommandsTabPanel.matching(ROWS, "find"));

        assertEquals(List.of("Find Interstellar Factors"), hits);
        assertFalse(hits.contains("Close Panels"));
    }

    @Test
    void aSearchWithNoHitsShowsNothingRatherThanAGuess() {
        assertTrue(BuiltInCommandsTabPanel.matching(ROWS, "test").isEmpty());
    }

    @Test
    void anEmptySearchKeepsEveryRow() {
        assertSame(ROWS, BuiltInCommandsTabPanel.matching(ROWS, ""));
        assertSame(ROWS, BuiltInCommandsTabPanel.matching(ROWS, "   "));
        assertSame(ROWS, BuiltInCommandsTabPanel.matching(ROWS, null));
    }

    // ── scope ────────────────────────────────────────────────────────────────

    @Test
    void allIsAScopeAndNotAPlace() {
        assertTrue(Scope.ALL.isAll());
        assertFalse(Scope.of(PlayerSituation.IN_SHIP_DOCKED).isAll());
        // ALL must never be mistaken for the "nothing known yet" situation, which clears the list.
        assertFalse(Scope.ALL.isSituation(PlayerSituation.UNKNOWN));
        assertTrue(Scope.of(PlayerSituation.UNKNOWN).isSituation(PlayerSituation.UNKNOWN));
    }

    @Test
    void anUndeterminedSituationShowsEverythingRatherThanNothing() {
        // The game not running is the common case for reading this tab, and an empty list reads as broken.
        assertTrue(BuiltInCommandsTabPanel.scopeFor(PlayerSituation.UNKNOWN).isAll());
        assertTrue(BuiltInCommandsTabPanel.scopeFor(null).isAll());
        assertEquals(Scope.of(PlayerSituation.IN_SRV), BuiltInCommandsTabPanel.scopeFor(PlayerSituation.IN_SRV));
    }

    @Test
    void thePickerOffersAllFirstAndNeverOffersUnknown() {
        List<Scope> choices = List.of(BuiltInCommandsTabPanel.scopeChoices());

        assertEquals(Scope.ALL, choices.getFirst());
        assertFalse(choices.stream().anyMatch(scope -> scope.isSituation(PlayerSituation.UNKNOWN)),
                "UNKNOWN is the game saying it cannot tell; ALL is what that means here");
        assertEquals(PlayerSituation.values().length, choices.size(),
                "every situation except UNKNOWN, plus ALL");
    }

    private static List<String> names(List<ActionRow> rows) {
        return rows.stream().map(ActionRow::name).toList();
    }
}
