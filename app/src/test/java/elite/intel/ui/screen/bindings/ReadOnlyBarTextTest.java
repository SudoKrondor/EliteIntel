package elite.intel.ui.screen.bindings;

import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The bar's line while BindForge is read only (storage.md, <em>An installation missing - read only</em>; Storage S14a):
 * every missing installation is named. Compared through the bundles, so it holds in any language.
 */
class ReadOnlyBarTextTest {

    private static final InstallationRow EPIC = new InstallationRow(2, "EPIC", "E:\\Epic", false, true);
    private static final InstallationRow FRONTIER = new InstallationRow(3, "FRONTIER", "F:\\Frontier", false, true);

    @Test
    void everyMissingInstallationIsNamed() {
        String epic = getText("bindings.firstTimeStartup.place.installation", "EPIC", "E:\\Epic");
        String frontier = getText("bindings.firstTimeStartup.place.installation", "FRONTIER", "F:\\Frontier");

        assertEquals(getText("bindings.readOnly.bar", epic + ", " + frontier),
                ApplyBar.readOnlyText(List.of(EPIC, FRONTIER)));
    }
}
