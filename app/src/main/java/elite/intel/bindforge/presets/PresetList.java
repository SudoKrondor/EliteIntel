package elite.intel.bindforge.presets;

import java.util.List;
import java.util.Optional;

/**
 * What the four dropdowns can offer, and what was left out.
 *
 * @param presets    one entry per name, in name order, ignoring case
 * @param notOffered every file with a {@code .binds} name that is not a preset, in the order found
 */
public record PresetList(List<PresetEntry> presets, List<NotOfferedFile> notOffered) {

    public PresetList {
        presets = List.copyOf(presets);
        notOffered = List.copyOf(notOffered);
    }

    /**
     * The entry a {@code StartPreset} line names. Exact match: the game's names are case-sensitive as far as
     * anyone has shown, and a near miss is the silent failure the spec warns about.
     */
    public Optional<PresetEntry> find(String name) {
        return presets.stream().filter(entry -> entry.name().equals(name)).findFirst();
    }
}
