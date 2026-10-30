package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.*;

/**
 * Changes the user's device elements in a {@code DeviceMappings.xml}: add, give other hardware, rename, remove, put
 * back, and first-time startup's assembly of the working copy's one file.
 * <p>
 * <strong>Change, never regenerate.</strong> Each method touches only the user's elements it is given, so everything
 * else in the file - Frontier's entries, comments, {@code <SupportsIcons>}, any child nobody has documented - is kept
 * exactly as it is. A user element already in the file keeps every child it has; at most its {@code <PID>} and
 * {@code <VID>} are brought into line with its entry. <em>The first attempt at writing this file emitted only
 * the tags it knew and silently dropped all five {@code <SupportsIcons>}.</em>
 * <p>
 * <strong>The user's elements go first.</strong> When two elements claim one VID/PID the game uses the first
 * in the file (measured 2026-10-03, domain doc §1.2d), so an entry of the user's written below Frontier's would
 * be silently ignored. Each one is placed among the user's elements at the top of {@code <Root>}, in the order the
 * caller gives.
 * <p>
 * Bytes in, bytes out: nothing here reads or writes a file. Since Storage S4a the edits on screen change the working
 * copy's file with it ({@code bindforge.storage.DeviceEdits}).
 */
public final class DeviceMappingsMerge {

    private static final String ROOT = "Root";
    private static final String PID = "PID";
    private static final String VID = "VID";

    private DeviceMappingsMerge() {
    }

    /**
     * One of the user's device elements: its tag and its own VID/PID.
     *
     * @param name the element tag, which {@code .binds} names the device by
     */
    public record UserEntry(String name, String vid, String pid) {
        public UserEntry {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(vid, "vid");
            Objects.requireNonNull(pid, "pid");
        }
    }

    /**
     * @param content the file to write - the input bytes themselves when nothing changed
     * @param changed whether any element was added, moved or given different hardware ids
     */
    public record Result(byte[] content, boolean changed) {
    }

    /**
     * One element given other hardware - the identity question's yes (A12; Storage S4b, 2026-10-28). The element keeps
     * its name, its place in the file and every other child; only its own {@code <VID>} and {@code <PID>} change.
     * Nothing else moves.
     *
     * @param name the element's tag, exactly as the file spells it
     * @throws IOException if {@code existing} is not a {@code DeviceMappings.xml}, or holds no element {@code name}
     */
    public static Result retarget(byte[] existing, String name, String vid, String pid) throws IOException {
        Document document = DeviceFileXml.parse(existing);
        Element element = firstChildNamed(rootOf(document), name);
        if (element == null) throw new IOException("no element " + name + " to give other hardware");
        if (!matchHardware(document, element, new UserEntry(name, vid, pid))) return new Result(existing, false);
        return new Result(DeviceFileXml.write(document), true);
    }

    /**
     * Adds new elements to the file and changes nothing else - onboarding's write (Alan, 2026-10-14, A6b).
     * <p>
     * An element already in the file is not brought into line or moved: onboarding names a
     * controller, and bringing some other device's {@code <PID>} into line would be a change nobody asked for. Each
     * new element goes among the user's elements that lead the file, at its place in {@code order}, and every element
     * already there is left as it is. An element whose tag is already in the file is left alone, and is not counted
     * as added.
     *
     * @param existing    the installation's current file, or Frontier's stock file when it has none
     * @param entries     the elements to add
     * @param order    every user's name leading the file, the new ones included, in the order they should lead it
     * @throws IOException if {@code existing} is not a {@code DeviceMappings.xml}, or a name cannot be an element tag
     */
    public static Result insert(byte[] existing, List<UserEntry> entries, List<String> order)
            throws IOException {
        Document document = DeviceFileXml.parse(existing);
        Element root = rootOf(document);

        boolean changed = false;
        for (UserEntry entry : entries) {
            if (firstChildNamed(root, entry.name()) != null) continue;
            int place = order.indexOf(entry.name());
            if (place < 0) {
                throw new IllegalArgumentException(entry.name() + " is not in the order it is placed by");
            }
            Element element = newEntry(document, entry);
            Node before = insertionPoint(root, order, place);
            root.insertBefore(element, before);
            changed = true;
        }
        return changed ? new Result(DeviceFileXml.write(document), true) : new Result(existing, false);
    }

    /**
     * Puts one element back, copied whole from {@code source} - its {@code <Alternative>} and {@code <SupportsIcons>}
     * with it, which {@link #insert} would lose - placed among the user's elements leading the file as {@link #insert}
     * places a new one. UNDO CLEAR's, putting back what the working copy holds (Storage S4a). An element already under
     * that tag is left alone.
     *
     * @param order every user's name leading the file, the one put back included, in the order they should lead it
     * @throws IOException if a file is not a {@code DeviceMappings.xml}, or {@code source} has no such element
     */
    public static Result copyIn(byte[] existing, byte[] source, String name, List<String> order) throws IOException {
        Document document = DeviceFileXml.parse(existing);
        Element root = rootOf(document);
        if (firstChildNamed(root, name) != null) return new Result(existing, false);
        Element original = firstChildNamed(rootOf(DeviceFileXml.parse(source)), name);
        if (original == null) throw new IOException(name + " is not in the file it was to be copied from");
        int place = order.indexOf(name);
        if (place < 0) throw new IllegalArgumentException(name + " is not in the order it is placed by");
        Element copy = (Element) document.importNode(original, true);
        root.insertBefore(copy, insertionPoint(root, order, place));
        return new Result(DeviceFileXml.write(document), true);
    }

