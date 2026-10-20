package elite.intel.bindforge.preseteditor;

import elite.intel.bindforge.io.StartPresetSections;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Whether a file with a {@code .binds} name is a binds file the game can load, judged by what is in it.
 * <p>
 * <strong>A {@code .binds} name does not make it a binds file</strong> (bind-editor.md, 2026-09-17): 445 of 48,354
 * shared files were something else, 363 of them a preset file picked by mistake. A binds file is a
 * {@code <Root>} element with at least one binding in it - a child holding a {@code <Primary>},
 * {@code <Secondary>} or {@code <Binding>}. Settings alone, such as {@code <KeyboardLayout>}, do not count.
 * <p>
 * <strong>The BindName is the file's {@code PresetName}</strong>, which is what {@code StartPreset.4.start} stores,
 * and it must match the BindName the file name gives (Alan, 2026-10-17). A file where the two disagree is not offered: the
 * dropdown would show one name for a file called another.
 * <p>
 * This is not {@code KeyBindingsParser}, which stays keyboard-only and must not be widened to answer this.
 */
final class BindsContent {

    private static final String ROOT = "Root";
    private static final String PRESET_NAME = "PresetName";
    private static final Set<String> BINDING_SLOTS = Set.of("Primary", "Secondary", "Binding");
    private static final int MAX_PRESET_FILE_LINES = 4;

    private BindsContent() {
    }

    /** What a file turned out to be. */
    sealed interface Judgement permits Offered, NotOffered {
    }

    /** A binds file, offered under {@code bindName}. */
    record Offered(String bindName) implements Judgement {
    }

    /** Not offered, and why. {@code detail} is what the file holds instead, for the sentence; never null. */
    record NotOffered(NotOfferedReason reason, String detail) implements Judgement {
    }

    /**
     * Reads and judges one file. A file that cannot be read is {@link NotOfferedReason#UNREADABLE}, not an
     * exception: one bad file must not hide the rest of the list.
     */
    static Judgement judge(Path file) {
        byte[] content;
        try {
            content = Files.readAllBytes(file);
        } catch (IOException e) {
            return new NotOffered(NotOfferedReason.UNREADABLE, String.valueOf(e.getMessage()));
        }
        return judge(file.getFileName().toString(), content);
    }

    static Judgement judge(String fileName, byte[] content) {
        String text = StartPresetSections.withoutByteOrderMark(new String(content, StandardCharsets.UTF_8)).strip();
        if (text.isEmpty()) {
            return new NotOffered(NotOfferedReason.EMPTY, "");
        }
        if (!text.startsWith("<")) {
            return judgeText(text);
        }

        Document document;
        try {
            document = newDocumentBuilder().parse(new ByteArrayInputStream(content));
        } catch (SAXException | IOException e) {
            return new NotOffered(NotOfferedReason.NOT_XML, String.valueOf(e.getMessage()));
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("The platform XML parser cannot be configured", e);
        }

        Element root = document.getDocumentElement();
        if (!ROOT.equals(root.getTagName())) {
            return new NotOffered(NotOfferedReason.OTHER_XML, root.getTagName());
        }
        if (!holdsABinding(root)) {
            return new NotOffered(NotOfferedReason.ROOT_WITHOUT_BINDINGS, "");
        }
        String bindName = root.getAttribute(PRESET_NAME).strip();
        if (bindName.isEmpty()) {
            return new NotOffered(NotOfferedReason.NO_PRESET_NAME, "");
        }
        if (!bindName.equals(BindName.fromFileName(fileName))) {
            return new NotOffered(NotOfferedReason.NAME_MISMATCH, bindName);
        }
        return new Offered(bindName);
    }

    /**
     * Text that is not XML: a preset file when it is what {@code StartPreset.4.start} holds - one to four lines,
     * none of them markup - and otherwise just not XML. The detail of a preset file is its BindNames, one per line.
     */
    private static Judgement judgeText(String text) {
        List<String> lines = text.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
        boolean presetFile = lines.size() <= MAX_PRESET_FILE_LINES
                && lines.stream().noneMatch(line -> line.contains("<") || line.contains(">"));
        if (presetFile) {
            return new NotOffered(NotOfferedReason.PRESET_FILE, String.join("\n", lines));
        }
        return new NotOffered(NotOfferedReason.NOT_XML, "");
    }

    private static boolean holdsABinding(Element root) {
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            for (Node slot = child.getFirstChild(); slot != null; slot = slot.getNextSibling()) {
                if (slot.getNodeType() == Node.ELEMENT_NODE && BINDING_SLOTS.contains(slot.getNodeName())) {
                    return true;
                }
            }
        }
        return false;
    }

    // WHY: the same settings as DeviceFileXml - external entities and DOCTYPEs off. Every file in the folder is
    // read, including ones that are not ours, and a .binds can be something downloaded from a forum.
    private static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        // WHY: the default handler prints "[Fatal Error]" to stderr for every file that is not XML, and here
        // that is an expected answer, carried in the judgement.
        builder.setErrorHandler(new DefaultHandler());
        return builder;
    }
}
