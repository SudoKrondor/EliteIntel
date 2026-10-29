package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.BindingSaveResult;
import elite.intel.bindforge.io.EditedBinds;
import elite.intel.bindforge.io.StartPresetSections;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * What the Bind Editor does to a set of the game input configuration files - the working copy with the edits on
 * screen (storage.md rules 6-7; Storage S5, 2026-10-28). Pure: each takes a set and returns what it read or the changed
 * set; {@link Edits} holds the set until APPLY. The slot rewriting itself is {@code BindingsWriter}'s.
 * <p>
 * <strong>Any binds file can be edited, not only the active one</strong> (overview.md, <em>Every binds file can be
 * edited</em>; bind-editor.md, <em>Shell</em>).
 */
public final class BindsEdits {

    private BindsEdits() {
    }

    /** One slot change to one binds file's bytes - a {@code BindingsWriter} edit. */
    @FunctionalInterface
    public interface SlotChange {
        EditedBinds apply(byte[] binds);
    }

    /**
     * What a Bind Editor change did: the outcome, and the set afterwards - the set it was given unless it is
     * {@link BindingSaveResult#SAVED}.
     */
    public record Changed(BindingSaveResult result, ConfigurationFiles files) {
    }

    /** Every binds file in the set, by file name, sorted - what the Bind Editor's Load list offers. */
    public static List<String> bindsFiles(ConfigurationFiles files) {
        return files.bindings().keySet().stream().filter(FileNames::isBindsFile).sorted().toList();
    }

    /**
     * The binds file the set's preset file ({@link PresetEdits#presetFile}) names on its General line, when the set
     * holds one by that name - the newest version of it, since a game update leaves the older one behind. Empty when
     * the set has no preset file, its General line is blank, or no binds file has that name.
     */
    public static Optional<String> generalFile(ConfigurationFiles files) {
        String general = PresetEdits.sections(files).map(StartPresetSections::general).orElse("");
        if (general.isEmpty()) return Optional.empty();
        return bindsFiles(files).stream()
                .filter(name -> FileNames.bindName(name).filter(general::equals).isPresent())
                .max(Comparator.comparingLong(FileNames::version));
    }

    /**
     * Makes the changes to one binds file, one after the other, all or none: the first that is neither
     * {@link BindingSaveResult#SAVED} nor {@link BindingSaveResult#NO_CHANGE} stops them, and the set comes back as
     * it was, so one SAVE never leaves half a row changed (Storage S5; until then the first write stayed in the file).
     *
     * @param bindsFile a binds file the set holds
     * @return {@code SAVED} when any change was made, else {@code NO_CHANGE}, else the refusal
     * @throws IllegalArgumentException if the set holds no such binds file
     */
    public static Changed change(ConfigurationFiles files, String bindsFile, List<SlotChange> changes) {
        byte[] content = files.bindings().get(bindsFile);
        if (content == null || !FileNames.isBindsFile(bindsFile)) {
            throw new IllegalArgumentException("no binds file " + bindsFile + " in the set");
        }
        BindingSaveResult result = BindingSaveResult.NO_CHANGE;
        for (SlotChange change : changes) {
            EditedBinds edited = change.apply(content);
            if (edited.result() != BindingSaveResult.SAVED && edited.result() != BindingSaveResult.NO_CHANGE) {
                return new Changed(edited.result(), files);
            }
            if (edited.changed()) {
                content = edited.content();
                result = BindingSaveResult.SAVED;
            }
        }
        if (result != BindingSaveResult.SAVED) return new Changed(result, files);
        return new Changed(result, files.withBindingsFile(bindsFile, content));
    }

    /** {@link #change(ConfigurationFiles, String, List)} for one change. */
    public static Changed change(ConfigurationFiles files, String bindsFile, SlotChange change) {
        return change(files, bindsFile, List.of(change));
    }
}
