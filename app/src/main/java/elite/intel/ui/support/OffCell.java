package elite.intel.ui.support;

/**
 * A row action that is off - CLEAR or AUTO FIX while BindForge is read only (Storage S14b). It shows greyed, says why
 * on hover, and a click on it does nothing.
 */
public record OffCell(String text, String tooltip) implements TooltipCell {

    /**
     * The shown words, so anything reading the model as text reads what the commander sees.
     */
    @Override
    public String toString() {
        return text;
    }
}
