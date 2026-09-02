package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonParser;
import elite.intel.gameapi.journal.events.CommitCrimeEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * How much the companion says while the commander works a settlement.
 *
 * <p>The spree here is the reported one, replayed from the journal line for line: an on-foot assassination
 * contract in Almeida-Vega Horticultural Biome that produced twenty crimes against Gaura Energy
 * Incorporated in thirteen minutes, every one of which was announced.
 */
class CrimeAlertGateTest {

    /**
     * The reported run, verbatim: eighteen murders, one failure to submit to police, one data transfer.
     */
    private static final String[] THE_SPREE = {
            "16:04:20 onFoot_failureToSubmitToPolice",
            "16:05:05 onFoot_murder",
            "16:05:37 onFoot_murder",
            "16:05:50 onFoot_murder",
            "16:06:13 onFoot_murder",
            "16:07:10 onFoot_murder",
            "16:07:46 onFoot_murder",
            "16:08:00 onFoot_murder",
            "16:08:39 onFoot_murder",
            "16:10:27 onFoot_murder",
            "16:11:12 onFoot_murder",
            "16:13:04 onFoot_murder",
            "16:13:06 onFoot_murder",
            "16:14:05 onFoot_murder",
            "16:14:13 onFoot_murder",
            "16:15:35 onFoot_dataTransfer",
            "16:15:40 onFoot_murder",
            "16:16:51 onFoot_murder",
            "16:17:05 onFoot_murder",
            "16:17:28 onFoot_murder",
    };

    private static CommitCrimeEvent crime(String timeOfDay, String crimeType) {
        return crime(timeOfDay, crimeType, "Gaura Energy Incorporated");
    }

    private static CommitCrimeEvent crime(String timeOfDay, String crimeType, String faction) {
        String json = """
                { "timestamp":"2026-09-01T%sZ", "event":"CommitCrime", "CrimeType":"%s",
                  "Faction":"%s", "Victim":"Kacey Bartlett", "Bounty":1000 }"""
                .formatted(timeOfDay, crimeType, faction);
        return new CommitCrimeEvent(JsonParser.parseString(json).getAsJsonObject());
    }

    private static List<String> announced(CrimeAlertGate gate, String[] spree) {
        List<String> spoken = new ArrayList<>();
        for (String line : spree) {
            String[] parts = line.split(" ");
            if (gate.admit(crime(parts[0], parts[1]))) spoken.add(line);
        }
        return spoken;
    }

    /**
     * Twenty crimes, three pieces of news. The commander is told he is a murderer once, not eighteen times.
     */
    @Test
    void aSettlementSpreeIsOneAlertPerCrime() {
        assertEquals(
                List.of("16:04:20 onFoot_failureToSubmitToPolice",
                        "16:05:05 onFoot_murder",
                        "16:15:35 onFoot_dataTransfer"),
                announced(new CrimeAlertGate(), THE_SPREE));
    }

    /**
     * The gap that decided the window. Twice during the run the commander went nearly two minutes between
     * kills while crossing the settlement, and neither pause is the end of anything.
     */
    @Test
    void aPauseToFindTheNextTargetDoesNotReArmTheAlert() {
        CrimeAlertGate gate = new CrimeAlertGate();

        assertTrue(gate.admit(crime("16:11:12", "onFoot_murder")));
        assertFalse(gate.admit(crime("16:13:04", "onFoot_murder")));
    }

    /**
     * A later settlement is a new spree, not a continuation of the last one, so it is announced again.
     */
    @Test
    void theAlertReArmsOnceTheCommanderStops() {
        CrimeAlertGate gate = new CrimeAlertGate();

        assertTrue(gate.admit(crime("16:05:05", "onFoot_murder")));
        assertFalse(gate.admit(crime("16:07:10", "onFoot_murder")));
        assertTrue(gate.admit(crime("16:40:00", "onFoot_murder")));
    }

    /**
     * Each kind of offence is its own piece of news, even inside one spree - the fine and the data transfer
     * in the reported run both spoke while the murders were being held quiet.
     */
    @Test
    void adifferentCrimeIsStillWorthHearing() {
        CrimeAlertGate gate = new CrimeAlertGate();

        assertTrue(gate.admit(crime("16:05:05", "onFoot_murder")));
        assertTrue(gate.admit(crime("16:05:10", "onFoot_theft")));
        assertFalse(gate.admit(crime("16:05:15", "onFoot_murder")));
    }

    /**
     * A second faction putting a price on the commander's head is news however alike the offences are: the
     * bounty is owed to someone new, in somewhere new, and the first one being recent says nothing about it.
     */
    @Test
    void aSecondFactionIsItsOwnAlert() {
        CrimeAlertGate gate = new CrimeAlertGate();

        assertTrue(gate.admit(crime("16:05:05", "onFoot_murder", "Gaura Energy Incorporated")));
        assertTrue(gate.admit(crime("16:05:20", "onFoot_murder", "LHS 3931 Blue Federal Group")));
        assertFalse(gate.admit(crime("16:05:40", "onFoot_murder", "Gaura Energy Incorporated")));
    }

    /**
     * The gate holds one entry per spree, not one per victim: the reported run would otherwise have grown a
     * map entry every time the commander pulled the trigger.
     */
    @Test
    void aFinishedSpreeIsForgotten() {
        CrimeAlertGate gate = new CrimeAlertGate();
        announced(gate, THE_SPREE);

        // Long after the settlement, every crime of the run is news again.
        assertTrue(gate.admit(crime("18:00:00", "onFoot_murder")));
        assertTrue(gate.admit(crime("18:00:01", "onFoot_dataTransfer")));
    }
}
