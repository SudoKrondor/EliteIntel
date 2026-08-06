package elite.intel.ai.brain.vega;

import elite.intel.ai.brain.CompanionIdentity;
import elite.intel.ai.brain.i18n.ResponseTextProvider;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The single place for the companion's settings. The companion is the application's only command mode; the
 * confirmation code word is still a hardcoded placeholder pending its own GUI/DB-backed value.
 */
public final class CompanionConfig {

    // TODO: back the confirmation code word by GUI/DB settings.
    private static final String CONFIRMATION_CODE_WORD = "password";

    // The name itself lives with the identity clause it appears in - see CompanionIdentity.
    private static final String COMPANION_NAME = CompanionIdentity.name();

    // Localization key (i18n.responses bundle) for the name's spoken/STT form per language: Latin "Vega" for
    // Latin-script languages, "Вега" for Cyrillic (ru/uk). Input matching only - never used in the prompt.
    private static final String NAME_SPOKEN_KEY = "companion.name.spoken";

    // --- runtime tuning (provisional; TODO: back by GUI/DB settings) ---
    /** Max read-only query handlers that may execute concurrently after their cognitive turns settle. */
    private static final int MAX_PARALLEL_QUERY_EXECUTIONS = 4;
    /**
     * How many function calls one commander turn may settle. Above one so a commander who asks two things in a
     * breath ("check the loadout, what is our cargo capacity") is answered rather than repaired down to half an
     * answer; two rather than more, because everything past the second call is far more often a model padding
     * its response than a commander asking for a third thing, and each extra call is another execution and
     * another spoken line.
     */
    private static final int MAX_COMMANDER_TOOL_CALLS = 2;
    /** Hard lifecycle ceiling for a live thought. */
    private static final Duration THOUGHT_WATCHDOG_TIMEOUT = Duration.ofSeconds(60);
    /** One deadline for queue wait plus all physical attempts of a logical LLM request. */
    private static final Duration LLM_LOGICAL_DEADLINE = Duration.ofSeconds(50);
    /**
     * Whether the companion's internal-detail diagnostics (compose/LLM/exec/memory/lifecycle) are
     * promoted onto the always-visible SYSTEM LOG. On by default while the detailed-log GUI toggle for this
     * channel is still pending; set to {@code false} to keep only the per-turn headlines.
     */
    private static final boolean DIAGNOSTICS_VERBOSE = true;
    private CompanionConfig() {
    }

    /** The companion's own name, woven into its persona prompt. */
    public static String companionName() {
        return COMPANION_NAME;
    }

    /**
     * The name forms recognized as a leading vocative on commander INPUT (the reflex vocative strip): the
     * canonical name plus its localized spoken/STT form for the current session language (e.g. Cyrillic "Вега"
     * for ru/uk, from the i18n.responses bundle). Input matching only - the prompt always uses {@link #companionName()}.
     * <p>
     * TODO: when the name becomes GUI/DB-configurable, its spoken variants must follow the configured value.
     */
    public static List<String> companionNameForms() {
        Language language = SystemSession.getInstance().getLanguage();
        Set<String> forms = new LinkedHashSet<>();
        forms.add(COMPANION_NAME);
        String spoken = ResponseTextProvider.getText(language, NAME_SPOKEN_KEY);
        if (spoken != null && !spoken.isBlank()) {
            forms.add(spoken.trim());
        }
        return List.copyOf(forms);
    }

    /** The spoken code word that confirms a frozen dangerous action (§2.13). */
    public static String confirmationCodeWord() {
        return CONFIRMATION_CODE_WORD;
    }

    /** Whether the commander input is exactly the confirmation code word (trimmed, case-insensitive). */
    public static boolean isConfirmationCodeWord(String input) {
        return input != null && input.strip().equalsIgnoreCase(CONFIRMATION_CODE_WORD);
    }

    /** Max read-only query handlers that may execute concurrently. */
    public static int maxParallelQueryExecutions() {
        return MAX_PARALLEL_QUERY_EXECUTIONS;
    }

    /**
     * Max function calls one commander turn settles; a longer response is repaired down to this.
     */
    public static int maxCommanderToolCalls() {
        return MAX_COMMANDER_TOOL_CALLS;
    }

    /** Hard lifecycle ceiling for a live thought. */
    public static Duration thoughtWatchdogTimeout() {
        return THOUGHT_WATCHDOG_TIMEOUT;
    }

    /** Total LLM request deadline, deliberately shorter than {@link #thoughtWatchdogTimeout()}. */
    public static Duration llmLogicalDeadline() {
        return LLM_LOGICAL_DEADLINE;
    }

    /** Whether the companion's internal-detail diagnostics are promoted onto the always-visible SYSTEM LOG (default on). */
    public static boolean diagnosticsVerbose() {
        return DIAGNOSTICS_VERBOSE;
    }

}
