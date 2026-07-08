package elite.intel.companion;

import elite.intel.ai.brain.i18n.LlmTextProvider;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The single place for the companion's settings. The companion-mode toggle is DB-backed (parallel to
 * conversation mode) and read through {@link SystemSession}; the confirmation code word is still a
 * hardcoded placeholder pending its own GUI/DB-backed value.
 */
public final class CompanionConfig {

    // TODO: back the confirmation code word by GUI/DB settings.
    private static final String CONFIRMATION_CODE_WORD = "password";

    // TODO: back the companion name by GUI/DB settings.
    private static final String COMPANION_NAME = "Vega";

    // Localization key (i18n.llm bundle) for the name's spoken/STT form per language: Latin "Vega" for
    // Latin-script languages, "Вега" for Cyrillic (ru/uk). Input matching only - never used in the prompt.
    private static final String NAME_SPOKEN_KEY = "companion.name.spoken";

    // --- runtime tuning (provisional; TODO: back by GUI/DB settings) ---
    /** Max entries kept in the hot short-term timeline (the primary eviction control). */
    private static final int SHORT_TERM_MEMORY_SIZE = 30;
    /** Max entries kept per topic in mid-term memory before older ones overflow to consolidation. */
    private static final int MID_TERM_MEMORY_SIZE_PER_TOPIC = 30;
    /** Max commander thoughts that may run concurrently on the commander lane. */
    private static final int MAX_PARALLEL_COMMANDER_THOUGHTS = 5;
    /** Absolute floor (cosine 0..1): below this a memory entry is unrelated and dropped from the semantic part of memory recall. e5-small cosines are compressed, so unrelated short-text pairs sit just under it. */
    private static final double SEMANTIC_RECALL_FLOOR = 0.85;
    /** At or above this meaning-closeness (cosine 0..1) two memory entries are treated as the same fact and collapsed (on write and in search results). */
    private static final double SEMANTIC_DEDUP_FLOOR = 0.95;
    /** Max characters a single memory entry may hold; a longer write is sent for silent LLM compression to a gist before storing (prompt-bloat guard). */
    private static final int MEMORY_ENTRY_MAX_CHARS = 200;
    /**
     * Whether the companion's internal-detail diagnostics (compose/LLM/classify/exec/memory/lifecycle) are
     * promoted onto the always-visible SYSTEM LOG. On by default while the detailed-log GUI toggle for this
     * channel is still pending; set to {@code false} to keep only the per-turn headlines.
     */
    private static final boolean DIAGNOSTICS_VERBOSE = true;
    /**
     * If true, non-PTT microphone transcripts are dropped while companion speech is active. Keep false to let
     * commander barge-in speech interrupt the current narration and route as a normal command.
     */
    private static final boolean DROP_HOT_MIC_TRANSCRIPTS_WHILE_COMPANION_SPEAKS = false;

    private CompanionConfig() {
    }

    /** The companion's own name, woven into its persona prompt. */
    public static String companionName() {
        return COMPANION_NAME;
    }

    /**
     * The name forms recognized as a leading vocative on commander INPUT (the reflex vocative strip): the
     * canonical name plus its localized spoken/STT form for the current session language (e.g. Cyrillic "Вега"
     * for ru/uk, from the i18n.llm bundle). Input matching only - the prompt always uses {@link #companionName()}.
     * <p>
     * TODO: when the name becomes GUI/DB-configurable, its spoken variants must follow the configured value.
     */
    public static List<String> companionNameForms() {
        Language language = SystemSession.getInstance().getLanguage();
        Set<String> forms = new LinkedHashSet<>();
        forms.add(COMPANION_NAME);
        String spoken = LlmTextProvider.getText(language, NAME_SPOKEN_KEY);
        if (spoken != null && !spoken.isBlank()) {
            forms.add(spoken.trim());
        }
        return List.copyOf(forms);
    }

    /**
     * Whether companion mode replaces the legacy command mode. DB-backed (defaults off).
     */
    public static boolean companionModeOn() {
        return SystemSession.getInstance().companionModeOn();
    }

    /** The spoken code word that confirms a frozen dangerous action (§2.13). */
    public static String confirmationCodeWord() {
        return CONFIRMATION_CODE_WORD;
    }

    /** Whether the commander input is exactly the confirmation code word (trimmed, case-insensitive). */
    public static boolean isConfirmationCodeWord(String input) {
        return input != null && input.strip().equalsIgnoreCase(CONFIRMATION_CODE_WORD);
    }

    /** Max entries kept in the hot short-term timeline (the primary eviction control). */
    public static int shortTermMemorySize() {
        return SHORT_TERM_MEMORY_SIZE;
    }

    /** Max entries kept per topic in mid-term memory before older ones overflow to consolidation. */
    public static int midTermMemorySizePerTopic() {
        return MID_TERM_MEMORY_SIZE_PER_TOPIC;
    }

    /** Max commander thoughts that may run concurrently on the commander lane. */
    public static int maxParallelCommanderThoughts() {
        return MAX_PARALLEL_COMMANDER_THOUGHTS;
    }

    /** Absolute floor (cosine 0..1) below which a semantic match is dropped from memory recall. */
    public static double semanticRecallFloor() {
        return SEMANTIC_RECALL_FLOOR;
    }

    /** At or above this meaning-closeness (cosine 0..1) two memory entries are treated as the same fact and collapsed (on write and in search results). */
    public static double semanticDedupFloor() {
        return SEMANTIC_DEDUP_FLOOR;
    }

    /** Max characters a single memory entry may hold; a longer write is sent for silent LLM compression to a gist before storing (prompt-bloat guard). */
    public static int memoryEntryMaxChars() {
        return MEMORY_ENTRY_MAX_CHARS;
    }

    /** Whether the companion's internal-detail diagnostics are promoted onto the always-visible SYSTEM LOG (default on). */
    public static boolean diagnosticsVerbose() {
        return DIAGNOSTICS_VERBOSE;
    }

    /** Whether hot-mic transcripts are suppressed while companion speech is active. */
    public static boolean dropHotMicTranscriptsWhileCompanionSpeaks() {
        return DROP_HOT_MIC_TRANSCRIPTS_WHILE_COMPANION_SPEAKS;
    }
}
