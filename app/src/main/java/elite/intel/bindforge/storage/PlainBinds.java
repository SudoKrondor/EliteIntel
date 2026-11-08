package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.FileNames;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Plain {@code .binds} in the bindings folder (storage.md rule 14, <em>Settled with Storage S13b's sources</em>): a
 * {@code <name>.binds} with no version in its name - a factory file copied in by hand, which the game has not converted.
 * It confuses which file a preset line loads, so it never stays in the bindings folder, a backup or the working copy.
 * <p>
 * <strong>Judged against the settled set</strong> - the working copy after the check's answers, with the edits on top at
 * APPLY - each one in name order:
 * <ul>
 *     <li>a binds file of the same BindName is there, ignoring case - it is removed ({@link Fate#TWIN});</li>
 *     <li>else it holds the values of the factory file of that name, the root's {@code PresetName} apart, as the reset
 *     test judges - removed, and the preset line loads the factory file ({@link Fate#FACTORY});</li>
 *     <li>else it is <strong>converted</strong>: {@code <name>.<root version, else 4.2>.binds}, every byte kept but the
 *     root's {@code PresetName}, set to the name ({@link Create#fileName}, {@link Create#withPresetName}), into the
 *     working copy and the bindings folder, and the plain file removed ({@link Fate#CONVERTED});</li>
 *     <li>one that does not read as a binds file is left alone and named ({@link Fate#UNREADABLE}).</li>
 * </ul>
 * Never backed up: it is not one of the user's files. Pure, but for {@link #read}.
 */
public final class PlainBinds {

    private static final Logger log = LogManager.getLogger(PlainBinds.class);

    private static final Pattern ROOT_START = Pattern.compile("<Root(?=[\\s/>])");

    /** What happens to one plain file. */
    public enum Fate {
        /** Removed: a binds file of its BindName is in the set. */
        TWIN,
        /** Removed: it holds the factory file of its name's values. */
        FACTORY,
        /** Made a binds file of the set, and the plain file removed. */
        CONVERTED,
        /** Left alone: it does not read as a binds file. */
        UNREADABLE
    }

    /**
     * One plain file, and what happens to it.
     *
     * @param file the plain file's name, as found
     * @param fate what happens to it
     * @param to   the binds file it is a twin of, or converted to; the BindName for {@link Fate#FACTORY}; {@code null}
     *             for {@link Fate#UNREADABLE}
     */
    public record Tidied(String file, Fate fate, String to) {
        public Tidied {
            Objects.requireNonNull(file, "file");
            Objects.requireNonNull(fate, "fate");
        }

        /** Whether the plain file goes from the bindings folder. */
        public boolean removed() {
            return fate != Fate.UNREADABLE;
        }
    }

    /**
     * The plain files the check found in the bindings folder.
     *
     * @param files      each one's bytes, by name as found
     * @param unreadable the names of those that do not read as a binds file, in name order
     */
    public record Found(Map<String, byte[]> files, List<String> unreadable) {
        public static final Found NONE = new Found(Map.of(), List.of());

        public Found {
            files = Collections.unmodifiableMap(new TreeMap<>(files));
            unreadable = List.copyOf(unreadable);
        }

        /** {@code files}, each judged readable or not. */
        static Found of(Map<String, byte[]> files) {
            List<String> unreadable = new TreeMap<>(files).entrySet().stream()
                    .filter(file -> !readsAsBindsFile(file.getValue()))
                    .map(Map.Entry::getKey).toList();
            return new Found(files, unreadable);
        }

        public boolean none() {
            return files.isEmpty();
        }

        /** The plain files that read as binds files - the ones to tidy up - in name order. */
        public List<String> toTidy() {
            return files.keySet().stream().filter(name -> !unreadable.contains(name)).toList();
        }

        /** Whether {@code other} holds the same files, byte for byte. */
        boolean sameAs(Found other) {
            if (!files.keySet().equals(other.files.keySet())) return false;
            return files.entrySet().stream().allMatch(file -> Arrays.equals(file.getValue(),
                    other.files.get(file.getKey())));
        }
    }

    /**
     * What tidying up does to the settled set.
     *
     * @param tidied    each plain file, in name order, and what happens to it
     * @param converted the binds files the converted ones become, by name - added to the set
     */
    public record Plan(List<Tidied> tidied, Map<String, byte[]> converted) {
        public static final Plan NONE = new Plan(List.of(), Map.of());

        public Plan {
            tidied = List.copyOf(tidied);
            converted = Collections.unmodifiableMap(new TreeMap<>(converted));
        }

        /** The plain files to remove from the bindings folder. */
        public Set<String> remove() {
            Set<String> remove = new TreeSet<>();
            tidied.stream().filter(Tidied::removed).map(Tidied::file).forEach(remove::add);
            return remove;
        }

        /** Whether anything is removed or converted. */
        public boolean anything() {
            return tidied.stream().anyMatch(Tidied::removed);
        }

        /** {@code bindings} with the converted files added. */
        public Map<String, byte[]> appliedTo(Map<String, byte[]> bindings) {
            Map<String, byte[]> with = new TreeMap<>(bindings);
            with.putAll(converted);
            return with;
        }
    }

    private PlainBinds() {
    }

    /**
     * The plain binds files in a bindings folder, by name; none when it is not there - the check's read of the binds and
     * preset files says so.
     */
    static Map<String, byte[]> read(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) return Map.of();
        List<Path> found;
        try (Stream<Path> listing = Files.list(folder)) {
            found = listing.filter(Files::isRegularFile)
                    .filter(file -> FileNames.isPlainBindsFile(file.getFileName().toString())).toList();
        }
        Map<String, byte[]> files = new TreeMap<>();
        for (Path file : found) {
            files.put(file.getFileName().toString(), Files.readAllBytes(file));
        }
        return files;
    }

    /**
     * What becomes of each plain file, against the settled set.
     *
     * @param found    the plain files the check found
     * @param settled  the binds and preset files the write would hold - with the edits on top at APPLY
     * @param factory  Frontier's factory binds files, by BindName - looked up ignoring case
     */
    static Plan plan(Found found, Map<String, byte[]> settled, Map<String, byte[]> factory) {
        if (found.none()) return Plan.NONE;
        Map<String, byte[]> factoryByName = new TreeMap<>(FileNames.ONE_FILE);
        factory.forEach(factoryByName::putIfAbsent);
        List<String> bindsFiles = new ArrayList<>(settled.keySet().stream().filter(FileNames::isBindsFile).toList());
        List<Tidied> tidied = new ArrayList<>();
        Map<String, byte[]> converted = new TreeMap<>();
        for (Map.Entry<String, byte[]> file : found.files().entrySet()) {
            String name = file.getKey();
            String bindName = FileNames.plainBindName(name).orElseThrow();
            if (found.unreadable().contains(name)) {
                tidied.add(new Tidied(name, Fate.UNREADABLE, null));
                continue;
            }
            BindsElements elements = BindsElements.of(file.getValue()).orElseThrow();
            // WHY: a converted one counts too, so a second spelling of it - only where the file system heeds case -
            // is a twin of the first, not a second conversion.
            Optional<String> twin = bindsFiles.stream()
                    .filter(binds -> FileNames.bindName(binds).filter(n -> FileNames.sameFile(n, bindName)).isPresent())
                    .findFirst();
            if (twin.isPresent()) {
                tidied.add(new Tidied(name, Fate.TWIN, twin.get()));
                continue;
            }
            byte[] stock = factoryByName.get(bindName);
            Optional<BindsElements> stockElements = stock != null ? BindsElements.of(stock) : Optional.empty();
            if (stockElements.isPresent() && elements.sameValuesButName(stockElements.get())) {
                tidied.add(new Tidied(name, Fate.FACTORY, bindName));
                continue;
            }
            try {
                String newName = Create.fileName(bindName, file.getValue());
                converted.put(newName, Create.withPresetName(file.getValue(), presetNameText(bindName)));
                bindsFiles.add(newName);
                tidied.add(new Tidied(name, Fate.CONVERTED, newName));
            } catch (IOException e) {
                // WHY: it reads as XML but has no <Root> to name - not a binds file the game would load either.
                log.warn("The plain binds file {} cannot be converted, so it is left alone: {}", name, e.getMessage());
                tidied.add(new Tidied(name, Fate.UNREADABLE, null));
            }
        }
        return new Plan(tidied, converted);
    }

    /**
     * Whether {@code content} reads as a binds file: element by element, as the check reads one, under a
     * {@code <Root>} - the element CREATE's naming reads the version from.
     */
    static boolean readsAsBindsFile(byte[] content) {
        return BindsElements.of(content).isPresent()
                && ROOT_START.matcher(new String(content, StandardCharsets.ISO_8859_1)).find();
    }

    /** Logs what tidying up did - each file removed or converted, and each left alone (Storage S13b, D4). */
    static void log(Plan plan) {
        for (Tidied file : plan.tidied()) {
            switch (file.fate()) {
                case TWIN -> log.info("Removed the plain binds file {}: {} is in the set", file.file(), file.to());
                case FACTORY -> log.info("Removed the plain binds file {}: it holds the factory file {}", file.file(),
                        file.to());
                case CONVERTED -> log.info("Converted the plain binds file {} to {}", file.file(), file.to());
                case UNREADABLE -> log.warn("Left the plain binds file {} alone: it does not read as a binds file",
                        file.file());
            }
        }
    }

    /**
     * The name as the root's {@code PresetName} holds it: XML's special characters escaped, and the text in the bytes
     * {@link Create#withPresetName} writes one character each - so a name a file name allows, {@code &} or a letter
     * beyond ASCII, is written as the file's own UTF-8.
     */
    static String presetNameText(String name) {
        String escaped = name.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
        return new String(escaped.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }
}
