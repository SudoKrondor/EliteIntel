package elite.intel.companion.prompt;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.companion.confirm.CommandFlagDangerousActionPolicy;
import elite.intel.companion.confirm.DangerousActionPolicy;
import elite.intel.companion.model.GameStateSnapshot;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.llm.LlmToolInvocation;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The companion's reflex gate (§2.5/§5.1): decides, before any thought is born, whether a commander utterance
 * is a pure reflex - an input that matches a training phrase verbatim and resolves to exactly one safe,
 * parameterless action. Such an input is executed directly (no LLM, a {@code ReflexThought}); everything else
 * falls through to the full {@link elite.intel.companion.mind.CommanderThought}.
 * <p>
 * Deliberately strict, so a reflex never misfires. It requires all of: a verbatim phrase match (not word
 * overlap), exactly one matching action, no parameters (the LLM is needed to extract arguments), the action
 * currently visible, and not dangerous (a dangerous command must keep its confirmation flow). It covers
 * parameterless COMMANDS and QUERIES (a verbatim query alias like "squadron carrier route" resolves it directly;
 * {@link elite.intel.companion.mind.ReflexThought} voices a query reflex from the query's own data), never macros.
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

    private final Function<GameStateSnapshot, List<CommandPhrase>> commandSource;
    private final DangerousActionPolicy dangerousActionPolicy;

    /** Production: commands from the live registries, visibility from the turn snapshot, danger from the command. */
    public ReflexResolver() {
        this(new CommandFlagDangerousActionPolicy());
    }

    /** Production reusing a shared danger policy (e.g. the dispatcher's own instance). */
    public ReflexResolver(DangerousActionPolicy dangerousActionPolicy) {
        // Lazy per-resolve: registries/language are read at resolve time, while visibility comes from the immutable
        // state supplied by the owning commander turn. Construction itself touches no game-state singleton.
        this(ReflexResolver::collectVisibleCommands, dangerousActionPolicy);
    }

    /** Test/advanced seam: supply the eligible commands and the danger policy directly. */
    public ReflexResolver(Supplier<List<CommandPhrase>> commandSource, DangerousActionPolicy dangerousActionPolicy) {
        this(snapshot -> commandSource.get(), dangerousActionPolicy);
    }

    /** Test seam: derive eligible commands from the exact commander-turn visibility snapshot. */
    public ReflexResolver(Function<GameStateSnapshot, List<CommandPhrase>> commandSource,
                          DangerousActionPolicy dangerousActionPolicy) {
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
        return resolve(input, GameStateSnapshot.capture());
    }

    /**
     * Resolves an exact reflex using the immutable visibility state captured for the owning commander turn.
     */
    public Optional<String> resolve(String input, GameStateSnapshot gameStateSnapshot) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String needle = canonicalizeForMatch(input);
        List<CommandPhrase> matches = commandSource.apply(Objects.requireNonNull(gameStateSnapshot)).stream()
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

    /**
     * Whether any phrase in the group equals the input verbatim (case-insensitive, ignoring trailing punctuation).
     */
    private static boolean matchesVerbatim(String phraseGroup, String needle) {
        for (String phrase : AiActionLocalizations.splitPhraseGroup(phraseGroup)) {
            if (canonicalizeForMatch(phrase).equals(needle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lower-cases, trims, and drops trailing sentence punctuation so a spoken question ("... carrier?") matches a
     * plain alias ("... carrier"). Only ever loosens matching (aliases never end in punctuation), so it can add a
     * match but never remove one - the "exactly one" guard in {@link #resolve} still protects against ambiguity.
     */
    private static String canonicalizeForMatch(String s) {
        String lower = s.trim().toLowerCase(Locale.ROOT);
        int end = lower.length();
        while (end > 0 && isTrailingPunctuation(lower.charAt(end - 1))) {
            end--;
        }
        return lower.substring(0, end).trim();
    }

    private static boolean isTrailingPunctuation(char c) {
        return c == '?' || c == '!' || c == '.' || c == ',' || c == ';' || c == ':'
                || c == '？' || c == '！' || c == '。' || c == '，';
    }

    /** The per-command danger flag via the shared owner (args are ignored; the flag is per-command). */
    private boolean isDangerous(String commandId) {
        return dangerousActionPolicy.isDangerous(
                new LlmToolInvocation(UUID.randomUUID().toString(), commandId, new JsonObject()));
    }

    /**
     * Commands and queries from the live registries, gated by the turn snapshot and projected onto the reflex
     * matching surface.
     */
    private static List<CommandPhrase> collectVisibleCommands(GameStateSnapshot gameStateSnapshot) {
        return new GameToolCandidates(gameStateSnapshot)
                .collect(Set.of(IntelActionCategory.ACTION, IntelActionCategory.QUERY)).stream()
                .map(candidate -> new CommandPhrase(
                        candidate.id(), candidate.localizedAliasGroup(), candidate.tool().parameters().isEmpty()))
                .toList();
    }
}
