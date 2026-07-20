package elite.intel.ai.brain.vega.prompt;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.ai.brain.i18n.AliasPhrase;
import elite.intel.ai.brain.vega.confirm.CommandFlagDangerousActionPolicy;
import elite.intel.ai.brain.vega.confirm.DangerousActionPolicy;
import elite.intel.ai.brain.vega.mind.CommanderThought;
import elite.intel.ai.brain.vega.model.GameStateSnapshot;
import elite.intel.ai.brain.vega.model.IntelActionCategory;
import elite.intel.ai.brain.vega.model.llm.LlmToolDefinition;
import elite.intel.ai.brain.vega.model.llm.LlmToolInvocation;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The companion's reflex gate (§2.5/§5.1): decides, before any thought is born, whether a commander utterance
 * is a pure reflex - an input that matches a training phrase verbatim and resolves to exactly one safe action
 * whose arguments are already known. Such an input is executed directly (no LLM, a {@code ReflexThought});
 * everything else falls through to the full {@link CommanderThought}.
 * <p>
 * Deliberately strict, so a reflex never misfires. It requires all of: a verbatim phrase match (not word
 * overlap), exactly one matching action, every required argument supplied without inference, the action
 * currently visible, and not dangerous (a dangerous command must keep its confirmation flow). It covers
 * COMMANDS and QUERIES (a verbatim query alias like "squadron carrier route" resolves it directly;
 * {@link elite.intel.vega.mind.ReflexThought} voices a query reflex from the query's own data), never macros.
 * <p>
 * An argument counts as known without inference when the alias itself pins it down - "target fsd {key:fsd}"
 * always means {@code key=fsd}, so nothing is left to extract and the phrase resolves here rather than asking a
 * small local model to tell a subsystem command from a same-named query. An alias whose value stands in for the
 * commander's own wording ("increase speed by {key:X}") still needs the LLM. See {@link AliasPhrase}.
 * <p>
 * It introduces no new classification, reusing the existing owners: {@link GameToolCandidates} for the visible
 * commands and their localized phrases/parameters, {@link AiActionLocalizations#splitPhraseGroup} for phrase
 * splitting, and the {@link DangerousActionPolicy} for the danger flag.
 */
public final class ReflexResolver {

    /**
     * One reflex-eligible command's matching surface: its id, its localized training-phrase group and the names
     * of every parameter it declares. A reflex has to supply all of them without the LLM, either because there
     * are none or because the matched alias pins them all down literally. Optional parameters count too: an
     * alias that leaves one unset is a phrase the commander may still be qualifying, so it keeps the LLM path.
     * The {@code danger} flag is sourced separately, from the {@link DangerousActionPolicy}.
     */
    public record CommandPhrase(String id, String phraseGroup, Set<String> parameters) {

        public CommandPhrase {
            parameters = Set.copyOf(parameters);
        }

        /**
         * A command whose arguments the alias never supplies: parameterless, or parameterized via the LLM.
         */
        public CommandPhrase(String id, String phraseGroup, boolean parameterless) {
            this(id, phraseGroup, parameterless ? Set.of() : Set.of(UNRESOLVABLE_PARAMETER));
        }
    }

    /**
     * Stands for "this command needs an argument the alias does not name", so a phrase group authored without
     * literal values can never satisfy {@link #resolve} however it is matched.
     */
    private static final String UNRESOLVABLE_PARAMETER = "\0";

    /**
     * One resolved reflex: the action to run and the arguments its alias already pinned down.
     */
    public record Reflex(String actionId, Map<String, String> arguments) {

        public Reflex {
            arguments = Map.copyOf(arguments);
        }

        /**
         * Renders the alias-supplied arguments as the invocation payload the handler expects.
         */
        public JsonObject argumentsJson() {
            JsonObject json = new JsonObject();
            arguments.forEach(json::addProperty);
            return json;
        }
    }

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
     * The single safe command whose training phrase the input matches verbatim, with its arguments, or
     * empty when the input is not a reflex (no match, an ambiguous tie, an argument the alias does not supply,
     * dangerous, or not a command) - in which case the input takes the normal LLM path.
     */
    public Optional<Reflex> resolve(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        return resolve(input, GameStateSnapshot.capture());
    }

    /**
     * Resolves an exact reflex using the immutable visibility state captured for the owning commander turn.
     */
    public Optional<Reflex> resolve(String input, GameStateSnapshot gameStateSnapshot) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String needle = canonicalizeForMatch(input);
        List<Reflex> matches = commandSource.apply(Objects.requireNonNull(gameStateSnapshot)).stream()
                .flatMap(command -> matchVerbatim(command, needle).stream())
                .toList();
        if (matches.size() != 1) {
            return Optional.empty(); // no command, or an ambiguous tie - let the LLM decide
        }
        Reflex only = matches.get(0);
        if (isDangerous(only.actionId())) {
            return Optional.empty(); // dangerous needs the confirmation flow
        }
        return Optional.of(only);
    }

    /**
     * The reflex for the one phrase in this command's group that the input matches verbatim (case-insensitive,
     * ignoring trailing punctuation), or empty when none matches or the matched phrase leaves an argument for
     * the LLM to extract. A phrase qualifies only when its own literal values cover every parameter the action
     * declares: "ziel fsd {key:fsd}" carries its own {@code key}, "erhöhe geschwindigkeit um {key:X}" does not.
     */
    private static Optional<Reflex> matchVerbatim(CommandPhrase command, String needle) {
        for (String phrase : AiActionLocalizations.splitPhraseGroup(command.phraseGroup())) {
            AliasPhrase alias = AliasPhrase.parse(phrase);
            if (!canonicalizeForMatch(alias.spokenText()).equals(needle)) {
                continue;
            }
            if (alias.hasVariableArgument()
                    || !alias.literalArguments().keySet().containsAll(command.parameters())) {
                return Optional.empty(); // the alias cannot supply every argument - the LLM must extract them
            }
            return Optional.of(new Reflex(command.id(), alias.literalArguments()));
        }
        return Optional.empty();
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
                        candidate.id(), candidate.localizedAliasGroup(), parameterNames(candidate.tool())))
                .toList();
    }

    /**
     * Every parameter an action declares - the alias has to account for all of them to reflex.
     */
    private static Set<String> parameterNames(LlmToolDefinition tool) {
        return tool.parameters().stream()
                .map(ActionParameterSpec::getName)
                .collect(Collectors.toUnmodifiableSet());
    }
}
