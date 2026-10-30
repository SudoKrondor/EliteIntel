package elite.intel.bindforge.io;

import elite.intel.bindforge.model.BindingInput;
import elite.intel.bindforge.model.BindingModifier;
import elite.intel.bindforge.model.BindingSlotType;
import elite.intel.bindforge.model.EliteKeyboardKeys;
import elite.intel.bindforge.rules.KeyboardKeyAvailabilityService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Safely assigns one V1-supported keyboard binding to one Primary or Secondary
 * slot, clears a selected V1-supported slot, or rejects unsupported XML without
 * rewriting it.
 * <p>
 * This writer is intentionally separate from {@link KeyBindingsParser}. The
 * parser feeds command execution and must remain a read-only, keyboard-only
 * boundary; write support should not make non-keyboard assignments executable
 * by accident.
 * <p>
 * A slot holding a controller (HOTAS/joystick/mouse/gamepad) assignment is never
 * editable through the keyboard methods. The user bound that device in the game and this
 * application does not take it away; only an empty {@code {NoDevice}} slot or an
 * existing keyboard slot the user is explicitly reassigning may be written.
 * <p>
 * The user-edit path - {@link #assignSlot(byte[], SlotAssignment)} and
 * {@link #assignAxis(byte[], AxisAssignment)} - is the exception, and only for an edit the user made on purpose in
 * the bindings editor, having been shown what was there: it may write any device. Nothing automatic calls it.
 * {@link #rewriteDeviceReferences(String, DeviceReferenceRewrite)} moves a device's references when its
 * {@code DeviceMappings.xml} name changes.
 * None of this widens {@link KeyBindingsParser}: writing a controller binding never makes one executable.
 * <p>
 * <strong>Nothing here reads or writes a file</strong> (Storage S5, 2026-10-28). Each method takes a binds file's
 * bytes - the edits not applied hold them - and returns the changed bytes; APPLY is the only thing that reaches the
 * game ({@code bindforge.storage.Edits}, storage.md rules 6-7). A UTF-8 byte-order mark is kept.
 */
public class BindingsWriter {
    private static final Logger log = LogManager.getLogger(BindingsWriter.class);
    private static final byte[] UTF_8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    // The elements that carry a Device attribute, and the attribute itself (not, say, "SomeDevice=").
    private static final Pattern DEVICE_SLOT_TAG = Pattern.compile("<(?:Primary|Secondary|Binding|Modifier)(?=[\\s/>])");
    private static final Pattern DEVICE_ATTRIBUTE = Pattern.compile("(?<![A-Za-z0-9_.:-])Device\\s*=\\s*([\"'])(.*?)\\1");
    private static final Pattern VALUE_ATTRIBUTE = Pattern.compile("(?<![A-Za-z0-9_.:-])Value\\s*=\\s*([\"'])(.*?)\\1");

    // The tokens the user-edit path writes, by device - the vocabulary the game itself writes.
    // Characters that would need escaping in an attribute are never part of one.
    // WHY no list of keys: a non-English layout writes the character itself (Key_ß, Key_é), so
    // EliteKeyboardKeys cannot hold every key the game writes; any Key_ name with something after it is a key.
    private static final Pattern KEYBOARD_TOKEN = Pattern.compile("Key_[^\\s\"'<>&]+");
    private static final Pattern MOUSE_BUTTON_TOKEN = Pattern.compile("Mouse_[1-9][0-9]*|(?:Pos|Neg)_Mouse_ZAxis");
    private static final String GAME_AXIS = "(?:X|Y|Z|RX|RY|RZ|U|V)Axis";
    private static final String GAMEPAD_TOKEN = "Pos_GamePad_(?:LTrigger|RTrigger)"
            + "|GamePad_(?:Face(?:Down|Right|Left|Up)|Back|LBumper|RBumper|LThumb|RThumb|DPad(?:Up|Right|Down|Left))";
    private static final Pattern CONTROLLER_BUTTON_TOKEN =
            Pattern.compile("Joy_[1-9][0-9]*|Joy_POV[1-4](?:Up|Down|Left|Right)|(?:Pos|Neg)_Joy_" + GAME_AXIS
                    + "|" + GAMEPAD_TOKEN);
    private static final Pattern CONTROLLER_AXIS_TOKEN = Pattern.compile("Joy_" + GAME_AXIS);

    // Any child element of a slot, by name - comments, processing instructions and closing tags excluded.
    private static final Pattern SLOT_CHILD = Pattern.compile("<(?![/!?])([A-Za-z_][A-Za-z0-9_.:-]*)(?=[\\s>/])");

    private final KeyboardKeyAvailabilityService availabilityService;

    public BindingsWriter() {
        this(new KeyboardKeyAvailabilityService());
    }

    BindingsWriter(KeyboardKeyAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /**
     * Replaces exactly one selected V1-supported slot with a plain keyboard
     * slot or the canonical Elite Dangerous empty slot.
     * <p>
     * The method validates the key against the whole file and edits raw XML text instead of using a DOM
     * Transformer. That avoids reformatting the whole file and keeps unrelated XML byte-for-byte identical apart
     * from the selected slot.
     *
     * @param binds the binds file's bytes
     */
    public EditedBinds assignKeyboardKey(byte[] binds, KeyboardBindingEdit edit) {
        return assignKeyboardKey(binds, edit, List.of(), false);
    }

    /**
     * Assigns a keyboard key with exactly one supported keyboard modifier.
     */
    public EditedBinds assignKeyboardKeyWithModifier(byte[] binds, KeyboardBindingEdit edit,
                                                     BindingModifier modifier) {
        return assignKeyboardKeyWithModifiers(binds, edit, modifier == null ? List.of() : List.of(modifier));
    }

    /**
     * Assigns a keyboard key with a chord of one or more supported keyboard
     * modifiers (e.g. Left Ctrl + Left Shift). The order of {@code modifiers} is
     * preserved in the written XML but is not significant to the game.
     */
    public EditedBinds assignKeyboardKeyWithModifiers(byte[] binds, KeyboardBindingEdit edit,
                                                      List<BindingModifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()
                || !modifiers.stream().allMatch(BindingModifier::isSupportedKeyboardModifier)) {
            return EditedBinds.unchanged(BindingSaveResult.UNSUPPORTED_XML, binds);
        }
        if (edit.clearsSlot()) {
            return EditedBinds.unchanged(BindingSaveResult.UNKNOWN_KEY, binds);
        }
        return assignKeyboardKey(binds, edit, modifiers, true);
    }

    /**
     * Writes one button slot as the user chose it in the editor: any device, up to three modifiers on any device,
     * and the slot's Hold flag - or clears it. This is the user-edit path: a deliberate edit may take a controller
     * slot away, which nothing automatic may do, so the assistant's writes stay on the keyboard methods above.
     * <p>
     * Unlike them it does not refuse a chord another slot already uses; the editor warns and the user decides, as
     * the game does. It refuses a token that does not suit a button slot ({@code UNKNOWN_KEY}) - a whole axis, a
     * token of the wrong device, or one not in the shape the game writes - and a slot holding structure it does not
     * know ({@code UNSUPPORTED_XML}), which it never rewrites. {@code <ToggleOn>} is never touched.
     * <p>
     * A keyboard token is any {@code Key_} name, not only the keys {@code EliteKeyboardKeys} lists: a non-English
     * layout writes the character itself ({@code Key_ß}). Hold is written as the child the game writes, unless
     * the slot already carries it as an attribute, which is kept.
     */
    public EditedBinds assignSlot(byte[] binds, SlotAssignment edit) {
        if ((!edit.clearsSlot() && !isButtonInput(edit.input()))
                || !edit.modifiers().stream().allMatch(BindingsWriter::isButtonInput)) {
            return EditedBinds.unchanged(BindingSaveResult.UNKNOWN_KEY, binds);
        }
        EncodedXml encodedXml = decode(binds);
        String xml = encodedXml.xml();
        LocatedAction action = locateAction(xml, edit.bindingId());
        if (action.result() != null) {
            return EditedBinds.unchanged(action.result(), binds);
        }
        LocatedSlot slot = locateSlot(xml, action.range(), edit.slotType());
        if (slot.result() != null) {
            return EditedBinds.unchanged(slot.result(), binds);
        }
        ExistingSlot existing = readUserEditableSlot(xml, slot.range(), edit.slotType());
        if (existing == null) {
            return EditedBinds.unchanged(BindingSaveResult.UNSUPPORTED_XML, binds);
        }
        if (existing.matches(edit)) {
            return EditedBinds.unchanged(BindingSaveResult.NO_CHANGE, binds);
        }

        String replacement = userSlotXml(edit, lineIndent(xml, slot.range().start()), existing.holdAsAttribute());
        String updatedXml = replaceRange(xml, slot.range(), replacement);
        return EditedBinds.saved(encode(new EncodedXml(updatedXml, encodedXml.hasUtf8Bom())));
    }

    /**
     * Writes one axis element as the user chose it in the editor - its {@code <Binding>}, {@code <Inverted>} and
     * {@code <Deadzone>} - or clears its binding, keeping Inverted and Deadzone as given. The user-edit path, as
     * {@link #assignSlot(byte[], SlotAssignment)}.
     * <p>
     * Takes a controller's whole axis only, {@code Joy_XAxis} to {@code Joy_VAxis} ({@code UNKNOWN_KEY} otherwise).
     * An element without exactly one of each of the three, or with any of them in a shape it does not know, is
     * refused ({@code UNSUPPORTED_XML}). A deadzone equal, as a float, to the one already there keeps its text.
     */
    public EditedBinds assignAxis(byte[] binds, AxisAssignment edit) {
        if (!edit.clearsSlot() && !isAxisInput(edit.axis())) {
            return EditedBinds.unchanged(BindingSaveResult.UNKNOWN_KEY, binds);
        }
        EncodedXml encodedXml = decode(binds);
        String xml = encodedXml.xml();
        LocatedAction action = locateAction(xml, edit.bindingId());
        if (action.result() != null) {
            return EditedBinds.unchanged(action.result(), binds);
        }
        AxisParts parts = locateAxisParts(xml, action.range());
        if (parts == null) {
            return EditedBinds.unchanged(BindingSaveResult.UNSUPPORTED_XML, binds);
        }

        List<Replacement> replacements = axisReplacements(edit, parts);
        if (replacements.isEmpty()) {
            return EditedBinds.unchanged(BindingSaveResult.NO_CHANGE, binds);
        }
        // Replace from the end of the file backwards, so an earlier range is not moved by a later replacement.
        replacements.sort(Comparator.comparingInt((Replacement r) -> r.range().start()).reversed());
        String updatedXml = xml;
        for (Replacement replacement : replacements) {
            updatedXml = replaceRange(updatedXml, replacement.range(), replacement.text());
        }
        return EditedBinds.saved(encode(new EncodedXml(updatedXml, encodedXml.hasUtf8Bom())));
    }

    /**
     * Moves every reference to a device in one {@code .binds} text: each {@code Device="..."} on a
     * {@code <Primary>}, {@code <Secondary>}, {@code <Binding>} or {@code <Modifier>} that {@code rewrite} matches
     * becomes its new name. Everything else is kept character for character. Pure.
     *
     * @throws IllegalArgumentException if one of those elements' start tags never closes - the text cannot be
     *                                  rewritten whole, and a partial rewrite would leave the old name behind
     */
    public RewrittenXml rewriteDeviceReferences(String xml, DeviceReferenceRewrite rewrite) {
        Matcher tag = DEVICE_SLOT_TAG.matcher(xml);
        StringBuilder out = new StringBuilder(xml.length());
        int copied = 0;
        int references = 0;
        int searchFrom = 0;
        while (tag.find(searchFrom)) {
            int tagEnd = findStartTagEnd(xml, tag.start());
            if (tagEnd < 0) {
                throw new IllegalArgumentException("Unterminated start tag at offset " + tag.start());
            }
            Matcher attribute = DEVICE_ATTRIBUTE.matcher(xml).region(tag.end(), tagEnd).useTransparentBounds(true);
            if (attribute.find() && rewrite.matches(attribute.group(2))) {
                out.append(xml, copied, attribute.start(2)).append(rewrite.to());
                copied = attribute.end(2);
                references++;
            }
            searchFrom = tagEnd + 1;
        }
        out.append(xml, copied, xml.length());
        return new RewrittenXml(out.toString(), references);
    }

    private EditedBinds assignKeyboardKey(
            byte[] binds,
            KeyboardBindingEdit edit,
            List<BindingModifier> requestedModifiers,
            boolean rewriteModifier
    ) {
        if (!edit.clearsSlot() && !EliteKeyboardKeys.isAssignable(edit.key())) {
            return EditedBinds.unchanged(BindingSaveResult.UNKNOWN_KEY, binds);
        }

        try {
            EncodedXml encodedXml = decode(binds);
            LocatedAction action = locateAction(encodedXml.xml(), edit.bindingId());
            if (action.result() != null) {
                return EditedBinds.unchanged(action.result(), binds);
            }

            LocatedSlot slot = locateSlot(encodedXml.xml(), action.range(), edit.slotType());
            if (slot.result() != null) {
                return EditedBinds.unchanged(slot.result(), binds);
            }

            SlotXml slotXml = inspectSlot(encodedXml.xml(), slot.range(), edit.slotType());
            if (!slotXml.supportedForV1Edit()) {
                return EditedBinds.unchanged(BindingSaveResult.UNSUPPORTED_XML, binds);
            }

            if (isNoChange(slotXml, edit, requestedModifiers, rewriteModifier)) {
                return EditedBinds.unchanged(BindingSaveResult.NO_CHANGE, binds);
            }

            if (!edit.clearsSlot() && availabilityService.isKeyOccupiedByOtherSlot(
                    binds,
                    edit.bindingId(),
                    edit.key(),
                    rewriteModifier ? requestedModifiers : List.of()
            )) {
                return EditedBinds.unchanged(BindingSaveResult.KEY_OCCUPIED, binds);
            }

            String updatedXml = replaceRange(
                    encodedXml.xml(),
                    slot.range(),
                    replacementSlot(edit, requestedModifiers, rewriteModifier, slotXml)
            );
            return EditedBinds.saved(encode(new EncodedXml(updatedXml, encodedXml.hasUtf8Bom())));
        } catch (Exception e) {
            // WHY: the occupancy check parses the whole file; a file it cannot parse is not rewritten.
            log.warn("Could not edit keyboard slot {} {}", edit.bindingId(), edit.slotType(), e);
            return EditedBinds.unchanged(BindingSaveResult.UNSUPPORTED_XML, binds);
        }
    }

    private boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= UTF_8_BOM.length
                && bytes[0] == UTF_8_BOM[0]
                && bytes[1] == UTF_8_BOM[1]
                && bytes[2] == UTF_8_BOM[2];
    }

    private byte[] encode(EncodedXml encodedXml) {
        byte[] content = encodedXml.xml().getBytes(StandardCharsets.UTF_8);
        if (!encodedXml.hasUtf8Bom()) {
            return content;
        }

        byte[] withBom = new byte[UTF_8_BOM.length + content.length];
        System.arraycopy(UTF_8_BOM, 0, withBom, 0, UTF_8_BOM.length);
        System.arraycopy(content, 0, withBom, UTF_8_BOM.length, content.length);
        return withBom;
    }

    private boolean isNoChange(
            SlotXml slotXml,
            KeyboardBindingEdit edit,
            List<BindingModifier> requestedModifiers,
            boolean rewriteModifier
    ) {
        if (edit.clearsSlot()) {
            return "{NoDevice}".equals(slotXml.device()) && slotXml.key().isBlank();
        }
        if (!"Keyboard".equals(slotXml.device()) || !edit.key().equals(slotXml.key())) {
            return false;
        }
        if (!rewriteModifier) {
            return slotXml.modifiers().isEmpty();
        }
        Set<BindingModifier> existing = slotXml.modifiers().stream()
                .map(ModifierXml::bindingModifier)
                .collect(Collectors.toSet());
        return existing.equals(new HashSet<>(requestedModifiers));
    }

    private SlotXml inspectSlot(String xml, TextRange slotRange, BindingSlotType slotType) {
        SlotShape shape = slotShape(xml, slotRange, slotType);
        if (shape == null) {
            return SlotXml.unsupported();
        }

        String device = attributeValue(shape.startTag(), "Device");
        String key = attributeValue(shape.startTag(), "Key");
        String holdAttr = attributeValue(shape.startTag(), "Hold");
        if (shape.selfClosing()) {
            return new SlotXml(device, key, List.of(), null, holdAttr, isEditableMainSlot(device, key));
        }

        String holdChild = firstSelfClosingElement(xml.substring(shape.bodyStart(), shape.bodyEnd()), "Hold");
        List<ModifierXml> modifiers = modifierXmls(xml, shape.bodyStart(), shape.bodyEnd());
        boolean supportedModifiers = modifiers.stream().allMatch(ModifierXml::supportedForV1Edit);
        return new SlotXml(device, key, modifiers, holdChild, holdAttr,
                isEditableMainSlot(device, key) && supportedModifiers);
    }

    /**
     * The slot's start tag and body, when its structure is one either writer may read: attributes {@code Device},
     * {@code Key} and {@code Hold} only, and children {@code <Modifier>} and {@code <Hold>} only. {@code null}
     * otherwise. What the slot's device and tokens may be is each writer's own rule.
     */
    private SlotShape slotShape(String xml, TextRange slotRange, BindingSlotType slotType) {
        String startTag = startTag(xml, slotRange.start());
        if (!Set.of("Device", "Key", "Hold").containsAll(attributeNames(startTag))) {
            return null;
        }
        int startTagEnd = findStartTagEnd(xml, slotRange.start());
        if (startTagEnd < 0) {
            return null;
        }
        if (isSelfClosingStartTag(xml, slotRange.start(), startTagEnd)) {
            return new SlotShape(startTag, true, startTagEnd + 1, startTagEnd + 1);
        }

        Matcher closingMatcher = closingTagPattern(slotType.xmlElementName()).matcher(xml);
        if (!closingMatcher.find(startTagEnd + 1) || closingMatcher.end() != slotRange.end()) {
            return null;
        }
        // A slot body may contain <Modifier> chord keys and a <Hold Value="..."/> flag (press-and-hold
        // bindings, e.g. on-foot weapon switch, fleet orders). Both are supported and preserved; any
        // other child element is an advanced structure these writers must not rewrite.
        Matcher childMatcher = SLOT_CHILD.matcher(xml).region(startTagEnd + 1, closingMatcher.start());
        while (childMatcher.find()) {
            String childName = childMatcher.group(1);
            if (!"Modifier".equals(childName) && !"Hold".equals(childName)) {
                return null;
            }
        }
        return new SlotShape(startTag, false, startTagEnd + 1, closingMatcher.start());
    }

    /**
     * The raw text of the first self-closing {@code <tagName ... />} in {@code body}, or {@code null}.
     */
    private String firstSelfClosingElement(String body, String tagName) {
        Matcher matcher = Pattern.compile("<" + Pattern.quote(tagName) + "\\b[^>]*?/>").matcher(body);
        return matcher.find() ? matcher.group() : null;
    }

    /**
     * The two slot states this writer may rewrite: an existing keyboard assignment, and
     * the canonical empty slot. Anything else - notably a controller assignment the
     * commander made in the game - fails closed and is never overwritten.
     */
    private boolean isEditableMainSlot(String device, String key) {
        return ("Keyboard".equals(device) && key != null && !key.isBlank() && !"Key_".equals(key))
                || ("{NoDevice}".equals(device) && (key == null || key.isBlank()));
    }

    private List<ModifierXml> modifierXmls(String xml, int bodyStart, int bodyEnd) {
        List<Integer> starts = openingTagStarts(xml, "Modifier", bodyStart, bodyEnd);
        List<ModifierXml> modifiers = new ArrayList<>();
        Set<String> allowedAttributes = Set.of("Device", "Key");
        for (int start : starts) {
            int tagEnd = findStartTagEnd(xml, start);
            if (tagEnd < 0 || tagEnd >= bodyEnd) {
                return List.of(ModifierXml.unsupported());
            }
            String startTag = xml.substring(start, tagEnd + 1);
            boolean supported = isSelfClosingStartTag(xml, start, tagEnd)
                    && allowedAttributes.containsAll(attributeNames(startTag))
                    && BindingModifier.isSupportedKeyboardModifier(
                    attributeValue(startTag, "Device"),
                    attributeValue(startTag, "Key")
            );
            modifiers.add(new ModifierXml(
                    new BindingModifier(attributeValue(startTag, "Device"), attributeValue(startTag, "Key")),
                    supported
            ));
        }
        return modifiers;
    }

    private String replacementSlot(
            KeyboardBindingEdit edit,
            List<BindingModifier> requestedModifiers,
            boolean rewriteModifier,
            SlotXml existing
    ) {
        String element = edit.slotType().xmlElementName();
        if (edit.clearsSlot()) {
            // Clearing removes the binding entirely, so any hold flag goes with it.
            return "<" + element + " Device=\"{NoDevice}\" Key=\"\" />";
        }

        // Preserve the press-and-hold nature of the binding across a key reassignment.
        String holdAttr = existing == null ? "" : existing.holdAttr();
        String holdChild = existing == null ? null : existing.holdChild();
        String openTag = "<" + element + " Device=\"Keyboard\" Key=\"" + edit.key() + "\""
                + (holdAttr == null || holdAttr.isBlank() ? "" : " Hold=\"" + holdAttr + "\"");

        boolean hasModifiers = rewriteModifier && !requestedModifiers.isEmpty();
        if (!hasModifiers && holdChild == null) {
            return openTag + " />";
        }

        StringBuilder slot = new StringBuilder(openTag).append(">\n");
        if (holdChild != null) {
            slot.append("    ").append(holdChild).append("\n");
        }
        if (hasModifiers) {
            for (BindingModifier modifier : requestedModifiers) {
                slot.append("    <Modifier Device=\"Keyboard\" Key=\"").append(modifier.key()).append("\" />\n");
            }
        }
        slot.append("</").append(element).append(">");
        return slot.toString();
    }

    private String replaceRange(String xml, TextRange range, String replacement) {
        return xml.substring(0, range.start()) + replacement + xml.substring(range.end());
    }

    private LocatedAction locateAction(String xml, String bindingId) {
        List<Integer> starts = openingTagStarts(xml, bindingId, 0, xml.length());
        if (starts.isEmpty()) {
            return new LocatedAction(BindingSaveResult.BINDING_NOT_FOUND, null);
        }
        if (starts.size() > 1) {
            return new LocatedAction(BindingSaveResult.UNSUPPORTED_XML, null);
        }

        int start = starts.get(0);
        int startTagEnd = findStartTagEnd(xml, start);
        if (startTagEnd < 0 || isSelfClosingStartTag(xml, start, startTagEnd)) {
            return new LocatedAction(BindingSaveResult.UNSUPPORTED_XML, null);
        }

        Matcher closingMatcher = closingTagPattern(bindingId).matcher(xml);
        if (!closingMatcher.find(startTagEnd + 1)) {
            return new LocatedAction(BindingSaveResult.UNSUPPORTED_XML, null);
        }

        int closeStart = closingMatcher.start();
        if (!openingTagStarts(xml, bindingId, startTagEnd + 1, closeStart).isEmpty()) {
            // Nested or overlapping action tags make textual replacement unsafe;
            // fail closed rather than guessing which close tag belongs to us.
            return new LocatedAction(BindingSaveResult.UNSUPPORTED_XML, null);
        }

        return new LocatedAction(null, new TextRange(startTagEnd + 1, closeStart));
    }

    private LocatedSlot locateSlot(String xml, TextRange actionBodyRange, BindingSlotType slotType) {
        List<Integer> starts = openingTagStarts(
                xml,
                slotType.xmlElementName(),
                actionBodyRange.start(),
                actionBodyRange.end()
        );
        if (starts.size() != 1) {
            // Missing and duplicate slots are both unsupported for the same
            // reason: this backend edits exactly one explicit slot.
            return new LocatedSlot(BindingSaveResult.UNSUPPORTED_XML, null);
        }

        int start = starts.get(0);
        int startTagEnd = findStartTagEnd(xml, start);
        if (startTagEnd < 0 || startTagEnd >= actionBodyRange.end()) {
            return new LocatedSlot(BindingSaveResult.UNSUPPORTED_XML, null);
        }

        if (isSelfClosingStartTag(xml, start, startTagEnd)) {
            return new LocatedSlot(null, new TextRange(start, startTagEnd + 1));
        }

        Matcher closingMatcher = closingTagPattern(slotType.xmlElementName()).matcher(xml);
        if (!closingMatcher.find(startTagEnd + 1) || closingMatcher.start() > actionBodyRange.end()) {
            return new LocatedSlot(BindingSaveResult.UNSUPPORTED_XML, null);
        }

        int closeStart = closingMatcher.start();
        if (!openingTagStarts(xml, slotType.xmlElementName(), startTagEnd + 1, closeStart).isEmpty()) {
            return new LocatedSlot(BindingSaveResult.UNSUPPORTED_XML, null);
        }

        return new LocatedSlot(null, new TextRange(start, closingMatcher.end()));
    }

    private List<Integer> openingTagStarts(String xml, String tagName, int from, int to) {
        Pattern pattern = Pattern.compile("<" + Pattern.quote(tagName) + "(?=[\\s>/])");
        Matcher matcher = pattern.matcher(xml);
        List<Integer> starts = new ArrayList<>();
        int searchFrom = Math.max(0, from);
        while (matcher.find(searchFrom) && matcher.start() < to) {
            starts.add(matcher.start());
            searchFrom = matcher.end();
        }
        return starts;
    }

    private Pattern closingTagPattern(String tagName) {
        return Pattern.compile("</" + Pattern.quote(tagName) + "\\s*>");
    }

    private int findStartTagEnd(String xml, int tagStart) {
        char quote = 0;
        for (int i = tagStart; i < xml.length(); i++) {
            char c = xml.charAt(i);
            if ((c == '"' || c == '\'') && quote == 0) {
                quote = c;
            } else if (c == quote) {
                quote = 0;
            } else if (c == '>' && quote == 0) {
                return i;
            }
        }
        return -1;
    }

    private boolean isSelfClosingStartTag(String xml, int tagStart, int tagEnd) {
        for (int i = tagEnd - 1; i > tagStart; i--) {
            char c = xml.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            return c == '/';
        }
        return false;
    }

    private String startTag(String xml, int tagStart) {
        int tagEnd = findStartTagEnd(xml, tagStart);
        if (tagEnd < 0) {
            return "";
        }
        return xml.substring(tagStart, tagEnd + 1);
    }

    private String attributeValue(String startTag, String attributeName) {
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(attributeName) + "\\s*=\\s*([\"'])(.*?)\\1");
        Matcher matcher = pattern.matcher(startTag);
        return matcher.find() ? matcher.group(2) : "";
    }

    private Set<String> attributeNames(String startTag) {
        Matcher matcher = Pattern.compile("\\s+([A-Za-z_:][A-Za-z0-9_.:-]*)\\s*=").matcher(startTag);
        Set<String> names = new HashSet<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private EncodedXml decode(byte[] bytes) {
        boolean hasBom = hasUtf8Bom(bytes);
        int offset = hasBom ? UTF_8_BOM.length : 0;
        return new EncodedXml(new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_8), hasBom);
    }

    // An input a button slot - or a modifier - can hold: a key, a mouse button or wheel direction, or a controller's
    // button, hat direction or axis direction. Never a whole axis.
    // The game's literals are written in its own spelling only; any other case of one is refused, never taken for a
    // controller's name.
    private static boolean isButtonInput(BindingInput input) {
        return switch (input.device()) {
            case BindingInput.KEYBOARD -> KEYBOARD_TOKEN.matcher(input.key()).matches();
            case BindingInput.MOUSE -> MOUSE_BUTTON_TOKEN.matcher(input.key()).matches();
            default -> BindingInput.isDeviceName(input.device())
                    && CONTROLLER_BUTTON_TOKEN.matcher(input.key()).matches();
        };
    }

    // An input an axis element can hold: a controller's whole axis, one of the eight the game names.
    private static boolean isAxisInput(BindingInput input) {
        return BindingInput.isDeviceName(input.device()) && CONTROLLER_AXIS_TOKEN.matcher(input.key()).matches();
    }

    /**
     * The slot as it stands, when the user-edit path may rewrite it: attributes {@code Device}, {@code Key} and
     * {@code Hold} only, and children {@code <Modifier Device Key/>} and {@code <Hold Value/>} only, of any device.
     * {@code null} for anything else.
     */
    private ExistingSlot readUserEditableSlot(String xml, TextRange slotRange, BindingSlotType slotType) {
        SlotShape shape = slotShape(xml, slotRange, slotType);
        if (shape == null) {
            return null;
        }
        String startTag = shape.startTag();
        BindingInput input = new BindingInput(attributeValue(startTag, "Device"), attributeValue(startTag, "Key"));
        boolean holdAsAttribute = attributeNames(startTag).contains("Hold");
        boolean holdAttribute = "1".equals(attributeValue(startTag, "Hold"));
        if (shape.selfClosing()) {
            return new ExistingSlot(input, List.of(), holdAttribute, holdAsAttribute);
        }

        int bodyStart = shape.bodyStart();
        int bodyEnd = shape.bodyEnd();
        String body = xml.substring(bodyStart, bodyEnd);
        List<BindingInput> modifiers = new ArrayList<>();
        for (int start : openingTagStarts(xml, "Modifier", bodyStart, bodyEnd)) {
            int tagEnd = findStartTagEnd(xml, start);
            if (tagEnd < 0 || tagEnd >= bodyEnd || !isSelfClosingStartTag(xml, start, tagEnd)) {
                return null;
            }
            String modifierTag = xml.substring(start, tagEnd + 1);
            if (!Set.of("Device", "Key").containsAll(attributeNames(modifierTag))) {
                return null;
            }
            modifiers.add(new BindingInput(attributeValue(modifierTag, "Device"), attributeValue(modifierTag, "Key")));
        }

        boolean holdChild = false;
        if (!openingTagStarts(xml, "Hold", bodyStart, bodyEnd).isEmpty()) {
            String hold = firstSelfClosingElement(body, "Hold");
            if (hold == null) {
                return null;
            }
            holdChild = "1".equals(attributeValue(hold, "Value"));
        }
        return new ExistingSlot(input, modifiers, holdAttribute || holdChild, holdAsAttribute);
    }

    // The game writes Hold as a child; a slot that already carries it as an attribute keeps that form.
    private String userSlotXml(SlotAssignment edit, String indent, boolean holdAsAttribute) {
        String element = edit.slotType().xmlElementName();
        if (edit.clearsSlot()) {
            return "<" + element + " Device=\"" + BindingInput.NO_DEVICE + "\" Key=\"\" />";
        }
        String openTag = "<" + element + " Device=\"" + edit.input().device() + "\" Key=\"" + edit.input().key() + "\""
                + (edit.hold() && holdAsAttribute ? " Hold=\"1\"" : "");
        boolean holdChild = edit.hold() && !holdAsAttribute;
        if (edit.modifiers().isEmpty() && !holdChild) {
            return openTag + " />";
        }
        // The game indents with tabs; follow whatever the slot's own line uses.
        String childIndent = indent + (indent.isEmpty() || indent.contains("\t") ? "\t" : "    ");
        StringBuilder slot = new StringBuilder(openTag).append(">\n");
        if (holdChild) {
            slot.append(childIndent).append("<Hold Value=\"1\" />\n");
        }
        for (BindingInput modifier : edit.modifiers()) {
            slot.append(childIndent).append("<Modifier Device=\"").append(modifier.device())
                    .append("\" Key=\"").append(modifier.key()).append("\" />\n");
        }
        return slot.append(indent).append("</").append(element).append(">").toString();
    }

    // The whitespace before position on its line, or "" when anything else precedes it there.
    private String lineIndent(String xml, int position) {
        int lineStart = xml.lastIndexOf('\n', position - 1) + 1;
        String prefix = xml.substring(lineStart, position);
        return prefix.isBlank() ? prefix : "";
    }

    // The three replacements an axis edit makes, or none when the element already holds exactly what it asks for.
    private List<Replacement> axisReplacements(AxisAssignment edit, AxisParts parts) {
        String binding = edit.clearsSlot()
                ? "<Binding Device=\"" + BindingInput.NO_DEVICE + "\" Key=\"\" />"
                : "<Binding Device=\"" + edit.axis().device() + "\" Key=\"" + edit.axis().key() + "\" />";
        String inverted = edit.inverted() ? "1" : "0";
        String deadzone = parts.deadzone() == edit.deadzone()
                ? parts.deadzoneText()
                : String.format(Locale.ROOT, "%.8f", (double) edit.deadzone());

        boolean sameBinding = edit.clearsSlot()
                ? BindingInput.NO_DEVICE.equals(parts.device()) && parts.key().isEmpty()
                : edit.axis().device().equals(parts.device()) && edit.axis().key().equals(parts.key());
        if (sameBinding && inverted.equals(parts.invertedText()) && deadzone.equals(parts.deadzoneText())) {
            return new ArrayList<>();
        }
        return new ArrayList<>(List.of(
                new Replacement(parts.binding(), binding),
                new Replacement(parts.invertedValue(), inverted),
                new Replacement(parts.deadzoneValue(), deadzone)));
    }

    /**
     * The axis element's one {@code <Binding Device Key/>}, {@code <Inverted Value/>} and {@code <Deadzone Value/>},
     * each self-closing; {@code null} when there is not exactly one of each in that shape, or the deadzone is not a
     * number.
     */
    private AxisParts locateAxisParts(String xml, TextRange actionBody) {
        TextRange binding = singleSelfClosingTag(xml, "Binding", actionBody, Set.of("Device", "Key"));
        TextRange inverted = singleSelfClosingTag(xml, "Inverted", actionBody, Set.of("Value"));
        TextRange deadzone = singleSelfClosingTag(xml, "Deadzone", actionBody, Set.of("Value"));
        if (binding == null || inverted == null || deadzone == null) {
            return null;
        }
        String bindingTag = xml.substring(binding.start(), binding.end());
        TextRange invertedValue = valueRange(xml, inverted);
        TextRange deadzoneValue = valueRange(xml, deadzone);
        if (invertedValue == null || deadzoneValue == null) {
            return null;
        }
        String deadzoneText = xml.substring(deadzoneValue.start(), deadzoneValue.end());
        float deadzoneNumber;
        try {
            deadzoneNumber = Float.parseFloat(deadzoneText);
        } catch (NumberFormatException e) {
            return null;
        }
        return new AxisParts(binding, attributeValue(bindingTag, "Device"), attributeValue(bindingTag, "Key"),
                invertedValue, xml.substring(invertedValue.start(), invertedValue.end()),
                deadzoneValue, deadzoneText, deadzoneNumber);
    }

    private TextRange singleSelfClosingTag(String xml, String tagName, TextRange within, Set<String> attributes) {
        List<Integer> starts = openingTagStarts(xml, tagName, within.start(), within.end());
        if (starts.size() != 1) {
            return null;
        }
        int start = starts.getFirst();
        int tagEnd = findStartTagEnd(xml, start);
        if (tagEnd < 0 || tagEnd >= within.end() || !isSelfClosingStartTag(xml, start, tagEnd)
                || !attributes.equals(attributeNames(xml.substring(start, tagEnd + 1)))) {
            return null;
        }
        return new TextRange(start, tagEnd + 1);
    }

    // Where the Value attribute's value sits inside the tag at tagRange, quotes excluded.
    private TextRange valueRange(String xml, TextRange tagRange) {
        Matcher value = VALUE_ATTRIBUTE.matcher(xml).region(tagRange.start(), tagRange.end()).useTransparentBounds(true);
        return value.find() ? new TextRange(value.start(2), value.end(2)) : null;
    }

    private record SlotShape(String startTag, boolean selfClosing, int bodyStart, int bodyEnd) {
    }

    private record ExistingSlot(BindingInput input, List<BindingInput> modifiers, boolean hold,
                                boolean holdAsAttribute) {
        // Same device, token, modifier set and Hold, compared exactly; modifier order is not significant.
        private boolean matches(SlotAssignment edit) {
            if (edit.clearsSlot()) {
                return BindingInput.NO_DEVICE.equals(input.device()) && input.key().isBlank() && modifiers.isEmpty()
                        && !hold;
            }
            return input.equals(edit.input()) && hold == edit.hold()
                    && new HashSet<>(modifiers).equals(new HashSet<>(edit.modifiers()))
                    && modifiers.size() == edit.modifiers().size();
        }
    }

    private record AxisParts(TextRange binding, String device, String key,
                             TextRange invertedValue, String invertedText,
                             TextRange deadzoneValue, String deadzoneText, float deadzone) {
    }

    private record Replacement(TextRange range, String text) {
    }

    private record TextRange(int start, int end) {
    }

    private record LocatedAction(BindingSaveResult result, TextRange range) {
    }

    private record LocatedSlot(BindingSaveResult result, TextRange range) {
    }

    private record EncodedXml(String xml, boolean hasUtf8Bom) {
    }

    private record SlotXml(
            String device,
            String key,
            List<ModifierXml> modifiers,
            String holdChild,
            String holdAttr,
            boolean supportedForV1Edit
    ) {
        private static SlotXml unsupported() {
            return new SlotXml("", "", List.of(), null, "", false);
        }
    }

    private record ModifierXml(BindingModifier bindingModifier, boolean supportedForV1Edit) {
        private static ModifierXml unsupported() {
            return new ModifierXml(new BindingModifier("", ""), false);
        }
    }
}
