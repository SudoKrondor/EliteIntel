package elite.intel.ai.brain.vega.memory.facts.sources;

import elite.intel.ai.brain.vega.memory.facts.MemoryFactContext;
import elite.intel.ai.brain.vega.memory.facts.MemoryFactSource;
import elite.intel.ai.brain.vega.memory.facts.RegisterMemoryFactSource;
import elite.intel.ai.brain.vega.model.ThoughtSource;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags.GuiFocus;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Always-on live fact for the panel, map, or mode currently on screen.
 * <p>
 * {@link CurrentSituationFactSource} reports only where the commander physically is - docked, on foot, in
 * supercruise - which left the model blind to what it was looking at. Asked to "close the window" while the
 * fleet carrier management screen was open, its only fact was "In ship - docked at station", so it answered
 * "We're docked, no open windows to close" and spoke instead of calling {@code exit_close}. The screen state
 * is the evidence that makes closing, backing out, and map commands actionable rather than a judgement call.
 * <p>
 * Silent when nothing is open: absence of this fact is itself the signal that the view is clear, and emitting
 * "nothing open" every turn would spend context on the common case.
 */
@RegisterMemoryFactSource
public final class OpenScreenFactSource implements MemoryFactSource {

    /**
     * Provenance label for the {@code <fact source="...">} attribute.
     */
    private static final String ID = "screen";

    /**
     * Stable English names for the model, deliberately not localized: like the situation fact these lines are
     * only ever read by the LLM, never shown to a human, and pinning English keeps prompts comparable across
     * the commander's UI language.
     */
    private static final Map<GuiFocus, String> SCREEN_NAMES = new EnumMap<>(GuiFocus.class);

    static {
        SCREEN_NAMES.put(GuiFocus.INTERNAL_PANEL, "right panel (internal)");
        SCREEN_NAMES.put(GuiFocus.EXTERNAL_PANEL, "left panel (external/navigation)");
        SCREEN_NAMES.put(GuiFocus.COMMS_PANEL, "comms panel");
        SCREEN_NAMES.put(GuiFocus.CENTRAL_PANEL, "central role panel");
        SCREEN_NAMES.put(GuiFocus.STATION_SERVICES, "station services / fleet carrier management");
        SCREEN_NAMES.put(GuiFocus.GALAXY_MAP, "galaxy map");
        SCREEN_NAMES.put(GuiFocus.SYSTEM_MAP, "system map");
        SCREEN_NAMES.put(GuiFocus.ORRERY, "orrery view");
        SCREEN_NAMES.put(GuiFocus.FSS_MODE, "full spectrum scanner");
        SCREEN_NAMES.put(GuiFocus.SAA_MODE, "surface scanner");
        SCREEN_NAMES.put(GuiFocus.CODEX, "codex");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isRelevant(MemoryFactContext context) {
        return context != null && context.source() == ThoughtSource.COMMANDER;
    }

    @Override
    public List<String> factsFor(MemoryFactContext context) {
        String fact = format(Status.getInstance().getGuiFocus());
        return fact == null ? List.of() : List.of(fact);
    }

    /**
     * Formats the on-screen UI as one fact line, or {@code null} when nothing is open and the source should stay
     * silent. {@code UNKNOWN} is treated as nothing open: a focus value we cannot name is not evidence of a window.
     * Pure and package-visible for testing.
     */
    static String format(GuiFocus focus) {
        String name = focus == null ? null : SCREEN_NAMES.get(focus);
        return name == null ? null : "On screen now: " + name + " (open - it can be closed)";
    }
}
