package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.preseteditor.BindNameList;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * What the Preset Editor does to a set of the game input configuration files, and what APPLY checks of it
 * (preset-editor.md, <em>What APPLY checks and writes for the preset file</em>; Storage S6, 2026-10-28). Pure, like
 * {@link BindsEdits}: each takes a set and returns what it read or the changed set; {@link Edits} holds the set until
 * APPLY.
 * <p>
 * <strong>The preset file is the set's highest-numbered {@code StartPreset.N.start}</strong>, found by
 * {@link #presetFile} alone, so the file read, the file changed and the file checked are one file (P3 review, finding
 * 2). {@code StartPreset.start}, the pre-Odyssey name, is not a file of the set.
 */
public final class PresetEdits {

    /** The name a set with no preset file gets one under - the one the Live game reads. */
    public static final String NEW_PRESET_FILE = "StartPreset.4.start";

    /** The sections the preset file has a line for, in the file's order. */
    public static final List<BindingSection> SECTIONS =
            List.of(BindingSection.GENERAL, BindingSection.SHIP, BindingSection.SRV, BindingSection.ON_FOOT);

    private PresetEdits() {
    }

    /** The set's preset file, by name - the highest N. Empty when the set holds none. */
    public static Optional<String> presetFile(ConfigurationFiles files) {
        return files.bindings().keySet().stream()
                .filter(FileNames::isPresetFile)
                .max(Comparator.comparingLong(FileNames::presetGeneration).thenComparing(Comparator.naturalOrder()));
    }

    /** The four lines of the set's preset file. Empty when the set holds none. */
    public static Optional<StartPresetSections> sections(ConfigurationFiles files) {
        return presetFile(files).map(name -> parse(files.bindings().get(name)));
    }

    /**
     * The set with the preset file's four lines changed to {@code sections} - an {@link Edits.EditFromApplied}.
     * <ul>
     *   <li>Lines that already hold these values leave the set as it is: values are compared, never bytes.</li>
     *   <li>Lines put back to what the working copy holds put back its bytes, so the edits are the working copy again
     *       and the bar goes dark (Alan, 2026-10-28).</li>
     *   <li>Otherwise the file is written in the game's form, {@link #gameForm}. With no preset file in the set, one
     *       is added as {@value #NEW_PRESET_FILE}.</li>
     * </ul>
     *
     * @param current the edits
     * @param applied the working copy they started from
     */
    public static ConfigurationFiles change(ConfigurationFiles current, ConfigurationFiles applied,
                                            StartPresetSections sections) {
        String name = presetFile(current).orElse(NEW_PRESET_FILE);
        byte[] now = current.bindings().get(name);
        if (now != null && parse(now).equals(sections)) return current;
        byte[] inWorkingCopy = applied.bindings().get(name);
        if (inWorkingCopy != null && parse(inWorkingCopy).equals(sections)) {
            return current.withBindingsFile(name, inWorkingCopy);
        }
        return current.withBindingsFile(name, gameForm(sections).getBytes(StandardCharsets.UTF_8));
    }

    /** The four lines as the game writes them: CRLF between them, no line ending after the last - the specimen's form. */
    public static String gameForm(StartPresetSections sections) {
        return String.join("\r\n", SECTIONS.stream().map(sections::forSection).toList());
    }

    /**
     * Refuses a set whose preset file APPLY must not write: any of the four lines empty, or naming no binds file
     * {@code bindNames} offers - unchanged lines included. A set with no preset file is refused for all four sections.
     *
     * @throws PresetRefusedException naming the sections, in the file's order
     */
    public static void check(ConfigurationFiles files, BindNameList bindNames) throws PresetRefusedException {
        StartPresetSections sections = sections(files).orElse(new StartPresetSections("", "", "", "", false));
        List<BindingSection> refused = SECTIONS.stream()
                .filter(section -> {
                    String value = sections.forSection(section);
                    return value.isEmpty() || bindNames.find(value).isEmpty();
                })
                .toList();
        if (!refused.isEmpty()) throw new PresetRefusedException(refused, sections);
    }

    /** The set's binds files, by name - what the BindName list offers of the player's own. */
    public static Map<String, byte[]> bindsFiles(ConfigurationFiles files) {
        return files.bindings().entrySet().stream()
                .filter(file -> FileNames.isBindsFile(file.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static StartPresetSections parse(byte[] content) {
        return StartPresetSections.parse(new String(content, StandardCharsets.UTF_8));
    }
}
