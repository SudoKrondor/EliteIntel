package elite.intel.ai.hands;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes keyboard-key availability from the whole active {@code .binds} file.
 * <p>
 * This must not be limited to {@link Bindings.GameCommand}; a key assigned to
 * any Elite Dangerous action can still create an in-game conflict if reused.
 */
public class KeyboardKeyAvailabilityService {

    /**
     * Returns every occupied keyboard token found in Primary and Secondary slots
     * across all action nodes.
     */
    public Set<String> occupiedKeyboardKeys(Path bindsFile) throws Exception {
        Set<String> occupied = new LinkedHashSet<>();
        for (SlotAssignment assignment : keyboardSlotAssignments(bindsFile)) {
            occupied.add(assignment.key());
        }
        return occupied;
    }

    /**
     * Returns assignable keys that are not occupied anywhere in the file.
     */
    public List<String> availableKeys(Path bindsFile) throws Exception {
        Set<String> occupied = occupiedKeyboardKeys(bindsFile);
        return EliteKeyboardKeys.assignableKeys().stream()
                .filter(key -> !occupied.contains(key))
                .toList();
    }

    /**
     * Returns keys that are free for the plain keyboard chord, ignoring only the
     * slot currently being edited.
     */
    public List<String> availableKeys(Path bindsFile, String bindingId, BindingSlotType slotType) throws Exception {
        return availableKeys(bindsFile, bindingId, slotType, List.of());
    }

    /**
     * Returns keys that are free for the selected single-modifier keyboard chord.
     */
    public List<String> availableKeys(
            Path bindsFile,
            String bindingId,
            BindingSlotType slotType,
            BindingModifier modifier
    ) throws Exception {
        return availableKeys(bindsFile, bindingId, slotType, asModifierList(modifier));
    }

    /**
     * Returns keys that are free for the selected keyboard chord, where the chord
     * may carry any number of supported keyboard modifiers (e.g. Left Ctrl +
     * Left Shift). Availability is per-chord: a key is free unless another slot
     * uses it with the exact same modifier set.
     */
    public List<String> availableKeys(
            Path bindsFile,
            String bindingId,
            BindingSlotType slotType,
            Collection<BindingModifier> modifiers
    ) throws Exception {
        Set<BindingModifier> normalizedModifiers = normalizeModifiers(modifiers);
        Set<String> occupied = occupiedKeyboardKeysForModifier(bindsFile, bindingId, slotType, normalizedModifiers);
        List<String> available = new ArrayList<>(EliteKeyboardKeys.assignableKeys().stream()
                .filter(key -> !occupied.contains(key))
                .toList());

        available.sort(String.CASE_INSENSITIVE_ORDER);
        return List.copyOf(available);
    }

    /**
     * Save-time conflict check that ignores only the slot currently being edited.
     */
    public boolean isKeyOccupiedByOtherSlot(
            Path bindsFile,
            String bindingId,
            BindingSlotType slotType,
            String key
    ) throws Exception {
        return isKeyOccupiedByOtherSlot(bindsFile, bindingId, slotType, key, List.of());
    }

    /**
     * Save-time conflict check for the selected single-modifier keyboard chord.
     */
    public boolean isKeyOccupiedByOtherSlot(
            Path bindsFile,
            String bindingId,
            BindingSlotType slotType,
            String key,
            BindingModifier modifier
    ) throws Exception {
        return isKeyOccupiedByOtherSlot(bindsFile, bindingId, slotType, key, asModifierList(modifier));
    }

