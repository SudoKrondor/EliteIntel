package elite.intel.ui.dialog;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudModalSpec;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * The JDK's colour chooser on the standard modal scaffold.
 * <p>
 * Only the chooser's tab strip is dressed. {@link AppTheme#applyDarkPalette} would walk the whole chooser and
 * repaint the preview swatches and the sample text in the app's own colours - painting over the one part of the
 * dialog whose job is to show the colour being picked. The tab strip needs its background set on the component
 * because the pane keeps the look-and-feel's near-white default there, which leaves a pale band beside the tabs
 * on an otherwise dark dialog; the selected tab's own white-on-white text is fixed globally, in the UI defaults.
 */
public final class HudColorChooserDialog extends JDialog {

    private final JColorChooser chooser;
    private Color chosen;

    private HudColorChooserDialog(Window owner, String title, Color initial) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        chooser = new JColorChooser(initial == null ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION : initial);
        setUndecorated(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dressTabStrip();

        JButton ok = AppTheme.makeButton(getText("button.ok"));
        ok.addActionListener(e -> {
            chosen = chooser.getColor();
            dispose();
        });
        JButton cancel = AppTheme.makeButtonSubtle(getText("button.cancel"));
        cancel.addActionListener(e -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(title)
                .onClose(this::dispose)
                .body(chooser)
                .primary(ok)
                .dismiss(cancel)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(ok);
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Shows the chooser and waits for it.
     *
     * @param parent the component or window it opens from
     * @return the chosen colour, or {@code null} if the commander cancelled
     */
    public static Color choose(Component parent, String title, Color initial) {
        Window owner = parent instanceof Window window ? window : SwingUtilities.getWindowAncestor(parent);
        HudColorChooserDialog dialog = new HudColorChooserDialog(owner, title, initial);
        AppTheme.runWithModalScrim(owner, () -> dialog.setVisible(true));
        return dialog.chosen;
    }

    private void dressTabStrip() {
        for (Component child : chooser.getComponents()) {
            if (child instanceof JTabbedPane tabs) {
                tabs.setOpaque(true);
                tabs.setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
            }
        }
    }
}
