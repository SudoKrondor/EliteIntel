package elite.intel.bindforge.rules;

import elite.intel.ai.hands.Bindings;
import elite.intel.bindforge.model.BindingModifier;
import elite.intel.bindforge.model.BindingSlotType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Whether a keyboard chord is already used, judged against a whole {@code .binds} file's bytes. The file-path API went
 * in Storage S6R (2026-10-29): since S5 the Bind Editor checks the edits, never a file on disk.
 * <p>
 * This must not be limited to {@link Bindings.GameCommand}; a key assigned to
 * any Elite Dangerous action can still create an in-game conflict if reused.
 */
public class KeyboardKeyAvailabilityService {

    /**
     * Save-time conflict check for a keyboard chord with any number of supported keyboard modifiers, against a binds
     * file held in memory - the edits not applied (Storage S5). No slot is named: a binding never conflicts with
     * itself, in either slot.
     *
     * @param binds the binds file's bytes
     */
    public boolean isKeyOccupiedByOtherSlot(
            byte[] binds,
            String bindingId,
            String key,
            Collection<BindingModifier> modifiers
    ) throws Exception {
        return isKeyOccupiedByOtherSlot(keyboardSlotAssignments(parse(binds)), bindingId, key, modifiers);
    }

    private boolean isKeyOccupiedByOtherSlot(
            List<SlotAssignment> assignments,
            String bindingId,
            String key,
            Collection<BindingModifier> modifiers
    ) {
        Set<BindingModifier> normalizedModifiers = normalizeModifiers(modifiers);
        for (SlotAssignment assignment : assignments) {
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

    private List<SlotAssignment> keyboardSlotAssignments(Document doc) {
        List<SlotAssignment> assignments = new ArrayList<>();
        collectKeyboardSlotAssignments(doc, BindingSlotType.PRIMARY, assignments);
        collectKeyboardSlotAssignments(doc, BindingSlotType.SECONDARY, assignments);
        return assignments;
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
            assignments.add(new SlotAssignment(bindingId, key, modifiers.get()));
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

    private Document parse(byte[] binds) throws Exception {
        Document doc = documentBuilder().parse(new ByteArrayInputStream(binds));
        doc.getDocumentElement().normalize();
        return doc;
    }

    private static DocumentBuilder documentBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder();
    }

    private record SlotAssignment(String bindingId, String key, Set<BindingModifier> modifiers) {
    }
}
