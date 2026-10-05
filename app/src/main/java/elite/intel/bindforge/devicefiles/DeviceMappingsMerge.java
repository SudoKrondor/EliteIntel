package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
        Element root = document.getDocumentElement();
        if (root == null || !ROOT.equals(root.getTagName())) {
            throw new IOException("Not a DeviceMappings.xml: its root element is <"
                    + (root == null ? "" : root.getTagName()) + ">, not <" + ROOT + ">");
        }

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
