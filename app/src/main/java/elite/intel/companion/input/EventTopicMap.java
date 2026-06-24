package elite.intel.companion.input;

import elite.intel.companion.model.ConversationTopic;
import elite.intel.gameapi.journal.events.BaseEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Static classifier of a game event's journal type ({@link BaseEvent#getEventType()}) to the
 * {@link ConversationTopic} its memory entry is tagged with. EVENT thoughts never choose a topic via the
 * LLM (they cannot call change_global_topic); this map provides it mechanically so an event is recorded
 * under the right topic without moving the commander's global conversation topic (see
 * COMPANION_ARCHITECTURE.md §2.5).
 * <p>
 * Unmapped (or null) event types fall back to {@link ConversationTopic#UNRESOLVED_GAME_EVENT}. The
 * mapping is the agreed gameplay taxonomy; a few entries have a deliberate primary choice among several
 * plausible topics (e.g. Powerplay -> MISSIONS, CodexEntry -> EXPLORATION, LaunchDrone -> MINING) and may
 * later be refined by event payload.
 */
public final class EventTopicMap {

    private static final Map<String, ConversationTopic> BY_EVENT = build();

    private EventTopicMap() {
    }

    /** Topic for a journal event type, or {@link ConversationTopic#UNRESOLVED_GAME_EVENT} if unmapped/null. */
    public static ConversationTopic topicFor(String eventType) {
        if (eventType == null) {
            return ConversationTopic.UNRESOLVED_GAME_EVENT;
        }
        return BY_EVENT.getOrDefault(eventType, ConversationTopic.UNRESOLVED_GAME_EVENT);
    }

    /** Convenience overload reading {@link BaseEvent#getEventType()}; null event -> fallback. */
    public static ConversationTopic topicFor(BaseEvent event) {
        return event == null ? ConversationTopic.UNRESOLVED_GAME_EVENT : topicFor(event.getEventType());
    }

    /**
     * Whether this journal event type is part of the companion's gameplay taxonomy. This doubles as the
     * EVENT allow-list ({@code GameEventFilter}): the same curated set that earns a memory topic is the
     * set worth waking the companion, so there is a single owner of "events that matter".
     */
    public static boolean isMapped(String eventType) {
        return eventType != null && BY_EVENT.containsKey(eventType);
    }

    private static Map<String, ConversationTopic> build() {
        Map<String, ConversationTopic> m = new HashMap<>();
        put(m, ConversationTopic.NAVIGATION,
                "ApproachBody", "ApproachSettlement", "CarrierJump", "CarrierJumpRequest", "CarrierLocation",
                "Disembark", "DockSRV", "Docked", "DockingGranted", "FSDJump", "FSDTarget", "LaunchSRV",
                "Liftoff", "Location", "NavRoute", "NavRouteClear", "StartJump", "SupercruiseDestinationDrop",
                "SupercruiseEntry", "SupercruiseExit", "Touchdown");
        put(m, ConversationTopic.COMBAT,
                "Bounty", "CommitCrime", "Scanned", "ShipTargeted");
        put(m, ConversationTopic.TRADE,
                "Cargo", "CargoTransfer", "CarrierTradeOrder", "MarketBuy", "MarketSell");
        put(m, ConversationTopic.MINING,
                "LaunchDrone", "MiningRefined", "ProspectedAsteroid");
        put(m, ConversationTopic.EXPLORATION,
                "CodexEntry", "FSSBodySignals", "FSSSignalDiscovered", "MultiSellExplorationData",
                "SAAScanComplete", "SAASignalsFound", "Scan", "ScanBaryCentre");
        put(m, ConversationTopic.EXOBIOLOGY,
                "ScanOrganic", "SellOrganicData");
        put(m, ConversationTopic.MISSIONS,
                "MissionAbandoned", "MissionAccepted", "MissionCompleted", "MissionFailed", "MissionRedirected",
                "Missions", "Powerplay", "RedeemVoucher", "Reputation");
        put(m, ConversationTopic.SHIP_STATUS,
                "CarrierDepositFuel", "CarrierStats", "Loadout", "ShipyardBuy", "ShipyardNew", "SwitchSuitLoadout");
        put(m, ConversationTopic.ENGINEERING,
                "EngineerCraft", "EngineerProgress", "MaterialCollected", "Materials");
        put(m, ConversationTopic.CREW,
                "DockFighter", "LaunchFighter", "NpcCrewPaidWage");
        put(m, ConversationTopic.SOCIAL,
                "Friends", "ReceiveText", "SquadronStartup");
        put(m, ConversationTopic.SYSTEM,
                "Commander", "LoadGame", "Progress", "Promotion", "Rank", "Shutdown", "Statistics");
        return Map.copyOf(m);
    }

    private static void put(Map<String, ConversationTopic> map, ConversationTopic topic, String... eventTypes) {
        for (String eventType : eventTypes) {
            map.put(eventType, topic);
        }
    }
}
