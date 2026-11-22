package elite.intel.ui.support;

import elite.intel.bindforge.rules.AiAccess;

/**
 * One AI ACCESS cell in the bindings grid: the state that colours it, the words it shows and the hover text
 * that says what to do about it.
 */
public record AiAccessCell(AiAccess.State state, String text, String tooltip) implements TooltipCell {

    /**
     * The shown words, so anything reading the model as text reads what the commander sees.
     */
    @Override
    public String toString() {
        return text;
    }
}
