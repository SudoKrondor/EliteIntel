package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.model.BindingSection;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * APPLY refused before anything was backed up or written: a line of the preset file is empty or names no binds file
 * the BindName list offers (preset-editor.md, <em>What APPLY checks and writes for the preset file</em>; Storage S6).
 * The edits are still on screen. The message is for the log; a screen names the {@link #sections()} in the user's
 * language.
 */
public final class PresetRefusedException extends IOException {

    private final transient List<BindingSection> sections;

    PresetRefusedException(List<BindingSection> sections, StartPresetSections lines) {
        super("The preset file names no binds file that can be found for "
                + sections.stream().map(section -> section + " ('" + lines.forSection(section) + "')")
                .collect(Collectors.joining(", ")));
        this.sections = List.copyOf(sections);
    }

    /** The sections refused, in the file's order - never empty. */
    public List<BindingSection> sections() {
        return sections;
    }
}
