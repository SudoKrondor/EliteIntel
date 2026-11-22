package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.install.Storefront;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every stored installation is backed up, with the bindings folder detection reports for it. */
class BackupSourcesTest {

    private static final Path BINDINGS = Path.of("Options", "Bindings");

    @Test
    void aDetectedInstallationTakesItsOwnBindingsFolder() {
        Path steamRoot = Path.of("Steam", "Elite Dangerous");
        Path protonBindings = Path.of("compatdata", "Bindings");
        List<InstallationRow> rows = List.of(new InstallationRow(1, "STEAM", steamRoot.toString(), false, false));
        List<GameInstallation> detected = List.of(new GameInstallation(Storefront.STEAM, steamRoot,
                GameInstallation.controlSchemesUnder(steamRoot), protonBindings));

        BackupSources sources = BackupSources.of(BINDINGS, rows, detected);

        assertEquals(BINDINGS, sources.bindingsFolder());
        assertEquals(protonBindings, sources.installations().getFirst().bindingsFolder());
    }

    @Test
    void anInstallationAddedByHandOrMissingIsStillBackedUp() {
        Path manual = Path.of("Games", "Frontier");
        Path unplugged = Path.of("E:", "Epic");
        List<InstallationRow> rows = List.of(
                new InstallationRow(1, "MANUAL", manual.toString(), true, false),
                new InstallationRow(2, "EPIC", unplugged.toString(), false, true));

        BackupSources sources = BackupSources.of(BINDINGS, rows, List.of());

        assertEquals(2, sources.installations().size());
        assertNull(sources.installations().getFirst().bindingsFolder(), "covered by the folder Elite-Intel is set to");
        assertTrue(sources.installations().get(1).missing(), "listed, so the backup can name it as not read");
    }
}
