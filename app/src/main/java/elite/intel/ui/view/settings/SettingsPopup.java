package elite.intel.ui.view.settings;

import elite.intel.ui.view.HudDialogHeader;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import static elite.intel.ui.view.AppTheme.FG_MUTED;
import static elite.intel.ui.view.AppTheme.HUD_BG;
import static elite.intel.ui.view.AppTheme.HUD_DIALOG_BODY;
import static elite.intel.ui.view.AppTheme.HUD_BORDER_THICKNESS_ACCENT;
import static elite.intel.ui.view.AppTheme.HUD_BORDER_THICKNESS;
import static elite.intel.ui.view.AppTheme.HUD_GAP;
import static elite.intel.ui.view.AppTheme.HUD_ORANGE_FILL_HOVER;
import static elite.intel.ui.view.AppTheme.hudScrollPane;
import static elite.intel.ui.view.AppTheme.makeButtonSubtle;
import static elite.intel.ui.view.AppTheme.transparentPanel;
import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

public class SettingsPopup extends JDialog {

    public SettingsPopup(Component parent, String title, List<SettingRow> rows) {
        this(parent, title, rows, null);
    }

    public SettingsPopup(Component parent, String title, List<SettingRow> rows, Runnable onClose) {
        super(SwingUtilities.getWindowAncestor(parent), title, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        JPanel content = transparentPanel(null);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(HUD_GAP * 2, HUD_GAP * 2, HUD_GAP * 2, HUD_GAP * 2));

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

        JScrollPane scroll = hudScrollPane(content);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getViewport().setBackground(HUD_DIALOG_BODY);

        HudDialogHeader header = new HudDialogHeader(title, this::dispose);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(HUD_BG);
        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(scroll, BorderLayout.CENTER);

        JButton cancel = makeButtonSubtle(getText("button.back"));
        cancel.addActionListener(e -> dispose());
        JPanel footer = transparentPanel(new FlowLayout(FlowLayout.RIGHT, HUD_GAP, HUD_GAP));
        int gap = HUD_GAP;
        int th  = HUD_BORDER_THICKNESS;
        Border topRule = new AbstractBorder() {
            @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                g.setColor(HUD_ORANGE_FILL_HOVER);
                g.fillRect(x + gap, y, w - gap * 2, th);
            }
            @Override public Insets getBorderInsets(Component c) { return new Insets(th, 0, 0, 0); }
            @Override public Insets getBorderInsets(Component c, Insets i) { i.set(th, 0, 0, 0); return i; }
        };
        footer.setBorder(BorderFactory.createCompoundBorder(
                topRule,
                BorderFactory.createEmptyBorder(HUD_GAP, HUD_GAP, HUD_GAP, HUD_GAP)));
        footer.add(cancel);
        wrapper.add(footer, BorderLayout.SOUTH);

        setContentPane(wrapper);

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

        getRootPane().setBorder(new LineBorder(HUD_ORANGE_FILL_HOVER, HUD_BORDER_THICKNESS_ACCENT));
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
