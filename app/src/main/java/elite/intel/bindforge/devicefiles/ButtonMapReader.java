package elite.intel.bindforge.devicefiles;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads a {@code .buttonMap}'s labels - the counterpart of {@link ButtonMapWriter}.
 * <p>
 * The file is a flat list under {@code <Root>}: each element is named for a {@code .binds} input token and holds
 * the label as its text, {@code <Joy_1>LV MAIN TRIGGER</Joy_1>}.
 */
public final class ButtonMapReader {

    private ButtonMapReader() {
    }

    /**
     * @return label by input token, in the file's order. A token named twice keeps its first label.
     * @throws IOException if the bytes are not well-formed XML
     */
    public static Map<String, String> read(byte[] buttonMap) throws IOException {
        Map<String, String> labels = new LinkedHashMap<>();
        Element root = DeviceFileXml.parse(buttonMap).getDocumentElement();
        if (root == null) return labels;

        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element element)) continue;
            // WHY: trimmed, because a hand-edited file may put the label on its own indented line. The game
            // shows the label, not the whitespace around it, so that is not a different label.
            labels.putIfAbsent(element.getTagName(), element.getTextContent().trim());
        }
        return labels;
    }
}
