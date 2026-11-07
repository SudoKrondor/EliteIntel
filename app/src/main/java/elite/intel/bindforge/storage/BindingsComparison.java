package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.FileNames;
import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.model.BindingSection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * The check's comparison for the binds and preset files: every live copy in the bindings folder against the working
 * copy, <strong>by value, never by date or bytes</strong>, as the line items the merge shows (storage.md rule 14;
 * overview.md, <em>Merge grain</em>; Storage S8a). Device files are Storage S9's.
 * <ul>
 *   <li><strong>The merge grain is the slot</strong>: a button's {@code <Primary>} and {@code <Secondary>} are a row
 *       each, and so is its {@code ToggleOn}; an axis, a setting and {@code KeyboardLayout} are one row each; the
 *       root's {@code PresetName} and version are one; the preset file is a row per line.</li>
 *   <li><strong>A file on one side only is a difference</strong>, one row (Alan, 2026-10-30): keeping the working
 *       copy's side of a live-only file removes it from the bindings folder.</li>
 *   <li><strong>A newer version is reconciled, never overwritten</strong> (storage.md rule 14; Storage S10): a live
 *       {@code X.4.3.binds} where the working copy's newest {@code X} is {@code X.4.2.binds} - the version in the file
 *       name, never the root's, which names the tool that wrote it (Alan, 2026-10-31). Its new slots, the elements only
 *       it has, are always taken; the lines of the working copy's file that differ in it are rows; the result is the
 *       newer file under its name, and the older name goes from the working copy and, after the backup, from the
 *       bindings folder. One that cannot be read element by element is left out and named, as before S10.</li>
 *   <li><strong>A reset is named</strong>: a live binds file equal by value to a factory one but for its
 *       {@code PresetName}, or a preset file whose four lines all name factory binds files where the working copy's
 *       name one of the user's own.</li>
 * </ul>
 * Pure: nothing is read or written here.
 */
public final class BindingsComparison {

    private static final Logger log = LogManager.getLogger(BindingsComparison.class);

    /** The row of a file's details - its root's {@code PresetName} and version. */
    static final String DETAILS = "#details";
    /** The row of a file that is compared whole. */
    static final String WHOLE_FILE = "#file";

    private BindingsComparison() {
    }

    /** Which side a merge row keeps. */
    public enum Side {
        /** The working copy's - <em>mine</em>. */
        WORKING_COPY,
        /** The live copy's - <em>in-game</em>. */
        LIVE
    }

    /** What a row is. */
    public enum Kind {
        /** A whole file - on one side only, or one that cannot be read element by element. */
        FILE,
        /** A binds file's {@code PresetName} and version. */
        DETAILS,
        /** A whole element: an axis, a setting, {@code KeyboardLayout}, or an element on one side only. */
        ELEMENT,
        /** One child of a button - {@code Primary}, {@code Secondary}, {@code ToggleOn}. */
        SLOT,
        /** One line of the preset file. */
        PRESET_LINE
    }

    /**
     * One line item that differs.
     *
     * @param file        the file it is in
     * @param key         what it is, unique in its file
     * @param element     the element's name for {@link Kind#ELEMENT} and {@link Kind#SLOT}, else {@code null}
     * @param part        the child's name for {@link Kind#SLOT}, else {@code null}
     * @param section     the section for {@link Kind#PRESET_LINE}, else {@code null}
     * @param workingCopy what the working copy holds, readably; {@code null} when it holds nothing there
     * @param live        what the live copy holds, readably; {@code null} when it holds nothing there
     */
    public record LineItem(String file, String key, Kind kind, String element, String part, BindingSection section,
                           String workingCopy, String live) {
        public LineItem {
            Objects.requireNonNull(file, "file");
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(kind, "kind");
        }

        /** The row's id across the whole set - its file and key. */
        public String id() {
            return file + "|" + key;
        }
    }

    /** Where a file is. */
    public enum Presence {
        BOTH,
        /** In the bindings folder, not the working copy. */
        LIVE_ONLY,
        /** In the working copy, not the bindings folder. */
        WORKING_COPY_ONLY
    }

    /**
     * One file that differs.
     *
     * @param reset whether the live copy looks reset by a game update
     * @param items its rows, at least one - an empty list is refused, since a file that differs always has something
     *              to answer
     */
    public record FileDifference(String file, Presence presence, boolean reset, List<LineItem> items) {
        public FileDifference {
            items = List.copyOf(items);
            if (items.isEmpty()) throw new IllegalArgumentException(file + " differs with no row");
        }
    }

