package elite.intel.bindforge.storage;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * One binds file's top-level elements, each with its value and where its text is - what the check compares by value
 * and what a merge splices from (storage.md rule 14; Storage S8a).
 * <p>
 * <strong>Values, never bytes</strong> (overview.md, <em>Freshness Checks</em>): an element's value is its name, its
 * attributes in name order, and its children's values, with whitespace between them ignored - so a line ending, an
 * indent or a byte-order mark is not a change. The values come from a parsed document; the places in the text from a
 * scan of the same text, matched element by element, so a merge replaces exactly the element that differed and every
 * other byte of the file stays as it was.
 * <p>
 * A file this cannot read - not XML, a DOCTYPE, two top-level elements of one name, or a scan that does not match the
 * parse - is {@link Optional#empty()}: the check then treats it as one whole file. Pure: nothing is read from disk.
 */
final class BindsElements {

    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final String PRESET_NAME = "PresetName";

    /** A stretch of the text, {@code start} inclusive, {@code end} exclusive. */
    record Range(int start, int end) {
    }

    /**
     * One child of a top-level element - a {@code <Primary>}, {@code <Secondary>}, {@code <ToggleOn>}, ...
     *
     * @param value its value, for comparing
     */
    record Child(String name, Range range, String value, Element node) {
    }

    /**
     * One top-level element.
     *
     * @param children its child elements in order; empty when two share a name, so it is compared and merged whole
     */
    record Top(String name, Range range, String value, Element node, List<Child> children) {

        /** Whether it is compared slot by slot: a button, with a {@code <Primary>} or {@code <Secondary>}. */
        boolean bySlot() {
            return children.stream()
                    .anyMatch(child -> child.name().equals("Primary") || child.name().equals("Secondary"));
        }

        /** The child of that name, if it has one. */
        Optional<Child> child(String name) {
            return children.stream().filter(child -> child.name().equals(name)).findFirst();
        }
    }

    private final String text;
    private final boolean bom;
    private final Range rootStartTag;
    private final int rootEndTag;
    private final Map<String, String> rootAttributes;
    private final Map<String, Top> elements;

    private BindsElements(String text, boolean bom, Range rootStartTag, int rootEndTag,
                          Map<String, String> rootAttributes, Map<String, Top> elements) {
        this.text = text;
        this.bom = bom;
        this.rootStartTag = rootStartTag;
        this.rootEndTag = rootEndTag;
        this.rootAttributes = rootAttributes;
        this.elements = elements;
    }

    /** Reads one binds file's bytes; empty when it cannot be read element by element. */
    static Optional<BindsElements> of(byte[] content) {
        boolean bom = startsWithBom(content);
        int start = bom ? BOM.length : 0;
        String text = new String(content, start, content.length - start, StandardCharsets.UTF_8);
        Document document;
        Scanned scanned;
        try {
            document = documentBuilder().parse(new ByteArrayInputStream(content));
            scanned = Scanned.scan(text);
        } catch (SAXException | IOException | IllegalArgumentException | ParserConfigurationException e) {
            return Optional.empty();
        }
        Element root = document.getDocumentElement();
        List<Element> tops = childElements(root);
        if (!root.getTagName().equals(scanned.name) || tops.size() != scanned.children.size()) return Optional.empty();
        Map<String, Top> elements = new LinkedHashMap<>();
        for (int i = 0; i < tops.size(); i++) {
            Element element = tops.get(i);
            Scanned node = scanned.children.get(i);
            if (!element.getTagName().equals(node.name) || elements.containsKey(node.name)) return Optional.empty();
            Optional<List<Child>> children = children(element, node);
            if (children.isEmpty()) return Optional.empty();
            elements.put(node.name, new Top(node.name, node.range(), value(element), element, children.get()));
        }
        int rootEndTag = text.lastIndexOf("</", scanned.end);
        if (rootEndTag < scanned.startTagEnd) rootEndTag = -1;
        return Optional.of(new BindsElements(text, bom, new Range(scanned.start, scanned.startTagEnd), rootEndTag,
                attributes(root), Collections.unmodifiableMap(elements)));
    }

    /**
     * The element's children matched to the scan; an empty list when two children share a name, so the element is
     * taken whole. Empty when the scan and the parse disagree.
     */
    private static Optional<List<Child>> children(Element element, Scanned node) {
        List<Element> parsed = childElements(element);
        if (parsed.size() != node.children.size()) return Optional.empty();
        List<Child> children = new ArrayList<>();
        Set<String> names = new HashSet<>();
        boolean repeated = false;
        for (int i = 0; i < parsed.size(); i++) {
            Element child = parsed.get(i);
            Scanned scanned = node.children.get(i);
            if (!child.getTagName().equals(scanned.name)) return Optional.empty();
            if (!names.add(scanned.name)) repeated = true;
            children.add(new Child(scanned.name, scanned.range(), value(child), child));
        }
        return Optional.of(repeated ? List.of() : List.copyOf(children));
    }

    /** The top-level elements, by name, in the file's order. */
    Map<String, Top> elements() {
        return elements;
    }

    /** The root's attributes, by name - {@code PresetName}, {@code MajorVersion}, {@code MinorVersion}. */
    Map<String, String> rootAttributes() {
        return rootAttributes;
    }

    /** Whether {@code other} holds the same values: the root's attributes and every element, in any order. */
    boolean sameValues(BindsElements other) {
        return rootAttributes.equals(other.rootAttributes) && sameElements(other);
    }

    /** Whether {@code other} holds the same values but for the root's {@code PresetName} - how a reset is told. */
    boolean sameValuesButName(BindsElements other) {
        Map<String, String> mine = new TreeMap<>(rootAttributes);
        Map<String, String> theirs = new TreeMap<>(other.rootAttributes);
        mine.remove(PRESET_NAME);
        theirs.remove(PRESET_NAME);
        return mine.equals(theirs) && sameElements(other);
    }

    private boolean sameElements(BindsElements other) {
        if (!elements.keySet().equals(other.elements.keySet())) return false;
        for (Top element : elements.values()) {
            if (!element.value().equals(other.elements.get(element.name()).value())) return false;
        }
        return true;
    }

    /** The text of a stretch of the file. */
    String text(Range range) {
        return text.substring(range.start(), range.end());
    }

    /** The root's start tag, for a merge that takes the other side's file details. */
    Range rootStartTag() {
        return rootStartTag;
    }

    /** Where a new element goes: the start of the line holding {@code </Root>}. */
    int insertionPoint() {
        if (rootEndTag < 0) throw new IllegalStateException("a root with no end tag holds no element to insert before");
        int lineStart = text.lastIndexOf('\n', rootEndTag - 1);
        return lineStart < 0 ? rootEndTag : lineStart + 1;
    }

    /** The file's line ending - CRLF if it has one anywhere. */
    String lineEnding() {
        return text.contains("\r\n") ? "\r\n" : "\n";
    }

    /** The whitespace at the start of the line {@code position} is on. */
    String indentOf(int position) {
        int lineStart = text.lastIndexOf('\n', position - 1) + 1;
        int end = lineStart;
        while (end < position && (text.charAt(end) == ' ' || text.charAt(end) == '\t')) end++;
        return text.substring(lineStart, end);
    }

    /** {@code range} widened to its whole line - the indent before it, the line ending after - for a removal. */
    Range wholeLines(Range range) {
        int start = range.start();
        while (start > 0 && (text.charAt(start - 1) == ' ' || text.charAt(start - 1) == '\t')) start--;
        int end = range.end();
        if (text.startsWith("\r\n", end)) end += 2;
        else if (text.startsWith("\n", end)) end += 1;
        return new Range(start, end);
    }

    /** One change a merge makes to the text. */
    record Splice(Range range, String replacement) {
    }

    /** The file with every splice made, encoded as it was read - its byte-order mark kept. */
    byte[] with(List<Splice> splices) {
        List<Splice> fromTheEnd = new ArrayList<>(splices);
        // WHY: from the end backwards, so an earlier range is not moved by a later change.
        fromTheEnd.sort((a, b) -> Integer.compare(b.range().start(), a.range().start()));
        StringBuilder changed = new StringBuilder(text);
        for (Splice splice : fromTheEnd) {
            changed.replace(splice.range().start(), splice.range().end(), splice.replacement());
        }
        byte[] encoded = changed.toString().getBytes(StandardCharsets.UTF_8);
        if (!bom) return encoded;
        byte[] content = new byte[BOM.length + encoded.length];
        System.arraycopy(BOM, 0, content, 0, BOM.length);
        System.arraycopy(encoded, 0, content, BOM.length, encoded.length);
        return content;
    }

    // ---- values

    /** An element's value: name, attributes in name order, then each child's value or text, whitespace ignored. */
    static String value(Element element) {
        StringBuilder value = new StringBuilder("<").append(element.getTagName());
        attributes(element).forEach((name, attribute) -> value.append(' ').append(name).append("=\"")
                .append(attribute).append('"'));
        value.append('>');
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            org.w3c.dom.Node node = nodes.item(i);
            if (node instanceof Element child) {
                value.append(value(child));
            } else if (node.getNodeType() == org.w3c.dom.Node.TEXT_NODE
                    || node.getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE) {
                String text = node.getNodeValue().strip();
                if (!text.isEmpty()) value.append('"').append(text).append('"');
            }
        }
        return value.append("</>").toString();
    }

    private static Map<String, String> attributes(Element element) {
        Map<String, String> attributes = new TreeMap<>();
        NamedNodeMap map = element.getAttributes();
        for (int i = 0; i < map.getLength(); i++) {
            attributes.put(map.item(i).getNodeName(), map.item(i).getNodeValue());
        }
        return Collections.unmodifiableMap(attributes);
    }

    private static List<Element> childElements(Element parent) {
        List<Element> children = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element child) children.add(child);
        }
        return children;
    }

    private static boolean startsWithBom(byte[] content) {
        return content.length >= BOM.length && content[0] == BOM[0] && content[1] == BOM[1] && content[2] == BOM[2];
    }

    /**
     * A builder that refuses a DOCTYPE and limits entity expansion, as {@code KeyBindingsParser} does for the same
     * bytes. A binds file never has a DOCTYPE. Its own error handler, so a bad file is not printed to the console.
     */
    private static DocumentBuilder documentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler() {
            @Override
            public void error(org.xml.sax.SAXParseException e) throws SAXException {
                throw e;
            }
        });
        return builder;
    }

    // ---- where each element is in the text

    /**
     * An element found by scanning the text: its name, where it starts and ends, and its child elements. Comments,
     * processing instructions and CDATA are skipped; text is not recorded.
     */
    private static final class Scanned {
        final String name;
        final int start;
        int startTagEnd;
        int end;
        final List<Scanned> children = new ArrayList<>();

        Scanned(String name, int start) {
            this.name = name;
            this.start = start;
        }

        Range range() {
            return new Range(start, end);
        }

        /** The text's root element, its children found. */
        static Scanned scan(String text) {
            Deque<Scanned> open = new ArrayDeque<>();
            Scanned root = null;
            int i = 0;
            while ((i = text.indexOf('<', i)) >= 0) {
                if (text.startsWith("<!--", i)) {
                    i = after(text, "-->", i + 4);
                } else if (text.startsWith("<![CDATA[", i)) {
                    i = after(text, "]]>", i + 9);
                } else if (text.startsWith("<?", i) || text.startsWith("<!", i)) {
                    i = after(text, ">", i + 2);
                } else if (text.startsWith("</", i)) {
                    int close = after(text, ">", i + 2);
                    String name = text.substring(i + 2, close - 1).strip();
                    if (open.isEmpty() || !open.peek().name.equals(name)) {
                        throw new IllegalArgumentException("unmatched end tag " + name);
                    }
                    open.pop().end = close;
                    i = close;
                } else {
                    int close = startTagEnd(text, i);
                    Scanned node = new Scanned(nameAt(text, i + 1), i);
                    node.startTagEnd = close;
                    if (!open.isEmpty()) {
                        open.peek().children.add(node);
                    } else if (root == null) {
                        root = node;
                    } else {
                        throw new IllegalArgumentException("a second root element");
                    }
                    if (text.charAt(close - 2) == '/') node.end = close;
                    else open.push(node);
                    i = close;
                }
            }
            if (root == null || !open.isEmpty()) throw new IllegalArgumentException("no complete root element");
            return root;
        }

        private static int after(String text, String end, int from) {
            int found = text.indexOf(end, from);
            if (found < 0) throw new IllegalArgumentException("unclosed markup");
            return found + end.length();
        }

        /** Just past a start tag's {@code >}, quotes respected. */
        private static int startTagEnd(String text, int from) {
            char quote = 0;
            for (int i = from + 1; i < text.length(); i++) {
                char c = text.charAt(i);
                if (quote != 0) {
                    if (c == quote) quote = 0;
                } else if (c == '"' || c == '\'') {
                    quote = c;
                } else if (c == '>') {
                    return i + 1;
                }
            }
            throw new IllegalArgumentException("unclosed start tag");
        }

        private static String nameAt(String text, int from) {
            int end = from;
            while (end < text.length()) {
                char c = text.charAt(end);
                if (Character.isWhitespace(c) || c == '/' || c == '>') break;
                end++;
            }
            if (end == from) throw new IllegalArgumentException("a tag with no name");
            return text.substring(from, end);
        }
    }
}
