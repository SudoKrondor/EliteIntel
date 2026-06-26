package elite.intel.companion.prompt;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.companion.confirm.CommandFlagDangerousActionPolicy;
import elite.intel.companion.confirm.DangerousActionPolicy;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.llm.LlmToolInvocation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The companion's reflex gate (§2.5/§5.1): decides, before any thought is born, whether a commander utterance
 * is a pure reflex - an input that matches a training phrase verbatim and resolves to exactly one safe,
 * parameterless command. Such an input is executed directly (no LLM, a {@code ReflexThought}); everything else
 * falls through to the full {@link elite.intel.companion.mind.CommanderThought}.
 * <p>
 * Deliberately strict, so a reflex never misfires. It requires all of: a verbatim phrase match (not word
 * overlap), exactly one matching command, no parameters (the LLM is needed to extract arguments), the command
 * currently visible, and not dangerous (a dangerous command must keep its confirmation flow). A reflex covers
 * commands only - never queries or macros.
 * <p>
 * It introduces no new classification, reusing the existing owners: {@link GameToolCandidates} for the visible
 * commands and their localized phrases/parameters, {@link AiActionLocalizations#splitPhraseGroup} for phrase
 * splitting, and the {@link DangerousActionPolicy} for the danger flag.
 */
public final class ReflexResolver {

    /**
     * One reflex-eligible command's matching surface: its id, its localized training-phrase group and whether
     * it is parameterless (a reflex requires no parameters - the LLM extracts arguments). The {@code danger}
     * flag is sourced separately, from the {@link DangerousActionPolicy}.
     */
    public record CommandPhrase(String id, String phraseGroup, boolean parameterless) {}

    private final Supplier<List<CommandPhrase>> commandSource;
    private final DangerousActionPolicy dangerousActionPolicy;

    /** Production: visible commands from the live registries, danger via the command's own flag. */
    public ReflexResolver() {
        this(new CommandFlagDangerousActionPolicy());
    }

    /** Production reusing a shared danger policy (e.g. the dispatcher's own instance). */
    public ReflexResolver(DangerousActionPolicy dangerousActionPolicy) {
        // Lazy per-resolve: the live registries/status/language are read at resolve time, not at construction,
        // so merely constructing the resolver (e.g. inside the dispatcher) touches no game-state singletons.
        this(ReflexResolver::collectVisibleCommands, dangerousActionPolicy);
    }

    /** Test/advanced seam: supply the eligible commands and the danger policy directly. */
    public ReflexResolver(Supplier<List<CommandPhrase>> commandSource, DangerousActionPolicy dangerousActionPolicy) {
        this.commandSource = commandSource;
        this.dangerousActionPolicy = dangerousActionPolicy;
    }

    /**
     * The id of the single safe, parameterless command whose training phrase the input matches verbatim, or
     * empty when the input is not a reflex (no match, an ambiguous tie, parameterized, dangerous, or not a
     * command) - in which case the input takes the normal LLM path.
     */
    public Optional<String> resolve(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String needle = input.trim().toLowerCase(Locale.ROOT);
        List<CommandPhrase> matches = commandSource.get().stream()
                .filter(command -> matchesVerbatim(command.phraseGroup(), needle))
                .toList();
        if (matches.size() != 1) {
            return Optional.empty(); // no command, or an ambiguous tie - let the LLM decide
        }
        CommandPhrase only = matches.get(0);
        if (!only.parameterless() || isDangerous(only.id())) {
            return Optional.empty(); // parameters need the LLM; dangerous needs the confirmation flow
        }
        return Optional.of(only.id());
    }

    /** Whether any phrase in the group equals the input verbatim (case-insensitive). */
    private static boolean matchesVerbatim(String phraseGroup, String needle) {
        for (String phrase : AiActionLocalizations.splitPhraseGroup(phraseGroup)) {
            if (phrase.trim().toLowerCase(Locale.ROOT).equals(needle)) {
                return true;
            }
        }
        return false;
    }

    /** The per-command danger flag via the shared owner (args are ignored; the flag is per-command). */
    private boolean isDangerous(String commandId) {
        return dangerousActionPolicy.isDangerous(
                new LlmToolInvocation(UUID.randomUUID().toString(), commandId, new JsonObject()));
    }

    /** Visible commands (only) from the live registries, projected onto the reflex matching surface. */
    private static List<CommandPhrase> collectVisibleCommands() {
        return new GameToolCandidates().collect(Set.of(IntelActionCategory.ACTION)).stream()
                .map(candidate -> new CommandPhrase(
                        candidate.id(), candidate.phraseKey(), candidate.tool().parameters().isEmpty()))
                .toList();
    }
}
