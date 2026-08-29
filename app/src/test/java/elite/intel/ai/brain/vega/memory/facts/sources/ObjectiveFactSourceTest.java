package elite.intel.ai.brain.vega.memory.facts.sources;

import elite.intel.gameapi.ReminderContact;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectiveFactSourceTest {

    @Test
    void buildsACompactLineFromAllFields() {
        assertEquals(
                "current objective: Shinrarta Dezhra, station Jameson Memorial, see the raw material trader, "
                        + "swap arsenic for iron",
                ObjectiveFactSource.format("Shinrarta Dezhra", "Jameson Memorial",
                        ReminderContact.MATERIAL_TRADER_RAW, "swap arsenic for iron"));
    }

    @Test
    void keepsTheDestinationWhenOnlyThatIsKnown() {
        assertEquals("current objective: Deciat",
                ObjectiveFactSource.format("Deciat", null, null, null));
    }

    @Test
    void keepsTheSentenceWhenTheStructuredFieldsAreAbsent() {
        assertEquals("current objective: pick up the samples we left behind",
                ObjectiveFactSource.format(null, null, null, "pick up the samples we left behind"));
    }

    @Test
    void emptyWhenTheReminderHoldsNothing() {
        assertTrue(ObjectiveFactSource.format(null, null, null, null).isEmpty());
    }

    /**
     * The destination and the contact are what a model can act on; the saved sentence is prose that may be any
     * length, so it must be the part the shared cap eats into.
     */
    @Test
    void theSavedSentenceIsWhatTheLineCapDropsFirst() {
        String longErrand = "x".repeat(FactLine.MAX_CHARS);

        String fact = ObjectiveFactSource.format("Deciat", "Farseer Inc",
                ReminderContact.INTERSTELLAR_FACTORS, longErrand);

        assertTrue(fact.contains("Deciat"));
        assertTrue(fact.contains("Farseer Inc"));
        assertTrue(fact.contains("see interstellar factors"));
        assertTrue(fact.length() <= FactLine.MAX_CHARS);
    }

    /**
     * The contact is stored as an identifier, so the words have to be chosen here. Every constant needs a phrase:
     * the switch is exhaustive, and a blank one would put an empty clause in the prompt.
     */
    @Test
    void everyContactHasAnErrandInTheGameOwnWords() {
        assertEquals("see the guardian technology broker",
                ObjectiveFactSource.errand(ReminderContact.TECHNOLOGY_BROKER_GUARDIAN));
        assertEquals("sell exobiology data at Vista Genomics",
                ObjectiveFactSource.errand(ReminderContact.VISTA_GENOMICS));
        for (ReminderContact contact : ReminderContact.values()) {
            String errand = ObjectiveFactSource.errand(contact);
            assertFalse(errand == null || errand.isBlank(), contact.name());
            assertFalse(errand.contains("_"), contact + " reads as its stored constant, not as words");
        }
    }

    @Test
    void anErrandIsCurrentWhileTheCommanderIsThereOrHeadedThere() {
        assertTrue(ObjectiveFactSource.isAhead("Deciat", "Deciat", null), "standing in the system");
        assertTrue(ObjectiveFactSource.isAhead("Deciat", "Sol", "Deciat"), "route plotted to it");
        assertTrue(ObjectiveFactSource.isAhead("deciat", "DECIAT", null), "system names are not case-sensitive");
    }

    /**
     * A reminder outlives its trip, so once the commander has left it describes a finished job. Reporting it as
     * the current objective would ground every later turn in it, and would contradict the HUD's route card, which
     * refuses the same reminder for the same reason.
     */
    @Test
    void anErrandLeftBehindIsNotTheCurrentObjective() {
        assertFalse(ObjectiveFactSource.isAhead("Deciat", "Sol", null));
        assertFalse(ObjectiveFactSource.isAhead("Deciat", "Sol", "Colonia"));
        assertFalse(ObjectiveFactSource.isAhead("Deciat", null, null), "an unknown position proves nothing");
    }

    /**
     * A reminder with no system is not about a place, so there is nowhere for it to be stale about.
     */
    @Test
    void aPlacelessReminderIsAlwaysCurrent() {
        assertTrue(ObjectiveFactSource.isAhead(null, "Sol", "Colonia"));
    }

    @Test
    void speaksOnEveryCommanderTurnRatherThanOnASubject() {
        assertTrue(new ObjectiveFactSource().isAmbient());
    }
}
