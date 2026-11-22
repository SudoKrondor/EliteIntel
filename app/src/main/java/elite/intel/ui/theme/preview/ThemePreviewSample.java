package elite.intel.ui.theme.preview;

import elite.intel.i18n.Language;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudLogArea;
import elite.intel.ui.widget.HudModalScaffold;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudPanel;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudSegmentedControl;
import elite.intel.ui.widget.HudSlider;
import elite.intel.ui.widget.HudTelemetryBlock;
import elite.intel.ui.widget.HudTwoColumns;
import elite.intel.ui.widget.StatusBadge;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * The panel the colour theme preview shows: the app's own widgets, built by the same factories the screens use, in
 * the states a commander meets them in - at rest, disabled, selected, hovered and pressed.
 * <p>
 * Every widget here must draw without the database or a session: the preview is rendered in a second process that
 * never opens the commander's data. That rules out the top status bar and the commander block, so the ship name and
 * credits colours are chosen from the swatches only. Keyboard focus cannot be shown either - a headless process has
 * no focus to give - so neither can the input focus colour.
 * <p>
 * Its text is in the app's language, passed in, because the plain {@code getText} would ask the database for it.
 */
final class ThemePreviewSample {

    private static final int GAP = HudPalette.HUD_GAP;
    private static final int CHAT_HEIGHT = 170;
    private static final int SYSTEM_LOG_HEIGHT = 40;
    private static final int READOUT_ROWS = 5;
    private static final int TABLE_HEIGHT = 120;

    private final Language language;

    private ThemePreviewSample(Language language) {
        this.language = language;
    }

    /** Builds the sample, already dressed in the active theme. */
    static JComponent build(Language language) {
        return new ThemePreviewSample(language).root();
    }

