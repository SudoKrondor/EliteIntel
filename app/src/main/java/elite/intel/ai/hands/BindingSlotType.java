package elite.intel.ai.hands;

/**
 * Editable Elite Dangerous binding slots supported by the first keyboard-editing MVP.
 */
public enum BindingSlotType {
    PRIMARY("Primary"),
    SECONDARY("Secondary");

    private final String xmlElementName;

    BindingSlotType(String xmlElementName) {
        this.xmlElementName = xmlElementName;
    }

    /**
     * Returns the exact XML element name used inside {@code .binds} action nodes.
     */
    public String xmlElementName() {
        return xmlElementName;
    }
}
