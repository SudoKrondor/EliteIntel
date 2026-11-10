package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.io.BindingsLoader.SplitChoice;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudModalSpec;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.makeButton;
import static elite.intel.ui.theme.AppTheme.makeButtonSubtle;
import static elite.intel.ui.theme.AppTheme.transparentPanel;

/**
 * Which binds file Elite-Intel reads when the preset file's lines name different ones (Storage S13f): the lines, one
 * per file, and a dropdown of the files. A dropdown, not a button each, since four sections can name four files. The
 * General line's file is shown first, as it is listed first; nothing is chosen until USE is clicked - Enter chooses
 * nothing (storage.md rule 14's click-only rule).
 */
final class SplitPresetDialog extends JDialog {

    private final List<SplitChoice> choices;
    private HudComboBox<SplitChoice> files;
    private SplitChoice answer;

    SplitPresetDialog(Component parent, List<SplitChoice> choices) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.choices = List.copyOf(choices);
        buildUi();
    }

    /** @return the BindName chosen, or empty when the user chose Not now or closed the dialog */
    Optional<String> showDialog() {
        AppTheme.runWithModalScrim(getOwner(), () -> setVisible(true));
        return Optional.ofNullable(answer).map(SplitChoice::bindName);
    }

    private void buildUi() {
        JPanel body = transparentPanel(new BorderLayout(0, 12));
        JTextArea intro = wrapped(getText("bindings.splitPreset.intro", lines(choices)));
        body.add(intro, BorderLayout.NORTH);
        files = new HudComboBox<>(choices.toArray(SplitChoice[]::new), SplitChoice::bindName);
        files.setSelectedIndex(0);
        body.add(files, BorderLayout.CENTER);

        JButton use = makeButton(getText("bindings.splitPreset.use"));
        use.addActionListener(e -> {
            answer = (SplitChoice) files.getSelectedItem();
            dispose();
        });
        JButton notNow = makeButtonSubtle(getText("bindings.restore.notNow"));
        notNow.addActionListener(e -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("bindings.splitPreset.title"))
                .onClose(this::dispose)
                .body(body)
                .scrollBody(false)
                .primary(use)
                .dismiss(notNow)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        HudConfirmDialog.chooseByClickOnly(this, intro);
        pack();
        setSize(Math.max(getWidth(), 560), getHeight());
        setLocationRelativeTo(getOwner());
    }

    /** One line per file - <em>"General, Ship: Custom"</em> - in the file's order. */
    static String lines(List<SplitChoice> choices) {
        return choices.stream()
                .map(choice -> choice.sections().stream().map(BindingSection::getLabelKey)
                        .map(key -> getText(key)).collect(Collectors.joining(", ")) + ": " + choice.bindName())
                .collect(Collectors.joining("\n"));
    }

    /** A message that wraps at the dialog's width, as {@link HudConfirmDialog}'s does. */
    private static JTextArea wrapped(String text) {
        JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setOpaque(false);
        area.setColumns(40);
        area.setForeground(HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = area.getFont();
        area.setFont(base.deriveFont(HudPalette.HUD_FONT_FIELD_VALUE));
        area.putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        area.setBorder(null);
        int width = area.getPreferredSize().width;
        area.setSize(width, Short.MAX_VALUE);
        area.setPreferredSize(new Dimension(width, area.getPreferredSize().height));
        return area;
    }
}
