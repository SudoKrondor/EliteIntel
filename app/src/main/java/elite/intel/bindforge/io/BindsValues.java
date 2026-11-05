package elite.intel.bindforge.io;

import elite.intel.bindforge.model.BindingInput;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

/**
 * What a binds file holds besides button slots: its axis elements ({@code <Binding>}, {@code <Inverted>},
 * {@code <Deadzone>}) and its bare settings ({@code <MouseSensitivity Value="1.0" />}). For the Bind Editor to show and
 * edit.
 * <p>
 * WHY apart from {@link KeyBindingsParser}: that parser feeds command execution and never widens. VEGA presses keys;
 * nothing read here can reach what it presses.
 * <p>
 * <strong>Real files are messier than the game's own output</strong> (bind-editor.md, <em>Real files are messier than any
 * specification</em>), so anything {@link BindingsWriter} would refuse reads as not editable rather than failing the
 * whole file: factory schemes hold {@code <Deadzone Value="DefaultPlatformDeadzone" />}, and old files repeat
 * elements such as {@code HeadlookSensitivity}.
 */
public record BindsValues(Map<String, Axis> axes, Map<String, Setting> settings) {

    public static final BindsValues NONE = new BindsValues(Map.of(), Map.of());

    /**
     * One axis element. An unbound one has {@link #input()} empty.
     *
     * @param device   the {@code Device} as written; {@code {NoDevice}} when unbound
     * @param key      the {@code Key} as written - usually {@code Joy_XAxis}, but factory files hold {@code GamePad_} and
     *                 {@code OculusTouch_} tokens, so nothing here assumes the shape
     * @param deadzone 0 to 1; meaningless unless {@code editable}
     * @param editable whether the writer can write this element
     */
    public record Axis(String id, String device, String key, boolean inverted, float deadzone, boolean editable) {

        public Optional<BindingInput> input() {
            return device == null || device.isBlank() || BindingInput.NO_DEVICE.equals(device)
                    || key == null || key.isBlank()
                    ? Optional.empty()
                    : Optional.of(new BindingInput(device, key));
        }

        public boolean bound() {
            return input().isPresent();
        }
    }

    /**
     * One bare setting. {@code value} is exactly what the file holds, an empty value being the field's default choice
     * rather than a missing one.
     *
     * @param editable whether the writer can write this element - false when the file repeats it
     */
    public record Setting(String id, String value, boolean editable) {
    }

    /**
     * @throws IOException if the bytes are not a binds file that can be parsed
     */
    public static BindsValues of(byte[] binds) throws IOException {
        Document document;
        try {
            document = SafeXml.documentBuilder().parse(new ByteArrayInputStream(binds));
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException("The binds file could not be parsed: " + e.getMessage(), e);
        }
        List<Element> elements = childElements(document.getDocumentElement());
        Set<String> seen = new HashSet<>();
        Set<String> repeated = new HashSet<>();
        for (Element element : elements) {
            if (!seen.add(element.getTagName())) repeated.add(element.getTagName());
        }
        Map<String, Axis> axes = new HashMap<>();
        Map<String, Setting> settings = new HashMap<>();
        for (Element element : elements) {
            String id = element.getTagName();
            boolean once = !repeated.contains(id);
            List<Element> children = childElements(element);
            if (!named(children, "Binding").isEmpty()) {
                axes.putIfAbsent(id, axis(id, children, once));
            } else if (children.isEmpty() && element.hasAttribute("Value")) {
                settings.putIfAbsent(id, new Setting(id, element.getAttribute("Value"),
                        once && element.getAttributes().getLength() == 1));
            }
        }
        return new BindsValues(Map.copyOf(axes), Map.copyOf(settings));
    }

    private static Axis axis(String id, List<Element> children, boolean once) {
        Element binding = named(children, "Binding").getFirst();
        List<Element> inverted = named(children, "Inverted");
        List<Element> deadzone = named(children, "Deadzone");
        Float zone = deadzone.size() == 1 ? number(deadzone.getFirst().getAttribute("Value")) : null;
        boolean editable = once && children.size() == 3
                && named(children, "Binding").size() == 1 && inverted.size() == 1
                && childElements(binding).isEmpty() && zone != null && zone >= 0f && zone <= 1f;
        return new Axis(id, binding.getAttribute("Device"), binding.getAttribute("Key"),
                !inverted.isEmpty() && "1".equals(inverted.getFirst().getAttribute("Value")),
                zone == null ? 0f : zone, editable);
    }

    private static Float number(String text) {
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<Element> named(List<Element> elements, String tag) {
        return elements.stream().filter(element -> tag.equals(element.getTagName())).toList();
    }

    private static List<Element> childElements(Node parent) {
        List<Element> elements = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element element) elements.add(element);
        }
        return elements;
    }
}
