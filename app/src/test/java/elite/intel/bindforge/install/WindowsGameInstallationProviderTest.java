package elite.intel.bindforge.install;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Detection is driven from fixture folders rather than this machine, so the tests run anywhere and describe
 * the shapes the storefronts really produce.
 */
class WindowsGameInstallationProviderTest {

    @TempDir
    Path machine;

    private Path bindings;
    private Path steamRoot;
    private Path epicManifests;
    private Path epicInstallLog;

    @BeforeEach
    void layOutTheMachine() throws IOException {
        bindings = Files.createDirectories(machine.resolve("AppData/Options/Bindings"));
        steamRoot = Files.createDirectories(machine.resolve("Program Files/Steam"));
        epicManifests = Files.createDirectories(machine.resolve("ProgramData/Epic/Manifests"));
        epicInstallLog = machine.resolve("AppData/Local/com.epicgames.eglv2/installations.jsonl");
    }

    @Test
    void findsASteamInstallInALibraryTheIndexNames() throws IOException {
        Path library = machine.resolve("E/SteamLibrary");
        Path root = gameAt(library.resolve("steamapps/common/Elite Dangerous"));
        writeLibraryIndex(library, "359320");

        List<GameInstallation> found = provider().findInstallations();

        assertEquals(1, found.size());
        assertEquals(Storefront.STEAM, found.getFirst().storefront());
        assertEquals(root, found.getFirst().root());
    }

    /**
     * The worked example from the install-paths reference: a library on another drive is still found, because
     * the index names it. A non-default location is not an undetectable one.
     */
    @Test
    void findsASteamInstallOutsideTheDefaultLibrary() throws IOException {
        Path library = machine.resolve("SomeOther/Drive/Games");
        gameAt(library.resolve("steamapps/common/Elite Dangerous"));
        writeLibraryIndex(library, "359320");

        assertEquals(1, provider().findInstallations().size());
    }

    @Test
    void ignoresALibraryThatDoesNotListTheGame() throws IOException {
        Path library = machine.resolve("E/SteamLibrary");
        gameAt(library.resolve("steamapps/common/Elite Dangerous"));
        writeLibraryIndex(library, "730");

        assertTrue(provider().findInstallations().isEmpty(),
                "the folder exists, but Steam does not say the game is installed there");
    }

    @Test
    void findsAnEpicInstallFromItsManifest() throws IOException {
        Path root = gameAt(machine.resolve("EpicLibrary/EliteDangerous"));
        writeEpicManifest("ed.item", "Elite Dangerous", root);

        List<GameInstallation> found = provider().findInstallations();

        assertEquals(1, found.size());
        assertEquals(Storefront.EPIC, found.getFirst().storefront());
        assertEquals(root, found.getFirst().root());
    }

    /**
     * Epic ships the soundtrack as its own title under a near-identical display name. It is told apart by the
     * folder it points at, never by that name.
     */
    @Test
    void ignoresTheEpicSoundtrackWhichIsNamedAlmostIdentically() throws IOException {
        Path soundtrack = Files.createDirectories(machine.resolve("EpicLibrary/EliteDangerousSoundtrack"));
        writeEpicManifest("soundtrack.item", "Elite Dangerous Original Soundtrack", soundtrack);

        assertTrue(provider().findInstallations().isEmpty(),
                "a title with no ControlSchemes folder is not an installation BindForge can work with");
    }

    /**
     * An installation can hold several products; only the Live one counts. A copy carrying just Horizons is
     * not something BindForge touches.
     */
    @Test
    void ignoresAnInstallThatOnlyHasTheLegacyProduct() throws IOException {
        Path root = machine.resolve("EpicLibrary/EliteDangerous");
        Files.createDirectories(root.resolve("Products/elite-dangerous-64/ControlSchemes"));
        writeEpicManifest("ed.item", "Elite Dangerous", root);

        assertTrue(provider().findInstallations().isEmpty());
    }