    /**
     * The game's newer version of one of the working copy's binds files (storage.md rule 14; Storage S10). Never
     * overwritten: whatever the answers, the result is the newer file, its new slots in it, under its name.
     *
     * @param older    the working copy's file
     * @param newer    the game's newer one, which the rows are in
     * @param reset    whether the newer one looks reset by a game update
     * @param newSlots the elements only the newer one has, in its order - always taken, and listed so the user binds
     *                 them
     * @param items    the lines of the working copy's file that differ in the newer one, or that it lacks; none when every
     *                 binding carried over unchanged (Alan, 2026-10-31: the root's version and {@code PresetName} aside)
     */
    public record NewerVersion(String older, String newer, boolean reset, List<String> newSlots,
                               List<LineItem> items) {
        public NewerVersion {
            Objects.requireNonNull(older, "older");
            Objects.requireNonNull(newer, "newer");
            newSlots = List.copyOf(newSlots);
            items = List.copyOf(items);
        }

        /** Whether every binding the working copy has is in the newer file, unchanged - approved, not reconciled. */
        public boolean carriedOver() {
            return items.isEmpty();
        }

        /** Whether {@code other} found the same - the reset flag aside, which only the warning's check reads. */
        boolean sameRows(NewerVersion other) {
            return older.equals(other.older) && newer.equals(other.newer) && newSlots.equals(other.newSlots)
                    && items.equals(other.items);
        }
    }

    /**
     * Everything the check found.
     *
     * @param files   each file that differs, by name - the warning's
     * @param newer   each newer version of a working-copy binds file, asked on its own before the warning (Storage S10)
     * @param leftOut a newer version that cannot be read element by element, and the file it replaces unless the live
     *                folder still holds it - left as they are, and named
     */
    public record Differences(List<FileDifference> files, List<NewerVersion> newer, List<String> leftOut) {
        public Differences {
            files = List.copyOf(files);
            newer = List.copyOf(newer);
            leftOut = List.copyOf(leftOut);
        }

        /** Whether nothing differs - no file, and no newer version. */
        public boolean none() {
            return files.isEmpty() && newer.isEmpty();
        }

        /** The warning's rows, file by file - not the newer versions'. */
        public List<LineItem> items() {
            return files.stream().flatMap(file -> file.items().stream()).toList();
        }

        /** Every row to answer: the warning's, then each newer version's. */
        public List<LineItem> rows() {
            List<LineItem> rows = new ArrayList<>(items());
            newer.forEach(version -> rows.addAll(version.items()));
            return rows;
        }

        /** Whether {@code other} holds the same rows and newer versions - what changed meanwhile is judged by. */
        boolean sameRows(Differences other) {
            if (!items().equals(other.items()) || newer.size() != other.newer.size()) return false;
            for (int i = 0; i < newer.size(); i++) {
                if (!newer.get(i).sameRows(other.newer.get(i))) return false;
            }
            return true;
        }

        /** Whether any file looks reset. */
        public boolean anyReset() {
            return files.stream().anyMatch(FileDifference::reset);
        }
    }

    /**
     * Frontier's factory binds files, for naming a reset: their BindNames, and their bytes.
     */
    public record Factory(Set<String> bindNames, Collection<byte[]> bindsFiles) {
        /** None known - the line at Elite-Intel start, which names no reset. */
        public static final Factory NONE = new Factory(Set.of(), List.of());

        public Factory {
            bindNames = Set.copyOf(bindNames);
            bindsFiles = List.copyOf(bindsFiles);
        }
    }

    /**
     * What the check settles on: the binds and preset files the working copy and the bindings folder will both hold,
     * and the live files to remove from the bindings folder - a live-only file the user kept the working copy's side
     * of, and the older name of a file a newer version replaced.
     *
     * @param renamed each working-copy binds file a newer version replaced, to the newer one's name - so the edits on
     *                the older one go on top of the newer (Storage S10)
     */
    public record Settled(Map<String, byte[]> bindings, Set<String> removeFromLive, Map<String, String> renamed) {
        public Settled {
            bindings = Map.copyOf(bindings);
            removeFromLive = Set.copyOf(removeFromLive);
            renamed = Map.copyOf(renamed);
        }
    }

    // ---- comparing

