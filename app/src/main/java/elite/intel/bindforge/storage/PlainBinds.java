package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.FileNames;
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
 * APPLY - each one that reads as a binds file, in name order:
 * <ul>
 *     <li>a binds file of the same BindName is there, ignoring case ({@link Fate#TWIN});</li>
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

    /** Why one plain file is moved, and whether a binds file was made of it first. */
    public enum Fate {
        /** A binds file of its BindName is in the set. */
        TWIN,
        /** It holds the factory file of its name's values. */
        FACTORY,
        /** Made a binds file of the set first. */
        CONVERTED
    }

    /**
     * One plain file, and why it is moved.
     *
     * @param file the plain file's name, as found
     * @param fate why it is moved
     * @param to   the binds file it is a twin of, or converted to; the BindName for {@link Fate#FACTORY}
     */
    public record Tidied(String file, Fate fate, String to) {
        public Tidied {
            Objects.requireNonNull(file, "file");
            Objects.requireNonNull(fate, "fate");
            Objects.requireNonNull(to, "to");
        }
    }

    /**
     * What judging the plain files does to the settled set.
     *
     * @param tidied    each plain file, in name order, and why it is moved
     * @param converted the binds files the converted ones become, by name - added to the set
     */
    public record Plan(List<Tidied> tidied, Map<String, byte[]> converted) {
        public static final Plan NONE = new Plan(List.of(), Map.of());

        public Plan {
            tidied = List.copyOf(tidied);
            converted = Collections.unmodifiableMap(new TreeMap<>(converted));
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
     * Why each plain file is moved, against the settled set, and the binds files the converted ones become.
     *
     * @param plain    the plain files that read as binds files ({@link #readsAsBindsFile}), by name as found
     * @param settled  the binds and preset files the write would hold - with the edits on top at APPLY
     * @param factory  Frontier's factory binds files, by BindName - looked up ignoring case
     */
    static Plan plan(Map<String, byte[]> plain, Map<String, byte[]> settled, Map<String, byte[]> factory) {
        if (plain.isEmpty()) return Plan.NONE;
        Map<String, byte[]> factoryByName = new TreeMap<>(FileNames.ONE_FILE);
        factory.forEach(factoryByName::putIfAbsent);
        List<String> bindsFiles = new ArrayList<>(settled.keySet().stream().filter(FileNames::isBindsFile).toList());
        List<Tidied> tidied = new ArrayList<>();
        Map<String, byte[]> converted = new TreeMap<>();
        for (Map.Entry<String, byte[]> file : new TreeMap<>(plain).entrySet()) {
            String name = file.getKey();
            String bindName = FileNames.plainBindName(name).orElseThrow();
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
            String newName;
            byte[] content;
            try {
                newName = Create.fileName(bindName, file.getValue());
                content = Create.withPresetName(file.getValue(), presetNameText(bindName));
            } catch (IOException e) {
                // WHY: fails fast - readsAsBindsFile found a <Root> and BindsElements parsed it, so this is a bug, not
                // the user's file (Storage S13d, from the S13aR-S13b review's finding 4).
                throw new IllegalStateException("a plain binds file that reads as one has no <Root>: " + name, e);
            }
            converted.put(newName, content);
            bindsFiles.add(newName);
            tidied.add(new Tidied(name, Fate.CONVERTED, newName));
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

    /** Logs why each plain file is moved, and what each converted one became (Storage S13b, D4; S13d). */
    static void log(Plan plan) {
        for (Tidied file : plan.tidied()) {
            switch (file.fate()) {
                case TWIN -> log.info("The plain binds file {} goes to Leftover User Files: {} is in the set",
                        file.file(), file.to());
                case FACTORY -> log.info("The plain binds file {} goes to Leftover User Files: it holds the factory "
                        + "file {}", file.file(), file.to());
                case CONVERTED -> log.info("Converted the plain binds file {} to {}; it goes to Leftover User Files",
                        file.file(), file.to());
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