    /**
     * The new Epic launcher keeps no {@code .item} files. Its per-user log is the only record, and on the machine
     * this was found on, {@code C:\ProgramData\Epic} did not exist at all.
     */
    @Test
    void findsAnEpicInstallFromTheNewLaunchersLogAlone() throws IOException {
        Files.delete(epicManifests);
        Path root = gameAt(machine.resolve("Users/Alan/Epic Games/EliteDangerous"));
        writeEpicInstallLog(
                logHeader(),
                logEntry("14D2F9CD66D75F107066CC163BAC3CD2", root, false));

        List<GameInstallation> found = provider().findInstallations();

        assertEquals(1, found.size());
        assertEquals(Storefront.EPIC, found.getFirst().storefront());
        assertEquals(root, found.getFirst().root());
    }

    /**
     * Epic lists the game and the small add-on it installs alongside as two installations with the same folder.
     * That is still one installation.
     */
    @Test
    void reportsOneInstallationForTheGameAndItsAddOnInTheSameFolder() throws IOException {
        Path root = gameAt(machine.resolve("Users/Alan/Epic Games/EliteDangerous"));
        writeEpicInstallLog(
                logHeader(),
                logEntry("14D2F9CD66D75F107066CC163BAC3CD2", root, false),
                logEntry("7B9DB0E0CC3653D6EAA51F2068E56256", root, false));

        assertEquals(1, provider().findInstallations().size());
    }

    /** An installation the log last marks deleted is dropped, even when its folder is still on disk. */
    @Test
    void dropsAnInstallTheNewLaunchersLogMarksDeleted() throws IOException {
        Path removed = gameAt(machine.resolve("EpicGames/EliteDangerous"));
        Path current = gameAt(machine.resolve("Users/Alan/Epic Games/EliteDangerous"));
        writeEpicInstallLog(
                logHeader(),
                logEntry("0B38BBAEDE84E2843AC565EB4279A249", removed, false),
                logEntry("0B38BBAEDE84E2843AC565EB4279A249", removed, true),
                logEntry("14D2F9CD66D75F107066CC163BAC3CD2", current, false));

        List<GameInstallation> found = provider().findInstallations();

        assertEquals(List.of(current), found.stream().map(GameInstallation::root).toList());
    }

    /** A later line for the same installation replaces the earlier one: the log records a move by rewriting it. */
    @Test
    void takesTheLastLineForAnInstallationInTheNewLaunchersLog() throws IOException {
        Path before = gameAt(machine.resolve("Old/EliteDangerous"));
        Path after = gameAt(machine.resolve("New/EliteDangerous"));
        writeEpicInstallLog(
                logHeader(),
                logEntry("14D2F9CD66D75F107066CC163BAC3CD2", before, false),
                logEntry("14D2F9CD66D75F107066CC163BAC3CD2", after, false));

        List<GameInstallation> found = provider().findInstallations();

        assertEquals(List.of(after), found.stream().map(GameInstallation::root).toList());
    }

    @Test
    void skipsAnUnreadableLineInTheNewLaunchersLog() throws IOException {
        Path root = gameAt(machine.resolve("Users/Alan/Epic Games/EliteDangerous"));
        writeEpicInstallLog(
                logHeader(),
                "{not json",
                logEntry("14D2F9CD66D75F107066CC163BAC3CD2", root, false));

        assertEquals(1, provider().findInstallations().size());
    }

    /** Players on either launcher exist, so both records are read; a folder both name is one installation. */
    @Test
    void readsBothEpicLaunchersRecords() throws IOException {
        Path oldLauncher = gameAt(machine.resolve("EpicLibrary/EliteDangerous"));
        Path newLauncher = gameAt(machine.resolve("Users/Alan/Epic Games/EliteDangerous"));
        writeEpicManifest("ed.item", "Elite Dangerous", oldLauncher);
        writeEpicManifest("again.item", "Elite Dangerous", newLauncher);
        writeEpicInstallLog(logHeader(), logEntry("14D2F9CD66D75F107066CC163BAC3CD2", newLauncher, false));

        List<GameInstallation> found = provider().findInstallations();

        // WHY: a set, because the order Files.list gives the .item files in is the file system's, not ours.
        assertEquals(2, found.size());
        assertEquals(Set.of(oldLauncher, newLauncher), found.stream().map(GameInstallation::root).collect(Collectors.toSet()));
    }

