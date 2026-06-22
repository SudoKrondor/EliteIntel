package elite.intel.junit.util;

import elite.intel.gameapi.journal.events.dto.RankAndProgressDto;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.Ranks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Honorific resolution in {@link Ranks}. The Empire/Federation arguments are the navy rank
 * indices (0-14); the honorific is derived from whichever superpower rank is higher.
 * <p>
 * Regression cover for two bugs:
 * - getPlayerHonorific() always queried the Federation honorific map, so an Imperial-highest
 * rank (e.g. Lord) resolved to null instead of "My Lord".
 * - getHonorific()'s federation-highest branch queried the Imperial honorific map with a
 * Federation rank name, resolving to null instead of the Federation honorific.
 */
class RanksHonorificTest {

    @BeforeEach
    void forceEnglishLocale() {
        // Honorific maps are keyed by English rank names; assert against the English bundle.
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @Test
    void playerHonorific_imperialHighest_resolvesImperialHonorific() {
        // Empire=6 ("Lord") > Federation=0 → "My Lord" (this was null before the fix)
        assertEquals("My Lord", Ranks.getPlayerHonorific(6, 0));
    }

    @Test
    void playerHonorific_imperialKing_resolvesYourMajesty() {
        // Empire=14 ("King") > Federation=0
        assertEquals("Your Majesty", Ranks.getPlayerHonorific(14, 0));
    }

    @Test
    void playerHonorific_federationHighest_resolvesFederationHonorific() {
        // Federation=14 ("Admiral") > Empire=0
        assertEquals("Admiral", Ranks.getPlayerHonorific(0, 14));
    }

    @Test
    void playerHonorific_federationPostCommander_resolvesCommander() {
        // Federation=10 ("Post Commander") > Empire=0 → "Commander"
        assertEquals("Commander", Ranks.getPlayerHonorific(0, 10));
    }

    @Test
    void playerHonorific_imperialHighest_isNeverNull() {
        // The original bug surfaced as a null honorific for every Imperial-highest rank.
        for (int imperial = 5; imperial <= 14; imperial++) {
            assertNotNull(Ranks.getPlayerHonorific(imperial, 0),
                    "Imperial rank index " + imperial + " should resolve an honorific");
        }
    }

    @Test
    void getHonorific_imperialHighest_resolvesImperialHonorific() {
        // Empire=6 ("Lord") > Federation=3
        assertEquals("My Lord", Ranks.getHonorific(6, 3));
    }

    @Test
    void getHonorific_federationHighest_resolvesFederationHonorific() {
        // Federation=14 ("Admiral") > Empire=3 (was null before the fix)
        assertEquals("Admiral", Ranks.getHonorific(3, 14));
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
        SystemSession.getInstance().setLanguage(Language.EN);

        assertEquals("My Lord", english);
        assertNotEquals(english, ukrainian, "honorific should follow the active language");
    }
}