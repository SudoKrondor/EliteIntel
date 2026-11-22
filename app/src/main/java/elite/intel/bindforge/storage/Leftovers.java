package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.FileNames;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * What does not belong in the bindings folder (storage.md rule 14, <em>Settled 2026-11-08</em>; principles.md
 * <em>Words</em>): anything there but a {@code <name>.<major>.<minor>.binds}, a {@code StartPreset.N.start} and
 * {@code BindingLoadingErrors.log} ({@link FileNames#staysInBindingsFolder}) - plain binds files, Windows copies such as
 * {@code Custom.4.2 (2).binds}, {@code .binds.backup} files, a leftover {@code StartPreset.start}, any stray file.
 * <p>
 * <strong>The check moves them to Leftover User Files, never deletes them</strong> - at the first click and at APPLY,
 * in its warning, backup and write ({@link LiveCopies}). A plain file that reads as a binds file is judged first
 * ({@link PlainBinds}), and may become a binds file of the set before it goes. Folders inside the bindings folder are
 * left alone (Alan, 2026-11-08, Storage S13d Q2). Pure, but for {@link #read}.
 */
public final class Leftovers {

    private static final Logger log = LogManager.getLogger(Leftovers.class);

    /**
     * What the check found to move.
     *
     * @param plain  the plain binds files that read as binds files, by name as found, with their bytes - judged before
     *               they go
     * @param others every other file to move, by name as found, in name order - a plain file that does not read as a
     *               binds file among them
     */
    public record Found(Map<String, byte[]> plain, List<String> others) {
        public static final Found NONE = new Found(Map.of(), List.of());

        public Found {
            plain = Collections.unmodifiableMap(new TreeMap<>(plain));
            others = others.stream().sorted().toList();
        }

        public boolean none() {
            return plain.isEmpty() && others.isEmpty();
        }

        /** Every file to move, in name order. */
        public List<String> files() {
            Set<String> all = new TreeSet<>(plain.keySet());
            all.addAll(others);
            return List.copyOf(all);
        }

        /**
         * Whether {@code other} holds the same files - the plain ones byte for byte, since they are judged; the rest by
         * name, since they are moved whole whatever they hold.
         */
        boolean sameAs(Found other) {
            if (!others.equals(other.others) || !plain.keySet().equals(other.plain.keySet())) return false;
            return plain.entrySet().stream().allMatch(file -> Arrays.equals(file.getValue(),
                    other.plain.get(file.getKey())));
        }
    }

    /**
     * What the check's write does with them.
     *
     * @param plain  why each plain file that reads as a binds file is moved, and the binds files converted ones become
     * @param others the other files, moved as they are
     */
    public record Plan(PlainBinds.Plan plain, List<String> others) {
        public static final Plan NONE = new Plan(PlainBinds.Plan.NONE, List.of());

        public Plan {
            Objects.requireNonNull(plain, "plain");
            others = List.copyOf(others);
        }

        /** Every file to move from the bindings folder, in name order. */
        public Set<String> move() {
            Set<String> move = new TreeSet<>(others);
            plain.tidied().forEach(file -> move.add(file.file()));
            return move;
        }

        /**
         * Whether a plain file is converted, or its lines go into its twin (Storage S13e) - the one thing here that
         * changes the working copy, so the backup then holds the working copy as it was.
         */
        public boolean changesTheSet() {
            return plain.changesTheSet();
        }
    }

    private Leftovers() {
    }

    /**
     * What does not belong in a bindings folder; none when the folder is not there - the check's read of the binds and
     * preset files says so. Reads the bytes of plain binds files only.
     */
    static Found read(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) return Found.NONE;
        List<Path> found;
        try (Stream<Path> listing = Files.list(folder)) {
            found = listing.filter(Files::isRegularFile)
                    .filter(file -> !FileNames.staysInBindingsFolder(file.getFileName().toString())).toList();
        }
        // WHY: case-sensitive, so a second spelling reaches PlainBinds.plan and is judged a twin (Storage S13b D3).
        Map<String, byte[]> plain = new TreeMap<>();
        List<String> others = new ArrayList<>();
        for (Path file : found) {
            String name = file.getFileName().toString();
            if (FileNames.isPlainBindsFile(name)) {
                byte[] content = Files.readAllBytes(file);
                if (PlainBinds.readsAsBindsFile(content)) {
                    plain.put(name, content);
                    continue;
                }
            }
            others.add(name);
        }
        return new Found(plain, others);
    }

    /**
     * What the write does with {@code found}, its plain files judged against {@code settled} - the set the write would
     * hold.
     *
     * @param leaving the files the write takes out of the bindings folder ({@link PlainBinds#plan})
     * @param factory Frontier's factory binds files, by BindName
     * @param answers the side each row keeps - a plain file's rows against its twin among them (Storage S13e)
     * @throws IOException if a twin merged into could not be read back
     */
    static Plan plan(Found found, Map<String, byte[]> settled, Set<String> leaving, Map<String, byte[]> factory,
                     Map<String, BindingsComparison.Side> answers) throws IOException {
        if (found.none()) return Plan.NONE;
        return new Plan(PlainBinds.plan(found.plain(), settled, leaving, factory, answers), found.others());
    }

    /** Logs why each file is moved (Storage S13b, D4; S13d) - {@link LiveCopies} logs each move. */
    static void log(Plan plan) {
        PlainBinds.log(plan.plain());
        if (!plan.others().isEmpty()) {
            log.info("Files that do not belong in the bindings folder go to Leftover User Files: {}",
                    String.join(", ", plan.others()));
        }
    }
}