    @Test
    void reportsOneInstallationWhenTwoLibrariesNameTheSameFolder() throws IOException {
        Path library = machine.resolve("E/SteamLibrary");
        gameAt(library.resolve("steamapps/common/Elite Dangerous"));
        writeLibraryIndex(steamRoot.resolve("config/libraryfolders.vdf"), library, "359320");
        writeLibraryIndex(steamRoot.resolve("steamapps/libraryfolders.vdf"), library, "359320");

        assertEquals(1, provider().findInstallations().size(),
                "Steam keeps two copies of the index; the same folder in both is still one installation");
    }

    @Test
    void reportsBothStorefrontsWhenTheMachineHasBoth() throws IOException {
        Path steamLibrary = machine.resolve("E/SteamLibrary");
        gameAt(steamLibrary.resolve("steamapps/common/Elite Dangerous"));
        writeLibraryIndex(steamLibrary, "359320");
        writeEpicManifest("ed.item", "Elite Dangerous", gameAt(machine.resolve("EpicLibrary/EliteDangerous")));

        List<GameInstallation> found = provider().findInstallations();

        assertEquals(2, found.size());
        assertEquals(List.of(Storefront.STEAM, Storefront.EPIC), found.stream().map(GameInstallation::storefront).toList());
    }

    /**
     * Every Windows installation reads the one shared bindings folder. The device files are the per-install
     * ones.
     */
    @Test
    void everyInstallationReportsTheSharedBindingsFolderAndItsOwnDeviceFiles() throws IOException {
        Path steamLibrary = machine.resolve("E/SteamLibrary");
        Path steamRootFolder = gameAt(steamLibrary.resolve("steamapps/common/Elite Dangerous"));
        writeLibraryIndex(steamLibrary, "359320");
        Path epicRoot = gameAt(machine.resolve("EpicLibrary/EliteDangerous"));
        writeEpicManifest("ed.item", "Elite Dangerous", epicRoot);

        List<GameInstallation> found = provider().findInstallations();

        assertEquals(bindings, found.getFirst().bindingsFolder());
        assertEquals(bindings, found.get(1).bindingsFolder());
        assertEquals(steamRootFolder.resolve("Products/elite-dangerous-odyssey-64/ControlSchemes/DeviceMappings.xml"),
                found.getFirst().deviceMappings());
        assertEquals(steamRootFolder.resolve("Products/elite-dangerous-odyssey-64/ControlSchemes/DeviceButtonMaps"),
                found.getFirst().deviceButtonMaps());
        assertNotEquals(found.getFirst().deviceMappings(), found.get(1).deviceMappings(),
                "the device files are per installation, not shared");
    }

    /**
     * Nothing records where a Frontier-launcher install went, so it is found only where Frontier publishes
     * that it will be.
     */
    @Test
    void findsAFrontierInstallAtItsPublishedDefault() throws IOException {
        Path root = gameAt(machine.resolve("Program Files (x86)/Frontier"));

        List<GameInstallation> found =
                provider(new WindowsGameInstallationProvider.Candidate(Storefront.FRONTIER, root)).findInstallations();

        assertEquals(1, found.size());
        assertEquals(Storefront.FRONTIER, found.getFirst().storefront());
    }

    /**
     * The alternative Frontier location spells the folder with an underscore. The space-spelled folder beside
     * it is the user-configuration tree and is a different thing entirely.
     */
    @Test
    void findsTheUnderscoreSpelledFrontierInstall() throws IOException {
        Path underscore = gameAt(machine.resolve("AppData/Local/Frontier_Developments"));
        Files.createDirectories(machine.resolve("AppData/Local/Frontier Developments"));

        List<GameInstallation> found = provider(
                new WindowsGameInstallationProvider.Candidate(Storefront.FRONTIER, underscore)).findInstallations();

        assertEquals(1, found.size());
        assertEquals(underscore, found.getFirst().root());
    }

