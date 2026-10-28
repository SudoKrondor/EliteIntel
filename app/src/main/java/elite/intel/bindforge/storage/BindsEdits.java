package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.BindingSaveResult;
import elite.intel.bindforge.io.EditedBinds;
import elite.intel.bindforge.io.StartPresetSections;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the Bind Editor does to a set of the game input configuration files - the working copy with the edits on
 * screen (storage.md rules 6-7; Storage S5, 2026-10-28). Pure: each takes a set and returns what it read or the changed
 * set; {@link Edits} holds the set until APPLY. The slot rewriting itself is {@code BindingsWriter}'s.
 * <p>
 * <strong>Any binds file can be edited, not only the active one</strong> (overview.md, <em>Every binds file can be
 * edited</em>; bind-editor.md, <em>Shell</em>).
 */
public final class BindsEdits {

    /** {@code <name>.<major>.<minor>.binds}, the version captured. */
    private static final Pattern VERSIONED = Pattern.compile("(.+)\\.(\\d+)\\.(\\d+)\\.binds");
    /** {@code StartPreset.<N>.start}, N captured. */
    private static final Pattern START_PRESET = Pattern.compile("StartPreset\\.(\\d+)\\.start");

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
        return files.bindings().keySet().stream().filter(Backups::isBindsFile).sorted().toList();
    }

    /**
     * The binds file the set's {@code StartPreset.N.start} names on its General line, when the set holds one by that
     * name - the newest version of it, since a game update leaves the older one behind. Empty when the set has no
     * preset file, its General line is blank, or no binds file has that name.
     */
    public static Optional<String> generalFile(ConfigurationFiles files) {
        Optional<String> preset = files.bindings().keySet().stream()
                .filter(name -> START_PRESET.matcher(name).matches())
                .max(Comparator.comparingLong(BindsEdits::presetGeneration));
        if (preset.isEmpty()) return Optional.empty();
        String text = new String(files.bindings().get(preset.get()), StandardCharsets.UTF_8);
        String general = StartPresetSections.parse(text).general();
        if (general.isEmpty()) return Optional.empty();
        return bindsFiles(files).stream()
                .filter(name -> bindNameOf(name).filter(general::equals).isPresent())
                .max(Comparator.comparingLong(BindsEdits::version));
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
        if (content == null || !Backups.isBindsFile(bindsFile)) {
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

    /** A binds file's BindName - {@code Custom} for {@code Custom.4.2.binds}. */
    public static Optional<String> bindNameOf(String bindsFile) {
        Matcher matcher = VERSIONED.matcher(bindsFile);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    /** A binds file's version as one number, major before minor - {@code -1} when the name carries none. */
    private static long version(String bindsFile) {
        Matcher matcher = VERSIONED.matcher(bindsFile);
        if (!matcher.matches()) return -1;
        // WHY: both numbers bounded so neither overflows into the other.
        return parse(matcher.group(2)) * 1_000_000L + parse(matcher.group(3));
    }

    private static long presetGeneration(String presetFile) {
        Matcher matcher = START_PRESET.matcher(presetFile);
        return matcher.matches() ? parse(matcher.group(1)) : -1;
    }

    private static long parse(String digits) {
        return digits.length() > 6 ? 999_999 : Long.parseLong(digits);
    }
}
