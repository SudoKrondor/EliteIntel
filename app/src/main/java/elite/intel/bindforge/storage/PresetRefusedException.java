package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * APPLY refused before anything was backed up or written: a line of the preset file is empty or names no binds file
 * the BindName list offers (preset-editor.md, <em>What APPLY checks and writes for the preset file</em>; Storage S6).
 * The edits are still on screen. The message is for the log; a screen names the {@link #sections()} in the user's
 * language, and the {@link #unreachable()} installations when there are any - their factory binds files could not be
 * listed, so they may be what a line names (Storage S6R, 2026-10-29).
 */
public final class PresetRefusedException extends IOException {

    private final transient List<BindingSection> sections;
    private final transient List<InstallationRow> unreachable;

    PresetRefusedException(List<BindingSection> sections, StartPresetSections lines) {
        super("The preset file names no binds file that can be found for "
                + sections.stream().map(section -> section + " ('" + lines.forSection(section) + "')")
                .collect(Collectors.joining(", ")));
        this.sections = List.copyOf(sections);
        this.unreachable = List.of();
    }

    private PresetRefusedException(PresetRefusedException refused, List<InstallationRow> unreachable) {
        super(refused.getMessage() + "; not reachable: " + unreachable.stream()
                .map(row -> row.storefront() + " (" + row.rootPath() + ")").toList());
        this.sections = refused.sections;
        this.unreachable = List.copyOf(unreachable);
    }

    /** This refusal, naming the installations that could not be reached when the BindName list was read. */
    public PresetRefusedException withUnreachable(List<InstallationRow> installations) {
        return new PresetRefusedException(this, installations);
    }

    /** The sections refused, in the file's order - never empty. */
    public List<BindingSection> sections() {
        return sections;
    }

    /** The installations that could not be reached when the BindName list was read - empty when every one was. */
    public List<InstallationRow> unreachable() {
        return unreachable;
    }
}
