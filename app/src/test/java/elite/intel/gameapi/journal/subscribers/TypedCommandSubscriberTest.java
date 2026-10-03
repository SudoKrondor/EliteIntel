package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonParser;
import elite.intel.gameapi.journal.events.SendTextEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A chat line opening with {@code @Vega} is an order typed instead of spoken; every other line the commander
 * sends is chat and must stay out of VEGA.
 */
class TypedCommandSubscriberTest {

    private static final List<String> NAMES = List.of("Vega", "Вега");

    @Test
    void theWordsAfterTheAddressAreTheCommand() {
        assertEquals("do my taxes", TypedCommandSubscriber.commandFor("@Vega do my taxes", NAMES));
        assertEquals("find Tritium", TypedCommandSubscriber.commandFor("  @vega, find Tritium ", NAMES));
        assertEquals("find Tritium", TypedCommandSubscriber.commandFor("@VEGA: find Tritium", NAMES));
    }

    @Test
    void theNameAsTheAppLanguageSpellsItWorksToo() {
        assertEquals("найди тритий", TypedCommandSubscriber.commandFor("@вега найди тритий", NAMES));
    }

    @Test
    void ordinaryChatIsNotACommand() {
        assertNull(TypedCommandSubscriber.commandFor("o7 cmdr", NAMES));
        assertNull(TypedCommandSubscriber.commandFor("Vega do my taxes", NAMES), "no @, no command");
        assertNull(TypedCommandSubscriber.commandFor("hey @Vega do my taxes", NAMES), "the address must open the line");
        assertNull(TypedCommandSubscriber.commandFor("@Vegas is a city", NAMES), "a longer name is someone else");
        assertNull(TypedCommandSubscriber.commandFor("@Vega", NAMES), "an address with nothing to do");
        assertNull(TypedCommandSubscriber.commandFor("@Vega  !", NAMES));
        assertNull(TypedCommandSubscriber.commandFor(null, NAMES));
    }

    @Test
    void aLineTheGameDidNotSendIsIgnored() {
        SendTextEvent failed = new SendTextEvent(JsonParser.parseString(
                "{\"timestamp\":\"2026-10-02T19:06:27Z\",\"event\":\"SendText\",\"To\":\"local\","
                        + "\"Message\":\"@Vega do my taxes\",\"Sent\":false}").getAsJsonObject());
        SendTextEvent sent = new SendTextEvent(JsonParser.parseString(
                "{\"timestamp\":\"2026-10-02T19:06:27Z\",\"event\":\"SendText\",\"To\":\"local\","
                        + "\"Message\":\"@Vega do my taxes\",\"Sent\":true}").getAsJsonObject());

        SendTextEvent unconfirmed = new SendTextEvent(JsonParser.parseString(
                "{\"timestamp\":\"2026-10-02T19:06:27Z\",\"event\":\"SendText\",\"To\":\"local\","
                        + "\"Message\":\"@Vega do my taxes\"}").getAsJsonObject());

        assertFalse(failed.wasSent());
        assertFalse(unconfirmed.wasSent(), "only an explicit Sent:true counts");
        assertTrue(sent.wasSent());
        assertEquals("@Vega do my taxes", sent.getMessage());
        assertEquals("local", sent.getTo());
    }
}
