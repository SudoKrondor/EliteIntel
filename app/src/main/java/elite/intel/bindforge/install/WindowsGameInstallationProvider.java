package elite.intel.bindforge.install;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Finds Elite Dangerous installations on Windows, through each storefront's own manifest.
 * <p>
 * Detection is not uniformly strong, and the difference is whether a storefront records where an installation
 * actually went:
 * <ul>
 *   <li><strong>Steam and Epic keep a manifest</strong> - {@code libraryfolders.vdf}, and for Epic the old
 *       launcher's {@code .item} files or the new launcher's {@code installations.jsonl} - so they are found
 *       wherever the user put them.</li>
 *   <li><strong>Frontier and Oculus record nothing</strong>, so they are found only at the locations their
 *       publishers document. One moved off those is undetectable here by construction, and reaches BindForge
 *       through the manual add instead.</li>
 * </ul>
 * Which is why the manual add is not a fallback of last resort: for the Frontier launcher it is the only
 * mechanism.
 */
public class WindowsGameInstallationProvider implements GameInstallationProvider {

    private static final Logger log = LogManager.getLogger(WindowsGameInstallationProvider.class);

    private static final String STEAM_REGISTRY_VALUE = "InstallPath";
    /** The old Epic launcher's records: one {@code .item} per installed title, shared by every user. */
    private static final Path EPIC_MANIFESTS =
            Path.of("C:\\ProgramData\\Epic\\EpicGamesLauncher\\Data\\Manifests");
    /** The new Epic launcher's ("eglv2") record, per user under {@code %LOCALAPPDATA%}; see {@link EpicInstallLog}. */
    private static final String EPIC_INSTALL_LOG = "com.epicgames.eglv2\\installations.jsonl";

    private final Path bindingsFolder;
    private final List<Path> steamRoots;
    private final Path epicManifests;
    private final List<Path> epicInstallLogs;
    private final List<Candidate> defaultRoots;

    /** A place an installation is published to live, used when nothing records where it actually went. */
    record Candidate(Storefront storefront, Path root) {
    }

    /**
     * @param bindingsFolder the one shared {@code Options\Bindings} folder every Windows installation reads
     */
    public WindowsGameInstallationProvider(Path bindingsFolder) {
        this(bindingsFolder, steamRootsFromRegistry(), EPIC_MANIFESTS, epicInstallLogsFromEnvironment(),
                publishedDefaultRoots());
    }

    /**
     * Test seam: the same, with the storefronts' own locations supplied rather than read from this machine.
     *
     * @param epicInstallLogs the new Epic launcher's {@code installations.jsonl}; empty where there is no
     *                        {@code %LOCALAPPDATA%} to find it under
     */
    WindowsGameInstallationProvider(Path bindingsFolder, List<Path> steamRoots, Path epicManifests,
                                    List<Path> epicInstallLogs, List<Candidate> defaultRoots) {
        this.bindingsFolder = bindingsFolder;
        this.steamRoots = steamRoots;
        this.epicManifests = epicManifests;
        this.epicInstallLogs = epicInstallLogs;
        this.defaultRoots = defaultRoots;
    }

    @Override
    public List<GameInstallation> findInstallations() {
        // WHY: keyed by real path, so the same folder found twice - by two Steam libraries, or through a
        // symlink - is reported once. LinkedHashMap keeps the order stable for anything that displays them.
        Map<Path, GameInstallation> found = new LinkedHashMap<>();
        for (GameInstallation installation : steamInstallations()) {
            found.putIfAbsent(realPathOf(installation.root()), installation);
        }
        for (GameInstallation installation : epicInstallations()) {
            found.putIfAbsent(realPathOf(installation.root()), installation);
        }
        for (GameInstallation installation : installationsAtPublishedDefaults()) {
            found.putIfAbsent(realPathOf(installation.root()), installation);
        }
        return List.copyOf(found.values());
    }

    /**
     * Frontier-launcher and Oculus installations, which are only found when they sit where their publisher
     * says they will.
     * <p>
     * Neither storefront records where an installation actually went, so one moved off these paths is
     * undetectable here by construction and reaches BindForge through the manual add instead. Looking is
     * still worth it: the defaults are where most of them are.
     */
    private List<GameInstallation> installationsAtPublishedDefaults() {
        return defaultRoots.stream()
                .filter(candidate -> GameInstallation.looksLikeAnInstall(candidate.root()))
                .map(candidate -> installationAt(candidate.storefront(), candidate.root()))
                .toList();
    }

    private List<GameInstallation> steamInstallations() {
        List<GameInstallation> installations = new ArrayList<>();
        for (Path steamRoot : steamRoots) {
            for (Path library : SteamLibraries.holdingTheGame(steamRoot)) {
                Path root = SteamLibraries.gameRootIn(library);
                if (GameInstallation.looksLikeAnInstall(root)) {
                    installations.add(installationAt(Storefront.STEAM, root));
                }
            }
        }
        return installations;
    }

