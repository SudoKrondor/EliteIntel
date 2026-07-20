package elite.intel.ai.brain.vega.memory.facts.sources;

import elite.intel.ai.brain.vega.memory.facts.MemoryFactContext;
import elite.intel.ai.brain.vega.memory.facts.RegisterMemoryFactSource;
import elite.intel.ai.brain.vega.model.ThoughtSource;
import elite.intel.ai.brain.vega.model.Urgency;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags.GuiFocus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenScreenFactSourceTest {

    private final int savedGuiFocus = Status.getInstance().getStatus().getGuiFocus();
    private final OpenScreenFactSource source = new OpenScreenFactSource();

    @AfterEach
    void restoreGuiFocus() {
        setGuiFocus(savedGuiFocus);
    }

    @Test
    void isAutomaticallyRegisteredAndRelevantToEveryCommanderTurn() {
        assertTrue(OpenScreenFactSource.class.isAnnotationPresent(RegisterMemoryFactSource.class));
        assertTrue(source.isRelevant(MemoryFactContext.forCommanderInput("close the window")));
        assertFalse(source.isRelevant(new MemoryFactContext("event", ThoughtSource.EVENT, Urgency.NORMAL)));
    }

    /**
     * The reported bug: with the fleet carrier management screen open the model saw only "docked at station",
     * concluded there was no window to close, and spoke instead of calling exit_close.
     */
    @Test
    void reportsTheCarrierManagementScreenSoClosingIsActionable() {
        setGuiFocus(GuiFocus.STATION_SERVICES.getValue());

        List<String> facts = source.factsFor(MemoryFactContext.forCommanderInput("close the window"));

        assertEquals(1, facts.size());
        assertTrue(facts.get(0).contains("fleet carrier management"), facts.get(0));
    }

    @Test
    void staysSilentWhenNothingIsOpen() {
        setGuiFocus(GuiFocus.NO_FOCUS.getValue());
        assertEquals(List.of(), source.factsFor(MemoryFactContext.forCommanderInput("close the window")));
        assertNull(OpenScreenFactSource.format(GuiFocus.NO_FOCUS));
    }

    @Test
    void anUnnameableFocusIsNotEvidenceOfAWindow() {
        assertNull(OpenScreenFactSource.format(GuiFocus.UNKNOWN));
        assertNull(OpenScreenFactSource.format(null));
    }

    @Test
    void everyOpenableScreenHasItsOwnDistinctName() {
        for (GuiFocus focus : GuiFocus.values()) {
            if (focus == GuiFocus.NO_FOCUS || focus == GuiFocus.UNKNOWN) {
                continue;
            }
            String fact = OpenScreenFactSource.format(focus);
            assertNotNull(fact, "no name for " + focus + " - the model would go blind on that screen");
            assertTrue(fact.startsWith("On screen now: "), fact);
        }
    }

    private static void setGuiFocus(int guiFocus) {
        GameEvents.StatusEvent snapshot = Status.getInstance().getStatus();
        snapshot.setGuiFocus(guiFocus);
        Status.getInstance().setStatus(snapshot);
    }
}
