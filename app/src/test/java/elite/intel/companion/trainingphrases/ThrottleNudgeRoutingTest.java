package elite.intel.companion.trainingphrases;

import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.query.QueryRegistry;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticSearchProvider;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.prompt.AliasEmbeddingText;
import elite.intel.companion.prompt.GameToolCandidates;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards throttle-nudge routing: the commander says "set speed plus five" and the reducer must put
 * {@code increase_speed} in the shortlist the model gets.
 *
 * <p>The measured KPI is offered@K, not rank 1. The reducer is a candidate generator, so the antonym pair
 * increase/decrease legitimately lands together and the model picks; see this package's README.
 *
 * <p>This regressed twice, both times because a language's nudge aliases were thinner than its neighbours':
 * <ul>
 *   <li>EN carried a single alias ("increase speed by {key:X}") and scored 0.862 against a 0.869 cutoff, so
 *       the model saw only the fixed-speed commands and truthfully said it could only set 100% or optimal;</li>
 *   <li>DE named only "Geschwindigkeit" while its fixed-speed commands owned "Schub", so every
 *       throttle-worded nudge ("erhöhe den schub um zehn") lost to {@code set_speed_100}.</li>
 * </ul>
 *
 * <p>Utterances here are authored independently of the bundles, and deliberately include forms no alias
 * spells out ("drossle den schub um zehn", "nimm zehn prozent schub weg", "bump speed up by ten"), so the
 * test measures generalization rather than the aliases against themselves.
 *
 * <p>Opt-in ({@code embedding-manual}, run by {@code :app:embeddingTest}); needs the embedding model in
 * {@code distribution/embed}, which is why it is out of the default suite rather than in CI.
 */
