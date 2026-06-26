package elite.intel.companion.prompt;

import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.query.QueryRegistry;

import java.util.function.Function;

/**
 * Classifies a tool-call id as a game command/query versus a neutral system function, so a {@code Thought}
 * can tell whether a commander turn ran a command/query. A command/query owns its spoken outcome
 * deterministically - the handler's {@code text_to_speech_response} is voiced verbatim and a side-effect
 * stays silent - so once one runs the LLM's own {@code speak} is withheld for that turn (no re-voicing or
 * rephrasing). A turn that ran no command/query (only system functions such as memory recall, or pure
 * conversation) still speaks.
 * <p>
 * Commands classify as {@link Narration#SILENT_COMMAND} or {@link Narration#NARRATABLE} (the
 * {@code IntelCommand#silentInCompanion} hint is retained but both are now treated alike for the
 * speak-withhold decision); queries are always {@link Narration#NARRATABLE}. System functions, macros and
 * anything unknown are {@link Narration#NEUTRAL}: they are not handler outcomes, so they neither vocalize
 * here nor withhold the LLM's speak.
 */
public final class CompanionNarrationPolicy {

    public enum Narration {
        /**
         * A command flagged {@code silentInCompanion()}: the companion may stay silent after running it.
         */
        SILENT_COMMAND,
        /**
         * A non-silent command or any query: the companion should narrate.
         */
        NARRATABLE,
        /**
         * System function, macro or unknown id: no opinion either way.
         */
        NEUTRAL
    }

    private final Function<String, Narration> classifier;

    /**
     * Production: classify against the self-describing command/query registries.
     */
    public CompanionNarrationPolicy() {
        this(CompanionNarrationPolicy::classifyFromRegistries);
    }

    /**
     * Seam for tests/advanced wiring: supply the id-to-{@link Narration} classifier directly.
     */
    public CompanionNarrationPolicy(Function<String, Narration> classifier) {
        this.classifier = classifier;
    }

    public Narration classify(String actionId) {
        return classifier.apply(actionId);
    }

    private static Narration classifyFromRegistries(String actionId) {
        IntelCommand command = CommandRegistry.getInstance().byId().get(actionId);
        if (command != null) {
            return command.silentInCompanion() ? Narration.SILENT_COMMAND : Narration.NARRATABLE;
        }
        if (QueryRegistry.getInstance().byId().containsKey(actionId)) {
            return Narration.NARRATABLE;
        }
        return Narration.NEUTRAL;
    }
}
