package elite.intel.companion.input;

import elite.intel.companion.model.ConversationTopic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Spot-checks the event-type to topic mapping across every topic, plus the fallback for unknown/null and
 * a couple of the deliberate primary-choice entries.
 */
class EventTopicMapTest {

    @Test
    void mapsRepresentativeEventsPerTopic() {
        assertEquals(ConversationTopic.NAVIGATION, EventTopicMap.topicFor("FSDJump"));
        assertEquals(ConversationTopic.COMBAT, EventTopicMap.topicFor("ShipTargeted"));
        assertEquals(ConversationTopic.TRADE, EventTopicMap.topicFor("MarketSell"));
        assertEquals(ConversationTopic.MINING, EventTopicMap.topicFor("MiningRefined"));
        assertEquals(ConversationTopic.EXPLORATION, EventTopicMap.topicFor("Scan"));
        assertEquals(ConversationTopic.EXOBIOLOGY, EventTopicMap.topicFor("ScanOrganic"));
        assertEquals(ConversationTopic.MISSIONS, EventTopicMap.topicFor("MissionAccepted"));
        assertEquals(ConversationTopic.SHIP_STATUS, EventTopicMap.topicFor("Loadout"));
        assertEquals(ConversationTopic.ENGINEERING, EventTopicMap.topicFor("EngineerCraft"));
        assertEquals(ConversationTopic.CREW, EventTopicMap.topicFor("LaunchFighter"));
        assertEquals(ConversationTopic.SOCIAL, EventTopicMap.topicFor("ReceiveText"));
        assertEquals(ConversationTopic.SYSTEM, EventTopicMap.topicFor("Rank"));
    }

    @Test
    void honorsDeliberatePrimaryChoices() {
        assertEquals(ConversationTopic.MISSIONS, EventTopicMap.topicFor("Powerplay"));
        assertEquals(ConversationTopic.EXPLORATION, EventTopicMap.topicFor("CodexEntry"));
        assertEquals(ConversationTopic.MINING, EventTopicMap.topicFor("LaunchDrone"));
        assertEquals(ConversationTopic.NAVIGATION, EventTopicMap.topicFor("Disembark"));
    }

    @Test
    void unknownOrNullFallsBackToUnresolved() {
        assertEquals(ConversationTopic.UNRESOLVED_GAME_EVENT, EventTopicMap.topicFor("TotallyMadeUpEvent"));
        assertEquals(ConversationTopic.UNRESOLVED_GAME_EVENT, EventTopicMap.topicFor((String) null));
        assertEquals(ConversationTopic.UNRESOLVED_GAME_EVENT, EventTopicMap.topicFor((elite.intel.gameapi.journal.events.BaseEvent) null));
    }
}