@Tag("embedding-manual")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ThrottleNudgeRoutingTest {

    private static final Set<IntelActionCategory> ALL = EnumSet.allOf(IntelActionCategory.class);
    /**
     * Mirrors SemanticActionReducer.SEM_MARGIN: the band below the best score that still reaches the model.
     */
    private static final double SEM_MARGIN = 0.04;

    private SemanticPhraseMatcher matcher;
    private final Map<Language, List<GameToolCandidates.Candidate>> catalogs = new EnumMap<>(Language.class);

    @BeforeAll
    void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        matcher = SemanticSearchProvider.matcher();
    }

    @ParameterizedTest(name = "EN \"{0}\" offers increase_speed")
    @ValueSource(strings = {
            "set speed plus five", "speed plus twenty", "increase speed by 25", "increase throttle by 10",
            "raise speed by five", "raise throttle by ten", "speed up by twenty", "throttle up by ten",
            "add ten to speed", "bump speed up by ten", "more throttle by five", "up the throttle by ten",
    })
    void anEnglishIncreaseUtteranceOffersIncreaseSpeed(String utterance) {
        assertOffered(Language.EN, utterance, "increase_speed");
    }

    @ParameterizedTest(name = "EN \"{0}\" offers decrease_speed")
    @ValueSource(strings = {
            "set speed minus five", "speed minus twenty", "decrease speed by 25", "decrease throttle by 10",
            "lower speed by five", "lower throttle by ten", "reduce speed by twenty", "slow down by ten",
            "throttle down by ten", "cut speed by five", "ease off the throttle by ten", "drop the throttle by five",
    })
    void anEnglishDecreaseUtteranceOffersDecreaseSpeed(String utterance) {
        assertOffered(Language.EN, utterance, "decrease_speed");
    }

    @ParameterizedTest(name = "RU \"{0}\" offers increase_speed")
    @ValueSource(strings = {
            "подними скорость на десять", "прибавь газу на двадцать", "ускорься на пятнадцать",
            "накинь двадцать процентов тяги", "скорость плюс пять", "поддай ходу на десять",
            "быстрее на двадцать", "плюс десять к тяге",
    })
    void aRussianIncreaseUtteranceOffersIncreaseSpeed(String utterance) {
        assertOffered(Language.RU, utterance, "increase_speed");
    }

    @ParameterizedTest(name = "RU \"{0}\" offers decrease_speed")
    @ValueSource(strings = {
            "притормози на десять", "сбрось скорость на двадцать", "сбавь газу на десять",
            "скорость минус пять", "медленнее на двадцать", "убери десять процентов скорости",
            "тормози на пятнадцать", "минус двадцать к тяге",
    })
    void aRussianDecreaseUtteranceOffersDecreaseSpeed(String utterance) {
        assertOffered(Language.RU, utterance, "decrease_speed");
    }

    @ParameterizedTest(name = "DE \"{0}\" offers increase_speed")
    @ValueSource(strings = {
            "erhöhe den schub um zehn", "gib mir zwanzig prozent mehr schub", "schub plus fünf",
            "beschleunige um zehn", "geschwindigkeit plus zehn", "steigere die geschwindigkeit um fünf",
            "schubregler um zehn hoch",
    })
    void aGermanIncreaseUtteranceOffersIncreaseSpeed(String utterance) {
        assertOffered(Language.DE, utterance, "increase_speed");
    }

    @ParameterizedTest(name = "DE \"{0}\" offers decrease_speed")
    @ValueSource(strings = {
            "reduziere den schub um zehn", "drossle den schub um zehn", "schub minus fünf",
            "geschwindigkeit minus zehn", "verlangsame um zwanzig", "nimm zehn prozent schub weg",
            "senke die geschwindigkeit um fünf", "schubregler um zehn runter",
    })
    void aGermanDecreaseUtteranceOffersDecreaseSpeed(String utterance) {
        assertOffered(Language.DE, utterance, "decrease_speed");
    }

    @ParameterizedTest(name = "FR \"{0}\" offers increase_speed")
    @ValueSource(strings = {
            "augmente la poussée de dix", "monte la poussée de vingt", "plus de gaz de dix",
            "vitesse plus cinq", "poussée plus cinq", "augmente les gaz de vingt",
            "monte la manette des gaz de dix", "pousse un peu plus, de quinze",
    })
    void aFrenchIncreaseUtteranceOffersIncreaseSpeed(String utterance) {
        assertOffered(Language.FR, utterance, "increase_speed");
    }

    @ParameterizedTest(name = "FR \"{0}\" offers decrease_speed")
    @ValueSource(strings = {
            "réduis la poussée de dix", "baisse la poussée de vingt", "diminue les gaz de dix",
            "vitesse moins cinq", "poussée moins cinq", "baisse les gaz de quinze",
            "descends la manette des gaz de dix", "lève le pied de dix",
    })
    void aFrenchDecreaseUtteranceOffersDecreaseSpeed(String utterance) {
        assertOffered(Language.FR, utterance, "decrease_speed");
    }

    @ParameterizedTest(name = "ES \"{0}\" offers increase_speed")
    @ValueSource(strings = {
            "sube el acelerador diez", "aumenta el empuje en veinte", "más acelerador, diez",
            "velocidad más cinco", "empuje más cinco", "acelera un veinte por ciento",
            "incrementa el acelerador en diez", "dale diez más de acelerador",
    })
    void aSpanishIncreaseUtteranceOffersIncreaseSpeed(String utterance) {
        assertOffered(Language.ES, utterance, "increase_speed");
    }

    @ParameterizedTest(name = "ES \"{0}\" offers decrease_speed")
    @ValueSource(strings = {
            "baja el acelerador diez", "reduce el empuje en veinte", "menos acelerador, diez",
            "velocidad menos cinco", "empuje menos cinco", "desacelera un veinte por ciento",
            "disminuye el acelerador en diez", "afloja el acelerador diez",
    })
    void aSpanishDecreaseUtteranceOffersDecreaseSpeed(String utterance) {
        assertOffered(Language.ES, utterance, "decrease_speed");
    }

    @ParameterizedTest(name = "IT \"{0}\" offers increase_speed")
    @ValueSource(strings = {
            "aumenta la spinta di dieci", "alza la spinta di venti", "più manetta di dieci",
            "velocità più cinque", "spinta più cinque", "accelera di quindici",
            "aumenta di dieci la velocità", "dammi dieci in più di velocità",
    })
    void anItalianIncreaseUtteranceOffersIncreaseSpeed(String utterance) {
        assertOffered(Language.IT, utterance, "increase_speed");
    }

    @ParameterizedTest(name = "IT \"{0}\" offers decrease_speed")
    @ValueSource(strings = {
            "riduci la spinta di dieci", "abbassa la spinta di venti", "meno manetta di dieci",
            "velocità meno cinque", "spinta meno cinque", "rallenta di quindici",
            "diminuisci di dieci la velocità", "togli dieci di velocità",
    })
    void anItalianDecreaseUtteranceOffersDecreaseSpeed(String utterance) {
        assertOffered(Language.IT, utterance, "decrease_speed");
    }

    @ParameterizedTest(name = "UK \"{0}\" offers increase_speed")
    @ValueSource(strings = {
            "підніми тягу на десять", "додай ходу на двадцять", "прискорся на п'ятнадцять",
            "швидкість плюс п'ять", "тяга плюс п'ять", "піддай газу на десять",
            "швидше на двадцять", "плюс десять до тяги",
    })
    void aUkrainianIncreaseUtteranceOffersIncreaseSpeed(String utterance) {
        assertOffered(Language.UK, utterance, "increase_speed");
    }

    @ParameterizedTest(name = "UK \"{0}\" offers decrease_speed")
    @ValueSource(strings = {
            "пригальмуй на десять", "скинь швидкість на двадцять", "зменш тягу на десять",
            "швидкість мінус п'ять", "тяга мінус п'ять", "повільніше на двадцять",
            "прибери десять відсотків швидкості", "мінус двадцять до тяги",
    })
    void aUkrainianDecreaseUtteranceOffersDecreaseSpeed(String utterance) {
        assertOffered(Language.UK, utterance, "decrease_speed");
    }

    /**
     * The nudge aliases must not turn into a semantic hub that swallows the fixed-speed commands. Rank 1 is
     * asserted here (not merely offered) because these utterances name one exact throttle setting.
     */
    @ParameterizedTest(name = "{0} \"{1}\" still routes to {2}")
    @CsvSource({
            "EN, full throttle,                  set_speed_100",
            "EN, maximum speed,                  set_speed_100",
            "EN, set speed to 25 percent,        set_speed_25",
            "EN, set speed to 50 percent,        set_speed_50",
            "EN, set speed to 75 percent,        set_speed_75",
            "EN, stop the ship,                  set_speed_to_zero_0_stop_ship",
            "EN, all stop,                       set_speed_to_zero_0_stop_ship",
            "EN, optimal approach speed,         set_optimal_speed",
            "RU, дай полную тягу,                set_speed_100",
            "RU, полная остановка,               set_speed_to_zero_0_stop_ship",
            "RU, четверть хода,                  set_speed_25",
            "RU, оптимальная скорость подхода,   set_optimal_speed",
            "DE, voller schub,                   set_speed_100",
            "DE, schiff anhalten,                set_speed_to_zero_0_stop_ship",
            "DE, halbe fahrt,                    set_speed_50",
            "DE, optimale anfluggeschwindigkeit, set_optimal_speed",
            "FR, pleine poussée,                 set_speed_100",
            "FR, arrête le vaisseau,             set_speed_to_zero_0_stop_ship",
            "FR, demi poussée,                   set_speed_50",
            "FR, quart de poussée,               set_speed_25",
            "FR, vitesse d'approche optimale,    set_optimal_speed",
            "ES, acelerador al máximo,           set_speed_100",
            "ES, detener nave,                   set_speed_to_zero_0_stop_ship",
            "ES, medio acelerador,               set_speed_50",
            "ES, un cuarto de acelerador,        set_speed_25",
            "ES, velocidad óptima de aproximación, set_optimal_speed",
            "IT, velocità massima,               set_speed_100",
            "IT, ferma la nave,                  set_speed_to_zero_0_stop_ship",
            "IT, velocità a metà,                set_speed_50",
            "IT, velocità a un quarto,           set_speed_25",
            "IT, velocità di avvicinamento ottimale, set_optimal_speed",
            "UK, повна тяга,                     set_speed_100",
            "UK, зупини корабель,                set_speed_to_zero_0_stop_ship",
            "UK, половина тяги,                  set_speed_50",
            "UK, чверть тяги,                    set_speed_25",
            "UK, оптимальна швидкість підходу,   set_optimal_speed",
    })
    void aFixedSpeedUtteranceStillWinsItsOwnCommand(Language language, String utterance, String expectedId) {
        Ranked ranked = rank(language, utterance);
        assertEquals(expectedId, ranked.ids.get(0), () -> String.format(Locale.ROOT,
                "%s \"%s\" should rank %s first but got %s (%.3f)",
                language, utterance, expectedId, ranked.ids.get(0), ranked.scores.get(0)));
    }

    private void assertOffered(Language language, String utterance, String expectedId) {
        Ranked ranked = rank(language, utterance);
        int at = ranked.ids.indexOf(expectedId);
        double cutoff = ranked.scores.get(0) - SEM_MARGIN;
        assertTrue(at >= 0 && ranked.scores.get(at) >= cutoff, () -> String.format(Locale.ROOT,
                "%s \"%s\" left %s out of the shortlist: score %.3f < cutoff %.3f (top: %s %.3f)",
                language, utterance, expectedId, at < 0 ? -1.0 : ranked.scores.get(at), cutoff,
                ranked.ids.get(0), ranked.scores.get(0)));
    }

    /**
     * All candidates for the language scored against one utterance, strongest first.
     */
    private Ranked rank(Language language, String utterance) {
        List<GameToolCandidates.Candidate> catalog = catalogFor(language);
        float[] query = matcher.embedQuery(utterance);
        double[] scores = new double[catalog.size()];
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < catalog.size(); i++) {
            GameToolCandidates.Candidate candidate = catalog.get(i);
            scores[i] = matcher.bestSimilarity(query,
                    AliasEmbeddingText.phrases(candidate.localizedAliasGroup(), candidate.tool().parameters()));
            order.add(i);
        }
        order.sort((a, b) -> Double.compare(scores[b], scores[a]));
        Ranked ranked = new Ranked();
        for (int i : order) {
            ranked.ids.add(catalog.get(i).id());
            ranked.scores.add(scores[i]);
        }
        return ranked;
    }

    /**
     * Aliases resolve against the session language, so the catalog is built (and cached) per language.
     *
     * <p>WHY the detached status: the no-arg {@code GameToolCandidates} gates visibility on
     * {@link Status#getInstance()}, which reflects the <em>live</em> game. Running this while the game sits
     * docked or on foot hides every throttle command, and the test then fails for a reason that has nothing
     * to do with routing. Pinning the situation reproduces the log this guards ("In ship - open space") and
     * keeps the result independent of whatever the commander is doing.
     */
    private List<GameToolCandidates.Candidate> catalogFor(Language language) {
        return catalogs.computeIfAbsent(language, key -> {
            SystemSession.getInstance().setLanguage(key);
            return new GameToolCandidates(Status.detached(PlayerSituation.IN_SHIP_DEEP_SPACE)).collect(ALL);
        });
    }

    private static final class Ranked {
        final List<String> ids = new ArrayList<>();
        final List<Double> scores = new ArrayList<>();
    }
}
