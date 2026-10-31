package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.storage.DeviceCheck.State;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.ui.theme.HudPalette;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Map;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Game Install Locations' Status cell (file-manager.md, <em>Game Install Locations</em>; Storage S9a, tested in S9R):
 * with a working copy to compare with, Matches / Differs / Not found as text colour; without one, Found or Missing from
 * the stored list. Compared through the bundles, so it holds in any language.
 */
class GameInstallLocationsStatusTest {

    private static final InstallationRow STEAM = new InstallationRow(1, "STEAM", "C:\\Steam", false, false);
    private static final InstallationRow MISSING = new InstallationRow(2, "EPIC", "D:\\Epic", false, true);

    @Test
    void withNothingToCompareWithTheStoredListSays() {
        GameInstallLocationsPanel.Status found = GameInstallLocationsPanel.listedStatus(STEAM);
        GameInstallLocationsPanel.Status missing = GameInstallLocationsPanel.listedStatus(MISSING);

        assertEquals(getText("bindings.installLocations.state.found"), found.text());
        assertNull(found.colour(), "the table's own colour");
        assertEquals(getText("bindings.installLocations.state.missing"), missing.text());
        assertEquals(HudPalette.HUD_COLOR_ROLE_WARNING, missing.colour());
    }

    @Test
    void aMatchAndADifferenceAreTheirOwnColours() {
        assertStatus("bindings.installLocations.state.matches", HudPalette.HUD_COLOR_ROLE_SUCCESS,
                GameInstallLocationsPanel.statusOf(STEAM, Map.of(1L, State.MATCHES)));
        assertStatus("bindings.installLocations.state.differs", HudPalette.HUD_COLOR_ROLE_DANGER,
                GameInstallLocationsPanel.statusOf(STEAM, Map.of(1L, State.RESET_MISSING)));
    }

    /** One the check did not judge - not reachable - is not found. */
    @Test
    void anInstallationTheCheckDidNotReachIsNotFound() {
        assertStatus("bindings.installLocations.state.notFound", HudPalette.HUD_COLOR_ROLE_WARNING,
                GameInstallLocationsPanel.statusOf(MISSING, Map.of(1L, State.MATCHES)));
    }

    private static void assertStatus(String key, Color colour, GameInstallLocationsPanel.Status status) {
        assertEquals(getText(key), status.text());
        assertEquals(colour, status.colour());
    }
}