    /**
     * Every binds and preset file that differs.
     *
     * @param workingCopy the working copy's binds and preset files, by name
     * @param found       the bindings folder's, by name as found - paired with the working copy's ignoring case
     */
    public static Differences compare(Map<String, byte[]> workingCopy, Map<String, byte[]> found, Factory factory) {
        Map<String, byte[]> live = paired(workingCopy, found);
        Set<String> leftOut = new TreeSet<>();
        List<NewerVersion> newer = new ArrayList<>();
        Set<String> names = new TreeSet<>(workingCopy.keySet());
        names.addAll(live.keySet());
        newerVersions(workingCopy.keySet(), live.keySet()).forEach((older, newest) -> {
            names.remove(newest);
            Optional<NewerVersion> version = newerVersion(older, newest, workingCopy.get(older), live.get(newest),
                    factory);
            if (version.isPresent()) {
                newer.add(version.get());
                // WHY: the older name goes from the bindings folder too (Alan, 2026-10-31, D3), so it is not compared.
                names.remove(older);
                return;
            }
            leftOut.add(newest);
            if (!live.containsKey(older)) {
                leftOut.add(older);
                names.remove(older);
            }
        });
        List<FileDifference> files = new ArrayList<>();
        for (String name : names) {
            byte[] mine = workingCopy.get(name);
            byte[] theirs = live.get(name);
            Optional<FileDifference> difference;
            if (mine == null) {
                difference = Optional.of(oneSide(name, Presence.LIVE_ONLY, null, describeFile(name, theirs)));
            } else if (theirs == null) {
                difference = Optional.of(oneSide(name, Presence.WORKING_COPY_ONLY, describeFile(name, mine), null));
            } else if (FileNames.isPresetFile(name)) {
                difference = presetFile(name, mine, theirs, workingCopy.keySet(), factory);
            } else {
                difference = bindsFile(name, mine, theirs, factory);
            }
            difference.ifPresent(files::add);
        }
        return new Differences(files, newer, List.copyOf(leftOut));
    }

    /**
     * The bindings folder's files under the working copy's spellings: a live file whose name differs from a working-copy
     * file's only in case is that file, compared by value as usual, and the working copy's spelling is the one the next
     * write leaves (Alan, 2026-11-07, Storage S13a D2). Two live spellings of one file - only where the file system
     * heeds case - are compared as the one spelled as the working copy's, else the first in name order; the write
     * removes the others (D3).
     */
    private static Map<String, byte[]> paired(Map<String, byte[]> workingCopy, Map<String, byte[]> found) {
        FileNames.OneSpelling<byte[]> one = FileNames.oneSpelling(found, workingCopy.keySet());
        if (!one.leftOut().isEmpty()) {
            log.info("The bindings folder holds two spellings of one file; compared without: {}", one.leftOut());
        }
        return one.files();
    }

    /**
     * Each working-copy binds file the game has a newer version of, to the newest such live file: for every BindName,
     * the working copy's newest version, and the newest live file of that BindName above it that the working copy does
     * not hold. Only the file name's version counts - the root's names the tool that last wrote the file, and BindForge
     * gates nothing on it (bind-editor.md, <em>A fourth source</em>; Alan, 2026-10-31, Storage S10 D2).
     */
    private static Map<String, String> newerVersions(Set<String> workingCopy, Set<String> live) {
        Map<String, String> newestMine = new TreeMap<>(FileNames.ONE_FILE);
        for (String file : workingCopy) {
            FileNames.bindName(file).ifPresent(bindName -> newestMine.merge(bindName, file,
                    (a, b) -> FileNames.version(a) >= FileNames.version(b) ? a : b));
        }
        Map<String, String> newer = new TreeMap<>();
        for (String older : newestMine.values()) {
            Optional<String> bindName = FileNames.bindName(older);
            live.stream()
                    .filter(file -> !workingCopy.contains(file)
                            && FileNames.bindName(file).filter(name -> FileNames.sameFile(name, bindName.orElseThrow()))
                            .isPresent()
                            && FileNames.version(file) > FileNames.version(older))
                    .max(Comparator.comparingLong(FileNames::version))
                    .ifPresent(newest -> newer.put(older, newest));
        }
        return newer;
    }

    /**
     * The working copy's {@code older} against the game's {@code newer}: the elements only the newer has are its new
     * slots, and every element of the older that the newer lacks or holds differently is a row, in the newer's name.
     * The root is the newer's whatever is answered, so it is no row. Empty when either cannot be read element by
     * element.
     */
    private static Optional<NewerVersion> newerVersion(String older, String newer, byte[] mine, byte[] theirs,
                                                       Factory factory) {
        Optional<BindsElements> a = BindsElements.of(mine);
        Optional<BindsElements> b = BindsElements.of(theirs);
        if (a.isEmpty() || b.isEmpty()) return Optional.empty();
        List<String> newSlots = b.get().elements().keySet().stream()
                .filter(element -> !a.get().elements().containsKey(element)).toList();
        List<LineItem> items = new ArrayList<>();
        for (BindsElements.Top element : a.get().elements().values()) {
            items.addAll(elementItems(newer, element, b.get().elements().get(element.name())));
        }
        return Optional.of(new NewerVersion(older, newer, isReset(b.get(), factory), newSlots, items));
    }

