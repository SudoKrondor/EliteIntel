package elite.intel.junit.util;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.Ranks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards against i18n key leaks in {@link Ranks}.
 * <p>
 * {@code EventsTextProvider.resolveText()} returns the raw dotted key when it is absent from both
 * the active locale and the English fallback bundle, so a typo'd or renamed key
 * (e.g. {@code ranks.federation.postcaptain} vs {@code ranks.federation.postCaptain}) is announced
 * verbatim through TTS instead of the translated rank. Every value the rank maps produce must be a
 * resolved display string, never a bundle key.
 * <p>
 * English completeness is sufficient: {@code resolveText} falls back to the English bundle, so a
 * key present in English never leaks in any locale.
 */
class RanksKeyResolutionTest {

    /**
     * An unresolved key looks like {@code ranks.federation.postcaptain} / {@code rank.trade.tycoon}:
     * lowercase first segment, dot-separated, no whitespace. Real display strings ("Post Captain",
     * "My Lord", "Elite I") never match this.
     */
    private static final Pattern UNRESOLVED_KEY = Pattern.compile("[a-z][a-z0-9]*(\\.[a-zA-Z0-9]+)+");

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
    void integerRankMapsResolveToDisplayText() {
        assertAllResolved("imperial", Ranks.getImperialRankMap().values());
        assertAllResolved("federation", Ranks.getFederationRankMap().values());
        assertAllResolved("combat", Ranks.getCombatRankMap().values());
        assertAllResolved("exobiology", Ranks.getExobiologyRankMap().values());
        assertAllResolved("exploration", Ranks.getExplorationRankMap().values());
        assertAllResolved("trade", Ranks.getTradeRankMap().values());
        assertAllResolved("mercenary", Ranks.getMercenaryRankMap().values());
    }

    @Test
    void federationRankIndicesMapToTheCorrectRank() {
        // The journal rank number indexes this map directly. Rank 9 is Lieutenant Commander; a prior
        // copy-paste made it announce "Lieutenant" (index 8's value), leaving Lieutenant Commander
        // unreachable. Pin the boundaries so the ordered list cannot silently drift again.
        var federation = Ranks.getFederationRankMap();
        assertEquals("Commander", federation.get(0));            // unranked tier
        assertEquals("Lieutenant", federation.get(8));
        assertEquals("Lieutenant Commander", federation.get(9)); // regression: was "Lieutenant"
        assertEquals("Admiral", federation.get(14));             // highest tier, list end
    }

    @Test
    void honorificMapsResolveToDisplayText() {
        assertAllResolved("imperialHonorific", Ranks.getImperialHonorificMap().values());
        assertAllResolved("federationHonorific", Ranks.getFederationHonorificMap().values());
        assertAllResolved("pilotFederation", Ranks.getLocalizedPilotFederationRankMap().values());
    }

    @Test
    void localizedRankNamesResolveToDisplayText() {
        // The honorific maps are keyed by the English rank names getLocalizedRankName expects.
        List<String> englishRankNames = new ArrayList<>();
        englishRankNames.addAll(Ranks.getImperialHonorificMap().keySet());
        englishRankNames.addAll(Ranks.getFederationHonorificMap().keySet());

        for (String name : englishRankNames) {
            if ("none".equalsIgnoreCase(name)) continue; // getLocalizedRankName filters "none" -> null by design
            String localized = Ranks.getLocalizedRankName(name);
            assertNotNull(localized, "getLocalizedRankName should localize: " + name);
            assertFalse(isUnresolvedKey(localized),
                    "getLocalizedRankName(\"" + name + "\") leaked unresolved i18n key: " + localized);
        }
    }

    @Test
    void legalStatusesResolveToDisplayText() {
        for (String status : List.of("Clean", "Wanted", "Hostile", "Lawless", "Allied", "IllegalCargo")) {
            String localized = Ranks.getLocalizedLegalStatus(status);
            assertNotNull(localized, "getLocalizedLegalStatus should localize: " + status);
            assertFalse(isUnresolvedKey(localized),
                    "getLocalizedLegalStatus(\"" + status + "\") leaked unresolved i18n key: " + localized);
        }
    }

    /**
     * The ticket case: the state announced with the commander's own legal standing stayed English while the
     * sentence around it was translated. English cannot catch that - "Clean" maps to "Clean" - so the
     * every-state check above proves only that keys resolve. This one proves they actually translate.
     */
    @Test
    void legalStatusesAreTranslatedNotPassedThrough() {
        SystemSession.getInstance().setLanguage(Language.DE);
        assertEquals("Gesucht", Ranks.getLocalizedLegalStatus("Wanted"));
        assertEquals("Verbündet", Ranks.getLocalizedLegalStatus("Allied"));
        assertEquals("Illegale Fracht", Ranks.getLocalizedLegalStatus("IllegalCargo"));

        SystemSession.getInstance().setLanguage(Language.RU);
        assertEquals("Разыскивается", Ranks.getLocalizedLegalStatus("Wanted"));
        assertEquals("Запрещённый груз", Ranks.getLocalizedLegalStatus("IllegalCargo"));
    }

    /**
     * The same vocabulary reaches us from two journal sources that do not agree on spelling -
     * {@code Status.json} writes {@code IllegalCargo}, {@code ShipTargeted} writes {@code Illegal cargo} -
     * and an exact-match lookup silently fell back to the raw English value for whichever form it did not hold.
     */
    @Test
    void legalStatusLookupIgnoresCaseSpacingAndUnderscores() {
        String expected = Ranks.getLocalizedLegalStatus("IllegalCargo");
        for (String variant : List.of("Illegal cargo", "illegal_cargo", "ILLEGALCARGO")) {
            assertEquals(expected, Ranks.getLocalizedLegalStatus(variant),
                    "spelling variant must resolve to the same translation: " + variant);
        }
    }

    @Test
    void unknownLegalStatusFallsBackToReadableEnglish() {
        // Frontier adds states over time (Speeding, PassengerWanted, Warrant); an unmapped one must still
        // be speakable rather than surfacing an i18n key.
        assertEquals("Passenger Wanted", Ranks.getLocalizedLegalStatus("Passenger_Wanted"));
        assertNull(Ranks.getLocalizedLegalStatus(null));
        assertNull(Ranks.getLocalizedLegalStatus("  "));
    }

    private static void assertAllResolved(String source, Iterable<String> values) {
        for (String value : values) {
            assertNotNull(value, source + " map produced a null rank value");
            assertFalse(isUnresolvedKey(value), source + " map leaked unresolved i18n key: " + value);
        }
    }

    private static boolean isUnresolvedKey(String value) {
        return UNRESOLVED_KEY.matcher(value).matches();
    }
}
