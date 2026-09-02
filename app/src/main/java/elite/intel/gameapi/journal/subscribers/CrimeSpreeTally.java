package elite.intel.gameapi.journal.subscribers;

import elite.intel.gameapi.journal.events.CommitCrimeEvent;
import elite.intel.util.TTSFriendlyNumberConverter;

import java.util.*;

import static elite.intel.util.StringUtls.localizedEvent;

/**
 * What the commander owes for the place he is leaving.
 *
 * <p>WHY: {@link CrimeAlertGate} keeps a spree quiet after the first alert, which is right for the channel
 * and wrong for the ledger - the reported run announced one murder and said nothing about the seventeen
 * that followed it, so the commander flew away believing he was worth a thousand credits when he was worth
 * eighteen thousand. The alert says what is happening; this says what it came to.
 *
 * <p>The spree ends at {@code SupercruiseEntry}, because that is the moment the commander stops being
 * somewhere and starts travelling. A liftoff is not the end of anything - his own journal has him touching
 * down outside a settlement, lifting off a minute later and landing on its pad - and neither is boarding
 * the ship. Supercruise is also what makes this one rule instead of two: a crime committed from the
 * cockpit is left behind the same way as one committed on foot.
 *
 * <p>Debts are kept per faction because that is how the game keeps them: two factions wanting the
 * commander are two problems, payable in two places.
 */
final class CrimeSpreeTally {

    private final Map<String, Debt> owed = new LinkedHashMap<>();

    /**
     * Adds a crime to the running total, and remembers whether the commander actually heard about it.
     *
     * <p>The flag is what keeps the summary from repeating the alerts: a lone crime announces itself in
     * full, and saying the same figure again on the way out is noise of a politer kind.
     */
    synchronized void record(CommitCrimeEvent event, boolean announced) {
        owed.computeIfAbsent(faction(event), faction -> new Debt()).add(event, announced);
    }

    /**
     * Closes the books and returns what is worth saying, biggest debt first - usually nothing, sometimes
     * one sentence, one per faction when the commander has annoyed more than one.
     *
     * <p>Always clears, whether or not anything is spoken: the spree is over either way.
     */
    synchronized List<String> close() {
        List<String> reckoning = new ArrayList<>();
        owed.entrySet().stream()
                .filter(entry -> entry.getValue().worthReporting())
                .sorted(Comparator.comparingLong((Map.Entry<String, Debt> entry) -> entry.getValue().total()).reversed())
                .forEach(entry -> reckoning.add(entry.getValue().spoken(entry.getKey())));
        owed.clear();
        return reckoning;
    }

    private static String faction(CommitCrimeEvent event) {
        return event.getFaction() == null ? "" : event.getFaction();
    }

    /**
     * What one faction is owed, and how much of it the commander was told about as it happened.
     */
    private static final class Debt {

        private long bounties;
        private long fines;
        private int unannounced;

        void add(CommitCrimeEvent event, boolean announced) {
            bounties += event.getBounty();
            fines += event.getFine();
            if (!announced) unannounced++;
        }

        /**
         * Only a spree that went partly unheard is worth summarising, and only if it left something to pay:
         * a crime carrying neither bounty nor fine costs the commander nothing and warns him of nothing.
         */
        boolean worthReporting() {
            return unannounced > 0 && total() > 0;
        }

        long total() {
            return bounties + fines;
        }

        String spoken(String faction) {
            if (fines == 0) {
                return localizedEvent("event.crime.spree.bounties", faction, credits(bounties));
            }
            if (bounties == 0) {
                return localizedEvent("event.crime.spree.fines", faction, credits(fines));
            }
            return localizedEvent("event.crime.spree.both", faction, credits(bounties), credits(fines));
        }

        /**
         * Spelled out before it reaches the template, for the same reason the individual alert spells its
         * figure out: a bare number is grouped by the reader's locale and read aloud as a decimal.
         */
        private static String credits(long amount) {
            return TTSFriendlyNumberConverter.formatCreditsForSpeech(amount);
        }
    }
}