    private static FileDifference oneSide(String name, Presence presence, String mine, String theirs) {
        return new FileDifference(name, presence, false,
                List.of(new LineItem(name, WHOLE_FILE, Kind.FILE, null, null, null, mine, theirs)));
    }

    private static Optional<FileDifference> presetFile(String name, byte[] mine, byte[] theirs,
                                                       Set<String> workingCopyFiles, Factory factory) {
        StartPresetSections mineLines = lines(mine);
        StartPresetSections theirLines = lines(theirs);
        List<LineItem> items = new ArrayList<>();
        for (BindingSection section : PresetEdits.SECTIONS) {
            String a = mineLines.forSection(section);
            String b = theirLines.forSection(section);
            if (!a.equals(b)) {
                items.add(new LineItem(name, section.name(), Kind.PRESET_LINE, null, null, section, a, b));
            }
        }
        if (items.isEmpty()) return Optional.empty();
        boolean reset = PresetEdits.SECTIONS.stream()
                .allMatch(section -> factory.bindNames().stream()
                        .anyMatch(factoryName -> FileNames.sameFile(factoryName, theirLines.forSection(section))))
                && PresetEdits.SECTIONS.stream()
                .anyMatch(section -> isUsersOwn(mineLines.forSection(section), workingCopyFiles));
        return Optional.of(new FileDifference(name, Presence.BOTH, reset, items));
    }

    private static boolean isUsersOwn(String bindName, Set<String> workingCopyFiles) {
        return workingCopyFiles.stream().anyMatch(file -> FileNames.bindName(file).map(bindName::equals).orElse(false));
    }

    private static Optional<FileDifference> bindsFile(String name, byte[] mine, byte[] theirs, Factory factory) {
        Optional<BindsElements> a = BindsElements.of(mine);
        Optional<BindsElements> b = BindsElements.of(theirs);
        if (a.isEmpty() || b.isEmpty()) {
            if (sameText(mine, theirs)) return Optional.empty();
            return Optional.of(new FileDifference(name, Presence.BOTH, false,
                    List.of(new LineItem(name, WHOLE_FILE, Kind.FILE, null, null, null,
                            describeFile(name, mine), describeFile(name, theirs)))));
        }
        if (a.get().sameValues(b.get())) return Optional.empty();
        List<LineItem> items = new ArrayList<>();
        if (!a.get().rootAttributes().equals(b.get().rootAttributes())) {
            items.add(new LineItem(name, DETAILS, Kind.DETAILS, null, null, null,
                    describeAttributes(a.get().rootAttributes()), describeAttributes(b.get().rootAttributes())));
        }
        Set<String> elements = new LinkedHashSet<>(a.get().elements().keySet());
        elements.addAll(b.get().elements().keySet());
        for (String element : elements) {
            items.addAll(elementItems(name, a.get().elements().get(element), b.get().elements().get(element)));
        }
        return Optional.of(new FileDifference(name, Presence.BOTH, isReset(b.get(), factory), items));
    }

    /**
     * The rows one element gives: none when it is the same, a row per slot for a button of one shape, else one. A button
     * whose value differs with no slot differing - its slots in another order, or text of its own - is one row for the
     * whole element, so a file that differs always has a row (Storage S8R, the S7-S8b review).
     */
    private static List<LineItem> elementItems(String file, BindsElements.Top mine, BindsElements.Top theirs) {
        String element = mine != null ? mine.name() : theirs.name();
        if (mine != null && theirs != null && mine.value().equals(theirs.value())) return List.of();
        if (mine != null && theirs != null && sameShape(mine, theirs)) {
            List<LineItem> items = new ArrayList<>();
            for (BindsElements.Child child : mine.children()) {
                BindsElements.Child other = theirs.child(child.name()).orElseThrow();
                if (child.value().equals(other.value())) continue;
                items.add(new LineItem(file, element + "/" + child.name(), Kind.SLOT, element, child.name(), null,
                        describe(child.node()), describe(other.node())));
            }
            if (!items.isEmpty()) return items;
        }
        return List.of(new LineItem(file, element, Kind.ELEMENT, element, null, null,
                mine == null ? null : describe(mine.node()), theirs == null ? null : describe(theirs.node())));
    }

    /**
     * Whether two versions of a button are compared slot by slot: the same children, in name, on both sides, and their
     * own attributes the same - otherwise one row for the whole element.
     */
    private static boolean sameShape(BindsElements.Top mine, BindsElements.Top theirs) {
        if (!mine.bySlot() || !theirs.bySlot()) return false;
        List<String> a = mine.children().stream().map(BindsElements.Child::name).sorted().toList();
        List<String> b = theirs.children().stream().map(BindsElements.Child::name).sorted().toList();
        return a.equals(b) && ownAttributes(mine.node()).equals(ownAttributes(theirs.node()));
    }

