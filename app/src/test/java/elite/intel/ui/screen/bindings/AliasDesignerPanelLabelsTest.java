package elite.intel.ui.screen.bindings;

import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Installations are identified by row id and only described by storefront, because a storefront is a label
 * that several installations can share.
 */
class AliasDesignerPanelLabelsTest {

    /**
     * The defect this pins: keyed by storefront, the second of two Frontier copies replaced the first and was
     * silently left out of the comparison, so a divergence on it never appeared.
     */
    @Test
    void twoInstallationsFromOneStorefrontKeepSeparateIdentities() {
        InstallationRow first = row(1, "FRONTIER", "C:\\Games\\Frontier");
        InstallationRow second = row(2, "FRONTIER", "D:\\Games\\Frontier2");

        assertNotEquals(AliasDesignerPanel.keyOf(first), AliasDesignerPanel.keyOf(second));
        assertEquals(2, AliasDesignerPanel.labelsFor(List.of(first, second)).size());
    }

    @Test
    void aStorefrontHeldByOneInstallationIsNamedByItself() {
        Map<String, String> labels = AliasDesignerPanel.labelsFor(List.of(
                row(1, "STEAM", "E:\\SteamLibrary\\Elite Dangerous"),
                row(2, "EPIC", "E:\\EpicLibrary\\EliteDangerous")));

        assertEquals("STEAM", labels.get("1"));
        assertEquals("EPIC", labels.get("2"));
    }

    /** Twice "FRONTIER" would tell the user nothing about which one to go and look at. */
    @Test
    void aSharedStorefrontIsNamedWithItsFolder() {
        Map<String, String> labels = AliasDesignerPanel.labelsFor(List.of(
                row(1, "FRONTIER", "C:\\Games\\Frontier"),
                row(2, "FRONTIER", "D:\\Games\\Frontier2")));

        assertEquals("FRONTIER (C:\\Games\\Frontier)", labels.get("1"));
        assertEquals("FRONTIER (D:\\Games\\Frontier2)", labels.get("2"));
    }

    /** One shared storefront does not make the unshared ones verbose too. */
    @Test
    void onlyTheAmbiguousOnesCarryTheirFolder() {
        Map<String, String> labels = AliasDesignerPanel.labelsFor(List.of(
                row(1, "STEAM", "E:\\SteamLibrary\\Elite Dangerous"),
                row(2, "MANUAL", "C:\\One"),
                row(3, "MANUAL", "C:\\Two")));

        assertEquals("STEAM", labels.get("1"));
        assertEquals("MANUAL (C:\\One)", labels.get("2"));
        assertEquals("MANUAL (C:\\Two)", labels.get("3"));
    }

    private InstallationRow row(long id, String storefront, String rootPath) {
        return new InstallationRow(id, storefront, rootPath, false, false);
    }
}
