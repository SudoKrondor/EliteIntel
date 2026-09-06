package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Which transmissions come from one individual, and which from a thing.
 * <p>
 * This is what decides whether a speaker keeps one voice on the radio channel or draws a stranger every
 * time: an NPC pilot the commander is fighting has to be recognisable across the lines they send, while a
 * station or a police wing has no individual behind it to recognise. Every {@code From} below is copied
 * verbatim out of a journal.
 */
class ReceiveTextEventTest {

    @Test
    void anNpcPilotIsIdentifiedByName() {
        assertEquals("Dave Knowles",
                event("$npc_name_decorate:#name=Dave Knowles;", "Dave Knowles").getNpcPilotName());
        // Some pilot names read as a title plus a name; the whole thing is still one pilot.
        assertEquals("Baker Matt Baker",
                event("$npc_name_decorate:#name=Baker Matt Baker;", "Baker Matt Baker").getNpcPilotName());
    }

    /**
     * The localised sibling is missing from some lines, so the name is read out of the symbol itself rather
     * than the transmission going unattributed.
     */
    @Test
    void anNpcPilotIsIdentifiedWithoutTheLocalisedSibling() {
        assertEquals("Dave Knowles", event("$npc_name_decorate:#name=Dave Knowles;", null).getNpcPilotName());
    }

    @Test
    void everyoneElseOnTheChannelIsNobodyInParticular() {
        assertNull(event("$ShipName_Police_Federation;", "Federal Security Service").getNpcPilotName(),
                "a police wing is a type, not a pilot");
        assertNull(event("$ShipName_PassengerLiner_Cruise;", "Cruise Liner").getNpcPilotName());
        assertNull(event("LONE WOLF GHY-L8X", null).getNpcPilotName(), "a carrier is voiced by assignment");
        assertNull(event("Abraham Lincoln", null).getNpcPilotName(), "a station");
        assertNull(event("Orbital Construction Site: Witt Hub", null).getNpcPilotName());
        assertNull(event("", null).getNpcPilotName());
    }

    private static ReceiveTextEvent event(String from, String fromLocalised) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", "2026-09-06T15:52:14Z");
        json.addProperty("event", "ReceiveText");
        json.addProperty("From", from);
        if (fromLocalised != null) json.addProperty("From_Localised", fromLocalised);
        json.addProperty("Message", "$BadKarmaCriticalDamage07;");
        json.addProperty("Message_Localised", "Too much damage!");
        json.addProperty("Channel", "npc");
        return GsonFactory.getGson().fromJson(json, ReceiveTextEvent.class);
    }
}
