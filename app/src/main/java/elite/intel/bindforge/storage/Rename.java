package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.preseteditor.BindNameEntry;
import elite.intel.bindforge.preseteditor.BindsFileOrigin;
import elite.intel.db.managers.BindForgeInstallationsManager;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

/**
 * Renaming a binds file - the Bind Editor's Name box (bind-editor.md, <em>Shell</em>; storage.md rule 7; Storage S12).
 * One edit not applied changes three things together: the file name, the {@code PresetName} inside it, and every line
 * of the preset file naming the old BindName. APPLY writes the new file and removes the old one from the bindings
 * folder ({@link Apply}).
 * <p>
 * Pure except where it says it touches the disk. The name rule is the controller name's ({@code DeviceNames.validate}),
 * checked by the screen against {@link Names#own()}; {@link #factoryClash} is checked as well, since a factory
 * BindName is refused by its characters, whatever their case, and not loosely (Alan, 2026-11-03).
 */
public final class Rename {

    private Rename() {
    }

    /**
     * What the Name box checks a typed name against.
     *
     * @param factory the factory BindNames of the installations that are there
     * @param own     the BindNames of the set's binds files, the file being renamed included - compared loosely, so a
     *                rename that changes only case, {@code -} or {@code _} is refused (Alan, 2026-11-03, Storage S12
     *                D3: APPLY writes the new file and removes the old, and on Windows the two would be one file)
     * @param applied the BindNames of the working copy's binds files - what is live until APPLY, so a rename by way of
     *                another name is held to the same rule (review, 2026-11-03)
     */
    public record Names(List<String> factory, List<String> own, List<String> applied) {
        public Names {
            factory = List.copyOf(factory);
            own = List.copyOf(own);
            applied = List.copyOf(applied);
        }

        public static final Names NONE = new Names(List.of(), List.of(), List.of());

        /**
         * The names {@code typed} must not be loosely the same as. A working-copy name exactly is a rename back.
         */
        public List<String> taken(String typed) {
            return Stream.concat(own.stream(), applied.stream().filter(name -> !name.equals(typed))).toList();
        }
    }

    /**
     * The names for the edits, from every installation Elite-Intel knows. Touches the disk - not for the EDT.
     */
    public static Names names(Edits.Snapshot snapshot) {
        return names(snapshot.current(), snapshot.applied(),
                Apply.offered(snapshot.current(), BindForgeInstallationsManager.getInstance().findAll()));
    }

    static Names names(ConfigurationFiles current, ConfigurationFiles applied, Apply.Offered offered) {
        // WHY: an installation that cannot be reached is left out, and nothing more is done about it (principle 13).
        List<String> factory = offered.list().entries().stream()
                .filter(entry -> entry.origin() == BindsFileOrigin.FACTORY || entry.shadowsFactory())
                .map(BindNameEntry::bindName)
                .toList();
        return new Names(factory, bindNames(current), bindNames(applied));
    }

    private static List<String> bindNames(ConfigurationFiles files) {
        return BindsEdits.bindsFiles(files).stream()
                .flatMap(file -> FileNames.bindName(file).stream())
                .distinct()
                .toList();
    }

    /**
     * Each binds file of {@code applied} the edits renamed, to its new name.
     * <p>
     * WHY worked out from the files: the edits hold only what the files will be (storage.md rule 7), and only a rename
     * takes a binds file out of them. It keeps the version, so that is the first test; with several renames not applied,
     * each old file goes with the new one closest to it, since either may carry other edits as well.
     */
    static Map<String, String> between(Map<String, byte[]> applied, Map<String, byte[]> current) {
        List<String> arrived = new ArrayList<>(new TreeMap<>(current).keySet().stream()
                .filter(name -> FileNames.isBindsFile(name) && !applied.containsKey(name))
                .toList());
        Map<String, String> renamed = new TreeMap<>();
        new TreeMap<>(applied).keySet().stream()
                .filter(name -> FileNames.isBindsFile(name) && !current.containsKey(name))
                .forEach(older -> arrived.stream()
                        .filter(newer -> suffix(newer).equals(suffix(older)))
                        .min(Comparator.comparingInt(newer -> differences(applied.get(older), current.get(newer))))
                        .ifPresent(newer -> {
                            renamed.put(older, newer);
                            arrived.remove(newer);
                        }));
        return renamed;
    }

    private static int differences(byte[] older, byte[] newer) {
        String sameName = "file.4.2.binds";
        return BindingsComparison.compare(Map.of(sameName, older), Map.of(sameName, newer),
                BindingsComparison.Factory.NONE).items().size();
    }

