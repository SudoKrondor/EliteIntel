package elite.intel.bindforge.install;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A Windows user folder the game keeps its two folders in: the bindings folder and the journal folder
 * (linux-several-installations.md, rule 1). The relation is the same on Windows and inside every Wine prefix, so an
 * installation's journal folder is found from its bindings folder, never guessed apart from it.
 *
 * @param root the user folder - {@code C:\Users\<name>} on Windows, {@code <prefix>/drive_c/users/<name>} under Wine
 */
public record GameUserFolder(Path root) {

    private static final List<String> BINDINGS = List.of("AppData", "Local", "Frontier Developments",
            "Elite Dangerous", "Options", "Bindings");
    private static final List<String> JOURNAL = List.of("Saved Games", "Frontier Developments", "Elite Dangerous");
    /** The folder of a Wine prefix that holds its Windows drive, and the users folder inside that. */
    static final String DRIVE_C = "drive_c";
    static final String USERS = "users";

    public GameUserFolder {
        Objects.requireNonNull(root, "root");
    }

    /** The game's bindings folder in this user folder, whether or not it is there. */
    public Path bindingsFolder() {
        return under(BINDINGS);
    }

    /** The game's journal folder in this user folder, whether or not it is there. */
    public Path journalFolder() {
        return under(JOURNAL);
    }

    /**
     * The user folder a bindings folder is in - empty when the path is not the game's bindings folder of some user
     * folder, such as one the user pointed Elite-Intel at by hand.
     */
    public static Optional<GameUserFolder> holding(Path bindingsFolder) {
        Path folder = bindingsFolder.toAbsolutePath().normalize();
        int depth = BINDINGS.size();
        if (folder.getNameCount() <= depth) return Optional.empty();
        for (int i = 0; i < depth; i++) {
            String expected = BINDINGS.get(depth - 1 - i);
            if (!expected.equals(folder.getName(folder.getNameCount() - 1 - i).toString())) return Optional.empty();
        }
        Path root = folder;
        for (int i = 0; i < depth; i++) root = root.getParent();
        return Optional.of(new GameUserFolder(root));
    }

    /**
     * The user folder inside a Wine prefix that holds the game's folders - empty when the game has not run in that
     * prefix yet, or the folder is not a prefix. With several users holding them, which a prefix seldom has, the first
     * by name is taken.
     *
     * @param prefix the Wine prefix, the folder holding {@code drive_c}
     * @throws IOException if the prefix's users folder is there but cannot be listed
     */
    public static Optional<GameUserFolder> inPrefix(Path prefix) throws IOException {
        Path users = prefix.resolve(DRIVE_C).resolve(USERS);
        if (!Files.isDirectory(users)) return Optional.empty();
        try (Stream<Path> listing = Files.list(users)) {
            return listing.filter(Files::isDirectory).sorted()
                    .map(GameUserFolder::new)
                    .filter(user -> Files.isDirectory(user.bindingsFolder()) || Files.isDirectory(user.journalFolder()))
                    .findFirst();
        }
    }

    /** Whether a folder the user chose is a Wine prefix: it holds {@code drive_c}. */
    public static boolean isWinePrefix(Path folder) {
        return Files.isDirectory(folder.resolve(DRIVE_C));
    }

    private Path under(List<String> names) {
        Path path = root;
        for (String name : names) path = path.resolve(name);
        return path;
    }
}
