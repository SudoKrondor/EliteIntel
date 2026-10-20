package elite.intel.bindforge.preseteditor;

import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.model.BindingSection;

import java.util.List;

/**
 * The four lines of a {@code StartPreset.4.start} as values, apart from the form the file was in.
 */
final class StartPresetLines {

    /**
     * The sections a {@code StartPreset.4.start} has a line for, in file order.
     */
    static final List<BindingSection> SECTIONS =
            List.of(BindingSection.GENERAL, BindingSection.SHIP, BindingSection.SRV, BindingSection.ON_FOOT);

    private StartPresetLines() {
    }

    /**
     * Whether two files name the same BindName on every line. The single-line form and four identical lines are the
     * same (Alan, 2026-10-18, P2): the game rewrites its files with nothing changed, and only the values matter.
     */
    static boolean same(StartPresetSections a, StartPresetSections b) {
        return values(a).equals(values(b));
    }

    static List<String> values(StartPresetSections sections) {
        return SECTIONS.stream().map(sections::forSection).toList();
    }
}
