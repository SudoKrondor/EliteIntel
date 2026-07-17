package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.IntelActionContext;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.i18n.AiActionAliasTextProvider;
import elite.intel.ai.brain.i18n.TrailingStringAliasMatcher;
import elite.intel.companion.CompanionAddressing;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.memory.CompanionMemoryPolicy;
import elite.intel.companion.model.memory.MemoryRecord;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.List;

/** Saves commander-provided text verbatim in explicit session memory. */
@RegisterCommand
public final class RememberCommand implements IntelCommand {

    private static final Logger log = LogManager.getLogger(RememberCommand.class);

    public static final String ID = "remember";
    public static final String PARAM_TEXT = "text";

    private static final List<ActionParameterSpec> PARAMETERS = List.of(new ActionParameterSpec(
            PARAM_TEXT,
            "string",
            true,
            "The exact content the commander explicitly asked the companion to remember.",
            List.of("our docking code is Sierra Nine Four", "the rendezvous is Hutton Orbital"),
            "Extract only the content to remember, verbatim in the commander's language. "
                    + "Do not include the remember/save/note trigger words and do not rewrite or summarize it."));

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Save text explicitly dictated by the commander in session memory.";
    }

    /** This stateful command belongs exclusively to a direct Companion Commander turn. */
    @Override
    public boolean isAvailableIn(IntelActionContext context) {
        return context == IntelActionContext.COMPANION_COMMANDER;
    }

    /** Selects the same canonical text that the model received for deterministic parsing. */
    @Override
    public String executionInput(String originalInput, String matchInput) {
        return matchInput;
    }

    /** Companion memory is available independently of the current game location. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    /** Parses the canonical utterance itself so model wording can never become the saved fact. */
    @Override
    public String execute(JsonObject params, String commanderInput) {
        JsonElement rawText = params == null ? null : params.get(PARAM_TEXT);
        if (!(rawText instanceof JsonPrimitive primitive) || !primitive.isString()) {
            throw new IllegalArgumentException("Remember text must be a JSON string");
        }

        String localizedAliases = AiActionAliasTextProvider.getText(
                SystemSession.getInstance().getLanguage(), ID);
        String commandText = CompanionAddressing.stripLeadingName(commanderInput);
        TrailingStringAliasMatcher.Match match = TrailingStringAliasMatcher.findBestMatch(
                        localizedAliases, PARAMETERS, commandText)
                .filter(candidate -> PARAM_TEXT.equals(candidate.parameterName()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Commander input does not match a localized remember phrase"));

        String text = match.value();
        if (text.isBlank()) {
            throw new IllegalArgumentException("Remember text must be a non-blank string");
        }
        if (text.length() > CompanionMemoryPolicy.savedTextMaxChars()) {
            throw new IllegalArgumentException("Remember text exceeds the saved-text limit");
        }
        if (!text.equals(primitive.getAsString())) {
            log.debug("Remember argument differed from the deterministic input suffix; using the input suffix");
        }
        CompanionRuntime.memory().write(MemoryRecord.savedText(Instant.now(), text));
        return null;
    }
}
