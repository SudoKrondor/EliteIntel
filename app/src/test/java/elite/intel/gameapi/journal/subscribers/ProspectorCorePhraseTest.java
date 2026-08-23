package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.EventRegistry;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.gameapi.journal.events.ProspectedAsteroidEvent;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A core asteroid is announced because it is a core, not because of what it holds.
 *
 * <p>WHY: the prospector fires on every rock and most of them are worth nothing, so the callout was
 * gated on the commander's mining targets. That gate silently swallowed the rarest thing the limpet
 * ever finds - a motherlode whose surface minerals happened to be off the list. {@code
 * MotherlodeMaterial} is the only field that marks a core; {@code Content} does not, and the fixtures
 * below are real journal lines showing two of the three cores in one session coming back "Medium".
 */
class ProspectorCorePhraseTest {

    private static final Set<String> NO_TARGETS = Set.of();

    @Test
    void aCoreIsAnnouncedEvenWhenNothingOnItIsWanted() {
        String phrase = ProspectorSubscriber.prospectorPhrase(prospected(PLATINUM_CORE), NO_TARGETS);

        assertEquals("Core asteroid detected. Motherlode: Platinum.", phrase);
    }

    /**
     * The case that motivated this: a Medium-content rock. Content level says how much surface
     * mineral there is and nothing at all about whether the rock has a core.
     */
    @Test
    void aMediumContentRockIsStillACore() {
        ProspectedAsteroidEvent event = prospected(MONAZITE_CORE);

        assertTrue(event.isCore());
        assertEquals("Material Content: Medium", event.getContentLocalised());
        assertEquals("Core asteroid detected. Motherlode: Monazite.", ProspectorSubscriber.prospectorPhrase(event, NO_TARGETS));
    }

    /**
     * Both reasons to speak are independent, so a core holding a tracked mineral is worth both
     * sentences - the commander wants the core and the yield.
     */
    @Test
    void aCoreThatAlsoHoldsATargetSaysBoth() {
        String phrase = ProspectorSubscriber.prospectorPhrase(prospected(PLATINUM_CORE), Set.of("Indite"));

        assertEquals("Core asteroid detected. Motherlode: Platinum. Prospector detected 23.84 percent indite.", phrase);
    }

    @Test
    void anOrdinaryRockOffTheTargetListIsStillPassedOverInSilence() {
        ProspectedAsteroidEvent event = prospected(PLAIN_ROCK);

        assertFalse(event.isCore());
        assertEquals("", ProspectorSubscriber.prospectorPhrase(event, NO_TARGETS));
    }

    @Test
    void anOrdinaryRockHoldingATargetIsUnchanged() {
        String phrase = ProspectorSubscriber.prospectorPhrase(prospected(PLAIN_ROCK), Set.of("Uraninite"));

        assertEquals("Prospector detected 17.48 percent uraninite.", phrase);
    }

    /**
     * The core-bearing commodities the game names as run-together symbols - there is no
     * {@code MotherlodeMaterial_Localised} to fall back on, so the name has to be resolved.
     */
    @Test
    void aRunTogetherSymbolIsResolvedToItsSpokenName() {
        String phrase = ProspectorSubscriber.prospectorPhrase(prospected(LTD_CORE), NO_TARGETS);

        assertEquals("Core asteroid detected. Motherlode: Low Temperature Diamonds.", phrase);
    }

    /**
     * A symbol the commodities table has never heard of must not cost the commander the callout;
     * only the name degrades, and it degrades into words rather than one run-on token.
     */
    @Test
    void anUnknownSymbolStillAnnouncesTheCore() {
        String phrase = ProspectorSubscriber.prospectorPhrase(prospected(UNKNOWN_CORE), NO_TARGETS);

        assertEquals("Core asteroid detected. Motherlode: Unobtainium Ore.", phrase);
    }

    // -- fixtures --------------------------------------------------------------

    /**
     * Real lines from a core-mining session in Eol Prou IW-W e1-69, ring 12 A.
     */
    private static final String PLATINUM_CORE = """
            { "timestamp":"%s", "event":"ProspectedAsteroid", "Materials":[ { "Name":"indite", "Proportion":23.836475 }, { "Name":"Praseodymium", "Proportion":10.659277 }, { "Name":"lepidolite", "Proportion":3.082870 } ], "MotherlodeMaterial":"Platinum", "Content":"$AsteroidMaterialContent_Medium;", "Content_Localised":"Material Content: Medium", "Remaining":100.000000 }
            """;

    private static final String MONAZITE_CORE = """
            { "timestamp":"%s", "event":"ProspectedAsteroid", "Materials":[ { "Name":"silver", "Proportion":45.266178 } ], "MotherlodeMaterial":"Monazite", "Content":"$AsteroidMaterialContent_Medium;", "Content_Localised":"Material Content: Medium", "Remaining":100.000000 }
            """;

    private static final String PLAIN_ROCK = """
            { "timestamp":"%s", "event":"ProspectedAsteroid", "Materials":[ { "Name":"uraninite", "Proportion":17.475636 }, { "Name":"lepidolite", "Proportion":4.153995 }, { "Name":"gallite", "Proportion":2.674272 } ], "Content":"$AsteroidMaterialContent_Medium;", "Content_Localised":"Material Content: Medium", "Remaining":100.000000 }
            """;

    private static final String LTD_CORE = """
            { "timestamp":"%s", "event":"ProspectedAsteroid", "Materials":[ { "Name":"lepidolite", "Proportion":21.720705 } ], "MotherlodeMaterial":"LowTemperatureDiamond", "Content":"$AsteroidMaterialContent_High;", "Content_Localised":"Material Content: High", "Remaining":100.000000 }
            """;

    private static final String UNKNOWN_CORE = """
            { "timestamp":"%s", "event":"ProspectedAsteroid", "Materials":[], "MotherlodeMaterial":"UnobtainiumOre", "Content":"$AsteroidMaterialContent_Low;", "Content_Localised":"Material Content: Low", "Remaining":100.000000 }
            """;

    /**
     * Built through the registry, so what is under test is the journal line rather than a
     * hand-assembled event object - {@code MotherlodeMaterial} has to survive the parse. The
     * timestamp is stamped fresh because the registry drops a line older than its threshold.
     */
    private static ProspectedAsteroidEvent prospected(String journalLine) {
        JsonObject json = GsonFactory.getGson().fromJson(journalLine.formatted(Instant.now().toString()), JsonObject.class);
        BaseEvent event = EventRegistry.createEvent("ProspectedAsteroid", json);
        return assertInstanceOf(ProspectedAsteroidEvent.class, event, "ProspectedAsteroid must be a registered event");
    }
}