    /**
     * Save-time conflict check for the selected keyboard chord with any number of
     * supported keyboard modifiers.
     */
    public boolean isKeyOccupiedByOtherSlot(
            Path bindsFile,
            String bindingId,
            BindingSlotType slotType,
            String key,
            Collection<BindingModifier> modifiers
    ) throws Exception {
        Set<BindingModifier> normalizedModifiers = normalizeModifiers(modifiers);
        for (SlotAssignment assignment : keyboardSlotAssignments(bindsFile)) {
            if (!assignment.key().equals(key)) {
                continue;
            }
            if (!assignment.modifiers().equals(normalizedModifiers)) {
                continue;
            }
            // A binding never conflicts with itself (either slot).
            if (assignment.bindingId().equals(bindingId)) {
                continue;
            }
            // The same chord in a mutually-exclusive context (ship / SRV / on-foot / camera /
            // FSS ...) does NOT collide: Elite only evaluates one context at a time. Without this,
            // e.g. bare Q used by FreeCamSpeedInc (camera) wrongly blocked it for a ship binding,
            // contradicting the context-aware keyboard widget. See BindingConflictRules.
            if (BindingConflictRules.isSafeOverlap(bindingId, assignment.bindingId())) {
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * Reads the current keyboard key from the selected slot, if the slot is a
     * keyboard assignment.
     */
    public Optional<String> currentKeyboardSlotKey(
            Path bindsFile,
            String bindingId,
            BindingSlotType slotType
    ) throws Exception {
        Document doc = parse(bindsFile);
        Element action = directChildElement(doc.getDocumentElement(), bindingId).orElse(null);
        if (action == null) {
            return Optional.empty();
        }

        Element slot = directChildElement(action, slotType.xmlElementName()).orElse(null);
        if (slot == null) {
            return Optional.empty();
        }

        String device = slot.getAttribute("Device");
        String key = slot.getAttribute("Key");
        return isOccupiedKeyboardKey(device, key) ? Optional.of(key) : Optional.empty();
    }

    private List<SlotAssignment> keyboardSlotAssignments(Path bindsFile) throws Exception {
        Document doc = parse(bindsFile);
        List<SlotAssignment> assignments = new ArrayList<>();
        collectKeyboardSlotAssignments(doc, BindingSlotType.PRIMARY, assignments);
        collectKeyboardSlotAssignments(doc, BindingSlotType.SECONDARY, assignments);
        return assignments;
    }

    private Set<String> occupiedKeyboardKeysForModifier(
            Path bindsFile,
            String bindingId,
            BindingSlotType slotType,
            Set<BindingModifier> modifiers
    ) throws Exception {
        Set<String> occupied = new LinkedHashSet<>();
        for (SlotAssignment assignment : keyboardSlotAssignments(bindsFile)) {
            if (assignment.bindingId().equals(bindingId) && assignment.slotType() == slotType) {
                continue;
            }
            if (assignment.modifiers().equals(modifiers)) {
                occupied.add(assignment.key());
            }
        }
        return occupied;
    }

    private void collectKeyboardSlotAssignments(
            Document doc,
            BindingSlotType slotType,
            List<SlotAssignment> assignments
    ) {
        NodeList slots = doc.getElementsByTagName(slotType.xmlElementName());
        for (int i = 0; i < slots.getLength(); i++) {
            Node slotNode = slots.item(i);
            if (!(slotNode instanceof Element slot)) {
                continue;
            }
            String device = slot.getAttribute("Device");
            String key = slot.getAttribute("Key");
            if (!isOccupiedKeyboardKey(device, key)) {
                continue;
            }

            Optional<Set<BindingModifier>> modifiers = supportedKeyboardModifiers(slot);
            if (modifiers.isEmpty()) {
                // Slot carries a modifier this editor does not understand (e.g. a
                // joystick modifier). Skip it so it neither blocks keyboard chords
                // nor is matched against one.
                continue;
            }

            Node parent = slot.getParentNode();
            String bindingId = parent instanceof Element action ? action.getTagName() : "";
            assignments.add(new SlotAssignment(bindingId, slotType, key, modifiers.get()));
        }
    }

    /**
     * Returns the slot's modifier set when it is empty or made up entirely of
     * supported keyboard modifiers; empty when any modifier is unsupported.
     */
    private Optional<Set<BindingModifier>> supportedKeyboardModifiers(Element slot) {
        List<BindingModifier> modifiers = directModifierChildren(slot);
        if (modifiers.stream().allMatch(BindingModifier::isSupportedKeyboardModifier)) {
            return Optional.of(Set.copyOf(modifiers));
        }
        return Optional.empty();
    }

    private List<BindingModifier> directModifierChildren(Element slot) {
        NodeList children = slot.getChildNodes();
        List<BindingModifier> modifiers = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element modifier && "Modifier".equals(modifier.getTagName())) {
                modifiers.add(new BindingModifier(modifier.getAttribute("Device"), modifier.getAttribute("Key")));
            }
        }
        return modifiers;
    }

    private List<BindingModifier> asModifierList(BindingModifier modifier) {
        return modifier == null ? List.of() : List.of(modifier);
    }

    /**
     * Keeps only supported keyboard modifiers, collapsing the chord to a set so
     * modifier order never affects conflict matching.
     */
    private Set<BindingModifier> normalizeModifiers(Collection<BindingModifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return Set.of();
        }
        return modifiers.stream()
                .filter(modifier -> modifier != null && modifier.isSupportedKeyboardModifier())
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean isOccupiedKeyboardKey(String device, String key) {
        // Only keyboard assignments block keyboard availability. HOTAS, joystick,
        // gamepad, and mouse bindings are displayed elsewhere but cannot consume
        // a keyboard key for this MVP.
        return "Keyboard".equals(device)
                && key != null
                && !key.isBlank()
                && !"{NoDevice}".equals(key)
                && !"Key_".equals(key);
    }

    private Optional<Element> directChildElement(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element element && tagName.equals(element.getTagName())) {
                return Optional.of(element);
            }
        }
        return Optional.empty();
    }

    private Document parse(Path bindsFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(bindsFile.toFile());
        doc.getDocumentElement().normalize();
        return doc;
    }

    private record SlotAssignment(String bindingId, BindingSlotType slotType, String key,
                                  Set<BindingModifier> modifiers) {
    }
}