    /** The factory BindName {@code typed} is, ignoring case only - {@code Keyboard_Mouse_Only} is not one. */
    public static Optional<String> factoryClash(String typed, List<String> factory) {
        return factory.stream().filter(name -> name.equalsIgnoreCase(typed)).findFirst();
    }

    /** Whether a binds file is named like a factory one, ignoring case - the Bind Editor warns of it. */
    public static boolean factoryNamed(String bindsFile, List<String> factory) {
        return FileNames.bindName(bindsFile).flatMap(name -> factoryClash(name, factory)).isPresent();
    }

    /** A binds file's BindName - {@code Custom} for {@code Custom.4.2.binds}. Empty for any other name. */
    public static Optional<String> bindName(String bindsFile) {
        return FileNames.bindName(bindsFile);
    }

    /** What follows the BindName: the version and extension, fixed text in the Name box - {@code .4.2.binds}. */
    public static String suffix(String bindsFile) {
        String bindName = FileNames.bindName(bindsFile)
                .orElseThrow(() -> new IllegalArgumentException("not a binds file name: " + bindsFile));
        return bindsFile.substring(bindName.length());
    }

    /**
     * The rename as an {@link Edits.EditFromApplied}: {@code bindsFile} taken out of the set and put back as
     * {@code newBindName} with the same version, its {@code PresetName} made {@code newBindName} and every other byte
     * kept, and every preset line naming the old BindName set to the new one.
     * <p>
     * A rename back to a name the working copy holds puts back its bytes, so the edits are the working copy again and
     * the bar goes dark, as a preset line put back does.
     *
     * @param newBindName a name {@code DeviceNames.validate} accepts, checked by the screen
     * @return {@code current} unchanged when {@code newBindName} is the file's own BindName
     * @throws IllegalArgumentException if the set holds no such binds file, the name is not a binds file name, or the
     *                                  set already holds a binds file the new name would be, ignoring case, or the
     *                                  working copy holds one differing from it only in case
     * @throws IOException              if the file has no {@code <Root>} element to name
     */
    public static ConfigurationFiles rename(ConfigurationFiles current, ConfigurationFiles applied, String bindsFile,
                                            String newBindName) throws IOException {
        byte[] content = current.bindings().get(bindsFile);
        String oldBindName = FileNames.bindName(bindsFile).orElse(null);
        if (content == null || oldBindName == null) {
            throw new IllegalArgumentException("no binds file " + bindsFile + " in the set");
        }
        if (!newBindName.matches("[A-Za-z][A-Za-z0-9_-]*")) {
            throw new IllegalArgumentException("not a binds file name: " + newBindName);
        }
        if (newBindName.equals(oldBindName)) return current;
        String newFile = newBindName + suffix(bindsFile);
        String newKey = newFile.toLowerCase(Locale.ROOT);
        // WHY: on Windows a name differing only in case is the same file, so APPLY would remove the one it wrote.
        if (current.bindings().keySet().stream().anyMatch(name -> name.toLowerCase(Locale.ROOT).equals(newKey))) {
            throw new IllegalArgumentException("the set already holds " + newFile);
        }
        if (applied.bindings().keySet().stream()
                .anyMatch(name -> !name.equals(newFile) && name.toLowerCase(Locale.ROOT).equals(newKey))) {
            throw new IllegalArgumentException("the working copy holds a file differing from " + newFile + " only in case");
        }
        byte[] renamed = Create.withPresetName(content, newBindName);
        byte[] inWorkingCopy = applied.bindings().get(newFile);
        if (inWorkingCopy != null && Arrays.equals(Create.withPresetName(inWorkingCopy, newBindName), renamed)) {
            renamed = inWorkingCopy;
        }
        Map<String, byte[]> bindings = new TreeMap<>(current.bindings());
        bindings.remove(bindsFile);
        bindings.put(newFile, renamed);
        ConfigurationFiles withFile = current.withBindings(bindings);
        Optional<StartPresetSections> sections = PresetEdits.sections(withFile);
        if (sections.isEmpty()) return withFile;
        return PresetEdits.change(withFile, applied, moved(sections.get(), oldBindName, newBindName));
    }

    /** The four lines with each one naming {@code from} naming {@code to}. */
    private static StartPresetSections moved(StartPresetSections sections, String from, String to) {
        String[] lines = PresetEdits.SECTIONS.stream()
                .map(section -> line(sections, section, from, to))
                .toArray(String[]::new);
        return new StartPresetSections(lines[0], lines[1], lines[2], lines[3], false);
    }

    private static String line(StartPresetSections sections, BindingSection section, String from, String to) {
        String value = sections.forSection(section);
        return value.equals(from) ? to : value;
    }
}
