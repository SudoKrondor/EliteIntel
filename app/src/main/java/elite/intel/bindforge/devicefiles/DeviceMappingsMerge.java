package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.*;

/**
 * Merges the user's device elements into one installation's {@code DeviceMappings.xml}.
 * <p>
 * <strong>Merge, never regenerate.</strong> The master holds only the user's elements, so everything else in
 * the file - Frontier's entries, comments, {@code <SupportsIcons>}, any child nobody has documented - is kept
 * exactly as it is. A user element already in the file keeps every child it has; only its {@code <PID>} and
 * {@code <VID>} are brought into line with the master. <em>The first attempt at writing this file emitted only
 * the tags it knew and silently dropped all five {@code <SupportsIcons>}.</em>
 * <p>
 * <strong>The user's elements go first.</strong> When two elements claim one VID/PID the game uses the first
 * in the file (measured 2026-10-03, domain doc §1.2d), so an entry of the user's written below Frontier's would
 * be silently ignored. Each one is placed at the top of {@code <Root>}, in master order, moving it up if it is
 * already further down.
 * <p>
 * Bytes in, bytes out: reading and writing the files is {@link DeviceFilesPush}'s job.
 */
public final class DeviceMappingsMerge {

    private static final String ROOT = "Root";
    private static final String PID = "PID";
    private static final String VID = "VID";

    private DeviceMappingsMerge() {
    }

    /**
     * One of the user's device elements, as the master holds it.
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
     * @param existing the installation's current file, or Frontier's stock file when it has none
     * @param entries  the user's elements, in the order they should lead the file
     * @throws IOException if {@code existing} is not a {@code DeviceMappings.xml}, or a name cannot be an
     *                     element tag
     */
    public static Result merge(byte[] existing, List<UserEntry> entries) throws IOException {
        Document document = DeviceFileXml.parse(existing);
        Element root = rootOf(document);

        boolean changed = false;
        List<Element> leading = new ArrayList<>();
        for (UserEntry entry : entries) {
            Element element = firstChildNamed(root, entry.name());
            if (element == null) {
                element = newEntry(document, entry);
                changed = true;
            } else {
                changed |= matchHardware(document, element, entry);
            }
            leading.add(element);
        }

        // WHY: a file that already says what the master says is returned untouched rather than re-laid-out.
        // Rewriting it would cost a write, an Edit History entry and a changed timestamp for no difference
        // the game could see.
        if (!changed && alreadyLeads(root, leading)) return new Result(existing, false);

        for (int i = leading.size() - 1; i >= 0; i--) {
            root.insertBefore(leading.get(i), root.getFirstChild());
        }
        return new Result(DeviceFileXml.write(document), true);
    }

    /**
     * Adds new elements to the file and changes nothing else - onboarding's write (Alan, 2026-10-14, A6b).
     * <p>
     * Unlike {@link #merge}, an element already in the file is not brought into line or moved: onboarding names a
     * controller, and bringing some other device's {@code <PID>} back to the master's would be a revert nobody asked
     * for. Each new element goes where the push would put it - among the user's elements that lead the file, at its
     * place in {@code masterOrder} - so a file that matched the master before still matches it after. An element
     * whose tag is already in the file is left alone, and is not counted as added.
     *
     * @param existing    the installation's current file, or Frontier's stock file when it has none
     * @param entries     the elements to add
     * @param masterOrder every name in the master, new ones included, in the order the push leads the file with
     * @throws IOException if {@code existing} is not a {@code DeviceMappings.xml}, or a name cannot be an element tag
     */
    public static Result insert(byte[] existing, List<UserEntry> entries, List<String> masterOrder)
            throws IOException {
        Document document = DeviceFileXml.parse(existing);
        Element root = rootOf(document);

        boolean changed = false;
        for (UserEntry entry : entries) {
            if (firstChildNamed(root, entry.name()) != null) continue;
            int place = masterOrder.indexOf(entry.name());
            if (place < 0) {
                throw new IllegalArgumentException(entry.name() + " is not in the master order it is placed by");
            }
            Element element = newEntry(document, entry);
            Node before = insertionPoint(root, masterOrder, place);
            root.insertBefore(element, before);
            changed = true;
        }
        return changed ? new Result(DeviceFileXml.write(document), true) : new Result(existing, false);
    }

    /**
     * Where an element at {@code place} in the master order goes: just after the last of the leading user elements
     * that comes before it, or at the very top when none does. The leading run is the root's first elements while
     * they are names the master holds - the run the push and the check both read.
     *
     * @return the node to insert before, or {@code null} to append
     */
    private static Node insertionPoint(Element root, List<String> masterOrder, int place) {
        Node after = null;
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            int index = masterOrder.indexOf(child.getNodeName());
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
     * 2026-10-14): an installation adopted or hand-edited as {@code <lvwap>} is still the device whose old name is
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
     * the master's to keep.
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
        // WHY: untouched when nothing matched, as merge does - no write, no Edit History entry, no new timestamp.
        if (gone.isEmpty()) return new Result(existing, false);
        gone.forEach(root::removeChild);
        return new Result(DeviceFileXml.write(document), true);
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
     * Brings an existing element's own {@code <PID>} and {@code <VID>} into line with the master, leaving every
     * other child - {@code <Alternative>} blocks included - alone.
     * <p>
     * A value equal to the master's ignoring case is left as written: Frontier's file has no case convention,
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

    /** Whether {@code leading} are already the root's first elements, in that order. */
    private static boolean alreadyLeads(Element root, List<Element> leading) {
        int index = 0;
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength() && index < leading.size(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            if (child != leading.get(index)) return false;
            index++;
        }
        return index == leading.size();
    }

    /**
     * The first child element with exactly this tag. Matched exactly because the tag is what the game reads
     * and XML tags are case-sensitive. A second element with the same tag is left where it is: it is not the
     * one the game resolves, and it is not the master's to remove.
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