    private JComponent root() {
        JPanel root = new JPanel(new BorderLayout(0, GAP));
        root.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));
        root.add(mainNavigation(), BorderLayout.NORTH);
        JPanel readouts = readoutsColumn();
        root.add(new HudTwoColumns(controlsColumn(), readouts), BorderLayout.CENTER);
        AppTheme.applyDarkPalette(root);
        // WHY after the palette pass: a dialog is dressed by its scaffold alone and never goes through that pass,
        // which would repaint its body and header in the screen's own background.
        addRow(readouts, READOUT_ROWS, dialog());
        addFiller(readouts, READOUT_ROWS + 1);
        return root;
    }

    private JComponent mainNavigation() {
        JTabbedPane tabs = AppTheme.makeMainNavTabs();
        for (String key : new String[]{"tab.ai", "tab.commander", "tab.actions", "tab.bindings"}) {
            tabs.addTab(text(key), new JPanel());
        }
        return tabRowOnly(tabs);
    }

    private JComponent controlsColumn() {
        HudSection section = new HudSection(text("themes.preview.section"), new GridBagLayout(), HudPanel.Variant.FRAMED);
        JPanel body = section.body();
        int row = 0;
        addRow(body, row++, sectionTabs());
        addRow(body, row++, field(text("themes.preview.fieldValue"), true));
        addRow(body, row++, field(text("themes.preview.disabledValue"), false));
        addRow(body, row++, AppTheme.makeComboBox(new String[]{text("themes.preview.comboValue")}));
        addRow(body, row++, checkBoxes());
        addRow(body, row++, new HudSegmentedControl(new String[]{"1", "2", "3"}, 1));
        addRow(body, row++, new HudSlider(0, 100, 5, 60));
        addRow(body, row++, buttons());
        addRow(body, row++, subtleButtons());
        addFiller(body, row);
        return section;
    }

    /** The readouts, without the dialog that goes under them: {@link #root()} adds it after the palette pass. */
    private JPanel readoutsColumn() {
        JPanel column = new JPanel(new GridBagLayout());
        int row = 0;
        addRow(column, row++, statusBadges());
        addRow(column, row++, telemetry());
        addRow(column, row++, chat());
        addRow(column, row++, systemLog());
        addRow(column, row, table());
        return column;
    }

    private JComponent sectionTabs() {
        JTabbedPane tabs = AppTheme.makeSectionTabs();
        for (String key : new String[]{"player.tab.bookmarks", "player.tab.exoMastery"}) {
            tabs.addTab(text(key), new JPanel());
        }
        return tabRowOnly(tabs);
    }

    private JComponent field(String value, boolean enabled) {
        JTextField field = AppTheme.makeTextField();
        field.setText(value);
        field.setEnabled(enabled);
        return field;
    }

    private JComponent checkBoxes() {
        JPanel row = flowRow();
        row.add(AppTheme.makeCheckBox(text("themes.preview.on"), true));
        row.add(AppTheme.makeCheckBox(text("themes.preview.off"), false));
        JCheckBox disabled = AppTheme.makeCheckBox(text("themes.preview.disabled"), true);
        disabled.setEnabled(false);
        row.add(disabled);
        return row;
    }

    private JComponent buttons() {
        JPanel row = flowRow();
        row.add(AppTheme.makeButton(text("themes.preview.button")));
        JButton hovered = AppTheme.makeButton(text("themes.preview.hover"));
        hovered.getModel().setRollover(true);
        row.add(hovered);
        JButton pressed = AppTheme.makeButton(text("themes.preview.pressed"));
        pressed.getModel().setArmed(true);
        pressed.getModel().setPressed(true);
        row.add(pressed);
        return row;
    }

    private JComponent subtleButtons() {
        JPanel row = flowRow();
        row.add(AppTheme.makeButtonSubtle(text("themes.preview.subtle")));
        JButton disabled = AppTheme.makeButton(text("themes.preview.disabled"));
        disabled.setEnabled(false);
        row.add(disabled);
        return row;
    }

    private JComponent statusBadges() {
        JPanel row = flowRow();
        row.add(new StatusBadge(text("themes.preview.state.ok"), StatusBadge.State.OK));
        row.add(new StatusBadge(text("themes.preview.state.standby"), StatusBadge.State.STANDBY));
        row.add(new StatusBadge(text("themes.preview.state.offline"), StatusBadge.State.OFFLINE));
        row.add(new StatusBadge(text("themes.preview.state.info"), StatusBadge.State.INFO));
        row.add(new StatusBadge(text("themes.preview.state.idle"), StatusBadge.State.IDLE));
        return row;
    }

    private JComponent telemetry() {
        HudTelemetryBlock block = new HudTelemetryBlock(text("themes.preview.readout"));
        block.setValue("32 t");
        return block;
    }

    private JComponent chat() {
        HudLogArea chat = HudLogArea.chat(0);
        chat.addMessage(text("themes.preview.commanderLine"), HudLogArea.Style.USER_INPUT, HudLogArea.Align.LEFT);
        chat.addMessage(text("themes.preview.vegaLine"), HudLogArea.Style.AI_RESPONSE, HudLogArea.Align.RIGHT);
        chat.finishTyping();
        return inScrollPane(chat, CHAT_HEIGHT);
    }

    /**
     * WHY a fixed time: the line carries a clock, and a preview that changed with the minute would redraw pixels no
     * colour changed.
     */
    private JComponent systemLog() {
        HudLogArea log = new HudLogArea(0, HudLogArea.Style.SYSTEM_LOG);
        log.addSystemLogEntry(LocalDate.of(3311, 1, 1).atTime(LocalTime.of(20, 15)).atZone(ZoneId.systemDefault()).toInstant(),
                text("themes.preview.systemLine"));
        log.finishTyping();
        return inScrollPane(log, SYSTEM_LOG_HEIGHT);
    }

    private JComponent table() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{
                        {"Sol", text("themes.preview.state.ok")},
                        {"Achenar", text("themes.preview.state.offline")},
                        {"Shinrarta Dezhra", text("themes.preview.state.standby")}},
                new Object[]{text("themes.preview.column.system"), text("themes.preview.column.state")});
        JTable table = new JTable(model);
        AppTheme.styleTable(table);
        table.setRowSelectionInterval(1, 1);
        JScrollPane scroll = AppTheme.hudScrollPane(table);
        scroll.setPreferredSize(new Dimension(0, TABLE_HEIGHT));
        return scroll;
    }

    private JComponent dialog() {
        JLabel body = new JLabel(text("themes.preview.dialogBody"));
        return HudModalScaffold.build(HudModalSpec.builder()
                .title(text("themes.preview.dialogTitle"))
                .onClose(() -> { })
                .body(body)
                .dismiss(AppTheme.makeButtonSubtle(text("button.cancel")))
                .primary(AppTheme.makeButton(text("button.ok")))
                .build());
    }

    /** A log in the scroll pane the AI tab puts it in: a log sizes itself to its messages, not to its panel. */
    private static JComponent inScrollPane(HudLogArea log, int height) {
        JScrollPane scroll = AppTheme.hudApplicationScrollPane(log);
        scroll.setPreferredSize(new Dimension(0, height));
        return scroll;
    }

    /**
     * A tab pane cut down to its tab row and underline: the preview needs the tabs, not the pages under them.
     * WHY measured: the pane gives a page a minimum height whatever the page asks for, so only a laid-out pane says
     * where its tab row ends.
     */
    private static JComponent tabRowOnly(JTabbedPane tabs) {
        tabs.setSize(tabs.getPreferredSize());
        tabs.doLayout();
        int rowHeight = tabs.getComponentAt(0).getY() + HudPalette.HUD_BORDER_THICKNESS * 2;
        tabs.setPreferredSize(new Dimension(tabs.getPreferredSize().width, rowHeight));
        return tabs;
    }

    private static JPanel flowRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, GAP, 0));
        row.setOpaque(false);
        return row;
    }

    private static void addRow(JPanel panel, int row, JComponent component) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(row == 0 ? 0 : GAP, 0, 0, 0);
        panel.add(component, gbc);
    }

    /** Takes up the space left under the last row, so the rows start at the top rather than in the middle. */
    private static void addFiller(JPanel panel, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = row;
        gbc.weighty = 1;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        panel.add(filler, gbc);
    }

    private String text(String key) {
        return getText(language, key);
    }
}
