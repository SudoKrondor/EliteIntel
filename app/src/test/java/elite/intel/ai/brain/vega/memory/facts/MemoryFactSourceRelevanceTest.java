package elite.intel.ai.brain.vega.memory.facts;

import elite.intel.ai.brain.vega.memory.facts.sources.CurrentBodyFactSource;
import elite.intel.ai.brain.vega.memory.facts.sources.CurrentStationFactSource;
import elite.intel.ai.brain.vega.memory.facts.sources.CurrentSystemFactSource;
import elite.intel.ai.brain.vega.memory.facts.sources.SystemSignalsFactSource;
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
    void useEnglishInputRules() {
        previousLanguage = SystemSession.getInstance().getLanguage();
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @AfterEach
    void restoreInputRules() {
        SystemSession.getInstance().setLanguage(previousLanguage);
    }

    @Test
    void eachRegisteredSourceOwnsItsLocalizedSubjectDecision() {
        assertTrue(new CurrentSystemFactSource().isRelevant(ctx("what is the security level in this system")));
        assertTrue(new CurrentStationFactSource().isRelevant(ctx("what services are at this station")));
        assertTrue(new CurrentBodyFactSource().isRelevant(ctx("what materials are on this planet")));
        assertTrue(new SystemSignalsFactSource().isRelevant(ctx("what signals are in the system")));
    }

    @Test
    void locationSourcesIndependentlyOptIntoAnExplicitLocationQuestion() {
        MemoryFactContext location = ctx("where are we now");

        assertTrue(new CurrentSystemFactSource().isRelevant(location));
        assertTrue(new CurrentStationFactSource().isRelevant(location));
        assertTrue(new CurrentBodyFactSource().isRelevant(location));
        assertFalse(new SystemSignalsFactSource().isRelevant(location));
    }

    @Test
    void aBareDomainWordIsNotAnExternalFactTrigger() {
        MemoryFactContext systemChat = ctx("so what do you think of this system");

        assertFalse(new CurrentSystemFactSource().isRelevant(systemChat));
        assertTrue(builtInSources().stream().noneMatch(source -> source.isRelevant(ctx("the stars are beautiful here"))));
    }

    @Test
    void unrelatedCommandsAndBroadPropertyWordsStayIrrelevant() {
        for (String input : List.of(
                "target the engines",
                "hello",
                "what is in our cargo hold right now",
                "which faction is our combat mission against",
                "what was the fighter pilot called")) {
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
                new SystemSignalsFactSource());
    }
}
