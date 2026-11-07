package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.FileNames;
import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.preseteditor.BindNameEntry;
import elite.intel.bindforge.preseteditor.BindNameList;
import elite.intel.bindforge.preseteditor.BindsFileOrigin;
import elite.intel.db.managers.BindForgeInstallationsManager;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CREATE - a new player's first binds file (storage.md rule 16; overview.md, <em>Create</em>; Storage S11). Shown while
 * the working copy holds no binds file of the user's; it copies one of Frontier's factory binds files under a name the
 * user gives, and points all four lines of the preset file at it - an edit not applied, like every other.
 * <p>
 * Pure except where it says it touches the disk. The name rule is the controller name's
 * ({@code DeviceNames.validate}), checked by the screen against {@link Choices#taken()}, since {@code bindforge.devices}
 * depends on this package and not the other way round.
 */
public final class Create {

    /** The factory binds file offered first and selected - the game's own default (overview.md, <em>Create</em>). */
    public static final String FIRST_FACTORY = "KeyboardMouseOnly";

    /** The name offered - the one the game gives the first binds file it makes. */
    public static final String DEFAULT_NAME = "Custom";

    /**
     * The version a new binds file is named with when its factory file's root carries none: the Live game's
     * (Alan, 2026-11-02, Storage S11, Q1).
     */
    static final String FALLBACK_MAJOR = "4";
    static final String FALLBACK_MINOR = "2";

    private static final Pattern ROOT_START = Pattern.compile("<Root(?=[\\s/>])");
    private static final String VERSION_DIGITS = "\\d{1,6}";

    private Create() {
    }

    /**
     * What CREATE shows, from one snapshot of the edits.
     *
     * @param offered whether CREATE is on the bar: the working copy holds no binds file of the user's
     * @param editors whether the editor tabs are usable: the edits hold a binds file - after CREATE, or ever since one
     *                existed
     */
    public record State(boolean offered, boolean editors) {

        /** Before first-time startup, or while the working copy cannot be read: no CREATE, and the tabs as they were. */
        public static final State NOT_SET_UP = new State(false, true);

        public static State of(Edits.Snapshot snapshot) {
            return new State(!hasBindsFile(snapshot.applied()), hasBindsFile(snapshot.current()));
        }
    }

    /** Whether the set holds a binds file of the user's - any {@code <name>.<major>.<minor>.binds}. */
    public static boolean hasBindsFile(ConfigurationFiles files) {
        return files.bindings().keySet().stream().anyMatch(FileNames::isBindsFile);
    }

    /** One of Frontier's factory binds files CREATE can start from. */
    public record Factory(String bindName, Path file) {
        public Factory {
            Objects.requireNonNull(bindName, "bindName");
            Objects.requireNonNull(file, "file");
        }
    }

    /**
     * What the CREATE dialog offers.
     *
     * @param factories   the factory binds files, {@value #FIRST_FACTORY} first, the rest by name
     * @param taken       every BindName a new file must not take - the factory ones and the set's own
     * @param unreachable the installations whose factory files could not be listed
     */
    public record Choices(List<Factory> factories, List<String> taken, List<InstallationRow> unreachable) {
        public Choices {
            factories = List.copyOf(factories);
            taken = List.copyOf(taken);
            unreachable = List.copyOf(unreachable);
        }
    }

    /** The choices for the edits, from every installation Elite-Intel knows. Touches the disk - not for the EDT. */
    public static Choices choices() throws IOException {
        ConfigurationFiles current = Edits.stored().snapshot().current();
        return choices(Apply.offered(current, BindForgeInstallationsManager.getInstance().findAll()));
    }

    static Choices choices(Apply.Offered offered) {
        BindNameList list = offered.list();
        // WHY: the same factory file in several installations is one entry; its first file is used, since every
        // installation runs one game version (Alan, 2026-11-02, Storage S11, Q2).
        List<Factory> factories = list.entries().stream()
                .filter(entry -> entry.origin() == BindsFileOrigin.FACTORY)
                .map(entry -> new Factory(entry.bindName(), entry.files().getFirst()))
                .sorted(Comparator.comparing((Factory factory) -> !factory.bindName().equals(FIRST_FACTORY))
                        .thenComparing(Factory::bindName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<String> taken = list.entries().stream().map(BindNameEntry::bindName).toList();
        return new Choices(factories, taken, offered.unreachable());
    }

    /** Reads a factory file's bytes. Touches the disk - not for the EDT. */
    public static byte[] read(Factory factory) throws IOException {
        return Files.readAllBytes(factory.file());
    }

    /**
     * CREATE as an {@link Edits.EditFromApplied}: {@code factoryContent} added under {@code name} and the version its
     * root carries, its {@code PresetName} made {@code name} and every other byte kept, and all four preset lines set
     * to {@code name}. A preset file is added as {@link PresetEdits#NEW_PRESET_FILE} when the set has none.
     *
     * @param name a name {@code DeviceNames.validate} accepts - letters, digits, {@code -} and {@code _}
     * @throws IllegalStateException if the edits already hold a binds file - CREATE is for a user with none
     * @throws IOException           if {@code factoryContent} has no {@code <Root>} element to name
     */
    public static ConfigurationFiles create(ConfigurationFiles current, ConfigurationFiles applied, String name,
                                            byte[] factoryContent) throws IOException {
        if (hasBindsFile(current)) throw new IllegalStateException("the edits already hold a binds file");
        if (!name.matches("[A-Za-z][A-Za-z0-9_-]*")) {
            throw new IllegalArgumentException("not a binds file name: " + name);
        }
        String fileName = fileName(name, factoryContent);
        ConfigurationFiles withFile = current.withBindingsFile(fileName, withPresetName(factoryContent, name));
        return PresetEdits.change(withFile, applied, new StartPresetSections(name, name, name, name, false));
    }

    /**
     * The new file's name: {@code <name>.<major>.<minor>.binds}, the version from the factory file's root
     * {@code MajorVersion} / {@code MinorVersion}, else {@value #FALLBACK_MAJOR}.{@value #FALLBACK_MINOR}. It only
     * names the file - nothing is decided by it (Alan, 2026-11-02, Storage S11, Q1; storage.md rule 14, <em>only the
     * file name's version counts</em>).
     */
    static String fileName(String name, byte[] factoryContent) throws IOException {
        String text = text(factoryContent);
        RootTag tag = rootTag(text);
        String root = text.substring(tag.start(), tag.end());
        String major = attribute(root, "MajorVersion");
        String minor = attribute(root, "MinorVersion");
        boolean versioned = major != null && minor != null
                && major.matches(VERSION_DIGITS) && minor.matches(VERSION_DIGITS);
        return name + "." + (versioned ? major : FALLBACK_MAJOR) + "." + (versioned ? minor : FALLBACK_MINOR)
                + ".binds";
    }

    /**
     * {@code content} with its root's {@code PresetName} set to {@code name} - added when there is none - and every
     * other byte as it was. The name must be ASCII, as the name rule makes it.
     *
     * @throws IOException if there is no {@code <Root>} start tag
     */
    static byte[] withPresetName(byte[] content, String name) throws IOException {
        String text = text(content);
        RootTag root = rootTag(text);
        String tag = text.substring(root.start(), root.end());
        Matcher presetName = attributeIn(tag, "PresetName");
        String newTag = presetName.find()
                ? tag.substring(0, presetName.start(2)) + name + tag.substring(presetName.end(2))
                : "<Root PresetName=\"" + name + "\"" + tag.substring("<Root".length());
        String changed = text.substring(0, root.start()) + newTag + text.substring(root.end());
        return changed.getBytes(StandardCharsets.ISO_8859_1);
    }

    /** The bytes one character each, so the text written back is the same bytes wherever nothing was changed. */
    private static String text(byte[] content) {
        return new String(content, StandardCharsets.ISO_8859_1);
    }

    /** Where the root's start tag is: from {@code <Root} up to, not including, its {@code >}. */
    private record RootTag(int start, int end) {
    }

    private static RootTag rootTag(String text) throws IOException {
        Matcher start = ROOT_START.matcher(text);
        if (!start.find()) throw new IOException("no <Root> element");
        int end = text.indexOf('>', start.end());
        if (end < 0) throw new IOException("the <Root> element is not closed");
        return new RootTag(start.start(), end);
    }

    private static String attribute(String tag, String attribute) {
        Matcher matcher = attributeIn(tag, attribute);
        return matcher.find() ? matcher.group(2) : null;
    }

    /** A matcher for {@code attribute="value"} in a start tag; group 2 is the value. */
    private static Matcher attributeIn(String tag, String attribute) {
        return Pattern.compile("\\s" + attribute + "\\s*=\\s*([\"'])(.*?)\\1").matcher(tag);
    }
}