    private static Map<String, String> ownAttributes(Element element) {
        Map<String, String> attributes = new TreeMap<>();
        for (int i = 0; i < element.getAttributes().getLength(); i++) {
            Node attribute = element.getAttributes().item(i);
            attributes.put(attribute.getNodeName(), attribute.getNodeValue());
        }
        return attributes;
    }

    private static boolean isReset(BindsElements live, Factory factory) {
        for (byte[] factoryFile : factory.bindsFiles()) {
            Optional<BindsElements> stock = BindsElements.of(factoryFile);
            if (stock.isPresent() && live.sameValuesButName(stock.get())) return true;
        }
        return false;
    }

    // ---- settling

    /**
     * The binds and preset files once every row is answered.
     *
     * @param workingCopy the working copy's binds and preset files, as compared
     * @param found       the bindings folder's, as compared
     * @param answers     the side each row keeps, by {@link LineItem#id()}
     * @throws IllegalArgumentException if a row has no answer
     * @throws IOException              if a merged binds file could not be made - the message names it
     */
    public static Settled settle(Map<String, byte[]> workingCopy, Map<String, byte[]> found, Differences differences,
                                 Map<String, Side> answers) throws IOException {
        Map<String, byte[]> live = paired(workingCopy, found);
        Map<String, byte[]> settled = new TreeMap<>(workingCopy);
        Set<String> remove = new TreeSet<>();
        for (FileDifference file : differences.files()) {
            List<Side> sides = file.items().stream().map(item -> answerOf(item, answers)).toList();
            String name = file.file();
            if (sides.stream().allMatch(Side.WORKING_COPY::equals)) {
                if (file.presence() == Presence.LIVE_ONLY) remove.add(name);
                continue;
            }
            if (sides.stream().allMatch(Side.LIVE::equals)) {
                if (file.presence() == Presence.WORKING_COPY_ONLY) settled.remove(name);
                else settled.put(name, live.get(name));
                continue;
            }
            if (FileNames.isPresetFile(name)) {
                settled.put(name, mergedPresetFile(file, workingCopy.get(name), live.get(name), answers));
            } else {
                settled.put(name, mergedBindsFile(file, workingCopy.get(name), live.get(name), answers));
            }
        }
        Map<String, String> renamed = new TreeMap<>();
        for (NewerVersion version : differences.newer()) {
            settled.remove(version.older());
            settled.put(version.newer(), mergedNewerVersion(version, workingCopy.get(version.older()),
                    live.get(version.newer()), answers));
            // WHY: the older name goes from the bindings folder after the backup, as a rename's does (Alan, 2026-10-31,
            // Storage S10 D3) - the backup holds it.
            if (live.containsKey(version.older())) remove.add(version.older());
            renamed.put(version.older(), version.newer());
        }
        return new Settled(settled, remove, renamed);
    }

    /**
     * The newer version, never written over (storage.md rule 14): the game's file, its new slots and root kept, with
     * each row answered as the working copy's spliced back in - a line the game changed or dropped, the user's. Every
     * other byte is the game's.
     */
    private static byte[] mergedNewerVersion(NewerVersion version, byte[] mine, byte[] theirs,
                                             Map<String, Side> answers) throws IOException {
        List<BindsElements.Splice> splices = new ArrayList<>();
        BindsElements a = BindsElements.of(mine).orElseThrow(() -> notMerged(version.newer()));
        BindsElements b = BindsElements.of(theirs).orElseThrow(() -> notMerged(version.newer()));
        for (LineItem item : version.items()) {
            if (answerOf(item, answers) != Side.WORKING_COPY) continue;
            switch (item.kind()) {
                case SLOT -> {
                    BindsElements.Child gameChild = b.elements().get(item.element()).child(item.part()).orElseThrow();
                    BindsElements.Child mineChild = a.elements().get(item.element()).child(item.part()).orElseThrow();
                    splices.add(new BindsElements.Splice(gameChild.range(), a.text(mineChild.range())));
                }
                // WHY: the roles swapped - the game's file is the one spliced, the working copy's text the one taken.
                case ELEMENT -> splices.add(elementSplice(b, a, item.element()));
                case FILE, DETAILS, PRESET_LINE -> throw new IllegalStateException("not a row of a newer version");
            }
        }
        if (splices.isEmpty()) return theirs;
        byte[] merged = b.with(splices);
        // WHY: never write a file the check cannot read back - a merge that broke it would reach the game.
        if (BindsElements.of(merged).isEmpty()) throw notMerged(version.newer());
        return merged;
    }

