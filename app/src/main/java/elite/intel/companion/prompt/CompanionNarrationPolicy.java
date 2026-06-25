package elite.intel.companion.prompt;

import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.query.QueryRegistry;

import java.util.function.Function;

/**
 * Classifies a tool-call id by how it should affect companion narration, so a {@code Thought} can decide
 * whether the commander turn's {@code speak} is worth voicing. The companion drops its {@code speak} only
 * when every game action a turn ran was a {@link Narration#SILENT_COMMAND} (see {@code IntelCommand#silentInCompanion}).
 * <p>
 * Queries are always {@link Narration#NARRATABLE} - they exist to answer the commander, so suppressing their
 * spoken result would defeat the request. System functions, macros and anything unknown are
 * {@link Narration#NEUTRAL}: they neither force nor suppress speech, so a turn made only of them keeps the
 * default (speak) behaviour.
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
