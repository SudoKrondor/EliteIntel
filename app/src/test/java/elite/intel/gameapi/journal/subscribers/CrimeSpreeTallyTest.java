package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonParser;
import elite.intel.gameapi.journal.events.CommitCrimeEvent;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the commander is told on the way out.
 *
 * <p>The run replayed here is the reported one: twenty crimes against Gaura Energy Incorporated in
 * Almeida-Vega Horticultural Biome, of which the gate announced three. He lifted off believing he was
 * worth a thousand credits. He was worth eighteen thousand, and owed a further thousand in fines.
 */
class CrimeSpreeTallyTest {

    /**
     * The reported run, verbatim: eighteen murders, one failure to submit to police, one data transfer.
     */
    private static final String[] THE_SPREE = {
            "16:04:20 onFoot_failureToSubmitToPolice Fine 500",
            "16:05:05 onFoot_murder Bounty 1000",
            "16:05:37 onFoot_murder Bounty 1000",
            "16:05:50 onFoot_murder Bounty 1000",
            "16:06:13 onFoot_murder Bounty 1000",
            "16:07:10 onFoot_murder Bounty 1000",
            "16:07:46 onFoot_murder Bounty 1000",
            "16:08:00 onFoot_murder Bounty 1000",
            "16:08:39 onFoot_murder Bounty 1000",
            "16:10:27 onFoot_murder Bounty 1000",
            "16:11:12 onFoot_murder Bounty 1000",
            "16:13:04 onFoot_murder Bounty 1000",
            "16:13:06 onFoot_murder Bounty 1000",
            "16:14:05 onFoot_murder Bounty 1000",
            "16:14:13 onFoot_murder Bounty 1000",
            "16:15:35 onFoot_dataTransfer Fine 500",
            "16:15:40 onFoot_murder Bounty 1000",
            "16:16:51 onFoot_murder Bounty 1000",
            "16:17:05 onFoot_murder Bounty 1000",
            "16:17:28 onFoot_murder Bounty 1000",
    };

    @BeforeEach
    @AfterEach
    void speakEnglish() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    private static CommitCrimeEvent crime(String line) {
        return crime(line, "Gaura Energy Incorporated");
    }

    private static CommitCrimeEvent crime(String line, String faction) {
        String[] parts = line.split(" ");
        String json = """
                { "timestamp":"2026-09-01T%sZ", "event":"CommitCrime", "CrimeType":"%s",
                  "Faction":"%s", "Victim":"Kacey Bartlett", "%s":%s }"""
                .formatted(parts[0], parts[1], faction, parts[2], parts[3]);
        return new CommitCrimeEvent(JsonParser.parseString(json).getAsJsonObject());
    }

    /**
     * The whole point of the tally: the gate spoke three times and left seventeen crimes unheard, so the
     * bill on the way out is news.
     */
    @Test
    void theSpreeIsAddedUpOnTheWayOut() {
        CrimeAlertGate gate = new CrimeAlertGate();
        CrimeSpreeTally tally = new CrimeSpreeTally();
        for (String line : THE_SPREE) {
            CommitCrimeEvent event = crime(line);
            tally.record(event, gate.admit(event));
        }

        assertEquals(
                List.of("Leaving with about eighteen thousand credits in bounties and one thousand credits in fines from Gaura Energy Incorporated."),
                tally.close());
    }

    /**
     * A crime the commander already heard in full is not worth repeating: the alert named the faction, the
     * offence and the amount, and saying the same figure again on the way out is noise of a politer kind.
     */
    @Test
    void aSingleCrimeIsNotSummarised() {
        CrimeSpreeTally tally = new CrimeSpreeTally();
        tally.record(crime("16:05:05 onFoot_murder Bounty 1000"), true);

        assertTrue(tally.close().isEmpty());
    }

