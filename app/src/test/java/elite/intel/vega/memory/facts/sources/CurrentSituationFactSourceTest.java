package elite.intel.vega.memory.facts.sources;

import elite.intel.vega.memory.facts.MemoryFactContext;
import elite.intel.vega.memory.facts.RegisterMemoryFactSource;
import elite.intel.vega.model.ThoughtSource;
import elite.intel.vega.model.Urgency;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.i18n.EventsTextProvider;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentSituationFactSourceTest {

    private final long savedFlags = Status.getInstance().getStatus().getFlags();
    private final long savedFlags2 = Status.getInstance().getStatus().getFlags2();
    private final Language savedLanguage = SystemSession.getInstance().getLanguage();
    private final CurrentSituationFactSource source = new CurrentSituationFactSource();

    @AfterEach
    void restoreStatus() {
        setStatus(savedFlags, savedFlags2);
        SystemSession.getInstance().setLanguage(savedLanguage);
    }

    @Test
    void isAutomaticallyRegisteredAndRelevantToEveryCommanderTurn() {
        assertTrue(CurrentSituationFactSource.class.isAnnotationPresent(RegisterMemoryFactSource.class));
        assertTrue(source.isRelevant(MemoryFactContext.forCommanderInput("deploy landing gear")));
        assertFalse(source.isRelevant(new MemoryFactContext("event", ThoughtSource.EVENT, Urgency.NORMAL)));
    }

    @Test
    void reportsRepresentativeLiveSituationsFromStatusFlags() {
        assertLiveFact(PlayerSituation.IN_SHIP_DOCKED);
        assertLiveFact(PlayerSituation.ON_FOOT_STATION);
        assertLiveFact(PlayerSituation.IN_SHIP_SUPERCRUISE);
    }

    @Test
    void resolvesEverySituationThroughItsEnglishLocalizationKey() {
        String label = EventsTextProvider.getText(Language.EN, "game.situation.label");
        assertNotEquals("game.situation.label", label);
        for (PlayerSituation situation : PlayerSituation.values()) {
            String description = EventsTextProvider.getText(Language.EN, situation.i18nKey());
            assertNotEquals(situation.i18nKey(), description);
            assertEquals(label + ": " + description, CurrentSituationFactSource.format(situation));
        }
    }

    @Test
    void usesEnglishFactRegardlessOfCommanderLanguage() {
        SystemSession.getInstance().setLanguage(Language.RU);
        long[] flags = StatusFlags.flagsForSituation(PlayerSituation.IN_SHIP_SUPERCRUISE);
        setStatus(flags[0], flags[1]);

        assertEquals(List.of("Game situation: In ship · supercruise"),
                source.factsFor(MemoryFactContext.forCommanderInput("status")));
    }

    private void assertLiveFact(PlayerSituation situation) {
        long[] flags = StatusFlags.flagsForSituation(situation);
        setStatus(flags[0], flags[1]);
        assertEquals(List.of(CurrentSituationFactSource.format(situation)),
                source.factsFor(MemoryFactContext.forCommanderInput("status")));
    }

    private static void setStatus(long flags, long flags2) {
        GameEvents.StatusEvent snapshot = Status.getInstance().getStatus();
        snapshot.setFlags(flags);
        snapshot.setFlags2(flags2);
        Status.getInstance().setStatus(snapshot);
    }
}
