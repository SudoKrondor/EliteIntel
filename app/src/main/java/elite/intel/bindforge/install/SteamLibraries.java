package elite.intel.bindforge.install;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Where a Steam client keeps Elite Dangerous: its library index, {@code libraryfolders.vdf}, read the same way on
 * Windows and Linux. The providers differ only in where they look for Steam itself.
 */
final class SteamLibraries {

    private static final Logger log = LogManager.getLogger(SteamLibraries.class);

    /**
     * Elite Dangerous on Steam. The same id appears in {@code libraryfolders.vdf}'s apps map.
     */
    static final String APP_ID = "359320";

    // WHY: "path" always precedes its library's "apps" block in the VDF, so a single forward pass can
    // attribute an app id to the library it was listed under. Values escape backslashes and nothing else.
    private static final Pattern VDF_PATH = Pattern.compile("\"path\"\\s+\"(.+?)\"");
    private static final Pattern VDF_APP_ID = Pattern.compile("\"(\\d+)\"\\s+\"\\d+\"");

    private SteamLibraries() {
    }

    /**
     * The game's folder inside a Steam library, whether or not it is there.
     */
    static Path gameRootIn(Path library) {
        return library.resolve("steamapps").resolve("common").resolve("Elite Dangerous");
    }

    /**
     * The library folders whose {@code apps} map lists Elite Dangerous.
     * <p>
     * Steam keeps two copies of {@code libraryfolders.vdf} and reconciles them at launch, so both are read and
     * whichever names the game is believed.
     */
    static List<Path> holdingTheGame(Path steamRoot) {
        List<Path> libraries = new ArrayList<>();
        for (Path vdf : List.of(steamRoot.resolve("config").resolve("libraryfolders.vdf"),
                steamRoot.resolve("steamapps").resolve("libraryfolders.vdf"))) {
            libraries.addAll(parse(vdf));
        }
        return libraries;
    }

    private static List<Path> parse(Path vdf) {
        if (!Files.isRegularFile(vdf)) return List.of();
        try {
            return listingTheGame(Files.readString(vdf));
        } catch (IOException e) {
            log.warn("Could not read Steam library index {}: {}", vdf, e.getMessage());
            return List.of();
        }
    }

    /**
     * Walks the file once, remembering the library path currently in scope and keeping it when the game's app
     * id turns up before the next one.
     */
    static List<Path> listingTheGame(String vdfText) {
        List<Path> libraries = new ArrayList<>();
        Matcher token = Pattern.compile(VDF_PATH.pattern() + "|" + VDF_APP_ID.pattern()).matcher(vdfText);
        String currentLibrary = null;
        while (token.find()) {
            if (token.group(1) != null) {
                currentLibrary = token.group(1).replace("\\\\", "\\");
            } else if (APP_ID.equals(token.group(2)) && currentLibrary != null) {
                addIfUsable(libraries, currentLibrary);
                currentLibrary = null;
            }
        }
        return libraries;
    }

    private static void addIfUsable(List<Path> libraries, String library) {
        try {
            libraries.add(Path.of(library));
        } catch (InvalidPathException e) {
            log.warn("Steam library index names a path this platform cannot use: {}", library);
        }
    }
}