    private static Side answerOf(LineItem item, Map<String, Side> answers) {
        Side side = answers.get(item.id());
        if (side == null) throw new IllegalArgumentException("no answer for " + item.id());
        return side;
    }

    private static byte[] mergedPresetFile(FileDifference file, byte[] mine, byte[] theirs,
                                           Map<String, Side> answers) {
        StartPresetSections a = lines(mine);
        StartPresetSections b = lines(theirs);
        String[] chosen = new String[PresetEdits.SECTIONS.size()];
        for (int i = 0; i < chosen.length; i++) {
            chosen[i] = a.forSection(PresetEdits.SECTIONS.get(i));
        }
        for (LineItem item : file.items()) {
            if (answers.get(item.id()) == Side.LIVE) {
                chosen[PresetEdits.SECTIONS.indexOf(item.section())] = b.forSection(item.section());
            }
        }
        return PresetEdits.gameForm(new StartPresetSections(chosen[0], chosen[1], chosen[2], chosen[3], false))
                .getBytes(StandardCharsets.UTF_8);
    }

    /** The working copy's file with each row answered in-game replaced by the live copy's text there. */
    private static byte[] mergedBindsFile(FileDifference file, byte[] mine, byte[] theirs,
                                          Map<String, Side> answers) throws IOException {
        BindsElements a = BindsElements.of(mine).orElseThrow(() -> notMerged(file.file()));
        BindsElements b = BindsElements.of(theirs).orElseThrow(() -> notMerged(file.file()));
        List<BindsElements.Splice> splices = new ArrayList<>();
        for (LineItem item : file.items()) {
            if (answers.get(item.id()) != Side.LIVE) continue;
            switch (item.kind()) {
                case DETAILS -> splices.add(new BindsElements.Splice(a.rootStartTag(), b.text(b.rootStartTag())));
                case SLOT -> {
                    BindsElements.Child mineChild = a.elements().get(item.element()).child(item.part()).orElseThrow();
                    BindsElements.Child theirChild = b.elements().get(item.element()).child(item.part()).orElseThrow();
                    splices.add(new BindsElements.Splice(mineChild.range(), b.text(theirChild.range())));
                }
                case ELEMENT -> splices.add(elementSplice(a, b, item.element()));
                case FILE, PRESET_LINE -> throw new IllegalStateException("not a row of a binds file merged by row");
            }
        }
        byte[] merged = a.with(splices);
        // WHY: never write a file the check cannot read back - a merge that broke it would reach the game.
        if (BindsElements.of(merged).isEmpty()) throw notMerged(file.file());
        return merged;
    }

    private static BindsElements.Splice elementSplice(BindsElements mine, BindsElements theirs, String element) {
        BindsElements.Top a = mine.elements().get(element);
        BindsElements.Top b = theirs.elements().get(element);
        if (a != null && b != null) return new BindsElements.Splice(a.range(), theirs.text(b.range()));
        if (a != null) return new BindsElements.Splice(mine.wholeLines(a.range()), "");
        int at = mine.insertionPoint();
        String line = theirs.indentOf(b.range().start()) + theirs.text(b.range()) + mine.lineEnding();
        return new BindsElements.Splice(new BindsElements.Range(at, at), line);
    }

    private static IOException notMerged(String file) {
        return new IOException(file + " could not be merged");
    }

    // ---- the edits, at APPLY

    /**
     * The line items the edits change: the working copy against the working copy with the edits on it. A binds file
     * the edits renamed is compared with the file it was, under its old name ({@link Rename#between}), so a rename
     * covers what it changed and not every line of the file: a binding changed in-game meanwhile is still asked about
     * (review, 2026-11-03).
     *
     * @param applied the working copy's binds and preset files
     * @param current the same with the edits on them
     */
    public static List<LineItem> edited(Map<String, byte[]> applied, Map<String, byte[]> current) {
        Map<String, String> oldNames = new TreeMap<>();
        Rename.between(applied, current).forEach((older, newer) -> oldNames.put(newer, older));
        return compare(applied, named(current, oldNames), Factory.NONE).items();
    }

    /**
     * {@link #edited} as the check needs it, knowing what it found: a binds file renamed while the game made a newer
     * version of it stays covered whole, so nothing is asked that would only be thrown away (Alan, 2026-11-03, Storage
     * S12 D1: no guard for the game updating a file mid-rename).
     */
    static List<LineItem> editedAtTheCheck(Differences found, Map<String, byte[]> applied, Map<String, byte[]> current) {
        List<LineItem> edits = new ArrayList<>(edited(applied, current));
        Map<String, String> renamed = Rename.between(applied, current);
        for (NewerVersion version : found.newer()) {
            if (!renamed.containsKey(version.older())) continue;
            edits.addAll(oneSide(version.older(), Presence.WORKING_COPY_ONLY, null, null).items());
        }
        return edits;
    }

