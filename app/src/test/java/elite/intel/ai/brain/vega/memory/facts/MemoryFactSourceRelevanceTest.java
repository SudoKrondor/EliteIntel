package elite.intel.ai.brain.vega.memory.facts;

import elite.intel.ai.brain.vega.memory.facts.MemoryFactContext;
import elite.intel.ai.brain.vega.memory.facts.MemoryFactSource;
import elite.intel.ai.brain.vega.memory.facts.sources.CommanderProfileFactSource;
import elite.intel.ai.brain.vega.memory.facts.sources.CurrentBodyFactSource;
import elite.intel.ai.brain.vega.memory.facts.sources.CurrentStationFactSource;
import elite.intel.ai.brain.vega.memory.facts.sources.CurrentSystemFactSource;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryFactSourceRelevanceTest {

    private Language previousLanguage;

    @BeforeEach
    void useRussianInputRules() {
        previousLanguage = SystemSession.getInstance().getLanguage();
        SystemSession.getInstance().setLanguage(Language.RU);
    }

    @AfterEach
    void restoreInputRules() {
        SystemSession.getInstance().setLanguage(previousLanguage);
    }

    @Test
    void eachRegisteredSourceOwnsItsLocalizedSubjectDecision() {
        assertTrue(new CurrentSystemFactSource().isRelevant(ctx("какой уровень безопасности в этой системе")));
        assertTrue(new CurrentStationFactSource().isRelevant(ctx("какие услуги есть на этой станции")));
        assertTrue(new CurrentBodyFactSource().isRelevant(ctx("какие материалы на этой планете")));
        assertTrue(new CommanderProfileFactSource().isRelevant(ctx("какой у меня ранг пилота")));
    }

    @Test
    void locationSourcesIndependentlyOptIntoAnExplicitLocationQuestion() {
        MemoryFactContext location = ctx("где мы сейчас");

        assertTrue(new CurrentSystemFactSource().isRelevant(location));
        assertTrue(new CurrentStationFactSource().isRelevant(location));
        assertTrue(new CurrentBodyFactSource().isRelevant(location));
        assertFalse(new CommanderProfileFactSource().isRelevant(location));
    }

    @Test
    void aBareDomainWordIsNotAnExternalFactTrigger() {
        MemoryFactContext systemChat = ctx("а что скажешь про эту систему");

        assertFalse(new CurrentSystemFactSource().isRelevant(systemChat));
        assertTrue(builtInSources().stream().noneMatch(source -> source.isRelevant(ctx("звёзды тут красивые"))));
    }

    @Test
    void unrelatedCommandsAndBroadPropertyWordsStayIrrelevant() {
        for (String input : List.of(
                "целься в двигатели",
                "привет",
                "что у нас сейчас в трюме",
                "против какой фракции у нас боевая миссия",
                "как звали пилота истребителя")) {
            assertTrue(builtInSources().stream().noneMatch(source -> source.isRelevant(ctx(input))), input);
        }
    }

    private static MemoryFactContext ctx(String query) {
        return MemoryFactContext.forCommanderInput(query);
    }

    private static List<MemoryFactSource> builtInSources() {
        return List.of(
                new CurrentSystemFactSource(),
                new CurrentStationFactSource(),
                new CurrentBodyFactSource(),
                new CommanderProfileFactSource());
    }
}
