package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.model.BindingSection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
 *   <li><strong>A newer version is left out until Storage S10</strong>: a live {@code X.4.3.binds} beside the working
 *       copy's {@code X.4.2.binds}, always; the {@code X.4.2.binds} too unless the live folder still holds it.</li>
 *   <li><strong>A reset is named</strong>: a live binds file equal by value to a factory one but for its
 *       {@code PresetName}, or a preset file whose four lines all name factory binds files where the working copy's
 *       name one of the user's own.</li>
 * </ul>
 * Pure: nothing is read or written here.
 */
public final class BindingsComparison {

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
     * Everything the check found.
     *
     * @param files   each file that differs, by name
     * @param leftOut the files left out until Storage S10 - a newer version beside an older one
     */
    public record Differences(List<FileDifference> files, List<String> leftOut) {
        public Differences {
            files = List.copyOf(files);
            leftOut = List.copyOf(leftOut);
        }

        /** Whether nothing differs. */
        public boolean none() {
            return files.isEmpty();
        }

        /** Every row, file by file. */
        public List<LineItem> items() {
            return files.stream().flatMap(file -> file.items().stream()).toList();
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
     * of.
     */
    public record Settled(Map<String, byte[]> bindings, Set<String> removeFromLive) {
        public Settled {
            bindings = Map.copyOf(bindings);
            removeFromLive = Set.copyOf(removeFromLive);
        }
    }

    // ---- comparing

    /**
     * Every binds and preset file that differs.
     *
     * @param workingCopy the working copy's binds and preset files, by name
     * @param live        the bindings folder's, by name
     */
    public static Differences compare(Map<String, byte[]> workingCopy, Map<String, byte[]> live, Factory factory) {
        Set<String> leftOut = leftOut(workingCopy.keySet(), live.keySet());
        Set<String> names = new TreeSet<>(workingCopy.keySet());
        names.addAll(live.keySet());
        names.removeAll(leftOut);
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
        return new Differences(files, List.copyOf(leftOut));
    }

    /**
     * A live binds file newer than one of the working copy's with the same BindName - left for Storage S10, always, so
     * no answer deletes it. The older one is left out too only when the live folder lacks it; one the live folder still
     * holds is compared as usual (Alan, 2026-10-31, Storage S8R).
     */
    private static Set<String> leftOut(Set<String> workingCopy, Set<String> live) {
        Set<String> leftOut = new TreeSet<>();
        for (String newer : live) {
            if (workingCopy.contains(newer)) continue;
            Optional<String> bindName = FileNames.bindName(newer);
            if (bindName.isEmpty()) continue;
            for (String older : workingCopy) {
                if (bindName.equals(FileNames.bindName(older)) && FileNames.version(older) < FileNames.version(newer)) {
                    leftOut.add(newer);
                    if (!live.contains(older)) leftOut.add(older);
                }
            }
        }
        return leftOut;
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
                .allMatch(section -> factory.bindNames().contains(theirLines.forSection(section)))
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
     * @param live        the bindings folder's, as compared
     * @param answers     the side each row keeps, by {@link LineItem#id()}
     * @throws IllegalArgumentException if a row has no answer
     * @throws IOException              if a merged binds file could not be made - the message names it
     */
    public static Settled settle(Map<String, byte[]> workingCopy, Map<String, byte[]> live, Differences differences,
                                 Map<String, Side> answers) throws IOException {
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
        return new Settled(settled, remove);
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
     * The line items the edits change: the working copy against the working copy with the edits on it.
     *
     * @param applied the working copy's binds and preset files
     * @param current the same with the edits on them
     */
    public static List<LineItem> edited(Map<String, byte[]> applied, Map<String, byte[]> current) {
        return compare(applied, current, Factory.NONE).items();
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
        return new Differences(files, differences.leftOut());
    }

    /**
     * The edits put on top of the settled files (storage.md rule 14, <em>At APPLY</em>): on every line item the edits
     * change, the edits' side; everywhere else, the settled side.
     *
     * @param settled the binds and preset files the check settled on
     * @param applied the working copy's, which the edits started from
     * @param current the working copy's with the edits on them
     * @throws IOException if a file could not be merged, or the edits are on a binds file whose newer version the
     *                     check left out - the message names it
     */
    public static Map<String, byte[]> withEditsOnTop(Map<String, byte[]> settled, Map<String, byte[]> applied,
                                                     Map<String, byte[]> current) throws IOException {
        List<LineItem> edits = edited(applied, current);
        if (edits.isEmpty()) return settled;
        Differences between = compare(settled, current, Factory.NONE);
        // WHY: a row left out is never merged, so an edit on it would be dropped unseen. Edits never make a newer
        // version, so this is a broken invariant rather than something the user did.
        if (!between.leftOut().isEmpty()) {
            throw new IOException("the edits cannot be put on top of " + String.join(", ", between.leftOut()));
        }
        Map<String, Side> answers = new HashMap<>();
        for (LineItem row : between.items()) {
            boolean edited = edits.stream().anyMatch(edit -> covers(edit, row));
            answers.put(row.id(), edited ? Side.LIVE : Side.WORKING_COPY);
        }
        return settle(settled, current, between, answers).bindings();
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
