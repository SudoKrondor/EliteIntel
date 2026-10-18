package elite.intel.bindforge.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Linux detection, driven from a fixture home folder: every place a Steam client can live is read, several clients
 * at once are all searched, and each installation carries the bindings folder of its own Proton prefix.
 */
class LinuxGameInstallationProviderTest {

    @TempDir
    Path home;

    @Test
    void findsTheGameInANativeSteamLibrary() throws IOException {
        Path steam = home.resolve(".local/share/Steam");
        Path root = gameIn(steam);
        writeLibraryIndex(steam, steam);

        List<GameInstallation> found = provider().findInstallations();

        assertEquals(1, found.size());
        assertEquals(Storefront.STEAM, found.getFirst().storefront());
        assertEquals(root, found.getFirst().root());
        assertEquals(LinuxGameInstallationProvider.protonBindingsIn(steam), found.getFirst().bindingsFolder());
        assertTrue(found.getFirst().bindingsFolder().startsWith(steam.resolve("steamapps/compatdata/359320/pfx")));
    }

    /**
     * Native, Flatpak and Snap Steam are independent; a game in each is a separate installation.
     */
    @Test
    void everySteamClientIsSearched() throws IOException {
        Path flatpak = home.resolve(".var/app/com.valvesoftware.Steam/.local/share/Steam");
        Path snap = home.resolve("snap/steam/common/.local/share/Steam");
        gameIn(flatpak);
        gameIn(snap);
        writeLibraryIndex(flatpak, flatpak);
        writeLibraryIndex(snap, snap);

        assertEquals(2, provider().findInstallations().size());
    }

    /**
     * {@code ~/.steam/steam} is normally a link to {@code ~/.local/share/Steam}: one client, one installation.
     */
    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "Windows refuses to create symbolic links for an ordinary user")
    void aSteamFolderReachedThroughALinkIsReadOnce() throws IOException {
        Path steam = home.resolve(".local/share/Steam");
        gameIn(steam);
        writeLibraryIndex(steam, steam);
        Files.createDirectories(home.resolve(".steam"));
        Files.createSymbolicLink(home.resolve(".steam/steam"), steam);

        assertEquals(1, provider().findInstallations().size());
    }

    /**
     * A library on another drive holds the game, and its Proton prefix sits in that library too.
     */
    @Test
    void theGameInAnotherLibraryCarriesThatLibrarysPrefix() throws IOException {
        Path steam = home.resolve(".local/share/Steam");
        Path library = home.resolve("games/SteamLibrary");
        gameIn(library);
        writeLibraryIndex(steam, library);

        List<GameInstallation> found = provider().findInstallations();

        assertEquals(1, found.size());
        assertEquals(LinuxGameInstallationProvider.protonBindingsIn(library), found.getFirst().bindingsFolder());
    }

    @Test
    void noSteamAtAllFindsNothing() {
        assertTrue(provider().findInstallations().isEmpty());
    }

    private LinuxGameInstallationProvider provider() {
        return new LinuxGameInstallationProvider(LinuxGameInstallationProvider.steamRootsUnder(home));
    }

    private static Path gameIn(Path library) throws IOException {
        Path root = SteamLibraries.gameRootIn(library);
        Files.createDirectories(GameInstallation.controlSchemesUnder(root));
        return root;
    }

    private static void writeLibraryIndex(Path steamRoot, Path library) throws IOException {
        Path vdf = steamRoot.resolve("config/libraryfolders.vdf");
        Files.createDirectories(vdf.getParent());
        Files.writeString(vdf, """
                "libraryfolders"
                {
                \t"0"
                \t{
                \t\t"path"\t\t"%s"
                \t\t"apps"
                \t\t{
                \t\t\t"359320"\t\t"68620086451"
                \t\t}
                \t}
                }
                """.formatted(library));
    }
}