    /**
     * Epic installations from both launchers' records. The old launcher's {@code .item} files and the new
     * launcher's install log are both read, because players on either launcher exist; a folder both name is one
     * installation, which {@link #findInstallations} sees to.
     */
    private List<GameInstallation> epicInstallations() {
        List<Path> logs = epicInstallLogs.stream().filter(Files::isRegularFile).toList();
        boolean manifests = Files.isDirectory(epicManifests);
        if (!manifests && logs.isEmpty()) {
            // WHY: said out loud, so a support bundle shows that Epic was looked for and had no records,
            // rather than leaving a missing Epic install unexplained.
            log.info("No Epic launcher records: neither {} nor {} exists", epicManifests, epicInstallLogs);
            return List.of();
        }
        List<Path> roots = new ArrayList<>();
        if (manifests) roots.addAll(installRootsFromManifests());
        for (Path installLog : logs) {
            roots.addAll(EpicInstallLog.liveInstallFolders(installLog));
        }
        return roots.stream()
                // WHY: judged by that folder, never by a title's name. Epic ships the soundtrack as its own
                // title under a near-identical name, and it has no ControlSchemes.
                .filter(this::looksLikeAnEpicInstall)
                .map(root -> installationAt(Storefront.EPIC, root))
                .toList();
    }

    private List<Path> installRootsFromManifests() {
        try (Stream<Path> items = Files.list(epicManifests)) {
            return items.filter(item -> item.getFileName().toString().endsWith(".item"))
                    .flatMap(item -> installRootFromManifest(item).stream())
                    .toList();
        } catch (IOException e) {
            log.warn("Could not list Epic manifests in {}: {}", epicManifests, e.getMessage());
            return List.of();
        }
    }

    /**
     * Whether an Epic record's folder is an installation, logging the ones that are not, so a support bundle
     * shows a record Epic kept for a folder BindForge cannot use.
     */
    private boolean looksLikeAnEpicInstall(Path root) {
        if (GameInstallation.looksLikeAnInstall(root)) return true;
        log.info("Epic records {}, which has no {} folder; not an installation BindForge can use",
                root, GameInstallation.controlSchemesUnder(root));
        return false;
    }

    private Optional<Path> installRootFromManifest(Path manifest) {
        try {
            JsonObject json = JsonParser.parseString(Files.readString(manifest)).getAsJsonObject();
            if (!json.has("InstallLocation")) return Optional.empty();
            return Optional.of(Path.of(json.get("InstallLocation").getAsString()));
        } catch (IOException | JsonParseException | IllegalStateException | InvalidPathException e) {
            log.warn("Could not read Epic manifest {}: {}", manifest, e.getMessage());
            return Optional.empty();
        }
    }

    private GameInstallation installationAt(Storefront storefront, Path root) {
        return new GameInstallation(storefront, root, GameInstallation.controlSchemesUnder(root), bindingsFolder);
    }

    /**
     * Resolves symlinks so the same installation reached by two routes is recognised as one, falling back to
     * the path as given when it cannot be resolved.
     */
    private Path realPathOf(Path root) {
        try {
            return root.toRealPath();
        } catch (IOException | UncheckedIOException e) {
            return root.toAbsolutePath().normalize();
        }
    }

    /**
     * The locations Frontier publishes for the launcher's own installs, and Oculus's for its store.
     * <p>
     * Two spellings sit side by side here and neither is a typo. {@code Frontier Developments} with a space
     * is the user-configuration tree; {@code Frontier_Developments} with an underscore is the launcher's
     * alternative install location. Oculus's {@code Software\Software} is doubled on purpose - that is what
     * Frontier publishes and what Oculus actually creates, and "correcting" it finds nothing.
     */
    private static List<Candidate> publishedDefaultRoots() {
        List<Candidate> roots = new ArrayList<>();
        roots.add(new Candidate(Storefront.FRONTIER, Path.of("C:\\Program Files (x86)\\Frontier")));
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            roots.add(new Candidate(Storefront.FRONTIER, Path.of(localAppData, "Frontier_Developments")));
        }
        roots.add(new Candidate(Storefront.OCULUS, Path.of(
                "C:\\Program Files\\Oculus\\Software\\Software\\frontier-developments-plc-elite-dangerous")));
        return roots;
    }

    private static List<Path> epicInstallLogsFromEnvironment() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.isBlank()) return List.of();
        try {
            return List.of(Path.of(localAppData).resolve(EPIC_INSTALL_LOG));
        } catch (InvalidPathException e) {
            log.debug("LOCALAPPDATA is not a usable path: {}", e.toString());
            return List.of();
        }
    }

    /**
     * Where the Steam client itself is installed - not where any game is. The registry holds this much
     * reliably; the library index inside it is what knows about individual games.
     */
    private static List<Path> steamRootsFromRegistry() {
        List<Path> roots = new ArrayList<>();
        readRegistryPath(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\WOW6432Node\\Valve\\Steam", roots);
        readRegistryPath(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\Valve\\Steam", roots);
        readRegistryPath(WinReg.HKEY_CURRENT_USER, "Software\\Valve\\Steam", roots);
        return roots;
    }

    private static void readRegistryPath(WinReg.HKEY hive, String key, List<Path> roots) {
        try {
            if (!Advapi32Util.registryValueExists(hive, key, STEAM_REGISTRY_VALUE)) return;
            String value = Advapi32Util.registryGetStringValue(hive, key, STEAM_REGISTRY_VALUE);
            if (value != null && !value.isBlank()) roots.add(Path.of(value));
        } catch (Win32Exception | InvalidPathException | UnsatisfiedLinkError | NoClassDefFoundError e) {
            // WHY: a missing key is the ordinary case on a machine without Steam, and this also runs on
            // non-Windows in tests, where the native library is absent. Neither is a failure worth raising.
            log.debug("Steam registry key {} unreadable: {}", key, e.toString());
        }
    }
}
