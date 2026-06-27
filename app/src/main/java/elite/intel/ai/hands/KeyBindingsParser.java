package elite.intel.ai.hands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A singleton class responsible for parsing key bindings from XML configuration files.
 * This class reads mappings between actions and keyboard inputs, including associated
 * keys, modifiers, and hold states.
 */
public class KeyBindingsParser {
    private static final Logger log = LogManager.getLogger(KeyBindingsParser.class);
    private static KeyBindingsParser instance;

    private KeyBindingsParser() {
        // Private constructor for singleton
    }

    public static synchronized KeyBindingsParser getInstance() {
        if (instance == null) {
            instance = new KeyBindingsParser();
        }
        return instance;
    }

    /**
     * Executable binding used by command handling.
     * <p>
     * This model intentionally contains only keyboard bindings that EliteIntel can press.
     * Non-keyboard devices are represented only in the read-only diagnostic models below.
     */
    public class KeyBinding {
        public String key;
        public String[] modifiers;
        public boolean hold;

        public KeyBinding(String key, String[] modifiers, boolean hold) {
            this.key = key;
            this.modifiers = modifiers;
            this.hold = hold;
        }
    }

    /**
     * Executable Primary/Secondary slots after non-keyboard assignments have been filtered out.
     */
    public record BindingSlots(KeyBinding primary, KeyBinding secondary) {
    }

    /**
     * Slot position as it appears in the Elite Dangerous binds file.
     */
    public enum BindingSlotType {
        PRIMARY,
        SECONDARY
    }

    /**
     * Read-only representation of one Primary or Secondary slot from a .binds file.
     * <p>
     * It keeps the raw device id and key for UI diagnostics, while {@code keyboardUsable}
     * records whether the slot is eligible for the existing keyboard-only execution path.
     * {@code editable} is narrower: it is true only for slots the basic V1 GUI can safely rewrite.
     */
    public record ReadOnlyBindingSlot(
            String device,
            String key,
            String[] modifiers,
            List<BindingModifier> bindingModifiers,
            boolean hold,
            BindingSlotType slotType,
            boolean keyboardUsable,
            boolean editable
    ) {
        public ReadOnlyBindingSlot {
            modifiers = modifiers == null ? new String[0] : modifiers.clone();
            bindingModifiers = bindingModifiers == null ? List.of() : List.copyOf(bindingModifiers);
        }

        @Override
        public String[] modifiers() {
            return modifiers.clone();
        }
    }

    /**
     * Read-only Primary/Secondary pair for the Bindings tab.
     * <p>
     * Unlike {@link BindingSlots}, this pair may contain HOTAS, joystick, mouse, or gamepad
     * assignments so the UI can show what exists in the game file without making it executable.
     */
    public record ReadOnlyBindingSlots(ReadOnlyBindingSlot primary, ReadOnlyBindingSlot secondary) {
    }

    /**
     * Parses the effective binding map used by command execution.
     * <p>
     * When both slots are present, the primary keyboard slot wins. Non-keyboard slots are excluded
     * before this map is built, preserving the historical keyboard-only behavior.
     */
    public Map<String, KeyBinding> parseBindings(File file) throws Exception {
        Map<String, BindingSlots> bindingSlots = parseBindingSlots(file);
        Map<String, KeyBinding> bindings = new HashMap<>();
        for (Map.Entry<String, BindingSlots> entry : bindingSlots.entrySet()) {
            KeyBinding keyBinding = entry.getValue().primary() != null
                    ? entry.getValue().primary()
                    : entry.getValue().secondary();
            if (keyBinding != null) {
                bindings.put(entry.getKey(), keyBinding);
            }
        }
        log.info("Parsed {} bindings from file: {}", bindings.size(), file.getName());
        return bindings;
    }

    /**
     * Parses only keyboard-usable binding slots for command execution and missing-binding checks.
     * <p>
     * This method preserves the existing executable behavior: HOTAS, joystick, mouse, and gamepad
     * assignments are ignored here even though {@link #parseReadOnlyBindingSlots(File)} can display them.
     */
    public Map<String, BindingSlots> parseBindingSlots(File file) throws Exception {
        Map<String, ReadOnlyBindingSlots> readOnlySlots = parseReadOnlyBindingSlots(file);
        Map<String, BindingSlots> bindings = new HashMap<>();
        for (Map.Entry<String, ReadOnlyBindingSlots> entry : readOnlySlots.entrySet()) {
            KeyBinding primaryBinding = toExecutableBinding(entry.getValue().primary());
            KeyBinding secondaryBinding = toExecutableBinding(entry.getValue().secondary());
            if (primaryBinding != null || secondaryBinding != null) {
                bindings.put(entry.getKey(), new BindingSlots(primaryBinding, secondaryBinding));
            }
        }
        log.info("Parsed {} binding slots from file: {}", bindings.size(), file.getName());
        return bindings;
    }