    /**
     * Where an element at {@code place} in {@code order} goes: just after the last of the leading user elements that
     * comes before it, or at the very top when none does. The leading run is the root's first elements while they are
     * names in {@code order}.
     *
     * @return the node to insert before, or {@code null} to append
     */
    private static Node insertionPoint(Element root, List<String> order, int place) {
        Node after = null;
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            int index = order.indexOf(child.getNodeName());
            if (index < 0) break;
            if (index < place) after = child;
        }
        return after == null ? root.getFirstChild() : after.getNextSibling();
    }

    /**
     * Renames a device's element in place - a rename, applied (Alan, 2026-10-14, A11). The element keeps its place and
     * every child it has ({@code <Alternative>}, {@code <SupportsIcons>}, anything undocumented); only its tag changes.
     * A second copy under the old tag further down is removed, as CLEAR removes one: the old name has left the user's
     * set, and two elements under the new name would leave the second dead.
     * <p>
     * When the new tag is already in the file - a rename carried out by hand, or one this installation half-took
     * before - every old copy is removed instead, and the element already under the new name is kept.
     * <p>
     * <strong>The old tag is matched ignoring case</strong>, as the game finds an element (format §4.3, measured
     * 2026-10-14): a file hand-edited to {@code <lvwap>} is still the device whose old name is
     * {@code LVWAP}, and left alone it would keep leading the file for that VID/PID (review, 2026-10-15). The element
     * already spelled exactly as the new name is never an old copy - that is a case-only rename already taken.
     *
     * @param existing the installation's current file
     * @param oldName  the tag to rename, matched ignoring case
     * @param newName  the tag it becomes, matched exactly
     * @throws IOException if {@code existing} is not a {@code DeviceMappings.xml}, or {@code newName} cannot be an
     *                     element tag
     */
    public static Result rename(byte[] existing, String oldName, String newName) throws IOException {
        Document document = DeviceFileXml.parse(existing);
        Element root = rootOf(document);
        if (oldName.equals(newName)) return new Result(existing, false);
        List<Element> old = childrenNamedIgnoringCase(root, oldName).stream()
                .filter(element -> !element.getNodeName().equals(newName))
                .toList();
        if (old.isEmpty()) return new Result(existing, false);

        boolean newAlreadyThere = firstChildNamed(root, newName) != null;
        for (Element element : old) {
            if (newAlreadyThere) {
                root.removeChild(element);
                continue;
            }
            newAlreadyThere = true;
            Element renamed = DeviceFileXml.element(document, newName);
            while (element.getFirstChild() != null) {
                renamed.appendChild(element.getFirstChild());
            }
            for (int i = element.getAttributes().getLength() - 1; i >= 0; i--) {
                Node attribute = element.getAttributes().item(i);
                renamed.setAttribute(attribute.getNodeName(), attribute.getNodeValue());
            }
            root.replaceChild(renamed, element);
        }
        return new Result(DeviceFileXml.write(document), true);
    }

    private static List<Element> childrenNamedIgnoringCase(Element parent, String tag) {
        List<Element> found = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && tag.equalsIgnoreCase(child.getNodeName())) {
                found.add((Element) child);
            }
        }
        return found;
    }

    /**
     * Takes the named elements out of the file - CLEAR, applied (Alan, 2026-10-11, A10). Every element with one of
     * those tags goes, a second copy further down included: the name has left the user's set, so no copy of it is
     * kept.
     * <p>
     * The caller passes only the user's names. Frontier's elements are never removed - on a built-in, RESET LABELS
     * removes a {@code .buttonMap}, never the entry.
     *
     * @param existing the installation's current file
     * @param names    element tags to remove, matched exactly as the game matches them
     * @throws IOException if {@code existing} is not a {@code DeviceMappings.xml}
     */
    public static Result remove(byte[] existing, Collection<String> names) throws IOException {
        Document document = DeviceFileXml.parse(existing);
        Element root = rootOf(document);

        List<Node> gone = new ArrayList<>();
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && names.contains(child.getNodeName())) gone.add(child);
        }
        // WHY: untouched when nothing matched - the same bytes back, so nothing is written.
        if (gone.isEmpty()) return new Result(existing, false);
        gone.forEach(root::removeChild);
        return new Result(DeviceFileXml.write(document), true);
    }

    /**
     * One of the user's elements first-time startup takes into the working copy whole, from the file of the
     * installation whose version won (alias-designer.md, <em>How the reconciled files are made</em>; Storage S2b).
     *
     * @param source that installation's {@code DeviceMappings.xml}
     * @param name   the element's tag
     * @param vid    the VID the user chose; empty, with {@code pid}, for an element that claims no hardware - it is
     *               copied as it is
     * @param pid    the PID the user chose
     */
    public record Taken(byte[] source, String name, String vid, String pid) {
        public Taken {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(vid, "vid");
            Objects.requireNonNull(pid, "pid");
        }
    }

    /**
     * The working copy's one {@code DeviceMappings.xml}, made by first-time startup's merge: {@code base} - one
     * installation's file, since Frontier's entries are the same everywhere - with every one of the user's elements
     * taken out, and the kept ones put at the top of {@code <Root>} in the order given, each copied whole from its
     * own installation's file and given the hardware the user chose. Its {@code <Alternative>} and
     * {@code <SupportsIcons>} come with it, which a new element would lose.
     *
     * @param base       the file Frontier's entries, and everything else that is not the user's, are kept from
     * @param usersNames every element tag that is the user's, in any installation - all taken out of {@code base}
     * @param taken      the elements kept, in the order they lead the file
     * @throws IOException if a file is not a {@code DeviceMappings.xml}, or a taken element is not in its source
     */
    public static byte[] assemble(byte[] base, Collection<String> usersNames, List<Taken> taken) throws IOException {
        Document document = DeviceFileXml.parse(base);
        Element root = rootOf(document);

        List<Node> gone = new ArrayList<>();
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && usersNames.contains(child.getNodeName())) gone.add(child);
        }
        gone.forEach(root::removeChild);

        Map<byte[], Element> sourceRoots = new IdentityHashMap<>();
        for (int i = taken.size() - 1; i >= 0; i--) {
            Taken one = taken.get(i);
            Element sourceRoot = sourceRoots.get(one.source());
            if (sourceRoot == null) {
                sourceRoot = rootOf(DeviceFileXml.parse(one.source()));
                sourceRoots.put(one.source(), sourceRoot);
            }
            Element original = firstChildNamed(sourceRoot, one.name());
            if (original == null) throw new IOException(one.name() + " is not in the file it was to be taken from");
            Element copy = (Element) document.importNode(original, true);
            if (!one.vid().isEmpty() || !one.pid().isEmpty()) {
                matchHardware(document, copy, new UserEntry(one.name(), one.vid(), one.pid()));
            }
            root.insertBefore(copy, root.getFirstChild());
        }
        return DeviceFileXml.write(document);
    }

    /**
     * @throws IOException if the document is not a {@code DeviceMappings.xml} - its root is not {@code <Root>}
     */
    private static Element rootOf(Document document) throws IOException {
        Element root = document.getDocumentElement();
        if (root == null || !ROOT.equals(root.getTagName())) {
            throw new IOException("Not a DeviceMappings.xml: its root element is <"
                    + (root == null ? "" : root.getTagName()) + ">, not <" + ROOT + ">");
        }
        return root;
    }

    private static Element newEntry(Document document, UserEntry entry) throws IOException {
        Element element = DeviceFileXml.element(document, entry.name());
        element.appendChild(textElement(document, PID, entry.pid()));
        element.appendChild(textElement(document, VID, entry.vid()));
        return element;
    }

    /**
     * Brings an existing element's own {@code <PID>} and {@code <VID>} into line with its entry, leaving every
     * other child - {@code <Alternative>} blocks included - alone.
     * <p>
     * A value equal to the entry's ignoring case is left as written: Frontier's file has no case convention,
     * and rewriting {@code 05c4} as {@code 05C4} would be a change with no meaning.
     *
     * @return whether anything changed
     */
    private static boolean matchHardware(Document document, Element element, UserEntry entry) {
        HardwareId wanted = new HardwareId(entry.vid(), entry.pid());
        boolean changed = setChildText(document, element, PID, entry.pid(), wanted.pid());
        changed |= setChildText(document, element, VID, entry.vid(), wanted.vid());
        return changed;
    }

    private static boolean setChildText(Document document, Element parent, String tag, String value,
                                        String normalised) {
        Element child = firstChildNamed(parent, tag);
        if (child == null) {
            parent.appendChild(textElement(document, tag, value));
            return true;
        }
        if (child.getTextContent().trim().toUpperCase(Locale.ROOT).equals(normalised)) return false;
        child.setTextContent(value);
        return true;
    }

    private static Element textElement(Document document, String tag, String text) {
        Element element = document.createElement(tag);
        element.setTextContent(text);
        return element;
    }

    /**
     * The first child element with exactly this tag. Matched exactly because the tag is what the game reads
     * and XML tags are case-sensitive. A second element with the same tag is not looked at: it is not the one the
     * game resolves.
     */
    private static Element firstChildNamed(Element parent, String tag) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && tag.equals(child.getNodeName())) {
                return (Element) child;
            }
        }
        return null;
    }
}
