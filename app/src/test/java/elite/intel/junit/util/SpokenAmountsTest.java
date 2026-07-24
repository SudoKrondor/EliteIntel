package elite.intel.junit.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.vega.SpokenAmounts;
import elite.intel.gameapi.journal.events.RedeemVoucherEvent;
import elite.intel.util.yaml.YamlFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The spoken amount is appended to YAML an event serialized itself, so it has to land as a sibling of the
 * numeric field rather than folding into whatever structure the event's last line opened.
 */
class SpokenAmountsTest {

    @Test
    void spokenLineParsesAsASiblingOfTheRawAmount() throws Exception {
        JsonObject json = JsonParser.parseString("""
                {"timestamp":"2026-07-22T18:00:00Z","event":"RedeemVoucher","Type":"bounty",
                 "Amount":1023309245,
                 "Factions":[{"Faction":"Sirius Corp","Amount":1023309245}]}
                """).getAsJsonObject();
        RedeemVoucherEvent event = new RedeemVoucherEvent(json);

        String payload = event.toYaml() + SpokenAmounts.yamlLine("amount", event.getAmount());

        Map<?, ?> parsed = YamlFactory.getMapper().readValue(payload, Map.class);
        assertEquals(1023309245L, ((Number) parsed.get("amount")).longValue(),
                "raw amount must survive, so the exact figure stays available on request");
        assertEquals("about one point zero two billion credits", parsed.get("amountSpoken"));
    }

    @Test
    void ruleNamesTheSuffixItReliesOn() {
        // The rule tells the model to look for fields ending in "Spoken"; yamlLine is what creates them.
        assertTrue(SpokenAmounts.RULE.contains("\"Spoken\""));
        assertTrue(SpokenAmounts.yamlLine("amount", 1).startsWith("\namountSpoken: "));
    }
}
