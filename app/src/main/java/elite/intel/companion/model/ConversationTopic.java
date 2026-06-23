package elite.intel.companion.model;

import java.util.Locale;

/**
 * Closed set of conversation/experience topics the companion can focus on. Always rendered (with
 * descriptions) into the prompt so the LLM knows the valid values for {@code change_global_topic} and
 * {@code recall(topic=...)}.
 * <p>
 * {@link #selectable} marks LLM-facing topics. Non-selectable members are internal fallbacks
 * ({@link #UNRESOLVED_COMMANDER_INPUT}, {@link #UNRESOLVED_GAME_EVENT}) and must not be offered to the LLM.
 * <p>
 * Names/coverage are provisional and expected to be tuned; keep the selectable set compact (~10-15).
 */
public enum ConversationTopic {

    // --- internal fallbacks (never shown as choices to the LLM) ---
    /** Fallback when a commander input could not be resolved to a topic. */
    UNRESOLVED_COMMANDER_INPUT("unresolved commander input", false),
    /** Fallback when a game event could not be resolved to a topic. */
    UNRESOLVED_GAME_EVENT("unresolved game event", false),

    // --- LLM-selectable topics ---
    NAVIGATION("jumps, routes, systems, docking, location changes", true),
    COMBAT("threats, attacks, weapons, shields, hull, hostiles", true),
    TRADE("market, commodities, prices, cargo, profit", true),
    MINING("prospecting, extraction, refinery, limpets", true),
    EXPLORATION("scanning, discoveries, fuel, jumponium, deep space", true),
    EXOBIOLOGY("biological signals, organic samples, genus, species", true),
    MISSIONS("missions, objectives, bounties, rewards, factions", true),
    SHIP_STATUS("modules, power, fuel, repairs, loadout health", true),
    ENGINEERING("blueprints, materials, engineers, modifications", true),
    CREW("crew, wing, multicrew, fighters, NPC pilots", true),
    SOCIAL("commander chat, roleplay, banter, small talk", true),
    SYSTEM("companion/system status, settings, diagnostics", true);

    private final String description;
    private final boolean selectable;

    ConversationTopic(String description, boolean selectable) {
        this.description = description;
        this.selectable = selectable;
    }

    /** Short description shown in the prompt topic index. */
    public String description() {
        return description;
    }

    /** Whether this topic may be chosen by the LLM (change_global_topic / recall). */
    public boolean selectable() {
        return selectable;
    }

    /**
     * Resolves an LLM-supplied topic id (case-insensitive) to a selectable topic, or {@code null} when it
     * is unknown or a non-selectable sentinel. Single owner of topic-id parsing for the tools.
     */
    public static ConversationTopic fromSelectableId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            ConversationTopic topic = valueOf(id.trim().toUpperCase(Locale.ROOT));
            return topic.selectable() ? topic : null;
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
