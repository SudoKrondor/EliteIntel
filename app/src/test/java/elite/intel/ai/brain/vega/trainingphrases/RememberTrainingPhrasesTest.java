package elite.intel.ai.brain.vega.trainingphrases;

import elite.intel.ai.brain.actions.handlers.commands.builtin.RememberCommand;
import elite.intel.ai.brain.i18n.AiActionAliasTextProvider;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.ai.brain.i18n.TrailingStringAliasMatcher;
import elite.intel.i18n.Language;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RememberTrainingPhrasesTest {

    private static final String PLACEHOLDER = "{text:X}";
    private static final String VALUE = "Sierra Nine Four / 42";
    private static final RememberCommand COMMAND = new RememberCommand();

    @ParameterizedTest
    @EnumSource(Language.class)
    void everyLocalizedAliasExtractsTheTrailingTextVerbatim(Language language) {
        String localizedAliases = AiActionAliasTextProvider.getText(language, RememberCommand.ID);
        List<String> aliases = AiActionLocalizations.splitPhraseGroup(localizedAliases);
        assertTrue(aliases.size() > 1, language + " must provide multiple remember aliases");

        for (String alias : aliases) {
            assertTrue(alias.endsWith(PLACEHOLDER), alias + " must end with " + PLACEHOLDER);
            String input = alias.substring(0, alias.length() - PLACEHOLDER.length()) + VALUE;

            TrailingStringAliasMatcher.Match match = TrailingStringAliasMatcher.findBestMatch(
                            localizedAliases, COMMAND.parameters(), input)
                    .orElseThrow(() -> new AssertionError("No match for " + language + ": " + input));

            assertEquals(RememberCommand.PARAM_TEXT, match.parameterName(), input);
            assertEquals(VALUE, match.value(), input);
            assertTrue(match.prefixWordCount() > 0, input);
        }
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void localizedAliasesDoNotMatchUnrelatedInput(Language language) {
        String localizedAliases = AiActionAliasTextProvider.getText(language, RememberCommand.ID);

        assertFalse(TrailingStringAliasMatcher.findBestMatch(
                localizedAliases, COMMAND.parameters(), "unrelated commander input").isPresent());
    }
}
