package elite.intel.ui.screen.bindings;

import org.junit.jupiter.api.Test;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Run First-Time Startup Again's confirmation (file-manager.md, <em>Game Install Locations</em>; Storage S19, Q1): it
 * says the run starts now, and adds that edits not applied are thrown away only when there are any (E10R, D4).
 * Compared through the bundles, so it holds in any language.
 */
class GameInstallLocationsRunAgainTextTest {

    @Test
    void withNoEditsItSaysOnlyThatItRunsNow() {
        assertEquals(getText("bindings.installLocations.runAgain.confirm.text"),
                GameInstallLocationsPanel.runAgainText(false));
    }

    @Test
    void withEditsItAlsoSaysTheyAreThrownAway() {
        assertEquals(getText("bindings.installLocations.runAgain.confirm.text") + "\n\n"
                        + getText("bindings.installLocations.runAgain.confirm.edits"),
                GameInstallLocationsPanel.runAgainText(true));
    }
}
