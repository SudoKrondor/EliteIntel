package elite.intel.companion.model;

/**
 * Closed set of conversation/experience topics the companion can focus on. Always rendered (with
 * descriptions) into the prompt so the LLM knows the valid values for {@code set_topic} and
 * {@code recall(topic=...)}.
 * <p>
 * {@link #selectable} marks LLM-facing topics. Non-selectable members are internal sentinels/fallbacks
 * ({@link #PENDING}, the {@code unresolved_*} fallbacks) and must not be offered to the LLM.
 * <p>
 * Names/coverage are provisional and expected to be tuned; keep the selectable set compact (~10-15).
 */
public enum ConversationTopic {

    // --- internal sentinels / fallbacks (never shown as choices to the LLM) ---
    /** Initial per-thought state before topic resolution. */
    PENDING("pending topic resolution", false),
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

    /** Whether this topic may be chosen by the LLM (set_topic / recall). */
    public boolean selectable() {
        return selectable;
    }
}
