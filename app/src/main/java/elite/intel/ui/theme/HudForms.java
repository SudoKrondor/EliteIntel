package elite.intel.ui.theme;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

import static elite.intel.ui.theme.HudPalette.*;

/**
 * GridBag form-layout helpers: a shared {@link GridBagConstraints} seed and the row builders
 * (labels, fields, checkboxes, full-width spans) used to assemble HUD forms. Split out of
 * {@link AppTheme} so layout plumbing lives apart from palette tokens and component factories.
 * Field-label styling itself is owned by {@link AppTheme#styleFieldLabel(JLabel)} (§5.1).
 */
public final class HudForms {

    private HudForms() {
    }

    // -- GridBagLayout helpers -------------------------------------------------

    public static GridBagConstraints baseGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        return gbc;
    }

    public static void nextRow(GridBagConstraints gbc) {
        gbc.gridy++;
    }

    /**
     * Single owner of form-label height: sizes a field label to the given column width and the canonical
     * field-row height ({@link #HUD_FIELD_HEIGHT}), so each label lines up with the field beside it.
     * Call sites pass only the width (their layout intent); the height lives here, not at the call site.
     */
    public static void sizeFieldLabel(JLabel label, int width) {
        label.setPreferredSize(new Dimension(width, HUD_FIELD_HEIGHT));
    }

    public static void addLabel(JPanel panel, String text, GridBagConstraints gbc) {
        addLabel(panel, text, gbc, 220);
    }

    /**
     * Adds a dim-aware field label (§5.1) at column 0. {@code labelWidth} fixes the label-column
     * width for aligned single-column forms; pass {@code <= 0} to size the label to its text so the
     * field hugs it (tight two-column forms — avoids the large gap after short labels).
     */
    public static void addLabel(JPanel panel, String text, GridBagConstraints gbc, int labelWidth) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        // Dim-aware field label: follows its enabled state (§0.6) so a disabled row's
        // label greys out together with its field, centrally for every form.
        JLabel label = new JLabel(text.toUpperCase()) {
            @Override public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                setForeground(enabled ? FG : HUD_DISABLED);
            }
        };
        AppTheme.styleFieldLabel(label); // shared §5.1 field-label styling (FG_MUTED, XS caps, locked)
        if (labelWidth > 0) {
            sizeFieldLabel(label, labelWidth);
        }
        panel.add(label, gbc);
    }

    public static void addField(JPanel panel, JComponent comp, GridBagConstraints gbc, int col, double weightX) {
        gbc.gridx = col;
        gbc.weightx = weightX;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        comp.setPreferredSize(new Dimension(0, comp.getPreferredSize().height));
        panel.add(comp, gbc);
    }

    public static void addCheck(JPanel panel, JCheckBox check, GridBagConstraints gbc) {
        gbc.gridx = 2;
        gbc.weightx = 0.2;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(check, gbc);
    }

    /**
     * Adds a full-width component spanning the whole form grid (label + field + check columns) at the
     * current row. Use for label-less, full-width controls inside a {@link #baseGbc} grid — e.g. a
     * section-wide segmented switch or banner — so the component shares the same row insets as labelled
     * rows instead of needing a hand-tuned border.
     */
    public static void addSpanComponent(JPanel panel, JComponent comp, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(comp, gbc);
        gbc.gridwidth = 1;
    }

    public static void addNestedPanel(JPanel parent, JPanel child, String title) {
        parent.add(new JLabel(title));
        parent.add(child);
    }

    public static void bindLock(JCheckBox lockCheck, JComponent field) {
        Runnable apply = () -> {
            boolean locked = lockCheck.isSelected();
            if (field instanceof JTextComponent tc) {
                tc.setEnabled(!locked);
            } else {
                field.setEnabled(!locked);
            }
        };
        lockCheck.addItemListener(e -> apply.run());
        apply.run();
    }
}
