package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * HUD number box: a value the commander can type or step, in the look of the other fields, with the ▲▼ in a cell of
 * their own as a combo has its ▼. Use {@link HudStepper} where typing is not wanted.
 */
public class HudSpinner extends JSpinner {

    private static final int STEP_CELL_WIDTH = 24;

    /**
     * @param value initial value
     * @param min   lowest allowed value (inclusive)
     * @param max   highest allowed value (inclusive)
     * @param step  increment applied per arrow click
     */
    public HudSpinner(int value, int min, int max, int step) {
        super(new SpinnerNumberModel(value, min, max, step));
        DefaultEditor editor = (DefaultEditor) getEditor();
        editor.setOpaque(false);
        JFormattedTextField text = editor.getTextField();
        // The combo editor's treatment: the frame is this control's, and the palette walk must not give the text its own.
        AppTheme.styleComboEditor(text);
        text.setHorizontalAlignment(SwingConstants.LEADING);
        // WHY: the focus sits on the text, and the frame it brightens is painted by this control.
        text.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    /**
     * WHY the HUD delegate always: the look-and-feel's own paints the white rounded shell.
     */
    @Override
    public void updateUI() {
        setUI(new HudSpinnerUI());
        invalidate();
    }

    /**
     * Holds the shared field height, so the box lines up with text fields and combos in a form row.
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(super.getPreferredSize().width, HudPalette.HUD_FIELD_HEIGHT);
    }

    /**
     * Repaints the corner marks over the children: the step cell reaches the frame and covers them.
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        AppTheme.paintHudFieldCornerMarks(this, g);
    }

    private static final class HudSpinnerUI extends BasicSpinnerUI {

        @Override
        protected void installDefaults() {
            super.installDefaults();
            spinner.setBackground(HudPalette.HUD_COLOR_ROLE_FIELD_BACKGROUND);
            spinner.setBorder(AppTheme.hudComboBorder());
        }

        @Override
        protected Component createNextButton() {
            Component button = new StepButton(true);
            button.setName("Spinner.nextButton");
            installNextButtonListeners(button);
            return button;
        }

        @Override
        protected Component createPreviousButton() {
            Component button = new StepButton(false);
            button.setName("Spinner.previousButton");
            installPreviousButtonListeners(button);
            return button;
        }
    }

    /**
     * One half of the step cell: pressed inverts, as a pressed {@link HudStepper} arrow does.
     */
    private static final class StepButton extends JButton {
        private final boolean up;

        private StepButton(boolean up) {
            this.up = up;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusable(false);
            setOpaque(false);
            setPreferredSize(new Dimension(STEP_CELL_WIDTH, 0));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                int w = getWidth();
                int h = getHeight();
                boolean pressed = isEnabled() && getModel().isPressed();
                g2.setColor(pressed ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION : HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
                g2.fillRect(0, 0, w, h);
                g2.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER);
                g2.drawLine(0, 0, 0, h - 1);
                if (up) {
                    g2.drawLine(0, h - 1, w - 1, h - 1);
                }
                Color arrow = !isEnabled() ? HudPalette.HUD_COLOR_ROLE_DISABLED
                        : pressed ? HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT
                        : HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION;
                if (up) {
                    HudGlyphs.paintHudArrowUp(g2, 0, 0, w, h, arrow);
                } else {
                    HudGlyphs.paintHudArrowDown(g2, 0, 0, w, h, arrow);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
