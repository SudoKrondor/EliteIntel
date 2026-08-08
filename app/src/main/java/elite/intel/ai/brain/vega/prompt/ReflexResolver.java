package elite.intel.ai.brain.vega.prompt;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandDefinition;
import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandRegistry;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.ai.brain.i18n.AliasPhrase;
import elite.intel.ai.brain.i18n.AliasVocabulary;
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
 * is a pure reflex - an input that matches a training phrase, word for word or as a damaged transcript of one,
 * and resolves to exactly one safe action whose arguments are already known. Such an input is executed directly (no LLM, a {@code ReflexThought});
 * everything else falls through to the full {@link CommanderThought}.
 * <p>
 * Deliberately strict, so a reflex never misfires. It requires all of: a full-phrase match (not word
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
 * <p>
 * A second pass ({@link FuzzyAliasMatch}) runs only when nothing matched word for word, and treats the input
 * as a damaged transcript of one alias - "request lending permission" for "request landing permission". It is
 * bounded by the same guards as the first pass plus its own (a word we authored is never repaired, word counts
 * must agree, one word must land exactly), because the transcript is the only text here nobody authored:
 * everything downstream of the microphone is ours, so an STT slip is the one input worth repairing rather than
 * escalating to a model that answers it differently on different days.
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
     * How the input reached its alias: word-for-word, or through {@link FuzzyAliasMatch}.
     */
    public enum MatchKind {
        EXACT, FUZZY
    }

    /**
     * One resolved reflex: the action to run and the arguments its alias already pinned down.
     */
    public record Reflex(String actionId, Map<String, String> arguments, MatchKind matchKind) {

        public Reflex {
            arguments = Map.copyOf(arguments);
            Objects.requireNonNull(matchKind, "matchKind");
        }

        /**
         * A word-for-word match, which is how most reflexes resolve.
         */
        public Reflex(String actionId, Map<String, String> arguments) {
            this(actionId, arguments, MatchKind.EXACT);
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
    /**
     * Words the fuzzy pass treats as heard rather than damaged; see {@link #spokenWords()}.
     */
    private final Supplier<Set<String>> vocabularySource;

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
        this(commandSource, dangerousActionPolicy, ReflexResolver::spokenWords);
    }

    /**
     * Test seam: supply the commands and the vocabulary together, so a case that injects its own commands is
     * not also matched against the live alias bundles.
     */
    ReflexResolver(Supplier<List<CommandPhrase>> commandSource, DangerousActionPolicy dangerousActionPolicy,
                   Supplier<Set<String>> vocabularySource) {
        this(snapshot -> commandSource.get(), dangerousActionPolicy, vocabularySource);
    }

    /**
     * Canonical constructor: commands, danger policy and vocabulary all explicit.
     */
    private ReflexResolver(Function<GameStateSnapshot, List<CommandPhrase>> commandSource,
                           DangerousActionPolicy dangerousActionPolicy,
                           Supplier<Set<String>> vocabularySource) {
        this.commandSource = commandSource;
        this.dangerousActionPolicy = dangerousActionPolicy;
        this.vocabularySource = vocabularySource;
    }

    /**
     * Every word the commander's own command set is built from: the authored aliases of the session language,
     * plus the phrases of their custom commands.
     * <p>
     * The macro phrases matter as much as the aliases here. A word only they contain would otherwise be
     * unknown to the vocabulary, and unknown means repairable, so a macro utterance could be aligned onto a
     * builtin alias of the same word count - and because macros are never reflex candidates, the macro itself
     * could not win the tie that would make {@link #resolve} abandon. Read live rather than cached: a macro
     * saved mid-session must count immediately, and the common case (no macros) allocates nothing.
     */
    private static Set<String> spokenWords() {
        Set<String> authored = AliasVocabulary.forCurrentLanguage();
        List<CustomCommandDefinition> macros = CustomCommandRegistry.getInstance().getCustomCommands();
        if (macros.isEmpty()) {
            return authored;
        }
        Set<String> words = new HashSet<>(authored);
        for (CustomCommandDefinition macro : macros) {
            words.addAll(AliasVocabulary.tokenize(macro.getPhrases()));
        }
        return words;
    }

    /**
     * The single safe command whose training phrase the input matches, word for word or as a damaged
     * transcript of one, with its arguments, or empty when the input is not a reflex (no match, an ambiguous tie, an argument the alias does not supply,
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
        List<CommandPhrase> visible = commandSource.apply(Objects.requireNonNull(gameStateSnapshot));
        List<Reflex> matches = visible.stream()
                .flatMap(command -> matchVerbatim(command, needle).stream())
                .toList();
        if (matches.isEmpty()) {
            // Nothing was said word for word. Before handing a phrase we may still recognize to the model,
            // try it as a damaged rendering of one alias - the transcript is the one part of this pipeline
            // nobody authored.
            matches = matchFuzzy(visible, needle);
        }
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
     * Every visible action whose alias the input matches as a damaged transcript, one entry per action.
     * <p>
     * Runs only when nothing matched word for word, so an authored alias always wins over a repaired one, and
     * it can add a reflex but never redirect an existing one. Ambiguity is handled where the verbatim pass
     * handles it: two actions matching means the caller abandons and the model decides.
     */
    private List<Reflex> matchFuzzy(List<CommandPhrase> visible, String needle) {
        List<String> heard = AliasVocabulary.tokenize(needle);
        if (heard.isEmpty()) {
            return List.of();
        }
        Set<String> vocabulary = vocabularySource.get();
        List<Reflex> matches = new ArrayList<>();
        for (CommandPhrase command : visible) {
            matchFuzzy(command, heard, vocabulary).ifPresent(matches::add);
        }
        return matches;
    }

    /**
     * The reflex for the one phrase in this command's group the input is a damaged rendering of, subject to the
     * same argument rule as {@link #matchVerbatim}: an alias that leaves a parameter for the model to extract
     * is not a reflex however well the words match.
     */
    private static Optional<Reflex> matchFuzzy(CommandPhrase command, List<String> heard, Set<String> vocabulary) {
        for (String phrase : AiActionLocalizations.splitPhraseGroup(command.phraseGroup())) {
            AliasPhrase alias = AliasPhrase.parse(phrase);
            if (!FuzzyAliasMatch.phraseMatches(heard, AliasVocabulary.tokenize(alias.spokenText()), vocabulary)) {
                continue;
            }
            if (alias.hasVariableArgument()
                    || !alias.literalArguments().keySet().containsAll(command.parameters())) {
                return Optional.empty();
            }
            return Optional.of(new Reflex(command.id(), alias.literalArguments(), MatchKind.FUZZY));
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
