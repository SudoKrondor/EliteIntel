package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.gameapi.journal.events.CommitCrimeEvent;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The crime alert as the commander hears it. Every line here is a journal entry from the reported session:
 * an on-foot assassination contract worked through a settlement, which is the case where the game names the
 * victim without a localised sibling and tags the crime {@code onFoot_murder}.
 *
 * <p>What was actually spoken, eighteen times: "Warning! Faction Gaura Energy Incorporated issued bounty of
 * 1,000 credits for onFoot_murder against null."
 */
class CrimeReportPhraseTest {

    @BeforeEach
    @AfterEach
    void speakEnglish() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    private static CommitCrimeEvent event(String journalLine) {
        return new CommitCrimeEvent(JsonParser.parseString(journalLine).getAsJsonObject());
    }

    /**
     * The reported line. The tag becomes a word, the victim is the name the game gave us, and the bounty is
     * spelled out so no separator reaches the voice.
     */
    @Test
    void anOnFootMurderNamesTheCrimeAndTheVictim() {
        assertEquals(
                "Warning! Faction Gaura Energy Incorporated issued a bounty of one thousand credits for murder against Kacey Bartlett.",
                CrimeReportPhrase.of(event("""
                        { "timestamp":"2026-09-01T16:05:05Z", "event":"CommitCrime", "CrimeType":"onFoot_murder",
                          "Faction":"Gaura Energy Incorporated", "Victim":"Kacey Bartlett", "Bounty":1000 }""")));
    }

    /**
     * The same settlement run: a data transfer has no victim at all, and the old sentence ended in "null"
     * because it read the missing field anyway.
     */
    @Test
    void aCrimeWithNoVictimDoesNotInventOne() {
        String phrase = CrimeReportPhrase.of(event("""
                { "timestamp":"2026-09-01T16:04:22Z", "event":"CommitCrime", "CrimeType":"onFoot_dataTransfer",
                  "Faction":"Gaura Energy Incorporated", "Fine":500 }"""));

        assertEquals("Warning! Faction Gaura Energy Incorporated issued a fine of five hundred credits for data transfer.", phrase);
        assertFalse(phrase.contains("null"));
    }

    /**
     * A fine is not a bounty. Reading only {@code Bounty} announced this one as a bounty of zero credits.
     */
    @Test
    void aFineIsAnnouncedAsAFine() {
        assertEquals(
                "Warning! Faction Gaura Energy Incorporated issued a fine of five hundred credits for failure to submit to police against Cara Dudley.",
                CrimeReportPhrase.of(event("""
                        { "timestamp":"2026-09-01T16:11:20Z", "event":"CommitCrime", "CrimeType":"onFoot_failureToSubmitToPolice",
                          "Faction":"Gaura Energy Incorporated", "Victim":"Cara Dudley", "Fine":500 }""")));
    }

    /**
     * A ship-side crime, where the game does supply a localised victim beside the token. The token is what
     * must never be spoken.
     */
    @Test
    void aLocalisedVictimIsPreferredToItsToken() {
        String phrase = CrimeReportPhrase.of(event("""
                { "timestamp":"2026-09-01T12:00:00Z", "event":"CommitCrime", "CrimeType":"murder",
                  "Faction":"Gaura Energy Incorporated", "Victim":"$npc_name_decorate:#name=Boris;",
                  "Victim_Localised":"Boris", "Bounty":24000 }"""));

        assertEquals("Warning! Faction Gaura Energy Incorporated issued a bounty of about twenty-four thousand credits for murder against Boris.", phrase);
        assertFalse(phrase.contains("$"));
    }

    /**
     * A token with no localised sibling is not a fault we have seen, but it is the one shape that would put
     * an identifier back in the commander's ear, so it is unwrapped rather than spoken.
     */
    @Test
    void aBareTokenVictimIsUnwrappedNotSpoken() {
        String phrase = CrimeReportPhrase.of(event("""
                { "timestamp":"2026-09-01T12:00:00Z", "event":"CommitCrime", "CrimeType":"assault",
                  "Faction":"Gaura Energy Incorporated", "Victim":"$npc_name_decorate:#name=Boris;", "Bounty":400 }"""));

        assertEquals("Warning! Faction Gaura Energy Incorporated issued a bounty of four hundred credits for assault against Boris.", phrase);
    }

    /**
     * A crime carrying neither penalty still gets a whole sentence rather than "a bounty of zero credits".
     */
    @Test
    void aCrimeWithNoPenaltyIsStillASentence() {
        assertEquals(
                "Warning! Faction Gaura Energy Incorporated recorded illegal cargo.",
                CrimeReportPhrase.of(event("""
                        { "timestamp":"2026-09-01T12:00:00Z", "event":"CommitCrime", "CrimeType":"illegalCargo",
                          "Faction":"Gaura Energy Incorporated" }""")));
    }

    /**
     * The tag is unfolded mechanically, so a crime type Frontier adds tomorrow is spoken as words on the day
     * it appears rather than as an identifier.
     */
    @Test
    void anUnknownCrimeTagIsStillSpokenAsWords() {
        JsonObject json = JsonParser.parseString("""
                { "timestamp":"2026-09-01T12:00:00Z", "event":"CommitCrime", "CrimeType":"onFoot_someBrandNewOffence",
                  "Faction":"Gaura Energy Incorporated", "Bounty":100 }""").getAsJsonObject();

        assertEquals("Warning! Faction Gaura Energy Incorporated issued a bounty of one hundred credits for some brand new offence.",
                CrimeReportPhrase.of(new CommitCrimeEvent(json)));
    }
}
