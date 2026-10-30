package elite.intel.bindforge.devicefiles;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.Map;

/**
 * Writes a device's button and axis labels as a {@code .buttonMap}.
 * <p>
 * <strong>The whole file comes from the device's label set</strong> (Alan, 2026-10-04), so a label the set no
 * longer holds - after RESET LABELS, or for an axis the device stopped reporting - is gone from the file too. Labels
 * a user wrote by hand in one installation are gathered into the working copy at first setup, and the files as they
 * were are in first setup's backup.
 */
public final class ButtonMapWriter {

    private ButtonMapWriter() {
    }

    /**
     * @param labels label by {@code .binds} input token - {@code Joy_1}, {@code Joy_XAxis} - written in the
     *               map's own order
     * @throws IOException if a token cannot be an element tag
     */
    public static byte[] write(Map<String, String> labels) throws IOException {
        Document document = DeviceFileXml.newDocument();
        Element root = document.createElement("Root");
        document.appendChild(root);
        for (Map.Entry<String, String> label : labels.entrySet()) {
            Element element = DeviceFileXml.element(document, label.getKey());
            element.setTextContent(label.getValue());
            root.appendChild(element);
        }
        return DeviceFileXml.write(document);
    }
}
