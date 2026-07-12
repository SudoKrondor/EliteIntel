package elite.intel.ai.brain.actions.handlers.query.carrier;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static elite.intel.ai.brain.actions.handlers.query.carrier.CarrierOwnership.FLEET;
import static elite.intel.ai.brain.actions.handlers.query.carrier.CarrierOwnership.SQUADRON;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Carrier ownership is resolved from the commander's words, not by the LLM picking between two near-identical
 * tools. These cases are the contract that replaced that guess, so they are drawn from the real alias groups in
 * {@code ai_action_aliases*.properties}: every phrase a commander is actually offered must land on the carrier
 * they meant.
 * <p>
 * The FLEET cases carry the weight. A stem that over-matches would silently answer every question about the
 * commander's own carrier with the squadron's data.
 */
class CarrierOwnershipTest {

    @Test
    void nothingSaidMeansTheCommandersOwnCarrier() {
        assertEquals(FLEET, CarrierOwnership.resolve(null, List.of("squadron")));
        assertEquals(FLEET, CarrierOwnership.resolve("", List.of("squadron")));
        assertEquals(FLEET, CarrierOwnership.resolve("   ", List.of("squadron")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQUADRON CARRIER STATUS", "Squadron Carrier Status", "squadron carrier status"})
    void stemMatchIgnoresCase(String utterance) {
        assertEquals(SQUADRON, CarrierOwnership.resolve(utterance, List.of("squadron")));
    }

    /**
     * One stem covers a whole inflectional family, which is why stems are matched as substrings.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "статус авианосца эскадрильи",
            "куда летит авианосец эскадрильи",
            "данные об авианосце эскадрона",
    })
    void oneStemCoversEveryInflection(String utterance) {
        assertEquals(SQUADRON, CarrierOwnership.resolve(utterance, List.of("эскадр")));
    }

    @ParameterizedTest(name = "[{index}] {0}: \"{1}\" -> {2}")
    @MethodSource("realAliasPhrases")
    void realAliasPhrasesResolveToTheCarrierTheyName(Language language, String phrase, CarrierOwnership expected) {
        assertEquals(expected, CarrierOwnership.resolve(phrase, CarrierOwnership.squadronStems(language)));
    }

    /**
     * Phrases lifted verbatim from each language's merged carrier alias groups.
     */
    private static Stream<Arguments> realAliasPhrases() {
        return Stream.of(
                Arguments.of(Language.EN, "carrier status", FLEET),
                Arguments.of(Language.EN, "where is carrier going", FLEET),
                Arguments.of(Language.EN, "fleet carrier balance", FLEET),
                Arguments.of(Language.EN, "squadron carrier status", SQUADRON),
                Arguments.of(Language.EN, "how many jumps on squadron carrier route", SQUADRON),

                Arguments.of(Language.DE, "carrier kontostand", FLEET),
                Arguments.of(Language.DE, "wohin fliegt der carrier", FLEET),
                Arguments.of(Language.DE, "squadron carrier übersicht", SQUADRON),
                Arguments.of(Language.DE, "endziel des squadron carriers", SQUADRON),

                Arguments.of(Language.ES, "estado del carrier", FLEET),
                Arguments.of(Language.ES, "a dónde va el carrier", FLEET),
                Arguments.of(Language.ES, "estado del carrier del escuadrón", SQUADRON),
                Arguments.of(Language.ES, "saltos restantes del carrier del escuadrón", SQUADRON),

                Arguments.of(Language.FR, "statut du porte-vaisseau", FLEET),
                Arguments.of(Language.FR, "où va le porte-vaisseau", FLEET),
                Arguments.of(Language.FR, "état du porte-vaisseau d'escadron", SQUADRON),
                Arguments.of(Language.FR, "route squadron carrier", SQUADRON),

                Arguments.of(Language.IT, "stato della fleet carrier", FLEET),
                Arguments.of(Language.IT, "destinazione finale fleet carrier", FLEET),
                Arguments.of(Language.IT, "stato della squadron carrier", SQUADRON),
                Arguments.of(Language.IT, "stato della portanavi della squadriglia", SQUADRON),

                Arguments.of(Language.PT, "status do carrier", FLEET),
                Arguments.of(Language.PT, "rumo do carrier", FLEET),
                Arguments.of(Language.PT, "status do carrier do esquadrão", SQUADRON),
                Arguments.of(Language.PT, "rota do carrier do esquadrão", SQUADRON),

                Arguments.of(Language.PTBZ, "status do carrier", FLEET),
                Arguments.of(Language.PTBZ, "rumo do carrier", FLEET),
                Arguments.of(Language.PTBZ, "status do carrier do esquadrão", SQUADRON),
                Arguments.of(Language.PTBZ, "rota do carrier do esquadrão", SQUADRON),

                Arguments.of(Language.RU, "статус авианосца", FLEET),
                Arguments.of(Language.RU, "куда летит авианосец", FLEET),
                Arguments.of(Language.RU, "дальность прыжка авианосца", FLEET),
                Arguments.of(Language.RU, "статус авианосца эскадрильи", SQUADRON),
                Arguments.of(Language.RU, "куда направляется авианосец эскадрильи", SQUADRON),

                Arguments.of(Language.UK, "статус авіаносця", FLEET),
                Arguments.of(Language.UK, "куди летить авіаносець", FLEET),
                Arguments.of(Language.UK, "дальність стрибка авіаносця", FLEET),
                Arguments.of(Language.UK, "статус авіаносця ескадрильї", SQUADRON),
                Arguments.of(Language.UK, "курс авіаносця ескадрильї", SQUADRON)
        );
    }
}
