package elite.intel.ai.hands;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
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
 */
public class BindingsWriter {
    private static final byte[] UTF_8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

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
     * The method rejects stale files before and after backup creation, validates
     * the key against the full active file, and edits raw XML text instead of
     * using a DOM Transformer. That avoids reformatting the whole file and keeps
     * unrelated XML byte-for-byte identical apart from the selected slot.
     */
    public BindingSaveResult assignKeyboardKey(KeyboardBindingEdit edit) {
        return assignKeyboardKey(edit, List.of(), false);
    }

    /**
     * Assigns a keyboard key with exactly one supported keyboard modifier.
     */
    public BindingSaveResult assignKeyboardKeyWithModifier(KeyboardBindingEdit edit, BindingModifier modifier) {
        return assignKeyboardKeyWithModifiers(edit, modifier == null ? List.of() : List.of(modifier));
    }

    /**
     * Assigns a keyboard key with a chord of one or more supported keyboard
     * modifiers (e.g. Left Ctrl + Left Shift). The order of {@code modifiers} is
     * preserved in the written XML but is not significant to the game.
     */
    public BindingSaveResult assignKeyboardKeyWithModifiers(KeyboardBindingEdit edit, List<BindingModifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()
                || !modifiers.stream().allMatch(BindingModifier::isSupportedKeyboardModifier)) {
            return BindingSaveResult.UNSUPPORTED_XML;
        }
        if (edit.clearsSlot()) {
            return BindingSaveResult.UNKNOWN_KEY;
        }
        return assignKeyboardKey(edit, modifiers, true);
    }

    private BindingSaveResult assignKeyboardKey(
            KeyboardBindingEdit edit,
            List<BindingModifier> requestedModifiers,
            boolean rewriteModifier
    ) {
        if (!edit.clearsSlot() && !EliteKeyboardKeys.isAssignable(edit.key())) {
            return BindingSaveResult.UNKNOWN_KEY;
        }

        try {
            if (isStale(edit)) {
                return BindingSaveResult.STALE_FILE;
            }

            EncodedXml encodedXml = readXml(edit.file());
            LocatedAction action = locateAction(encodedXml.xml(), edit.bindingId());
            if (action.result() != null) {
                return action.result();
            }

            LocatedSlot slot = locateSlot(encodedXml.xml(), action.range(), edit.slotType());
            if (slot.result() != null) {
                return slot.result();
            }

            SlotXml slotXml = inspectSlot(encodedXml.xml(), slot.range(), edit.slotType());
            if (!slotXml.supportedForV1Edit()) {
                return BindingSaveResult.UNSUPPORTED_XML;
            }

            if (isNoChange(slotXml, edit, requestedModifiers, rewriteModifier)) {
                return BindingSaveResult.NO_CHANGE;
            }

            if (!edit.clearsSlot() && availabilityService.isKeyOccupiedByOtherSlot(
                    edit.file(),
                    edit.bindingId(),
                    edit.slotType(),
                    edit.key(),
                    rewriteModifier ? requestedModifiers : List.of()
            )) {
                return BindingSaveResult.KEY_OCCUPIED;
            }

            if (isStale(edit)) {
                return BindingSaveResult.STALE_FILE;
            }

            String updatedXml = replaceRange(
                    encodedXml.xml(),
                    slot.range(),
                    replacementSlot(edit, requestedModifiers, rewriteModifier, slotXml)
            );
            return writeReplacement(edit.file(), new EncodedXml(updatedXml, encodedXml.hasUtf8Bom()));
        } catch (IOException e) {
            return BindingSaveResult.WRITE_FAILED;
        } catch (Exception e) {
            return BindingSaveResult.UNSUPPORTED_XML;
        }
    }

    private EncodedXml readXml(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        boolean hasBom = hasUtf8Bom(bytes);
        int offset = hasBom ? UTF_8_BOM.length : 0;
        return new EncodedXml(new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_8), hasBom);
    }

    private boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= UTF_8_BOM.length
                && bytes[0] == UTF_8_BOM[0]
                && bytes[1] == UTF_8_BOM[1]
                && bytes[2] == UTF_8_BOM[2];
    }

    private BindingSaveResult writeReplacement(Path file, EncodedXml updatedXml) {
        Path tempFile = file.getParent().resolve(
                "." + file.getFileName() + ".elite-intel-" + UUID.randomUUID() + ".tmp");
        try {
            Files.write(
                    tempFile,
                    encode(updatedXml),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
            try {
                Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return BindingSaveResult.SAVED;
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // Best-effort cleanup; the save result already reports failure.
            }
            return BindingSaveResult.WRITE_FAILED;
        }
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

    private boolean isStale(KeyboardBindingEdit edit) throws IOException {
        if (edit.expectedLastModified() == null || edit.expectedFileSize() < 0) {
            return true;
        }
        FileTime actualLastModified = Files.getLastModifiedTime(edit.file());
        long actualSize = Files.size(edit.file());
        return !actualLastModified.equals(edit.expectedLastModified())
                || actualSize != edit.expectedFileSize();
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
        String startTag = startTag(xml, slotRange.start());
        Set<String> allowedAttributes = Set.of("Device", "Key", "Hold");
        if (!allowedAttributes.containsAll(attributeNames(startTag))) {
            return SlotXml.unsupported();
        }

        int startTagEnd = findStartTagEnd(xml, slotRange.start());
        if (startTagEnd < 0) {
            return SlotXml.unsupported();
        }

        String device = attributeValue(startTag, "Device");
        String key = attributeValue(startTag, "Key");
        String holdAttr = attributeValue(startTag, "Hold");
        boolean selfClosing = isSelfClosingStartTag(xml, slotRange.start(), startTagEnd);
        if (selfClosing) {
            return new SlotXml(device, key, List.of(), null, holdAttr, isEditableMainSlot(device, key));
        }

        Matcher closingMatcher = closingTagPattern(slotType.xmlElementName()).matcher(xml);
        if (!closingMatcher.find(startTagEnd + 1) || closingMatcher.end() != slotRange.end()) {
            return SlotXml.unsupported();
        }

        String body = xml.substring(startTagEnd + 1, closingMatcher.start());
        // A slot body may contain <Modifier> chord keys and a <Hold Value="..."/> flag (press-and-hold
        // bindings, e.g. on-foot weapon switch, fleet orders). Both are supported and preserved; any
        // other child element is an advanced structure this minimal writer must not rewrite.
        Matcher childMatcher = Pattern.compile("<(?!/|!|\\?)([A-Za-z_][A-Za-z0-9_.:-]*)(?=[\\s>/])").matcher(body);
        while (childMatcher.find()) {
            String childName = childMatcher.group(1);
            if (!"Modifier".equals(childName) && !"Hold".equals(childName)) {
                return SlotXml.unsupported();
            }
        }

        String holdChild = firstSelfClosingElement(body, "Hold");
        List<ModifierXml> modifiers = modifierXmls(xml, startTagEnd + 1, closingMatcher.start());
        boolean supportedModifiers = modifiers.stream().allMatch(ModifierXml::supportedForV1Edit);
        return new SlotXml(device, key, modifiers, holdChild, holdAttr,
                isEditableMainSlot(device, key) && supportedModifiers);
    }

    /**
     * The raw text of the first self-closing {@code <tagName ... />} in {@code body}, or {@code null}.
     */
    private String firstSelfClosingElement(String body, String tagName) {
        Matcher matcher = Pattern.compile("<" + Pattern.quote(tagName) + "\\b[^>]*?/>").matcher(body);
        return matcher.find() ? matcher.group() : null;
    }

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
