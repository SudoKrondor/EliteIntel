package elite.intel.ui.view;

import java.awt.*;

/**
 * Compatibility wrapper for titled HUD cards with optional footer strips.
 */
public class HudTitledCard extends HudSection {

    /**
     * Creates a titled card surface for dense cockpit dashboard modules.
     *
     * @param title localized section title
     * @param borderColor restrained frame color used for the card outline
     */
    public HudTitledCard(String title, Color borderColor) {
        super(title, new BorderLayout(), Variant.FRAMED, 6, borderColor);
    }
}
