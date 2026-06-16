package elite.intel.ui.screen.settings;

import elite.intel.ui.widget.HudModalSpec;


import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import static elite.intel.ui.theme.HudPalette.FG_MUTED;
import static elite.intel.ui.theme.HudPalette.HUD_DIALOG_BODY;
import static elite.intel.ui.theme.AppTheme.hudModalScaffold;
import static elite.intel.ui.theme.AppTheme.hudScrollPane;
import static elite.intel.ui.theme.AppTheme.makeButtonSubtle;
import static elite.intel.ui.theme.AppTheme.transparentPanel;
import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.HudPalette.*;

public class SettingsPopup extends JDialog {

    public SettingsPopup(Component parent, String title, List<SettingRow> rows) {
        this(parent, title, rows, null);
    }

    public SettingsPopup(Component parent, String title, List<SettingRow> rows, Runnable onClose) {
        super(SwingUtilities.getWindowAncestor(parent), title, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        JPanel content = transparentPanel(null);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        if (rows.isEmpty()) {
            JLabel placeholder = new JLabel(getText("popup.noSettings"));
            placeholder.setForeground(FG_MUTED);
            placeholder.setBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));
            placeholder.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(placeholder);
        } else {
            for (SettingRow row : rows) {
                JPanel built = row.build();
                built.setAlignmentX(Component.LEFT_ALIGNMENT);
                content.add(built);
            }
        }

        // Keep manual scroll to retain HORIZONTAL_SCROLLBAR_NEVER + scrollbar ref for size logic
        JScrollPane scroll = hudScrollPane(content);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getViewport().setBackground(HUD_DIALOG_BODY);

        JButton cancel = makeButtonSubtle(getText("button.back"));
        cancel.addActionListener(e -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(title)
                .onClose(this::dispose)
                .body(scroll)        // scroll pane passed directly; scrollBody=false (already wrapped)
                .scrollBody(false)
                .dismiss(cancel)
                .build();

        setContentPane(hudModalScaffold(spec));

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        pack();
        Rectangle scr = getGraphicsConfiguration().getBounds();
        int sbW = scroll.getVerticalScrollBar().getPreferredSize().width;
        int maxH = scr.height - 80;
        int w = Math.min(getWidth(), parent.getWidth() - 120);
        // desiredH adds the V-scrollbar width as headroom so AS_NEEDED doesn't
        // trigger when content fits; capped at maxH so real overflow still scrolls
        int h = Math.min(getHeight() + sbW, maxH);
        if (w != getWidth() || h != getHeight()) {
            setSize(new Dimension(w, h));
        }
        setLocationRelativeTo(getOwner());
        setResizable(true);
        setModal(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        if (onClose != null) {
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    onClose.run();
                }
            });
        }
    }
}
