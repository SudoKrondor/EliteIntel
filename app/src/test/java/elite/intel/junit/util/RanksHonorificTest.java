package elite.intel.junit.util;

import elite.intel.gameapi.journal.events.dto.RankAndProgressDto;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.Ranks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Honorific and highest-rank resolution in {@link Ranks}. The Empire/Federation arguments are the navy
 * rank indices (0-14); both forms are derived from whichever superpower rank is higher.
 * <p>
 * Regression cover for three bugs:
 * - getPlayerHonorific() resolved a tie with {@code imperial >= federation}, so a commander holding the
 * same tier in both navies (e.g. Count / Lieutenant Commander) was only ever addressed as an Imperial.
 * - the honorific maps are keyed by English rank names but were queried with the localized name, so in
 * every non-English locale the honorific resolved to null and vanished from the ways to address the
 * commander.
 * - getPlayerHonorific() always queried the Federation honorific map, so an Imperial-highest rank
 * (e.g. Lord) resolved to null instead of "My Lord".
 */
class RanksHonorificTest {

    /**
     * Enough draws that a side which can be picked is certain to appear (a stuck resolver fails at once,
     * a fair coin misses one side with probability 2^-200).
     */
    private static final int DRAWS = 200;

    @BeforeEach
    void forceEnglishLocale() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    /**
     * Restore here, not at the end of each test, so a failed assertion cannot leak a foreign locale onward.
     */
    @AfterEach
    void restoreEnglishLocale() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @Test
    void honorific_imperialHighest_resolvesImperialHonorific() {
        // Empire=6 ("Lord") > Federation=0 → "My Lord" (this was null before the fix)
        assertEquals("My Lord", Ranks.getHonorific(6, 0));
    }

    @Test
    void honorific_imperialKing_resolvesYourMajesty() {
        assertEquals("Your Majesty", Ranks.getHonorific(14, 0));
    }

    @Test
    void honorific_federationHighest_resolvesFederationHonorific() {
        // Federation=14 ("Admiral") > Empire=3 (was null before the fix)
        assertEquals("Admiral", Ranks.getHonorific(3, 14));
    }

    /**
     * The reported bug: rank 9 in both navies (Count and Lieutenant Commander) is not an Imperial win.
     * Neither navy outranks the other at the same tier, so both honorifics must be reachable.
     */
    @Test
    void honorific_equalRanks_drawsFromBothNavies() {
        Set<String> drawn = new HashSet<>();
        for (int draw = 0; draw < DRAWS; draw++) {
            drawn.add(Ranks.getHonorific(9, 9));
        }
        assertEquals(Set.of("My Lord", "XO"), drawn,
                "equal navy ranks must be addressed by either navy's honorific");
    }

    @Test
    void highestRank_equalRanks_drawsFromBothNavies() {
        Set<String> drawn = new HashSet<>();
        for (int draw = 0; draw < DRAWS; draw++) {
            drawn.add(Ranks.getHighestRankAsString(9, 9));
        }
        assertEquals(Set.of("Count", "Lieutenant Commander"), drawn,
                "equal navy ranks must be named by either navy's rank");
    }

    @Test
    void highestRank_resolvesTheHigherNavy() {
        assertEquals("Count", Ranks.getHighestRankAsString(9, 3));
        assertEquals("Lieutenant Commander", Ranks.getHighestRankAsString(3, 9));
    }

    /**
     * The honorific maps are keyed by the English rank names the journal reports. Feeding them the
     * localized name missed in every non-English locale, which cost those commanders the honorific
     * entirely - English cannot catch that, because there the two names are the same string.
     */
    @Test
    void honorific_isTranslatedNotDropped() {
        SystemSession.getInstance().setLanguage(Language.RU);
        assertEquals("Милорд", Ranks.getHonorific(9, 0));
        assertEquals("Старпом", Ranks.getHonorific(0, 9));

        SystemSession.getInstance().setLanguage(Language.DE);
        assertEquals("Mein Herr", Ranks.getHonorific(9, 0));
    }

    /**
     * The two Federation tiers the journal names "... Commander" are addressed by billet rather
     * than by rank. "Commander" is what the game calls every pilot regardless of service record,
     * so reusing it here told the commander nothing about the rank they had earned.
     */
    @Test
    void honorific_federationCommanderTiers_areAddressedByBillet() {
        assertEquals("XO", Ranks.getHonorific(0, 9));
        assertEquals("Skipper", Ranks.getHonorific(0, 10));
    }

    /**
     * Post Captain and above keep their own honorifics, so the billet titles must not have leaked
     * upward into the ranks that already read as ranks.
     */
    @Test
    void honorific_federationRanksAboveCommanderAreUnchanged() {
        assertEquals("Captain", Ranks.getHonorific(0, 11));
        assertEquals("Admiral", Ranks.getHonorific(0, 12));
    }

    @Test
    void honorific_imperialHighest_isNeverNull() {
        // The original bug surfaced as a null honorific for every Imperial-highest rank.
        for (int imperial = 5; imperial <= 14; imperial++) {
            assertNotNull(Ranks.getHonorific(imperial, 0),
                    "Imperial rank index " + imperial + " should resolve an honorific");
        }
    }

    /**
     * The rank numbers default to -1 until the game reports them, and the Federation navy has no
     * honorific of its own for the unranked tier. Neither may reach the commander as a null address.
     */
    @Test
    void honorific_unknownOrUnrankedFallsBackToCommander() {
        assertEquals("Commander", Ranks.getHonorific(-1, -1));
        assertEquals("Commander", Ranks.getHonorific(null, null));
        for (int draw = 0; draw < DRAWS; draw++) {
            assertEquals("Commander", Ranks.getHonorific(0, 0));
        }
    }

    @Test
    void highestRank_unknownRankIsDropped() {
        assertNull(Ranks.getHighestRankAsString(-1, -1));
        assertNull(Ranks.getHighestRankAsString(null, null));
    }

    @Test
    void dtoHonorific_isResolvedDynamicallyFromStoredRankIndices() {
        // The honorific is no longer a captured string; the DTO derives it from the navy
        // rank indices at read time, so it tracks the active UI language.
        RankAndProgressDto dto = new RankAndProgressDto();
        dto.setCombatRankEmpire(6);     // Lord
        dto.setCombatRankFederation(0);

        SystemSession.getInstance().setLanguage(Language.EN);
        String english = dto.getHonorific();
        SystemSession.getInstance().setLanguage(Language.UK);
        String ukrainian = dto.getHonorific();

        assertEquals("My Lord", english);
        assertNotEquals(english, ukrainian, "honorific should follow the active language");
    }
}
