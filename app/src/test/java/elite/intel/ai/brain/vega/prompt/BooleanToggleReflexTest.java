package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.ai.brain.i18n.AliasPhrase;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A command that only takes booleans must be switchable both ways without the local model, in every locale.
 *
 * <p>{@link ReflexResolver} dispatches only when the matched phrase's own <em>literal</em> arguments cover
 * every parameter the action declares, and {@link AliasPhrase} counts {@code {state:true/false}} as variable
 * - a choice the model still has to read out of the utterance. A phrase carrying no argument block at all
 * supplies nothing either. So an alias group written as {@code "turn it on, turn it off {state:true/false}"}
 * has no reflex-capable phrase whatsoever, and every utterance reaches the model.
 *
 * <p>That is not hypothetical: "turn off radio" answered "I don't have a function for that, you gotta use
 * the comms panel directly" (2026-08-21) and only routed once the commander named "radio chatter" himself.
 * The audit that followed found the same shape on ten commands across all nine locales - the trailing
 * {@code {state:true/false}} binds to the LAST phrase on the line only, so the rest silently had no
 * parameters and never could dispatch.
 *
 * <p>The fix is to pin the value per phrase ({@code turn radio off {state:false}}). This test pins the
 * invariant instead of the wording: a boolean-only command needs at least one phrase that turns it on and
 * one that turns it off. Neutral phrases ("toggle radio") may stay variable - they name no state and must
 * not execute on a guess. See {@link RadioReflexTest} for the concrete utterances.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BooleanToggleReflexTest {

    @BeforeAll
    void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
    }

    @ParameterizedTest(name = "{0} can switch every boolean command both ways")
    @EnumSource(Language.class)
    void everyBooleanCommandHasAnOnPhraseAndAnOffPhrase(Language language) {
        SystemSession.getInstance().setLanguage(language);
        try {
            List<String> failures = new ArrayList<>();
            for (IntelCommand command : CommandRegistry.getInstance().byId().values()) {
                if (!isBooleanOnly(command)) {
                    continue;
                }
                Set<String> pinned = new LinkedHashSet<>();
                for (String phrase : AiActionLocalizations.phrasesForAction(command.id())) {
                    AliasPhrase alias = AliasPhrase.parse(phrase);
                    if (alias.hasVariableArgument()
                            || !alias.literalArguments().keySet().containsAll(parameterNames(command))) {
                        continue; // still needs the model to read the value out - not a reflex
                    }
                    pinned.addAll(alias.literalArguments().values());
                }
                if (!pinned.contains("true")) {
                    failures.add(command.id() + " has no phrase that turns it ON");
                }
                if (!pinned.contains("false")) {
                    failures.add(command.id() + " has no phrase that turns it OFF");
                }
            }
            assertTrue(failures.isEmpty(),
                    () -> language + ": " + failures.size() + " boolean command(s) reach the model for a "
                            + "value the alias could pin itself:\n  " + String.join("\n  ", failures));
        } finally {
            SystemSession.getInstance().setLanguage(Language.EN);
        }
    }

    /**
     * True when the command declares at least one parameter and every one of them is a boolean.
     */
    private static boolean isBooleanOnly(IntelCommand command) {
        List<ActionParameterSpec> parameters = command.parameters();
        return !parameters.isEmpty()
                && parameters.stream().allMatch(parameter -> "boolean".equals(parameter.getType()));
    }

    private static Set<String> parameterNames(IntelCommand command) {
        Set<String> names = new LinkedHashSet<>();
        command.parameters().forEach(parameter -> names.add(parameter.getName()));
        return names;
    }
}
