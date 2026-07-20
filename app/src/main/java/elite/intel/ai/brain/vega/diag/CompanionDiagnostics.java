package elite.intel.ai.brain.vega.diag;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.vega.CompanionConfig;
import elite.intel.ai.brain.vega.model.llm.LlmToolDefinition;
import elite.intel.ai.brain.vega.model.llm.LlmToolInvocation;
import elite.intel.ai.brain.vega.prompt.Fact;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.AppLogDebugEvent;
import elite.intel.ui.event.AppLogEvent;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Single owner of the companion's runtime diagnostics: it renders each consciousness stage into one compact,
 * scannable SYSTEM LOG line and publishes it on the program-wide diagnostics bus ({@link UiBus} +
 * {@link AppLogEvent}/{@link AppLogDebugEvent}) - the same channel the legacy brain logs to, so companion and
 * legacy diagnostics share one surface. Call sites only choose the stage and its values; the line format, the
 * {@code companion} prefix, the per-thought trace tag and the info/detail routing live here (no call site
 * builds a diagnostic string itself).
 * <p>
 * Routing: {@link #info} lines are per-turn headlines the operator should always see (intake, settle outcome,
 * dangerous confirmation) and always render. {@link #debug} lines are internal detail (compose, LLM round,
 * exec, voice, memory, thought lifecycle, watchdog) and render only in the detailed-log view - or, when
 * {@link CompanionConfig#diagnosticsVerbose()} is on (the default while the detailed-log GUI toggle is pending),
 * they are promoted onto the always-visible channel.
 */
public final class CompanionDiagnostics {

    /**
     * Longest inlined free text (input, spoken line, injected fact) before it is elided. The SYSTEM
     * LOG panel word-wraps, so this only bounds runaway text; it is generous enough that a normal companion sentence
     * or a full injected fact is never cut.
     */
    private static final int MAX_TEXT = 400;
    /**
     * Longest rendered tool-call argument JSON before it is elided. Larger than {@link #MAX_TEXT} so a normal
     * tool call shows every field instead of being cut mid-field; the panel wraps
     * the extra onto the next row.
     */
    private static final int MAX_ARGS = 300;

    /** Trace tag for a cross-thread stage that no single thought owns (barge-in, off-lane consolidation). */
    public static final String SYSTEM = "system";

    /**
     * The trace of the thought running on the current lane thread, bound by {@link #enterThought} around
     * {@code Thought.run()}. Lets leaf components invoked synchronously on that thread (the reducer, the memory
     * gateway) tag their lines with the owning thought's trace without threading it through their contracts.
     */
    private static final ThreadLocal<String> AMBIENT_TRACE = new ThreadLocal<>();

    private CompanionDiagnostics() {
    }

    /** Binds {@code trace} to this lane thread so {@link #debugAmbient} calls carry it (paired with {@link #exitThought}). */
    public static void enterThought(String trace) {
        AMBIENT_TRACE.set(trace);
    }

    /** Clears the thread's bound trace when the thought finishes. */
    public static void exitThought() {
        AMBIENT_TRACE.remove();
    }

    /** A per-turn headline the operator should always see (intake, settle outcome, dangerous confirmation). */
    public static void info(String tag, String stage, String detail) {
        UiBus.publish(new AppLogEvent(format(tag, stage, detail)));
    }

    /**
     * An internal-detail line for one consciousness stage. Shown only in the detailed-log view, unless
     * {@link CompanionConfig#diagnosticsVerbose()} is on, which promotes it onto the always-visible channel.
     */
    public static void debug(String tag, String stage, String detail) {
        String line = format(tag, stage, detail);
        UiBus.publish(CompanionConfig.diagnosticsVerbose() ? new AppLogEvent(line) : new AppLogDebugEvent(line));
    }

    /**
     * A detail line tagged with the ambient thought trace bound by {@link #enterThought} (or {@link #SYSTEM}
     * when none is bound), for leaf components (reducer, memory recall) that have no thought reference. Named
     * apart from {@link #debug(String, String, String)} so the two-argument call cannot be mistaken for a
     * tag+stage line with a dropped detail.
     */
    public static void debugAmbient(String stage, String detail) {
        String trace = AMBIENT_TRACE.get();
        debug(trace == null ? SYSTEM : trace, stage, detail);
    }

    /** {@code Companion <tag> <stage>: <detail>} - the one line format every companion diagnostic shares. */
    private static String format(String tag, String stage, String detail) {
        String head = "Companion " + tag + " " + stage;
        return (detail == null || detail.isBlank()) ? head : head + ": " + detail;
    }

    /** Compact tool-name list for a compose line, e.g. {@code [find_action, set_reminder]} (empty -> {@code []}). */
    public static String names(List<LlmToolDefinition> tools) {
        return tools.stream().map(LlmToolDefinition::name).collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * Provenance-tagged rendering of one injected {@code <facts>} candidate, e.g. {@code [system] current system
     * Sol...}, so the log can show each grounding fact on its own line, mirroring the prompt's per-fact block.
     */
    public static String fact(Fact fact) {
        return "[" + fact.source() + "] " + truncate(fact.text());
    }

    /** Compact rendering of the LLM's tool calls, e.g. {@code speak{...}} ({@code none} when empty). */
    public static String calls(List<LlmToolInvocation> invocations) {
        if (invocations.isEmpty()) {
            return "none";
        }
        return invocations.stream()
                .map(inv -> inv.name() + args(inv.arguments()))
                .collect(Collectors.joining(", "));
    }

    /** Compact one-line JSON of a tool-call's arguments ({@code {}} when none), elided past {@link #MAX_ARGS}. */
    public static String args(JsonObject arguments) {
        if (arguments == null || arguments.size() == 0) {
            return "{}";
        }
        return elide(arguments.toString(), MAX_ARGS);
    }

    /** Flattens free text to one line and elides it past {@link #MAX_TEXT}, so a diagnostic line stays scannable. */
    public static String truncate(String text) {
        if (text == null) {
            return "";
        }
        return elide(text.replace('\n', ' ').replace('\r', ' ').strip(), MAX_TEXT);
    }

    /** One-line elision helper: returns {@code s} unchanged, or its first {@code max-1} chars plus an ellipsis. */
    private static String elide(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
