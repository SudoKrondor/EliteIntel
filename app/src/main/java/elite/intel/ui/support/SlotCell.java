package elite.intel.ui.support;

/**
 * One Primary or Secondary cell in the bindings grid: the slot as the commander reads it, and the raw tokens
 * behind it when a controller input shows by its given name.
 */
public record SlotCell(String text, String tooltip) implements TooltipCell {

    /**
     * The shown words, so anything reading the model as text reads what the commander sees.
     */
    @Override
    public String toString() {
        return text;
    }
}