    /**
     * Two offences that both spoke for themselves are likewise left alone, however different they were.
     */
    @Test
    void crimesThatAllSpokeForThemselvesAreLeftAlone() {
        CrimeSpreeTally tally = new CrimeSpreeTally();
        tally.record(crime("16:04:20 onFoot_failureToSubmitToPolice Fine 500"), true);
        tally.record(crime("16:05:05 onFoot_murder Bounty 1000"), true);

        assertTrue(tally.close().isEmpty());
    }

    /**
     * Bounties and fines are different instruments - one gets the commander shot at, the other does not -
     * so a spree of only one kind names only that kind.
     */
    @Test
    void bountiesAndFinesAreNotAddedTogether() {
        CrimeSpreeTally tally = new CrimeSpreeTally();
        tally.record(crime("16:05:05 onFoot_murder Bounty 1000"), true);
        tally.record(crime("16:05:37 onFoot_murder Bounty 1000"), false);

        assertEquals(List.of("Leaving with two thousand credits in bounties from Gaura Energy Incorporated."),
                tally.close());
    }

    /**
     * Two factions wanting the commander are two problems, payable in two places, so they are two
     * sentences - the larger debt first, because that is the one that decides where he can dock.
     */
    @Test
    void eachFactionIsOwedItsOwnSentence() {
        CrimeSpreeTally tally = new CrimeSpreeTally();
        tally.record(crime("16:05:05 onFoot_murder Bounty 1000", "Gaura Energy Incorporated"), true);
        tally.record(crime("16:05:37 onFoot_murder Bounty 1000", "Gaura Energy Incorporated"), false);
        tally.record(crime("16:06:00 onFoot_murder Bounty 25000", "Brothers of Urvantju"), true);
        tally.record(crime("16:06:30 onFoot_murder Bounty 25000", "Brothers of Urvantju"), false);

        assertEquals(
                List.of("Leaving with about fifty thousand credits in bounties from Brothers of Urvantju.",
                        "Leaving with two thousand credits in bounties from Gaura Energy Incorporated."),
                tally.close());
    }

    /**
     * Supercruise ends the spree whether or not anything was said, so the next settlement starts from zero
     * rather than inheriting the last one's bill.
     */
    @Test
    void theNextSettlementStartsFromZero() {
        CrimeSpreeTally tally = new CrimeSpreeTally();
        tally.record(crime("16:05:05 onFoot_murder Bounty 1000"), true);
        tally.record(crime("16:05:37 onFoot_murder Bounty 1000"), false);
        tally.close();

        tally.record(crime("17:05:05 onFoot_murder Bounty 1000"), true);
        tally.record(crime("17:05:37 onFoot_murder Bounty 1000"), false);

        assertEquals(List.of("Leaving with two thousand credits in bounties from Gaura Energy Incorporated."),
                tally.close());
    }

    /**
     * A crime carrying neither bounty nor fine costs the commander nothing and warns him of nothing, so it
     * buys no sentence however many of them there were.
     */
    @Test
    void aSpreeThatCostNothingSaysNothing() {
        CrimeSpreeTally tally = new CrimeSpreeTally();
        tally.record(crime("16:05:05 illegalCargo Bounty 0"), true);
        tally.record(crime("16:05:37 illegalCargo Bounty 0"), false);

        assertTrue(tally.close().isEmpty());
    }

    /**
     * The apostrophe trap: these templates carry placeholders, so an ASCII apostrophe in the French or
     * Italian prose would swallow the amount rather than be spoken.
     */
    @Test
    void theFrenchSummaryKeepsItsFigures() {
        SystemSession.getInstance().setLanguage(Language.FR);
        CrimeSpreeTally tally = new CrimeSpreeTally();
        tally.record(crime("16:04:20 onFoot_failureToSubmitToPolice Fine 500"), true);
        tally.record(crime("16:05:05 onFoot_murder Bounty 1000"), false);

        String spoken = tally.close().getFirst();
        assertTrue(spoken.contains("mille"), spoken);
        assertTrue(spoken.contains("cinq cents"), spoken);
    }
}
