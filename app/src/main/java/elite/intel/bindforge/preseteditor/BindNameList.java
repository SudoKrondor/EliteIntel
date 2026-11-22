package elite.intel.bindforge.preseteditor;

import elite.intel.bindforge.io.FileNames;

import java.util.List;
import java.util.Optional;

/**
 * What the four dropdowns can offer, and what was left out.
 *
 * @param entries    one entry per BindName, in name order, ignoring case
 * @param notOffered every file with a {@code .binds} name that is not a binds file the game can load, in the order
 *                   found
 */
public record BindNameList(List<BindNameEntry> entries, List<NotOfferedFile> notOffered) {

    public BindNameList {
        entries = List.copyOf(entries);
        notOffered = List.copyOf(notOffered);
    }

    /**
     * The entry a line of {@code StartPreset.4.start} names, ignoring case: the game reads names the Windows way, under
     * Proton too (Alan, 2026-11-07, Storage S13a; storage.md, <em>How the files are written</em>). Exact otherwise -
     * a {@code -} or {@code _} apart is another name, and a near miss is the silent failure the spec warns about.
     */
    public Optional<BindNameEntry> find(String bindName) {
        return entries.stream().filter(entry -> FileNames.sameFile(entry.bindName(), bindName)).findFirst();
    }
}
