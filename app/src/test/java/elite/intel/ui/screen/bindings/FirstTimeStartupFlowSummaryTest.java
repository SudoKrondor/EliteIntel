package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devicefiles.FrontierStockButtonMaps;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.bindforge.storage.ConfigurationFiles;
import elite.intel.bindforge.storage.FirstSetupFiles;
import elite.intel.bindforge.storage.FirstSetupReading;
import elite.intel.bindforge.storage.InstallationFiles;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Storage S14a removed the stop when no installation can be reached, and with it the one message that named the
 * unplugged drive - so the summary has to say it.
 */
class FirstTimeStartupFlowSummaryTest {

    private static final InstallationRow UNPLUGGED = new InstallationRow(1, "STEAM", "E:\\Steam", false, true);
    private static final InstallationFiles READ =
            new InstallationFiles(2, "EPIC", Path.of("Epic"), null, Map.of());

    /**
     * The defect this pins: it said "Your installations hold the same device files" with none read at all.
     */
    @Test
    void withNothingReachableItSaysSoAndNamesTheInstallation() throws IOException {
        String text = summary(List.of(), List.of(UNPLUGGED));

        assertFalse(text.contains(getText("bindings.firstTimeStartup.summary.identical", 0)));
        assertTrue(text.contains(getText("bindings.firstTimeStartup.summary.noInstallations")));
        assertTrue(text.contains(getText("bindings.aliasDesigner.firstSetup.unreachable", "Steam on E")));
    }

    @Test
    void anInstallationLeftOutBesideOneReadIsNamed() throws IOException {
        String text = summary(List.of(READ), List.of(UNPLUGGED));

        assertTrue(text.contains(getText("bindings.firstTimeStartup.summary.identical", 0)));
        assertTrue(text.contains(getText("bindings.aliasDesigner.firstSetup.unreachable", "Steam on E")));
    }

    @Test
    void withEveryInstallationReadNothingIsSaidOfOneLeftOut() throws IOException {
        String text = summary(List.of(READ), List.of());

        assertTrue(text.contains(getText("bindings.firstTimeStartup.summary.identical", 0)));
        assertFalse(text.contains(getText("bindings.aliasDesigner.firstSetup.unreachable", "Steam on E")));
    }

    private static String summary(List<InstallationFiles> read, List<InstallationRow> unreachable) throws IOException {
        FirstSetupReading reading = FirstSetupReading.of(Path.of("Bindings"), Map.of(), read, unreachable,
                FrontierStockDevices.getInstance(), FrontierStockButtonMaps.getInstance());
        ConfigurationFiles identical = FirstSetupFiles.identical(reading);
        return FirstTimeStartupFlow.identicalSummary(reading, identical, Map.of("1", "Steam on E"));
    }
}