    /**
     * Whether an edit covers a row - the same line item, or one of a coarser grain holding it. A whole file covers every
     * row of it; an element covers each of its slots, and a slot the whole element it is in, so where the two meet at a
     * different grain the edit's whole element wins (Alan, 2026-10-30, Storage S8b).
     */
    static boolean covers(LineItem edit, LineItem row) {
        if (!edit.file().equals(row.file())) return false;
        if (edit.kind() == Kind.FILE || row.kind() == Kind.FILE) return true;
        if (edit.kind() == Kind.DETAILS || row.kind() == Kind.DETAILS) return edit.kind() == row.kind();
        if (edit.kind() == Kind.PRESET_LINE || row.kind() == Kind.PRESET_LINE) return edit.section() == row.section();
        if (!edit.element().equals(row.element())) return false;
        return edit.part() == null || row.part() == null || edit.part().equals(row.part());
    }

    /**
     * The differences without the rows an edit covers - at APPLY the edit goes on top of whichever side is kept, so the
     * user is not asked about them (Alan, 2026-10-30, Storage S8b). A file left with no row is left out.
     */
    public static Differences without(Differences differences, List<LineItem> edits) {
        if (edits.isEmpty()) return differences;
        List<FileDifference> files = new ArrayList<>();
        for (FileDifference file : differences.files()) {
            List<LineItem> asked = file.items().stream()
                    .filter(row -> edits.stream().noneMatch(edit -> covers(edit, row)))
                    .toList();
            if (!asked.isEmpty()) files.add(new FileDifference(file.file(), file.presence(), file.reset(), asked));
        }
        // WHY: an edit on the older file covers the same line of the newer, which it goes on top of (Storage S10). A
        // newer version is kept with no rows left: it is still approved, and its new slots listed.
        List<NewerVersion> newer = new ArrayList<>();
        for (NewerVersion version : differences.newer()) {
            List<LineItem> renamed = edits.stream()
                    .map(edit -> edit.file().equals(version.older()) ? inFile(edit, version.newer()) : edit)
                    .toList();
            List<LineItem> asked = version.items().stream()
                    .filter(row -> renamed.stream().noneMatch(edit -> covers(edit, row)))
                    .toList();
            newer.add(new NewerVersion(version.older(), version.newer(), version.reset(), version.newSlots(), asked));
        }
        return new Differences(files, newer, differences.leftOut());
    }

    private static LineItem inFile(LineItem item, String file) {
        return new LineItem(file, item.key(), item.kind(), item.element(), item.part(), item.section(),
                item.workingCopy(), item.live());
    }

    /**
     * The edits put on top of the settled files (storage.md rule 14, <em>At APPLY</em>): on every line item the edits
     * change, the edits' side; everywhere else, the settled side.
     *
     * @param settled the binds and preset files the check settled on
     * @param renamed each file a newer version replaced, to its new name ({@link Settled#renamed()}): the edits on the
     *                older go on top of the newer (Storage S10)
     * @param applied the working copy's, which the edits started from
     * @param current the working copy's with the edits on them. A binds file they renamed takes what was settled for
     *                the file it was, under its new name and {@code PresetName}
     * @throws IOException if a file could not be merged, or the edits are on a binds file whose newer version the
     *                     check left out - the message names it
     */
    public static Map<String, byte[]> withEditsOnTop(Map<String, byte[]> settled, Map<String, String> renamed,
                                                     Map<String, byte[]> applied, Map<String, byte[]> current)
            throws IOException {
        Map<String, String> renamedByEdits = Rename.between(applied, current);
        Map<String, byte[]> settledNamed = withRenamesMade(settled, renamedByEdits);
        Map<String, byte[]> appliedNamed = named(named(applied, renamed), renamedByEdits);
        Map<String, byte[]> currentNamed = named(current, renamed);
        List<LineItem> edits = edited(appliedNamed, currentNamed);
        if (edits.isEmpty()) return settledNamed;
        Differences between = compare(settledNamed, currentNamed, Factory.NONE);
        // WHY: a row left out or asked as a newer version is never merged here, so an edit on it would be dropped
        // unseen. Edits never make a newer version, so this is a broken invariant rather than something the user did.
        if (!between.leftOut().isEmpty() || !between.newer().isEmpty()) {
            List<String> names = new ArrayList<>(between.leftOut());
            between.newer().forEach(version -> names.add(version.newer()));
            throw new IOException("the edits cannot be put on top of " + String.join(", ", names));
        }
        Map<String, Side> answers = new HashMap<>();
        for (LineItem row : between.items()) {
            boolean edited = edits.stream().anyMatch(edit -> covers(edit, row));
            answers.put(row.id(), edited ? Side.LIVE : Side.WORKING_COPY);
        }
        return settle(settledNamed, currentNamed, between, answers).bindings();
    }