    /**
     * Parses Primary and Secondary slots exactly enough for the read-only Bindings tab.
     * <p>
     * The returned model includes raw device ids such as {@code 044F0422}. These values are diagnostic
     * only; callers must use {@link ReadOnlyBindingSlot#keyboardUsable()} before treating a slot as
     * executable.
     */
    public Map<String, ReadOnlyBindingSlots> parseReadOnlyBindingSlots(File file) throws Exception {
        Map<String, ReadOnlyBindingSlots> bindings = new HashMap<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);
        doc.getDocumentElement().normalize();

        NodeList nodes = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element) {
                Element element = (Element) nodes.item(i);
                String actionName = element.getTagName();

                NodeList primaryList = element.getElementsByTagName("Primary");
                NodeList secondaryList = element.getElementsByTagName("Secondary");

                ReadOnlyBindingSlot primaryBinding = null;
                ReadOnlyBindingSlot secondaryBinding = null;

                if (primaryList.getLength() > 0) {
                    Element primary = (Element) primaryList.item(0);
                    primaryBinding = readOnlySlot(primary, BindingSlotType.PRIMARY);
                    log.debug("Parsed primary binding for {}: device={}, key={}, hold={}",
                            actionName, primaryBinding.device(), primaryBinding.key(), primaryBinding.hold());
                }

                if (secondaryList.getLength() > 0) {
                    Element secondary = (Element) secondaryList.item(0);
                    secondaryBinding = readOnlySlot(secondary, BindingSlotType.SECONDARY);
                    log.debug("Parsed secondary binding for {}: device={}, key={}, hold={}",
                            actionName, secondaryBinding.device(), secondaryBinding.key(), secondaryBinding.hold());
                }

                if (primaryList.getLength() > 0 && secondaryList.getLength() > 0) {
                    log.warn("No keyboard binding for action '{}' (primary={}, secondary={})",
                            actionName,
                            ((Element) primaryList.item(0)).getAttribute("Device"),
                            ((Element) secondaryList.item(0)).getAttribute("Device"));
                }

                if (primaryBinding != null || secondaryBinding != null) {
                    bindings.put(actionName, new ReadOnlyBindingSlots(primaryBinding, secondaryBinding));
                }
            }
        }
        log.info("Parsed {} read-only binding slots from file: {}", bindings.size(), file.getName());
        return bindings;
    }

    private ReadOnlyBindingSlot readOnlySlot(Element slot, BindingSlotType slotType) {
        String device = slot.getAttribute("Device");
        String key = slot.getAttribute("Key");
        boolean hold = "1".equals(slot.getAttribute("Hold"));
        List<BindingModifier> bindingModifiers = getBindingModifiers(slot);
        return new ReadOnlyBindingSlot(
                device,
                key,
                modifierKeys(bindingModifiers),
                bindingModifiers,
                hold,
                slotType,
                isKeyboardUsable(device, key, bindingModifiers),
                isEditableKeyboardSlot(device, key, bindingModifiers)
        );
    }

    private KeyBinding toExecutableBinding(ReadOnlyBindingSlot slot) {
        if (slot == null || !slot.keyboardUsable()) return null;
        return new KeyBinding(slot.key(), slot.modifiers(), slot.hold());
    }

    private boolean hasKeyboardMainKey(String device, String key) {
        // This guard is the boundary that prevents diagnostic non-keyboard slots from becoming executable.
        return "Keyboard".equals(device) && key != null && !key.isBlank() && !"Key_".equals(key);
    }

    private boolean isKeyboardUsable(String device, String key, List<BindingModifier> modifiers) {
        return hasKeyboardMainKey(device, key)
                && modifiers.stream().allMatch(modifier -> "Keyboard".equals(modifier.device()));
    }

    private boolean isEditableKeyboardSlot(String device, String key, List<BindingModifier> modifiers) {
        if (!hasKeyboardMainKey(device, key)) {
            return false;
        }
        // The chord editor can rewrite any number of supported keyboard modifiers
        // (e.g. Left Ctrl + Left Shift), so a slot is editable as long as every
        // modifier is a supported keyboard modifier.
        return modifiers.stream().allMatch(BindingModifier::isSupportedKeyboardModifier);
    }

    private List<BindingModifier> getBindingModifiers(Element binding) {
        NodeList children = binding.getChildNodes();
        List<BindingModifier> modifiers = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element modifier && "Modifier".equals(modifier.getTagName())) {
                modifiers.add(new BindingModifier(modifier.getAttribute("Device"), modifier.getAttribute("Key")));
            }
        }
        return modifiers;
    }

    private String[] modifierKeys(List<BindingModifier> modifiers) {
        return modifiers.stream()
                .map(BindingModifier::key)
                .toArray(String[]::new);
    }
}
