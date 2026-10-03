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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Finds Elite Dangerous installations on Windows, through each storefront's own manifest.
 * <p>
 * Detection is not uniformly strong, and the difference is whether a storefront records where an installation
 * actually went:
 * <ul>
 *   <li><strong>Steam and Epic keep a manifest</strong> - {@code libraryfolders.vdf} and the {@code .item}
 *       files - so they are found wherever the user put them.</li>
 *   <li><strong>Frontier and Oculus record nothing</strong>, so they are found only at the locations their
 *       publishers document. One moved off those is undetectable here by construction, and reaches BindForge
 *       through the manual add instead.</li>
 * </ul>
 * Which is why the manual add is not a fallback of last resort: for the Frontier launcher it is the only
 * mechanism.
 */
public class WindowsGameInstallationProvider implements GameInstallationProvider {

    private static final Logger log = LogManager.getLogger(WindowsGameInstallationProvider.class);

    /** Elite Dangerous on Steam. The same id appears in {@code libraryfolders.vdf}'s apps map. */
    private static final String STEAM_APP_ID = "359320";
    private static final String STEAM_REGISTRY_VALUE = "InstallPath";
    private static final Path EPIC_MANIFESTS =
            Path.of("C:\\ProgramData\\Epic\\EpicGamesLauncher\\Data\\Manifests");

    // WHY: "path" always precedes its library's "apps" block in the VDF, so a single forward pass can
    // attribute an app id to the library it was listed under. Values escape backslashes and nothing else.
    private static final Pattern VDF_PATH = Pattern.compile("\"path\"\\s+\"(.+?)\"");
    private static final Pattern VDF_APP_ID = Pattern.compile("\"(\\d+)\"\\s+\"\\d+\"");

    private final Path bindingsFolder;
    private final List<Path> steamRoots;
    private final Path epicManifests;
    private final List<Candidate> defaultRoots;

    /** A place an installation is published to live, used when nothing records where it actually went. */
    record Candidate(Storefront storefront, Path root) {
    }

    /**
     * @param bindingsFolder the one shared {@code Options\Bindings} folder every Windows installation reads
     */
    public WindowsGameInstallationProvider(Path bindingsFolder) {
        this(bindingsFolder, steamRootsFromRegistry(), EPIC_MANIFESTS, publishedDefaultRoots());
    }

    /** Test seam: the same, with the storefronts' own locations supplied rather than read from this machine. */
    WindowsGameInstallationProvider(Path bindingsFolder, List<Path> steamRoots, Path epicManifests,
                                    List<Candidate> defaultRoots) {
        this.bindingsFolder = bindingsFolder;
        this.steamRoots = steamRoots;
        this.epicManifests = epicManifests;
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
            for (Path library : steamLibrariesHoldingTheGame(steamRoot)) {
                Path root = library.resolve("steamapps").resolve("common").resolve("Elite Dangerous");
                if (GameInstallation.looksLikeAnInstall(root)) {
                    installations.add(installationAt(Storefront.STEAM, root));
                }
            }
        }
        return installations;
    }

    /**
     * The library folders whose {@code apps} map lists Elite Dangerous.
     * <p>
     * Steam keeps two copies of {@code libraryfolders.vdf} and reconciles them at launch, so both are read and
     * whichever names the game is believed.
     */
    private List<Path> steamLibrariesHoldingTheGame(Path steamRoot) {
        List<Path> libraries = new ArrayList<>();
        for (Path vdf : List.of(steamRoot.resolve("config").resolve("libraryfolders.vdf"),
                steamRoot.resolve("steamapps").resolve("libraryfolders.vdf"))) {
            libraries.addAll(parseLibrariesHoldingTheGame(vdf));
        }
        return libraries;
    }

    private List<Path> parseLibrariesHoldingTheGame(Path vdf) {
        if (!Files.isRegularFile(vdf)) return List.of();
        try {
            return librariesListingTheGame(Files.readString(vdf));
        } catch (IOException e) {
            log.warn("Could not read Steam library index {}: {}", vdf, e.getMessage());
            return List.of();
        }
    }

    /**
     * Walks the file once, remembering the library path currently in scope and keeping it when the game's app
     * id turns up before the next one.
     */
    private List<Path> librariesListingTheGame(String vdfText) {
        List<Path> libraries = new ArrayList<>();
        Matcher token = Pattern.compile(VDF_PATH.pattern() + "|" + VDF_APP_ID.pattern()).matcher(vdfText);
        String currentLibrary = null;
        while (token.find()) {
            if (token.group(1) != null) {
                currentLibrary = token.group(1).replace("\\\\", "\\");
            } else if (STEAM_APP_ID.equals(token.group(2)) && currentLibrary != null) {
                addPathIfUsable(libraries, currentLibrary);
                currentLibrary = null;
            }
        }
        return libraries;
    }

    private List<GameInstallation> epicInstallations() {
        if (!Files.isDirectory(epicManifests)) return List.of();
        try (Stream<Path> items = Files.list(epicManifests)) {
            return items.filter(item -> item.getFileName().toString().endsWith(".item"))
                    .map(this::installRootFromManifest)
                    .filter(GameInstallation::looksLikeAnInstall)
                    // WHY: judged by that folder, never by the manifest's DisplayName. Epic ships the
                    // soundtrack as its own title under a near-identical name, and it has no ControlSchemes.
                    .map(root -> installationAt(Storefront.EPIC, root))
                    .toList();
        } catch (IOException e) {
            log.warn("Could not list Epic manifests in {}: {}", epicManifests, e.getMessage());
            return List.of();
        }
    }

    private Path installRootFromManifest(Path manifest) {
        try {
            JsonObject json = JsonParser.parseString(Files.readString(manifest)).getAsJsonObject();
            if (!json.has("InstallLocation")) return manifest;
            return Path.of(json.get("InstallLocation").getAsString());
        } catch (IOException | JsonParseException | IllegalStateException | InvalidPathException e) {
            log.warn("Could not read Epic manifest {}: {}", manifest, e.getMessage());
            // WHY: the manifest's own path can never look like an install, so returning it drops this entry
            // without a null the caller has to filter for.
            return manifest;
        }
    }

    private GameInstallation installationAt(Storefront storefront, Path root) {
        return new GameInstallation(storefront, root, GameInstallation.controlSchemesUnder(root), bindingsFolder);
    }

    private void addPathIfUsable(List<Path> libraries, String library) {
        try {
            libraries.add(Path.of(library));
        } catch (InvalidPathException e) {
            log.warn("Steam library index names a path this platform cannot use: {}", library);
        }
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
