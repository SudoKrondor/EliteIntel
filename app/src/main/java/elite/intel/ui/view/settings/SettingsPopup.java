package elite.intel.ui.view.settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import elite.intel.ui.view.HudSection;

import static elite.intel.ui.view.AppTheme.FG_MUTED;
import static elite.intel.ui.view.AppTheme.HUD_BG;
import static elite.intel.ui.view.AppTheme.hudScrollPane;
import static elite.intel.ui.view.AppTheme.transparentPanel;
import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

public class SettingsPopup extends JDialog {

    public SettingsPopup(Component parent, String title, List<SettingRow> rows) {
        this(parent, title, rows, null);
    }

    public SettingsPopup(Component parent, String title, List<SettingRow> rows, Runnable onClose) {
        super(SwingUtilities.getWindowAncestor(parent), title, ModalityType.APPLICATION_MODAL);

        JPanel content = transparentPanel(null);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

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

        HudSection section = new HudSection(title, new BorderLayout());
        section.body().add(content, BorderLayout.CENTER);

        JScrollPane scroll = hudScrollPane(section);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        setContentPane(scroll);
        getContentPane().setBackground(HUD_BG);

        int height = Math.clamp(60 + rows.size() * 40L, 140, 520);
        setPreferredSize(new Dimension(parent.getWidth() - 120, height));
        pack();
        setLocationRelativeTo(getOwner());
        setResizable(true);
        setModal(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

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
