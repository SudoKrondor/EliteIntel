package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.i18n.AiActionAliasTextProvider;
import elite.intel.ai.brain.i18n.AiActionCues;
import elite.intel.ai.brain.i18n.AliasPhrase;
import elite.intel.ai.brain.vega.model.llm.LlmToolDefinition;
import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the cue gate on {@code clear_fleet_carrier_route}. A support bundle showed "float route back to the
 * fleet carrier" (a mis-heard "plot") offered only the three carrier-route tools, and the model cleared the
 * commander's carrier route. The destructive tool must now only reach the model when a wiping verb was said.
 */
class ActionCueGateTest {

    private static final String CLEAR = "clear_fleet_carrier_route";
    private static final String NAVIGATE = "navigate_to_fleet_carrier";

    /**
     * Every authored way of clearing the route must still carry a cue, or the gate would hide the tool.
     */
    @ParameterizedTest(name = "{0}: every clear-route alias carries a cue")
    @EnumSource(Language.class)
    void everyClearAliasIsAdmitted(Language language) {
        List<String> stems = AiActionCues.stems(language, CLEAR);
        assertFalse(stems.isEmpty(), language + " has no cues for " + CLEAR);
        for (String utterance : spokenAliases(language, CLEAR)) {
            assertTrue(AiActionCues.admits(stems, utterance), () -> language + ": \"" + utterance + "\" is gated out");
        }
    }

    /**
     * Asking to fly to the carrier must never put the route-wiping tool in front of the model.
     */
    @ParameterizedTest(name = "{0}: navigating to the carrier never offers the clear")
    @EnumSource(Language.class)
    void navigateAliasesNeverAdmitTheClear(Language language) {
        List<String> stems = AiActionCues.stems(language, CLEAR);
        for (String utterance : spokenAliases(language, NAVIGATE)) {
            assertFalse(AiActionCues.admits(stems, utterance), () -> language + ": \"" + utterance + "\" admits the clear");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"float route back to the fleet carrier", "plot route back to the fleet carrier",
            "fleet carrier route", "how many jumps left on the carrier route"})
    void supportBundleUtterancesWithholdTheClear(String utterance) {
        List<GameToolCandidates.Candidate> admitted = GameToolCandidates.admitted(candidates(), utterance);
        assertEquals(List.of("query_carrier_voyage"), admitted.stream().map(GameToolCandidates.Candidate::id).toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"cancel the carrier route", "clear fleet carrier route", "stop carrier route"})
    void aWipingVerbAdmitsTheClear(String utterance) {
        assertEquals(2, GameToolCandidates.admitted(candidates(), utterance).size());
    }

    /**
     * An inflection still counts: the stem only has to open the word.
     */
    @Test
    void stemsMatchInflectedWords() {
        List<String> stems = AiActionCues.stems(Language.RU, CLEAR);
        assertTrue(AiActionCues.admits(stems, "сбрось маршрут авианосца"));
        assertTrue(AiActionCues.admits(stems, "отменить маршрут авианосца"));
        assertTrue(AiActionCues.admits(stems, "cancel carrier route"), "English stems apply in every language");
    }

    @Test
    void anUngatedToolAdmitsEverything() {
        assertTrue(AiActionCues.stems(Language.EN, NAVIGATE).isEmpty());
        assertTrue(AiActionCues.admits(List.of(), ""));
    }

    @Test
    void aBlankInputNeverAdmitsAGatedTool() {
        assertTrue(GameToolCandidates.admitted(candidates(), " ").stream().noneMatch(c -> c.id().equals(CLEAR)));
    }

    private static List<GameToolCandidates.Candidate> candidates() {
        return List.of(
                new GameToolCandidates.Candidate("query_carrier_voyage", "carrier route",
                        new LlmToolDefinition("query_carrier_voyage", "", "carrier route", List.of())),
                new GameToolCandidates.Candidate(CLEAR, "clear carrier route",
                        new LlmToolDefinition(CLEAR, "", "clear carrier route", List.of()),
                        AiActionCues.stems(Language.EN, CLEAR)));
    }

    private static List<String> spokenAliases(Language language, String actionId) {
        return Arrays.stream(AiActionAliasTextProvider.getText(language, actionId).split(","))
                .map(String::strip)
                .filter(phrase -> !phrase.isEmpty())
                .map(phrase -> AliasPhrase.parse(phrase).spokenText())
                .toList();
    }
}
