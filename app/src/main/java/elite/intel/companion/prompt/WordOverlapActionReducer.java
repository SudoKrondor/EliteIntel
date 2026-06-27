package elite.intel.companion.prompt;

import elite.intel.ai.brain.actions.command.builtin.IgnoreNonsensicalInputCommand;
import elite.intel.ai.brain.actions.handlers.query.ConnectionCheckQueryCommand;
import elite.intel.ai.brain.actions.handlers.query.GeneralConversationQueryCommand;
import elite.intel.ai.brain.i18n.InputNormalizerLocalizations;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Picks which game commands are shown to the model for a given commander phrase. A command is kept when the
 * phrase shares a meaningful word with one of the command's training phrases - matched with
 * {@link CompanionWordMatch}, which treats inflected forms of a word as the same word, so Russian (and other)
 * word endings no longer hide a command. A blank input offers all commands (no narrowing).
 * <p>
 * This is the companion's own word-overlap, replacing the legacy {@code Reducer} on this path: the legacy one
 * matched whole words exactly, which broke on inflected languages. The reflex fast-path is unaffected - it
 * keeps its own exact, full-phrase match (see {@link ReflexResolver}); only this command-list narrowing is
 * inflection-tolerant.
 */
public final class WordOverlapActionReducer implements CompanionActionReducer {

    /** Fallback ids the companion never offers; it has its own speak/nothing_to_do. */
    private static final Set<String> FALLBACK_IDS = Set.of(
            GeneralConversationQueryCommand.ID,
            ConnectionCheckQueryCommand.ID,
            IgnoreNonsensicalInputCommand.ID);

    /** Words shorter than this carry no selection signal (mirrors the legacy tokenizer's length filter). */
    private static final int MIN_WORD_LEN = 3;

    /** Cap on offered commands so a common word cannot flood the prompt with a whole command family. */
    private static final int MAX_TOOLS = 20;
    /** Keep only commands scoring at least this fraction of the top score; drops the weak tail of a family. */
    private static final double KEEP_FRACTION = 0.5;
    /** Small score bonus nudging panel/info commands to the top for a bare single-word input. */
    private static final double PANEL_BOOST = 1.0;

    /**
     * Analytic languages whose words barely change form, so exact word-overlap is enough and tolerant matching
     * would only over-surface (e.g. English "navigation" pulling in "navigate" commands). Every other supported
     * language (Slavic, Germanic, Romance) inflects, so it uses the tolerant matcher by default - including any
     * language added later, which is far more likely to inflect than to be analytic like English.
     */
    private static final Set<Language> ANALYTIC = Set.of(Language.EN);

    private final Function<Set<IntelActionCategory>, List<GameToolCandidates.Candidate>> candidateSource;
    private final boolean inflectionTolerant;

    /** Production: candidates from the live registries/status, matcher chosen by the session language. */
    public WordOverlapActionReducer() {
        this(new GameToolCandidates()::collect, SystemSession.getInstance().getLanguage());
    }

    /** Test seam: inject a fixed candidate source; defaults to exact (English) matching. */
    WordOverlapActionReducer(Function<Set<IntelActionCategory>, List<GameToolCandidates.Candidate>> candidateSource) {
        this(candidateSource, Language.EN);
    }

    /** Test seam: inject a fixed candidate source and the matching language. */
    WordOverlapActionReducer(Function<Set<IntelActionCategory>, List<GameToolCandidates.Candidate>> candidateSource,
                             Language language) {
        this.candidateSource = candidateSource;
        this.inflectionTolerant = !ANALYTIC.contains(language);
    }

