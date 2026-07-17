package elite.intel.companion.prompt;

import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.IntelActionContext;
import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.command.builtin.IgnoreNonsensicalInputCommand;
import elite.intel.ai.brain.actions.customcommand.CustomCommandDefinition;
import elite.intel.ai.brain.actions.customcommand.CustomCommandRegistry;
import elite.intel.ai.brain.actions.handlers.query.ConnectionCheckQuery;
import elite.intel.ai.brain.actions.handlers.query.GeneralConversationQuery;
import elite.intel.ai.brain.actions.query.QueryRegistry;
import elite.intel.ai.brain.i18n.AiActionAliasTextProvider;
import elite.intel.companion.model.GameStateSnapshot;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.i18n.Language;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Builds provider-neutral game-tool candidates from visible actions and localized aliases. */
public final class GameToolCandidates {

    /** Legacy-path fallback ids the companion never offers; it has its own speak. */
    private static final Set<String> EXCLUDED_IDS = Set.of(
            GeneralConversationQuery.ID,
            ConnectionCheckQuery.ID,
            IgnoreNonsensicalInputCommand.ID);

    /**
     * One selectable game tool.
     *
     * @param id                  the action id matched against the reducer's survivors
     * @param localizedAliasGroup the localized alias group, or the id when none is defined
     * @param tool                the rendered, provider-neutral definition carrying the parameter schema
     */
    public record Candidate(String id, String localizedAliasGroup, LlmToolDefinition tool) {}

    private final Map<String, ? extends IntelAction> commands;
    private final Map<String, ? extends IntelAction> queries;
    private final List<CustomCommandDefinition> macros;
    private final Status status;
    private final Language language;
    /** English exonym of {@link #language} (e.g. "English", "Russian"), naming the example-phrase language. */
    private final String languageName;

    /** Production: pulls the self-describing registries, the live game status, and the configured language. */
    public GameToolCandidates() {
        this(Status.getInstance());
    }

    /** Turn-scoped source: gates every candidate against one immutable commander visibility snapshot. */
    GameToolCandidates(GameStateSnapshot snapshot) {
        this(snapshot == null ? Status.getInstance() : snapshot.visibilityStatus());
    }

    /**
     * Pulls the self-describing registries and the configured language, but gates visibility against the given
     * status instead of the live one - e.g. a {@link Status#detached} status standing in for a what-if
     * situation, so the offered tools reflect that context, not the live game.
     */
    public GameToolCandidates(Status status) {
        this(CommandRegistry.getInstance().byId(),
                QueryRegistry.getInstance().byId(),
                CustomCommandRegistry.getInstance().getCustomCommands(),
                status,
                SystemSession.getInstance().getLanguage());
    }

    /** Test seam: inject fixed actions/macros, status, and language (no singletons, no registry scan). */
    GameToolCandidates(Map<String, ? extends IntelAction> commands,
                       Map<String, ? extends IntelAction> queries,
                       List<CustomCommandDefinition> macros,
                       Status status,
                       Language language) {
        this.commands = commands;
        this.queries = queries;
        this.macros = macros;
        this.status = status;
        this.language = language;
        this.languageName = language.displayName();
    }

    /** Visible game tools in the allowed categories, ordered commands, then queries, then macros. */
    public List<Candidate> collect(Set<IntelActionCategory> allowed) {
        List<Candidate> result = new ArrayList<>();
        if (allowed.contains(IntelActionCategory.ACTION)) {
            addActions(result, commands);
        }
        if (allowed.contains(IntelActionCategory.QUERY)) {
            addActions(result, queries);
        }
        if (allowed.contains(IntelActionCategory.MACRO)) {
            addMacros(result);
        }
        return result;
    }

    private void addActions(List<Candidate> out, Map<String, ? extends IntelAction> actions) {
        for (IntelAction action : actions.values()) {
            String id = action.id();
            if (EXCLUDED_IDS.contains(id)
                    || !action.isAvailableIn(IntelActionContext.COMPANION_COMMANDER)
                    || !action.isVisibleForLLM(status)) {
                continue;
            }
            boolean hasPhrases = AiActionAliasTextProvider.hasKey(language, id);
            String phraseGroup = hasPhrases ? AiActionAliasTextProvider.getText(language, id) : "";
            // The localized phrase group stays the reducer/reflex matching surface (it must match the
            // commander's own language). The tool DESCRIPTION the model reads carries the action's English
            // purpose plus its English trigger phrases - concrete targets to match, since the model selects
            // from the English schema and translates non-English input to English first. English phrases keep
            // the description identical across languages (one cache prefix), unlike localized phrases.
            String authored = action.llmDescription();
            String base = authored == null ? "" : authored.strip();
            String description = appendEnglishPhrases(base, id);
            out.add(new Candidate(id, hasPhrases ? phraseGroup : id,
                    new LlmToolDefinition(id, description, phraseGroup, action.parameters())));
        }
    }

    private void addMacros(List<Candidate> out) {
        for (CustomCommandDefinition macro : macros) {
            String id = macro.getActionKey();
            if (EXCLUDED_IDS.contains(id)) {
                continue;
            }
            String phraseGroup = macro.getPhrases() == null ? "" : macro.getPhrases().strip();
            String base = macro.getDescription().isBlank()
                    ? "User-defined macro \"" + id + "\"."
                    : macro.getDescription().strip();
            out.add(new Candidate(id, phraseGroup.isBlank() ? id : phraseGroup,
                    new LlmToolDefinition(id, describe(base, phraseGroup), phraseGroup, macro.getParameters())));
        }
    }

    /** Appends localized example phrases to a macro description when present. */
    private String describe(String base, String phraseGroup) {
        String phrases = examplePhrases(phraseGroup);
        return phrases.isEmpty() ? base : base + " " + phrases;
    }

    /** Returns the localized example-phrase sentence, or an empty string when none exists. */
    private String examplePhrases(String phraseGroup) {
        return phraseGroup.isBlank() ? "" : "Example phrases in " + languageName + ": " + phraseGroup + ".";
    }

    /**
     * Appends the action's English trigger phrases to its tool description, giving the model concrete phrasings
     * to match against. The companion path otherwise shows it only the abstract English purpose, never the
     * phrases (unlike the legacy "action &lt;- phrases" prompt). English on purpose: the schema is English, the
     * model translates non-English input to English before selecting, and an English description stays
     * identical across languages (one cache prefix). Parameter annotations ({@code {key:X}}) are stripped - the
     * JSON schema already carries the parameters.
     */
    private String appendEnglishPhrases(String base, String id) {
        if (!AiActionAliasTextProvider.hasKey(Language.EN, id)) {
            return base;
        }
        String english = stripParamAnnotations(AiActionAliasTextProvider.getText(Language.EN, id));
        if (english.isEmpty()) {
            return base;
        }
        String phrases = "Example phrases: " + english + ".";
        return base.isEmpty() ? phrases : base + " " + phrases;
    }

    /** Strips {@code {key:X}} parameter annotations and tidies the leftover separators/whitespace. */
    private static String stripParamAnnotations(String phrases) {
        return phrases.replaceAll("\\{[^}]*}", "")
                .replaceAll("\\s+,", ",")
                .replaceAll("\\s{2,}", " ")
                .strip();
    }
}
