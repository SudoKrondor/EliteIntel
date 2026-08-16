package elite.intel.gameapi.journal.subscribers;

import elite.intel.util.Ranks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the bridge between the two halves of a target's rank. {@code ShipTargeted} reports the
 * target's combat rank by name ({@code "Dangerous"}), while {@link Ranks#getCombatRankMap()} is keyed
 * by the rank number, so the subscriber holds the names in rank order and looks the number up by
 * index.
 *
 * <p>Nothing in the type system ties those two orderings together: reorder or insert into the rank map
 * and every target would be announced one rank off, silently, which is the exact bug this bridge
 * replaced. The map used to be the mercenary ladder, so a Dangerous pilot was announced as
 * "Entrepreneur" and a Master as "Warrior". These tests turn that drift into a red build.
 */
class PilotCombatRankTest {

    /**
     * Every rank the journal can report in {@code PilotRank}, paired with the number it stands for.
     */
    @ParameterizedTest(name = "[{index}] {0} is rank {1}")
    @CsvSource({
            "Harmless, 0",
            "Mostly Harmless, 1",
            "Novice, 2",
            "Competent, 3",
            "Expert, 4",
            "Master, 5",
            "Dangerous, 6",
            "Deadly, 7",
            "Elite, 8",
            "Elite I, 9",
            "Elite II, 10",
            "Elite III, 11",
            "Elite IV, 12",
            "Elite V, 13"
    })
    void eachJournalRankNameResolvesToItsOwnRankNumber(String pilotRank, int rankNumber) {
        String expected = Ranks.getCombatRankMap().get(rankNumber);

        assertNotNull(expected, "the combat rank map has no entry for rank " + rankNumber);
        assertEquals(expected, ShipTargetedEventSubscriber.localizedCombatRank(pilotRank));
    }

    /**
     * The index lookup only stays honest while the two lists are the same length: an entry appended to
     * the rank map without a name beside it would go unnoticed by the pairs above.
     */
    @Test
    @DisplayName("the combat rank map holds exactly the ranks the journal can report")
    void theRankMapAndTheJournalNamesAreTheSameLength() {
        Map<Integer, String> combatRanks = Ranks.getCombatRankMap();

        assertEquals(14, combatRanks.size(),
                "a rank was added to or removed from the combat ladder; the names in "
                        + "ShipTargetedEventSubscriber must follow it");
    }

    /**
     * A rank we cannot place must reach the caller as {@code null} so the announcement says "rank
     * unknown". Guessing at it would put a wrong rank in the commander's ear during a fight, which is
     * worse than admitting we do not know.
     */
    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"", "   ", "Elite VI", "Gladiator", "Entrepreneur", "harmless"})
    void anUnknownRankIsNotGuessedAt(String pilotRank) {
        assertNull(ShipTargetedEventSubscriber.localizedCombatRank(pilotRank));
    }

    @Test
    @DisplayName("a target the journal gave no rank for is rank unknown")
    void aMissingRankIsNull() {
        assertNull(ShipTargetedEventSubscriber.localizedCombatRank(null));
    }

    /**
     * The journal is the only source of these names, and it does not pad them, but a transcript-shaped
     * stray space must not cost the commander the rank.
     */
    @Test
    @DisplayName("surrounding whitespace does not lose the rank")
    void aPaddedRankStillResolves() {
        assertEquals(Ranks.getCombatRankMap().get(7),
                ShipTargetedEventSubscriber.localizedCombatRank("  Deadly  "));
    }

    /**
     * The ranks are what the commander hears, so they must be distinct: two ranks sharing a word would
     * make "Deadly" and "Elite" the same announcement.
     */
    @Test
    @DisplayName("no two combat ranks say the same thing")
    void everyRankReadsDifferently() {
        List<String> spoken = Ranks.getCombatRankMap().values().stream().toList();

        assertEquals(spoken.size(), spoken.stream().distinct().count(), spoken.toString());
    }
}