    @Override
    public List<LlmToolDefinition> selectTools(Set<IntelActionCategory> allowedCategories, String currentInput) {
        List<GameToolCandidates.Candidate> candidates = candidateSource.apply(allowedCategories);
        if (candidates.isEmpty()) {
            return List.of();
        }
        // A blank input has no signal to narrow on - offer everything, mirroring the legacy "offer all".
        if (currentInput == null || currentInput.isBlank()) {
            return allTools(candidates);
        }
        List<String> inputWords = List.copyOf(significantWords(currentInput));
        if (inputWords.isEmpty()) {
            return List.of(); // only stop/short words: no signal to match on
        }

        // Which input words each command's training phrases match, and how many commands each input word hits.
        int commandCount = candidates.size();
        List<GameToolCandidates.Candidate> matched = new ArrayList<>();
        List<boolean[]> masks = new ArrayList<>();
        int[] documentFrequency = new int[inputWords.size()];
        for (GameToolCandidates.Candidate candidate : candidates) {
            if (FALLBACK_IDS.contains(candidate.id())) {
                continue;
            }
            Set<String> triggerWords = significantWords(candidate.phraseKey());
            boolean[] mask = new boolean[inputWords.size()];
            boolean any = false;
            for (int i = 0; i < inputWords.size(); i++) {
                if (matchesAny(inputWords.get(i), triggerWords)) {
                    mask[i] = true;
                    documentFrequency[i]++;
                    any = true;
                }
            }
            if (any) {
                matched.add(candidate);
                masks.add(mask);
            }
        }
        if (matched.isEmpty()) {
            return List.of();
        }

        // A word shared by many commands carries little signal (inverse document frequency): "авианосцем" in
        // 18 carrier commands weighs far less than "управление" in a few, so a command that matched the rarer
        // word ranks above ones that only matched the common word.
        double[] weight = new double[inputWords.size()];
        for (int i = 0; i < inputWords.size(); i++) {
            weight[i] = Math.log((double) commandCount / Math.max(1, documentFrequency[i])) + 1.0;
        }
        boolean singleWord = inputWords.size() == 1;

        List<Scored> scored = new ArrayList<>(matched.size());
        for (int c = 0; c < matched.size(); c++) {
            boolean[] mask = masks.get(c);
            double score = 0;
            for (int i = 0; i < inputWords.size(); i++) {
                if (mask[i]) {
                    score += weight[i];
                }
            }
            // A bare single word under-specifies the action; nudge "show panel"/info commands to the top.
            if (singleWord && isPanelOrInfo(matched.get(c).id())) {
                score += PANEL_BOOST;
            }
            scored.add(new Scored(matched.get(c), score));
        }
        // Highest score first; ties keep candidate order (stable sort). Cap the list so a common word cannot
        // flood the prompt with a whole command family.
        scored.sort((a, b) -> Double.compare(b.score(), a.score()));
        double cutoff = scored.get(0).score() * KEEP_FRACTION;
        List<LlmToolDefinition> result = new ArrayList<>();
        Set<String> added = new LinkedHashSet<>();
        for (Scored s : scored) {
            if (result.size() >= MAX_TOOLS || s.score() < cutoff) {
                break; // sorted high-to-low: nothing past the cap or below the cutoff survives
            }
            if (added.add(s.candidate().id())) {
                result.add(s.candidate().tool());
            }
        }
        return result;
    }

    private record Scored(GameToolCandidates.Candidate candidate, double score) {}

    /** Whether the input word matches any of a command's training-phrase words (inflection-tolerant or exact). */
    private boolean matchesAny(String inputWord, Set<String> triggerWords) {
        for (String trigger : triggerWords) {
            boolean match = inflectionTolerant ? CompanionWordMatch.similar(inputWord, trigger) : inputWord.equals(trigger);
            if (match) {
                return true;
            }
        }
        return false;
    }

    /** Panel / info commands, identified by their stable English id - language-agnostic across all languages. */
    private static boolean isPanelOrInfo(String id) {
        return id.startsWith("show_") || id.startsWith("display_") || id.startsWith("query_");
    }

    /** Every non-fallback candidate's tool, de-duplicated in candidate order (the blank-input "offer all"). */
    private static List<LlmToolDefinition> allTools(List<GameToolCandidates.Candidate> candidates) {
        List<LlmToolDefinition> result = new ArrayList<>();
        Set<String> added = new LinkedHashSet<>();
        for (GameToolCandidates.Candidate candidate : candidates) {
            if (!FALLBACK_IDS.contains(candidate.id()) && added.add(candidate.id())) {
                result.add(candidate.tool());
            }
        }
        return result;
    }

    /** Lowercased word tokens worth matching on: long enough and not a stop word for the current language. */
    private static Set<String> significantWords(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        Set<String> stopWords = InputNormalizerLocalizations.stopWords();
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}_]+"))
                .filter(word -> word.length() >= MIN_WORD_LEN)
                .filter(word -> !stopWords.contains(word))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
