package elite.intel.gameapi.journal.subscribers;

import elite.intel.gameapi.journal.events.CommitCrimeEvent;
import elite.intel.util.TTSFriendlyNumberConverter;

import static elite.intel.util.StringUtls.localizedEvent;

/**
 * The spoken sentence for a crime the commander has just committed.
 *
 * <p>WHY a class of its own: the announcement is built from four journal fields and every one of them can
 * be absent or unspeakable. The reported case was an on-foot assassination contract, where the game names
 * a settlement worker in {@code Victim} with no {@code Victim_Localised} beside it and tags the crime
 * {@code onFoot_murder} - so the commander heard "a bounty of 1,000 credits for onFoot_murder against
 * null" after every kill. In the same settlement run, a data transfer has no victim at all and a failure
 * to submit to police carries a fine where the code only ever read a bounty.
 *
 * <p>The crime type is a game identifier, not prose, so it is unfolded into words here rather than spoken
 * raw; like other game nouns it stays in the game's own English while the sentence around it is localized.
 */
final class CrimeReportPhrase {

    private CrimeReportPhrase() {
    }

    /**
     * The crime as it may be announced: always a whole sentence, never a field name and never "null".
     */
    static String of(CommitCrimeEvent event) {
        String faction = event.getFaction() == null ? "" : event.getFaction();
        String crime = readableCrime(event.getCrimeType());
        String victim = event.getSpokenVictim();

        if (event.getBounty() > 0) {
            return penalty("event.crime.bountyIssued", faction, event.getBounty(), crime, victim);
        }
        if (event.getFine() > 0) {
            return penalty("event.crime.fineIssued", faction, event.getFine(), crime, victim);
        }
        return victim == null
                ? localizedEvent("event.crime.recorded.noVictim", faction, crime)
                : localizedEvent("event.crime.recorded", faction, crime, victim);
    }

    /**
     * The amount is spelled out before it reaches the template: {@link java.text.MessageFormat} groups a
     * bare number by the reader's locale, and "1,000" - "1.000" in German and Italian - is read aloud by
     * the voice as a decimal. The spelled-out form already carries the word "credits", so the templates
     * name no currency of their own.
     */
    private static String penalty(String key, String faction, long amount, String crime, String victim) {
        String spokenAmount = TTSFriendlyNumberConverter.formatCreditsForSpeech(amount);
        return victim == null
                ? localizedEvent(key + ".noVictim", faction, spokenAmount, crime)
                : localizedEvent(key, faction, spokenAmount, crime, victim);
    }

    /**
     * A journal crime tag as words: {@code onFoot_murder} is "murder",
     * {@code onFoot_failureToSubmitToPolice} is "failure to submit to police".
     *
     * <p>The {@code onFoot_} prefix is dropped because it names where the crime happened, not what it was,
     * and the commander already knows he is on foot. Everything else is unfolded mechanically rather than
     * from a table, so a crime type the game adds later is still spoken as words on the day it appears.
     */
    private static String readableCrime(String crimeType) {
        if (crimeType == null || crimeType.isBlank()) return localizedEvent("event.crime.unspecified");

        String tag = crimeType.startsWith("onFoot_") ? crimeType.substring("onFoot_".length()) : crimeType;
        StringBuilder words = new StringBuilder(tag.length() + 8);
        for (int i = 0; i < tag.length(); i++) {
            char c = tag.charAt(i);
            if (c == '_') {
                words.append(' ');
            } else {
                boolean startsWord = i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(tag.charAt(i - 1));
                if (startsWord) words.append(' ');
                words.append(Character.toLowerCase(c));
            }
        }
        String readable = words.toString().replaceAll("\\s+", " ").trim();
        return readable.isEmpty() ? localizedEvent("event.crime.unspecified") : readable;
    }
}
