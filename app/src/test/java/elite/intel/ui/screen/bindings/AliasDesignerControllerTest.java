package elite.intel.ui.screen.bindings;

import elite.intel.ui.screen.bindings.AliasDesignerController.OfferConditions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When the Alias Designer's naming offers may open (alias-designer.md, the identity question's built notes and the
 * hot-plug offer's rules): each condition alone holds them back. Not offered is held, not spent - the next refresh
 * asks again.
 */
class AliasDesignerControllerTest {

    private static final OfferConditions ALL_MET = new OfferConditions(true, true, true, true, false);

    @Test
    void theAutomaticOfferOpensWhenEveryConditionIsMet() {
        assertTrue(ALL_MET.automaticMayOpen());
    }

    @Test
    void eachConditionAloneHoldsTheAutomaticOfferBack() {
        assertFalse(new OfferConditions(false, true, true, true, false).automaticMayOpen(),
                "first-time startup not complete");
        assertFalse(new OfferConditions(true, false, true, true, false).automaticMayOpen(),
                "the working copy not read - the tab click is offering RESTORE (Alan, 2026-11-14)");
        assertFalse(new OfferConditions(true, true, false, true, false).automaticMayOpen(),
                "the hardware not listed");
        assertFalse(new OfferConditions(true, true, true, false, false).automaticMayOpen(),
                "the user not on the screen");
        assertFalse(new OfferConditions(true, true, true, true, true).automaticMayOpen(),
                "another dialog showing");
    }

    @Test
    void theHotPlugOfferOpensForAControllerWaitingWithNoChangeRunning() {
        assertTrue(ALL_MET.hotPlugMayOpen(true, false));
    }

    @Test
    void theHotPlugOfferWaitsForAControllerAndForAChangeToFinish() {
        assertFalse(ALL_MET.hotPlugMayOpen(false, false), "no controller waiting");
        assertFalse(ALL_MET.hotPlugMayOpen(true, true), "a change being made");
    }

    @Test
    void theHotPlugOfferKeepsTheAutomaticOffersConditions() {
        assertFalse(new OfferConditions(true, false, true, true, false).hotPlugMayOpen(true, false),
                "the working copy not read");
        assertFalse(new OfferConditions(true, true, true, true, true).hotPlugMayOpen(true, false),
                "another dialog showing - the Device Editor included");
    }
}