    /** Oculus really does double the folder; correcting it finds nothing. */
    @Test
    void findsAnOculusInstallUnderItsDoubledFolder() throws IOException {
        Path root = gameAt(machine.resolve(
                "Program Files/Oculus/Software/Software/frontier-developments-plc-elite-dangerous"));

        List<GameInstallation> found =
                provider(new WindowsGameInstallationProvider.Candidate(Storefront.OCULUS, root)).findInstallations();

        assertEquals(1, found.size());
        assertEquals(Storefront.OCULUS, found.getFirst().storefront());
    }

    @Test
    void ignoresAPublishedDefaultWithNoInstallAtIt() {
        List<GameInstallation> found = provider(new WindowsGameInstallationProvider.Candidate(
                Storefront.FRONTIER, machine.resolve("Program Files (x86)/Frontier"))).findInstallations();

        assertTrue(found.isEmpty(), "the published default is a place to look, not a promise");
    }

    @Test
    void findsNothingOnAMachineWithNeitherStorefront() {
        assertTrue(provider().findInstallations().isEmpty(), "no installation found is an ordinary answer");
    }

    private WindowsGameInstallationProvider provider(WindowsGameInstallationProvider.Candidate... defaults) {
        return new WindowsGameInstallationProvider(bindings, List.of(steamRoot), epicManifests,
                List.of(epicInstallLog), List.of(defaults));
    }

    /** Creates the Live product's ControlSchemes folder, which is what makes a folder an installation. */
    private Path gameAt(Path root) throws IOException {
        Files.createDirectories(GameInstallation.controlSchemesUnder(root));
        return root;
    }

    private void writeLibraryIndex(Path library, String appId) throws IOException {
        writeLibraryIndex(steamRoot.resolve("config/libraryfolders.vdf"), library, appId);
    }

    private void writeLibraryIndex(Path vdf, Path library, String appId) throws IOException {
        Files.createDirectories(vdf.getParent());
        Files.writeString(vdf, """
                "libraryfolders"
                {
                    "0"
                    {
                        "path"          "%s"
                        "label"         ""
                        "contentid"     "1234567890"
                        "totalsize"     "0"
                        "apps"
                        {
                            "%s"        "84934656512"
                        }
                    }
                }
                """.formatted(library.toString().replace("\\", "\\\\"), appId));
    }

    private void writeEpicManifest(String fileName, String displayName, Path installLocation) throws IOException {
        Files.writeString(epicManifests.resolve(fileName), """
                {
                  "DisplayName": "%s",
                  "InstallLocation": "%s",
                  "LaunchExecutable": "EDLaunch.exe"
                }
                """.formatted(displayName, installLocation.toString().replace("\\", "\\\\")));
    }

    private void writeEpicInstallLog(String... lines) throws IOException {
        Files.createDirectories(epicInstallLog.getParent());
        Files.write(epicInstallLog, List.of(lines));
    }

    private static String logHeader() {
        return "{\"version\":1,\"writtenAt\":\"2026-11-24T02:57:03Z\",\"entries\":3}";
    }

    /** One line in the shape the new launcher writes, cut to the fields detection reads plus a few around them. */
    private static String logEntry(String installationId, Path dir, boolean deleted) {
        if (deleted) {
            return "{\"installationId\":\"%s\",\"deleted\":true,\"writtenAt\":\"2026-11-20T03:26:50Z\"}"
                    .formatted(installationId);
        }
        return ("{\"installationId\":\"%s\",\"complete\":true,\"dir\":\"%s\",\"platform\":\"Windows\","
                + "\"launchCommand\":\"/Epic\",\"launchExecutable\":\"EDLaunch.exe\","
                + "\"writtenAt\":\"2026-11-20T03:53:28Z\"}")
                .formatted(installationId, dir.toString().replace("\\", "\\\\"));
    }
}
