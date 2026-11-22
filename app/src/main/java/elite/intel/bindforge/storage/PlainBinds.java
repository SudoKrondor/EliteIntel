package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.FileNames;
import elite.intel.bindforge.storage.BindingsComparison.FileDifference;
import elite.intel.bindforge.storage.BindingsComparison.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Plain {@code .binds} in the bindings folder (storage.md rule 14, <em>Settled with Storage S13b's sources</em>): a
 * {@code <name>.binds} with no version in its name - a factory file copied in by hand, which the game has not converted.
 * It confuses which file a preset line loads, so it never stays in the bindings folder, a backup or the working copy.
 * <p>
 * <strong>Judged against the settled set</strong> - the working copy after the check's answers, with the edits on top at
 * APPLY - each one that reads as a binds file, in name order (Storage S13e, replacing S13b's D2):
 * <ul>
 *     <li>a binds file of the same BindName is there, ignoring case: compared with it by value ({@link #rows}). Lacking
 *     only what the twin has, nothing more is needed ({@link Fate#TWIN}); holding lines the twin lacks, those are rows
 *     of the check's merge, and each the user answered in-game goes into the twin ({@link Fate#MERGED}) - none, and it
 *     is {@link Fate#TWIN} too;</li>
 *     <li>else the write takes a binds file of that BindName out of the bindings folder: nothing is made of the plain
 *     file, and above all it is not converted into the name the user just took away ({@link Fate#TWIN_LEAVES});</li>
 *     <li>else it holds the values of the factory file of that name, the root's {@code PresetName} apart, as the reset
 *     test judges - the preset line loads the factory file ({@link Fate#FACTORY});</li>
 *     <li>else it is <strong>converted</strong>: {@code <name>.<root version, else 4.2>.binds}, every byte kept but the
 *     root's {@code PresetName}, set to the name ({@link Create#fileName}, {@link Create#withPresetName}), into the
 *     working copy and the bindings folder ({@link Fate#CONVERTED}).</li>
 * </ul>
 * <strong>Whichever it is, the plain file then goes to Leftover User Files</strong>, never deleted (Alan, 2026-11-08,
 * Storage S13d; {@link Leftovers}). One that does not read as a binds file is moved there unjudged. Never backed up: it
 * is not one of the user's files, and the move keeps it. Pure.
 */
public final class PlainBinds {

    private static final Logger log = LogManager.getLogger(PlainBinds.class);

    private static final Pattern ROOT_START = Pattern.compile("<Root(?=[\\s/>])");

    /** Why one plain file is moved, and what was made of it first. */
    public enum Fate {
        /** A binds file of its BindName is in the set, and nothing of the plain file's went into it. */
        TWIN,
        /** A binds file of its BindName is in the set, and the lines the user kept of the plain file went into it. */
        MERGED,
        /**
         * The binds file of its BindName leaves the bindings folder in this write - a live-only one the user put the
         * working copy's side of, or one the edits renamed - so nothing of the plain file's goes anywhere.
         */
        TWIN_LEAVES,
        /** It holds the factory file of its name's values. */
        FACTORY,
        /** Made a binds file of the set first. */
        CONVERTED
    }

    /**
     * One plain file, and why it is moved.
     *
     * @param file  the plain file's name, as found
     * @param fate  why it is moved
     * @param about for {@link Fate#TWIN}, {@link Fate#MERGED} and {@link Fate#TWIN_LEAVES} the twin's file name, for
     *              {@link Fate#CONVERTED} the file it became, and for {@link Fate#FACTORY} the factory file's BindName
     */
    public record Tidied(String file, Fate fate, String about) {
        public Tidied {
            Objects.requireNonNull(file, "file");
            Objects.requireNonNull(fate, "fate");
            Objects.requireNonNull(about, "about");
        }
    }

    /**
     * The lines one plain file holds and its twin lacks, to ask about (Storage S13e).
     *
     * @param twin the twin's file name
     * @param rows the rows, in the plain file's name - {@link BindingsComparison#plainAgainstTwin}
     */
    public record Rows(String twin, FileDifference rows) {
        public Rows {
            Objects.requireNonNull(twin, "twin");
            Objects.requireNonNull(rows, "rows");
        }

        /** The plain file's name, as found. */
        public String plain() {
            return rows.file();
        }
    }

    /**
     * What judging the plain files does to the settled set.
     *
     * @param tidied    each plain file, in name order, and why it is moved
     * @param converted the binds files the converted ones become, by name - added to the set
     * @param merged    each twin a plain file's lines went into, by name - in place of the set's
     */
    public record Plan(List<Tidied> tidied, Map<String, byte[]> converted, Map<String, byte[]> merged) {
        public static final Plan NONE = new Plan(List.of(), Map.of(), Map.of());

        public Plan {
            tidied = List.copyOf(tidied);
            converted = Collections.unmodifiableMap(new TreeMap<>(converted));
            merged = Collections.unmodifiableMap(new TreeMap<>(merged));
        }

        /** {@code bindings} with the converted files added and the merged twins in place. */
        public Map<String, byte[]> appliedTo(Map<String, byte[]> bindings) {
            Map<String, byte[]> with = new TreeMap<>(bindings);
            with.putAll(converted);
            with.putAll(merged);
            return with;
        }

        /** Whether the set changes - a file converted, or a twin merged into. */
        public boolean changesTheSet() {
            return !converted.isEmpty() || !merged.isEmpty();
        }
    }

    private PlainBinds() {
    }

    /**
     * The rows to ask about for each plain file that has a twin and holds lines it lacks (Storage S13e, Q4): the twin as
     * BindForge holds it - the working copy's with the edits on top - else, when only the bindings folder holds it, the
     * live one.
     *
     * @param plain the plain files that read as binds files, by name as found
     * @param twins the binds files a twin is looked for in, by name - the edits' first, the bindings folder's after
     */
    static List<Rows> rows(Map<String, byte[]> plain, Map<String, byte[]> twins) {
        if (plain.isEmpty()) return List.of();
        List<Rows> rows = new ArrayList<>();
        for (Map.Entry<String, byte[]> file : new TreeMap<>(plain).entrySet()) {
            Optional<String> twin = twinOf(file.getKey(), twins.keySet());
            if (twin.isEmpty()) continue;
            BindingsComparison.plainAgainstTwin(file.getKey(), twins.get(twin.get()), file.getValue())
                    .ifPresent(found -> rows.add(new Rows(twin.get(), found)));
        }
        return List.copyOf(rows);
    }

    /**
     * Why each plain file is moved, against the settled set, and what it makes of the set.
     *
     * @param plain   the plain files that read as binds files ({@link #readsAsBindsFile}), by name as found
     * @param settled the binds and preset files the write would hold - with the edits on top at APPLY
     * @param leaving the files the write takes out of the bindings folder - a live-only one the user kept the working
     *                copy's side of, and at APPLY a renamed one's old name
     * @param factory Frontier's factory binds files, by BindName - looked up ignoring case
     * @param answers the side each row keeps, by {@link BindingsComparison.LineItem#id()}. A plain file's rows are
     *                compared again with the twin the write holds, and one with no answer keeps the twin's (Q4)
     * @throws IOException if a twin merged into could not be read back - the message names the plain file
     */
    static Plan plan(Map<String, byte[]> plain, Map<String, byte[]> settled, Set<String> leaving,
                     Map<String, byte[]> factory, Map<String, Side> answers) throws IOException {
        if (plain.isEmpty()) return Plan.NONE;
        Judging judging = new Judging(settled, leaving, factory, answers);
        for (Map.Entry<String, byte[]> file : new TreeMap<>(plain).entrySet()) {
            judging.judge(file.getKey(), file.getValue());
        }
        return new Plan(judging.tidied, judging.converted, judging.merged);
    }

    /** The plain files judged so far, in name order, and what they made of the set. */
    private static final class Judging {
        private final Map<String, byte[]> set;
        private final Set<String> leaving;
        private final Map<String, byte[]> factoryByName = new TreeMap<>(FileNames.ONE_FILE);
        private final Map<String, Side> answers;
        final List<Tidied> tidied = new ArrayList<>();
        final Map<String, byte[]> converted = new TreeMap<>();
        final Map<String, byte[]> merged = new TreeMap<>();

        Judging(Map<String, byte[]> settled, Set<String> leaving, Map<String, byte[]> factory,
                Map<String, Side> answers) {
            this.set = new TreeMap<>(settled);
            this.leaving = leaving;
            factory.forEach(factoryByName::putIfAbsent);
            this.answers = answers;
        }

        void judge(String name, byte[] content) throws IOException {
            // WHY: a converted one counts too, so a second spelling of it - only where the file system heeds case -
            // is a twin of the first, not a second conversion.
            Optional<String> twin = twinOf(name, set.keySet());
            if (twin.isPresent()) {
                tidied.add(againstTwin(name, content, twin.get()));
                return;
            }
            // WHY: converted, the plain file would put back under the same name the binds file the user just took out
            // of the bindings folder - the opposite of their answer. Its bytes are kept in Leftover User Files.
            Optional<String> left = twinOf(name, leaving);
            if (left.isPresent()) {
                tidied.add(new Tidied(name, Fate.TWIN_LEAVES, left.get()));
                return;
            }
            String bindName = FileNames.plainBindName(name).orElseThrow();
            tidied.add(isFactoryFile(content, bindName) ? new Tidied(name, Fate.FACTORY, bindName)
                    : converted(name, bindName, content));
        }

        /** The twin left as it is, or with the lines the user kept of the plain file put in (Storage S13e). */
        private Tidied againstTwin(String name, byte[] content, String twin) throws IOException {
            byte[] twinContent = set.get(twin);
            Optional<FileDifference> rows = BindingsComparison.plainAgainstTwin(name, twinContent, content);
            boolean takesAny = rows.isPresent() && rows.get().items().stream()
                    .anyMatch(item -> answers.get(item.id()) == Side.LIVE);
            if (!takesAny) return new Tidied(name, Fate.TWIN, twin);
            byte[] into = BindingsComparison.mergedIntoTwin(rows.get(), twinContent, content, answers);
            set.put(twin, into);
            merged.put(twin, into);
            return new Tidied(name, Fate.MERGED, twin);
        }

        /** Whether {@code content} holds the values of the factory file of its name, the root's name apart. */
        private boolean isFactoryFile(byte[] content, String bindName) {
            BindsElements elements = BindsElements.of(content).orElseThrow();
            byte[] stock = factoryByName.get(bindName);
            Optional<BindsElements> stockElements = stock != null ? BindsElements.of(stock) : Optional.empty();
            return stockElements.isPresent() && elements.sameValuesButName(stockElements.get());
        }

        /** The plain file made a binds file of the set - every byte kept but the root's {@code PresetName}. */
        private Tidied converted(String name, String bindName, byte[] content) {
            String newName;
            byte[] made;
            try {
                newName = Create.fileName(bindName, content);
                made = Create.withPresetName(content, bindName);
            } catch (IOException e) {
                // WHY: fails fast - readsAsBindsFile found a <Root> and BindsElements parsed it, so this is a bug, not
                // the user's file (Storage S13d, from the S13aR-S13b review's finding 4).
                throw new IllegalStateException("a plain binds file that reads as one has no <Root>: " + name, e);
            }
            converted.put(newName, made);
            set.put(newName, made);
            return new Tidied(name, Fate.CONVERTED, newName);
        }
    }

    /** The binds file among {@code files} whose BindName is the plain file's, ignoring case; the first in name order. */
    private static Optional<String> twinOf(String plain, Collection<String> files) {
        String bindName = FileNames.plainBindName(plain).orElseThrow();
        return files.stream().filter(FileNames::isBindsFile)
                .filter(binds -> FileNames.bindName(binds).filter(n -> FileNames.sameFile(n, bindName)).isPresent())
                .sorted()
                .findFirst();
    }

    /**
     * Whether {@code content} reads as a binds file: element by element, as the check reads one, under a
     * {@code <Root>} - the element CREATE's naming reads the version from.
     */
    static boolean readsAsBindsFile(byte[] content) {
        return BindsElements.of(content).isPresent()
                && ROOT_START.matcher(new String(content, StandardCharsets.ISO_8859_1)).find();
    }

    /** Logs why each plain file is moved, and what was made of it (Storage S13b, D4; S13d; S13e). */
    static void log(Plan plan) {
        for (Tidied file : plan.tidied()) {
            switch (file.fate()) {
                case TWIN -> log.info("The plain binds file {} goes to Leftover User Files: {} is in the set",
                        file.file(), file.about());
                case MERGED -> log.info("The lines kept of the plain binds file {} went into {}; it goes to Leftover "
                        + "User Files", file.file(), file.about());
                case TWIN_LEAVES -> log.info("The plain binds file {} goes to Leftover User Files: {} leaves the "
                        + "bindings folder, so nothing was taken from it", file.file(), file.about());
                case FACTORY -> log.info("The plain binds file {} goes to Leftover User Files: it holds the factory "
                        + "file {}", file.file(), file.about());
                case CONVERTED -> log.info("Converted the plain binds file {} to {}; it goes to Leftover User Files",
                        file.file(), file.about());
            }
        }
    }
}
