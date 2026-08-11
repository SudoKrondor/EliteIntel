package elite.intel.util;

import elite.intel.gameapi.journal.events.dto.RankAndProgressDto;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The generic address the game and the LLM use ("Commander", ", pilot") is swapped for one of the
 * commander's own forms of address inside {@link StringUtls#sanitizeTts(String)}.
 * <p>
 * Regression cover: the swap used to be a chain of {@code String.replace} calls, each scanning what the
 * previous one had just inserted. A form of address that itself contains the word - the Federation ranks
 * "Post Commander" and "Lieutenant Commander" - was therefore substituted twice, and a Post Commander was
 * greeted as "Post Post Commander".
 * <p>
 * The commander here holds Federation rank 10 with no name on file, so exactly two forms of address exist:
 * the rank "Post Commander" and the honorific "Skipper". Every sentence must come back as one of the two,
 * which pins both the substitution and the fact that it happened only once.
 */
class StringUtlsAddressPersonalizationTest {

    /**
     * Enough draws that both forms of address are certain to appear (a fair coin misses one side with
     * probability 2^-200).
     */
    private static final int DRAWS = 200;

    @BeforeEach
    void commanderIsAnUnnamedPostCommander() {
        SystemSession.getInstance().setLanguage(Language.EN);
        PlayerSession playerSession = PlayerSession.getInstance();
        playerSession.setAlternativeName(null);
        playerSession.setPlayerName(null);
        RankAndProgressDto ranks = new RankAndProgressDto();
        ranks.setCombatRankEmpire(0);
        ranks.setCombatRankFederation(10); // Post Commander
        playerSession.setRankAndProgressDto(ranks);
    }

    @Test
    void aRankContainingTheWordCommanderIsSubstitutedOnlyOnce() {
        assertEquals(
                Set.of("Good to see you Post Commander.", "Good to see you Skipper."),
                sanitizeRepeatedly("Good to see you, Commander."),
                "the address must be replaced exactly once (was \"Post Post Commander\")");
    }

    @Test
    void anAddressWithoutACommaIsSubstitutedOnlyOnce() {
        assertEquals(
                Set.of("Welcome back Post Commander.", "Welcome back Skipper."),
                sanitizeRepeatedly("Welcome back Commander."));
    }

    @Test
    void anAddressOpeningTheSentenceKeepsItsPunctuation() {
        assertEquals(
                Set.of("Post Commander, we have arrived.", "Skipper, we have arrived."),
                sanitizeRepeatedly("Commander, we have arrived."));
    }

    @Test
    void pilotIsAnAddressOnlyAfterAComma() {
        assertEquals(
                Set.of("Understood Post Commander.", "Understood Skipper."),
                sanitizeRepeatedly("Understood, pilot."));
        // A bare "pilot" is a noun about someone else, not a way of addressing the commander.
        assertEquals(Set.of("The pilot ejected."), sanitizeRepeatedly("The pilot ejected."));
    }

    /**
     * Other commanders are not the commander: only the address itself is swapped, never the word inside a
     * plural or a possessive.
     */
    @Test
    void theWordIsLeftAloneWhenItIsNotAnAddress() {
        assertEquals(Set.of("Two Commanders are docked."), sanitizeRepeatedly("Two Commanders are docked."));
    }

    private static Set<String> sanitizeRepeatedly(String text) {
        Set<String> results = new HashSet<>();
        for (int draw = 0; draw < DRAWS; draw++) {
            results.add(StringUtls.sanitizeTts(text));
        }
        return results;
    }
}