    /**
     * {@code settled} with each binds file the edits renamed under its new name and {@code PresetName}.
     */
    private static Map<String, byte[]> withRenamesMade(Map<String, byte[]> settled, Map<String, String> renamedByEdits)
            throws IOException {
        if (renamedByEdits.isEmpty()) return settled;
        Map<String, byte[]> named = new TreeMap<>(settled);
        for (Map.Entry<String, String> rename : renamedByEdits.entrySet()) {
            byte[] content = named.get(rename.getKey());
            if (content == null || named.containsKey(rename.getValue())) continue;
            named.remove(rename.getKey());
            named.put(rename.getValue(),
                    Create.withPresetName(content, FileNames.bindName(rename.getValue()).orElseThrow()));
        }
        return named;
    }

    /** {@code files} with each older name in {@code renamed} under its newer one - the bytes unchanged. */
    private static Map<String, byte[]> named(Map<String, byte[]> files, Map<String, String> renamed) {
        if (renamed.isEmpty()) return files;
        Map<String, byte[]> named = new TreeMap<>(files);
        renamed.forEach((older, newer) -> {
            byte[] content = named.get(older);
            if (content != null && !named.containsKey(newer)) {
                named.remove(older);
                named.put(newer, content);
            }
        });
        return named;
    }

    // ---- reading and describing

    private static StartPresetSections lines(byte[] content) {
        return StartPresetSections.parse(new String(content, StandardCharsets.UTF_8));
    }

    /** Whether two files hold the same text but for a byte-order mark, line endings and trailing whitespace. */
    private static boolean sameText(byte[] a, byte[] b) {
        return normal(a).equals(normal(b));
    }

    private static String normal(byte[] content) {
        return StartPresetSections.withoutByteOrderMark(new String(content, StandardCharsets.UTF_8))
                .replace("\r\n", "\n").strip();
    }

    /** A whole file, readably: its BindName for a binds file, its lines for the preset file. */
    private static String describeFile(String name, byte[] content) {
        if (FileNames.isPresetFile(name)) {
            StartPresetSections sections = lines(content);
            return String.join(" / ", PresetEdits.SECTIONS.stream().map(sections::forSection).toList());
        }
        return BindsElements.of(content)
                .map(elements -> describeAttributes(elements.rootAttributes()))
                .orElse(name);
    }

    private static String describeAttributes(Map<String, String> attributes) {
        String name = attributes.getOrDefault("PresetName", "");
        String major = attributes.get("MajorVersion");
        String minor = attributes.get("MinorVersion");
        return major == null || minor == null ? name : name + " " + major + "." + minor;
    }

    /**
     * An element, readably: <em>Key_LeftShift + Key_A</em> for a keyboard slot, <em>RVWAP Joy_XAxis, Inverted 1</em>
     * for an axis, a setting's value. Game tokens, not translated - they are the game's own names. An unbound slot is
     * empty.
     */
    static String describe(Element element) {
        if (element.hasAttribute("Device")) return describeSlot(element);
        if (element.hasAttribute("Value") && childElements(element).isEmpty()) return element.getAttribute("Value");
        List<Element> children = childElements(element);
        if (children.isEmpty()) return element.getTextContent().strip();
        List<String> parts = new ArrayList<>();
        for (Element child : children) {
            String described = describe(child);
            if (described.isEmpty()) continue;
            boolean slot = child.hasAttribute("Device");
            parts.add(slot ? described : child.getTagName() + " " + described);
        }
        return String.join(", ", parts);
    }

    private static String describeSlot(Element slot) {
        String main = input(slot);
        if (main.isEmpty()) return "";
        List<String> keys = new ArrayList<>();
        boolean hold = false;
        for (Element child : childElements(slot)) {
            if (child.getTagName().equals("Modifier")) keys.add(input(child));
            else if (child.getTagName().equals("Hold") && "1".equals(child.getAttribute("Value"))) hold = true;
        }
        keys.add(main);
        return String.join(" + ", keys) + (hold ? " (Hold)" : "");
    }

    private static String input(Element element) {
        String device = element.getAttribute("Device");
        String key = element.getAttribute("Key");
        if (device.isEmpty() || device.equals("{NoDevice}")) return "";
        return device.equals("Keyboard") ? key : device + " " + key;
    }

    private static List<Element> childElements(Element parent) {
        List<Element> children = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element child) children.add(child);
        }
        return children;
    }
}
