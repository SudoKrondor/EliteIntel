package elite.intel.community;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.db.dao.MassacreMissionDao.Shareable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The upload format the nightly job reads. Its decoder (elite-intel-nightly, {@code PairsPayload}) drops what it
 * cannot accept, so these pin what this side must never send.
 */
class PairsPayloadTest {

    private static Shareable entry(String provider, String faction, String target, String lastSeen) {
        return new Shareable(provider, "Bruce Dock", faction, target, target + " Raiders", 0, 1, 2, 0, lastSeen);
    }

    @Test
    void anEntryCarriesTheShortKeysTheNightlyJobReads() {
        JsonObject o = JsonParser.parseString(PairsPayload.encode(List.of(entry("Bean", "Earls of Bean", "Bellite", "2026-11-13"))))
                .getAsJsonArray().get(0).getAsJsonObject();

        assertEquals("Bean", o.get("ps").getAsString());
        assertEquals("Bruce Dock", o.get("st").getAsString());
        assertEquals("Earls of Bean", o.get("pf").getAsString());
        assertEquals("Bellite", o.get("ts").getAsString());
        assertEquals("Bellite Raiders", o.get("tf").getAsString());
        assertEquals("[0,1,2,0]", o.get("r").toString());
        assertEquals("2026-11-13", o.get("d").getAsString());
    }

    @Test
    void entriesTheNightlyJobWouldRefuseAreNotSent() {
        String tooLong = "x".repeat(65);
        String payload = PairsPayload.encode(List.of(
                entry("Bean", "Earls of Bean", "Bellite", null),
                entry(tooLong, "Earls of Bean", "Bellite", null),
                entry("Bean", "Earls of Bean", "bean", null),
                new Shareable("Bean", null, null, "Bellite", "Raiders", 0, 0, 0, 0, null)));

        assertEquals(1, JsonParser.parseString(payload).getAsJsonArray().size());
    }

    @Test
    void aDateThatIsNotADayIsLeftOutButTheEntryKept() {
        JsonObject o = JsonParser.parseString(PairsPayload.encode(List.of(entry("Bean", "F", "Bellite", "2026-11-13T20:01:02Z"))))
                .getAsJsonArray().get(0).getAsJsonObject();

        assertFalse(o.has("d"));
    }

    @Test
    void theSameLedgerAlwaysEncodesTheSameWay() {
        List<Shareable> one = List.of(entry("A", "F", "Z", null), entry("B", "G", "Y", null));
        List<Shareable> two = List.of(entry("B", "G", "Y", null), entry("A", "F", "Z", null));

        assertEquals(PairsPayload.encode(one), PairsPayload.encode(two));
        assertEquals(PairsPayload.digest(PairsPayload.encode(one)), PairsPayload.digest(PairsPayload.encode(two)));
    }

    @Test
    void aLedgerTooBigForTheLimitKeepsTheMostRecentlySeen() {
        List<Shareable> many = new ArrayList<>();
        for (int i = 0; i < 1500; i++) {
            many.add(entry("Provider " + i, "Faction " + i, "Target " + i, "2026-%02d-01".formatted(1 + i % 12)));
        }
        many.add(entry("Newest", "F", "Z", "2027-01-01"));

        String payload = PairsPayload.encode(many);
        JsonArray kept = JsonParser.parseString(payload).getAsJsonArray();

        assertTrue(payload.length() <= PairsPayload.MAX_LENGTH);
        assertTrue(kept.size() < many.size());
        assertTrue(payload.contains("\"Newest\""));
    }
}
