package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.FirstSetupPlan;
import elite.intel.bindforge.devicefiles.FirstSetupPlan.Option;
import elite.intel.bindforge.devicefiles.FirstSetupPlan.Row;
import elite.intel.bindforge.devicefiles.LabelMerge;
import elite.intel.bindforge.storage.ConfigurationFiles;
import elite.intel.bindforge.storage.FirstSetupFiles;
import elite.intel.bindforge.storage.FirstSetupReading;
import elite.intel.bindforge.storage.InstallationFiles;
import elite.intel.bindforge.storage.ReconcileRecord;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudTable;
import elite.intel.ui.screen.bindings.ReconcileWidgets.ChoiceCard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static elite.intel.bindforge.devicefiles.FirstSetupPlan.labelAnswerKey;
import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.screen.bindings.ReconcileWidgets.badge;
import static elite.intel.ui.screen.bindings.ReconcileWidgets.column;
import static elite.intel.ui.screen.bindings.ReconcileWidgets.line;
import static elite.intel.ui.screen.bindings.ReconcileWidgets.stack;
import static elite.intel.ui.screen.bindings.ReconcileWidgets.wrapped;
import static elite.intel.ui.theme.AppTheme.makeButton;
import static elite.intel.ui.theme.AppTheme.makeButtonSubtle;
import static elite.intel.ui.theme.AppTheme.transparentPanel;

/**
 * Making several copies of the game agree on the player's controllers (multi-install-reconcile.md): one plain
 * question at a time, about a controller they own, answered with big cards that say what each answer does; then the
 * result, before anything is written. File names never appear. It replaces the old reconcile grid for first-time
 * startup, the check and RESTORE.
 * <p>
 * <strong>Steps.</strong> First-time startup: Welcome, Plug in, Choose, Check. The check and RESTORE: Choose and Check -
 * the check opening on <em>which setup is current?</em> (Alan, 2026-11-18, Q3), RESTORE on the questions.
 * <p>
 * <strong>Nothing is chosen for the player</strong> (Alan, 2026-11-18, D6): Next waits until the question is answered.
 * <strong>Nothing is written here</strong>: the last button makes the working copy off the EDT and hands it back; the
 * caller writes it.
 */
final class ReconcileWizard extends JDialog {

    private static final Logger log = LogManager.getLogger(ReconcileWizard.class);
    private static final int WIDTH = 980;
    private static final int HEIGHT = 720;
    private static final int TEXT_WIDTH = 860;
    /** A table row tall enough for a controller's picture. */
    private static final int ICON_ROW_HEIGHT = ControllerIcons.TABLE + 14;
    /** Keeps the text of a row with no picture in line with the rows that have one. */
    private static final Icon EMPTY_ICON = new Icon() {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        @Override
        public int getIconWidth() {
            return ControllerIcons.TABLE;
        }

        @Override
        public int getIconHeight() {
            return ControllerIcons.TABLE;
        }
    };

    /**
     * The files the player settled on.
     *
     * @param files          the working copy to write
     * @param moved          how many bindings moved to a kept name
     * @param moves          each name dropped, by the name its bindings moved to
     * @param favoured       the copy "make everything like one copy" took, when the player used it and changed nothing
     *                       after - {@code null} otherwise
     * @param unpluggedInUse the controllers the bindings use that were not plugged in, by what they are called
     */
    record Choice(ConfigurationFiles files, int moved, Map<String, String> moves, Long favoured,
                  List<String> unpluggedInUse) {
        Choice {
            moves = Map.copyOf(moves);
            unpluggedInUse = List.copyOf(unpluggedInUse);
        }

        /** Files that needed no reconcile - identical installations, or a backup whose installations agree. */
        static Choice whole(ConfigurationFiles files) {
            return new Choice(files, 0, Map.of(), null, List.of());
        }
    }

    /**
     * Which flow the wizard serves, in its words.
     *
     * @param title          the dialog's title
     * @param introduce      whether to open with Welcome and Plug in - first-time startup only
     * @param lead           said above every question, or {@code null}
     * @param shortcutFirst  whether to open on "which setup is current?" - the check
     * @param finish         the last button
     * @param checkTrailer   said under the result, or {@code null}
     * @param purpose        which flow it is, in English, for BindForge's log ({@link ReconcileRecord})
     */
    record Mode(String title, boolean introduce, String lead, boolean shortcutFirst, String finish,
                String checkTrailer, String purpose) {

        static Mode firstSetup() {
            return new Mode(getText("bindings.reconcile.title"), true, null, false, null, null,
                    "first-time startup");
        }

        static Mode check(String lead) {
            return new Mode(getText("bindings.check.title"), false, lead, true,
                    getText("bindings.reconcile.check.useThis"), null, "the check");
        }

        static Mode restore(String trailer) {
            return new Mode(getText("bindings.restore.title"), false, getText("bindings.restore.reconcile.intro"),
                    false, getText("bindings.restore.confirm.button"), trailer, "RESTORE");
        }
    }

    private enum Page { WELCOME, PLUG_IN, QUESTION, SHORTCUT, CHECK }

    private final ReconcileQuestions questions;
    private final FirstSetupReading reading;
    private final Map<String, String> installNames;
    private final Mode mode;
    private final Long mineSide;
    private final LiveControllers live;
    private final Map<String, String> answers = new LinkedHashMap<>();
    /** Each button-names question answered by the button-by-button route, rather than by one copy's names. */
    private final Set<String> answeredByButton = new LinkedHashSet<>();

    private final JPanel page = transparentPanel(new BorderLayout());
    private final Stepper stepper;
    private final JButton dismiss = makeButtonSubtle(getText("bindings.restore.notNow"));
    private final JButton back = makeButtonSubtle(getText("bindings.reconcile.back"));
    private final JButton next = makeButton(getText("bindings.reconcile.next"));
    private final JButton shortcutLink;

    private Page current;
    /** How wide a card's text may be on the page being built - set by {@link #cardRow}. */
    private int cardTextWidth = 360;
    private Row question;
    private Page beforeShortcut;
    private Long favoured;
    private HardwareId lastPressed;
    private Choice choice;
    private boolean finishing;

    /**
     * @param installNames what each copy is called, keyed by its id as text - BindForge's own side included at the
     *                     check
     * @param mineSide     BindForge's own side's id, for the check; {@code null} otherwise
     */
    ReconcileWizard(Component parent, FirstSetupReading reading, Map<String, String> installNames, Mode mode,
                    Long mineSide) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.reading = reading;
        this.questions = new ReconcileQuestions(reading);
        this.installNames = installNames;
        this.mode = mode;
        this.mineSide = mineSide;
        this.live = LiveControllers.open();
        this.stepper = new Stepper(mode.introduce());
        ReconcileRecord.logReading(mode.purpose(), reading);
        this.shortcutLink = ReconcileWidgets.link(getText("bindings.reconcile.shortcut.link"));
        buildUi();
    }

    /**
     * Opens with {@code side} already chosen - the check's "use the game's files" for one installation: the result
     * shows first, after any question that copy cannot answer. On the EDT, before {@link #showDialog()}.
     */
    void favour(long side) {
        answers.clear();
        answeredByButton.clear();
        answers.putAll(reading.plan().answersFavouring(side));
        favoured = side;
        firstQuestionOrCheck();
    }

    /** @return the files settled on, or empty when the player left */
    Optional<Choice> showDialog() {
        try {
            AppTheme.runWithModalScrim(getOwner(), () -> setVisible(true));
        } finally {
            live.close();
        }
        return Optional.ofNullable(choice);
    }

    // ------------------------------------------------------------------ frame and navigation

    private void buildUi() {
        dismiss.addActionListener(e -> dispose());
        back.addActionListener(e -> goBack());
        next.addActionListener(e -> goNext());
        shortcutLink.addActionListener(e -> showShortcut());

        JPanel body = transparentPanel(new BorderLayout(0, HudPalette.HUD_GAP));
        body.add(stepper, BorderLayout.NORTH);
        body.add(page, BorderLayout.CENTER);
        body.add(safeLine(), BorderLayout.SOUTH);

        HudModalSpec spec = HudModalSpec.builder()
                .title(mode.title())
                .onClose(this::dispose)
                .body(body)
                .scrollBody(false)
                .middle(shortcutLink)
                .extra(back)
                .primary(next)
                .dismiss(dismiss)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));
        setGlassPane(clickBlocker());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(getOwner());
        live.listen(liveListener());
        if (mode.introduce()) show(Page.WELCOME);
        else if (mode.shortcutFirst() && questions.shortcutSides().size() > 1) showShortcut();
        else firstQuestionOrCheck();
    }

    /** "Nothing changes until the last step. Everything is backed up first." - under every step. */
    private JComponent safeLine() {
        JPanel panel = transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.add(line("✔ " + getText("bindings.reconcile.safe"), HudPalette.HUD_COLOR_ROLE_SUCCESS,
                HudPalette.HUD_FONT_SM, Font.PLAIN));
        return panel;
    }

    private void goNext() {
        switch (current) {
            case WELCOME -> show(Page.PLUG_IN);
            case PLUG_IN -> firstQuestionOrCheck();
            case QUESTION -> nextQuestionOrCheck(question);
            case SHORTCUT -> firstQuestionOrCheck();
            case CHECK -> finish();
        }
    }

    private void goBack() {
        switch (current) {
            case PLUG_IN -> show(Page.WELCOME);
            case QUESTION -> previousQuestion(question).ifPresentOrElse(this::showQuestion, () -> {
                if (mode.introduce()) show(Page.PLUG_IN);
                else if (mode.shortcutFirst()) showShortcut();
            });
            case SHORTCUT -> {
                if (beforeShortcut == Page.QUESTION && question != null) showQuestion(question);
                else if (beforeShortcut != null && beforeShortcut != Page.SHORTCUT) show(beforeShortcut);
            }
            case CHECK -> previousQuestion(null).ifPresentOrElse(this::showQuestion, () -> {
                if (mode.introduce()) show(Page.PLUG_IN);
                else if (mode.shortcutFirst()) showShortcut();
            });
            default -> {
            }
        }
    }

    private void firstQuestionOrCheck() {
        nextQuestionOrCheck(null);
    }

    /** The next question still needed after {@code after} - or the first, for {@code null} - else the result. */
    private void nextQuestionOrCheck(Row after) {
        List<Row> all = questions.questions();
        int from = after == null ? 0 : all.indexOf(after) + 1;
        for (int i = from; i < all.size(); i++) {
            if (questions.needed(all.get(i), answers) && !questions.isAnswered(all.get(i), answers)) {
                showQuestion(all.get(i));
                return;
            }
        }
        // WHY: a question answered before but left needing another look - two controllers on one VID/PID - is sent
        // back to, with what is wrong said on it.
        Optional<Row> clash = questions.clash(answers);
        if (clash.isPresent()) {
            showQuestion(clash.get());
            return;
        }
        for (Row row : all) {
            if (questions.needed(row, answers) && !questions.isAnswered(row, answers)) {
                showQuestion(row);
                return;
            }
        }
        show(Page.CHECK);
    }

    private Optional<Row> previousQuestion(Row before) {
        List<Row> all = questions.questions();
        int from = before == null ? all.size() - 1 : all.indexOf(before) - 1;
        for (int i = from; i >= 0; i--) {
            if (questions.needed(all.get(i), answers)) return Optional.of(all.get(i));
        }
        return Optional.empty();
    }

    /** The questions still in play, for "Question 2 of 3". */
    private List<Row> neededQuestions() {
        return questions.questions().stream().filter(row -> questions.needed(row, answers)).toList();
    }

    private void showQuestion(Row row) {
        question = row;
        show(Page.QUESTION);
    }

    private void showShortcut() {
        if (current != Page.SHORTCUT) beforeShortcut = current;
        show(Page.SHORTCUT);
    }

    private void show(Page target) {
        if (target == null) return;
        current = target;
        JComponent content = switch (target) {
            case WELCOME -> welcome();
            case PLUG_IN -> plugIn();
            case QUESTION -> questionPage(question);
            case SHORTCUT -> shortcut();
            case CHECK -> check();
        };
        page.removeAll();
        JScrollPane scroll = AppTheme.hudScrollPane(ReconcileWidgets.scrollablePage(content));
        scroll.getViewport().setBackground(HudPalette.HUD_COLOR_ROLE_DIALOG_BODY_BACKGROUND);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.putClientProperty(AppTheme.HUD_SCROLL_STYLE_LOCKED, Boolean.TRUE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        page.add(scroll, BorderLayout.CENTER);
        page.revalidate();
        page.repaint();
        updateFooter();
    }

    private void updateFooter() {
        stepper.at(switch (current) {
            case WELCOME -> -1;
            case PLUG_IN -> 0;
            case QUESTION, SHORTCUT -> 1;
            case CHECK -> 2;
        });
        dismiss.setText(getText(current == Page.WELCOME ? "bindings.restore.notNow" : "button.cancel"));
        back.setVisible(current != Page.WELCOME && hasBack());
        shortcutLink.setVisible(current == Page.QUESTION && questions.shortcutSides().size() > 1);
        next.setText(switch (current) {
            case WELCOME -> getText("bindings.reconcile.welcome.start");
            case PLUG_IN -> getText("bindings.reconcile.next");
            case QUESTION -> getText(isLastQuestion() ? "bindings.reconcile.toCheck" : "bindings.reconcile.nextQuestion");
            case SHORTCUT -> getText("bindings.reconcile.toCheck");
            case CHECK -> mode.finish() != null ? mode.finish()
                    : getText("bindings.reconcile.finish", reading.installations().size());
        });
        next.setEnabled(switch (current) {
            case QUESTION -> questions.isAnswered(question, answers) && !clashes(question);
            case SHORTCUT -> favoured != null;
            default -> true;
        });
        getRootPane().setDefaultButton(next);
    }

    private boolean hasBack() {
        return switch (current) {
            case PLUG_IN -> true;
            case QUESTION -> previousQuestion(question).isPresent() || mode.introduce() || mode.shortcutFirst();
            case SHORTCUT -> beforeShortcut != null && beforeShortcut != Page.SHORTCUT;
            case CHECK -> previousQuestion(null).isPresent() || mode.introduce() || mode.shortcutFirst();
            default -> false;
        };
    }

    private boolean isLastQuestion() {
        List<Row> all = questions.questions();
        for (int i = all.indexOf(question) + 1; i < all.size(); i++) {
            if (questions.needed(all.get(i), answers)) return false;
        }
        return true;
    }

    private boolean clashes(Row row) {
        return questions.clash(answers).map(clash -> clash.id().equals(row.id())).orElse(false);
    }

    /** An answer changed by hand: the shortcut no longer describes the result. */
    private void answered() {
        favoured = null;
        updateFooter();
    }

    // ------------------------------------------------------------------ 01 Welcome

    private JComponent welcome() {
        JPanel column = page(16);
        stack(column, headline(getText("bindings.reconcile.welcome.headline", reading.installations().size())));
        stack(column, wrapped(getText("bindings.reconcile.welcome.body", copiesList()),
                HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT, HudPalette.HUD_FONT_MD, TEXT_WIDTH));
        stack(column, diagram());
        JPanel counts = transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        counts.add(count(neededQuestions().size(), "bindings.reconcile.welcome.questions",
                HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION));
        counts.add(Box.createHorizontalStrut(12));
        counts.add(count(questions.settledAlone(), "bindings.reconcile.welcome.settled",
                HudPalette.HUD_COLOR_ROLE_SUCCESS));
        counts.add(Box.createHorizontalStrut(12));
        counts.add(count(questions.alreadyMatching(), "bindings.reconcile.welcome.matching",
                HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT));
        stack(column, counts);
        stack(column, note(getText("bindings.reconcile.welcome.after"), HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION));
        unreachableNote().ifPresent(note -> stack(column, note));
        return column;
    }

    /** The copies, as "Steam and Epic". */
    private String copiesList() {
        List<String> names = reading.installations().stream().map(files -> nameOf(files.installId())).toList();
        if (names.size() <= 1) return String.join("", names);
        return String.join(", ", names.subList(0, names.size() - 1)) + " "
                + getText("bindings.reconcile.and") + " " + names.getLast();
    }

    /** Each copy with its own list of controllers, all wired to one set of controls in the middle. */
    private JComponent diagram() {
        List<InstallationFiles> copies = reading.installations();
        JPanel row = transparentPanel(new GridBagLayout());
        GridBagConstraints at = new GridBagConstraints();
        at.fill = GridBagConstraints.BOTH;
        at.weightx = 1;
        at.insets = new Insets(0, 0, 0, 0);
        int half = (copies.size() + 1) / 2;
        at.gridx = 0;
        row.add(copyStack(copies.subList(0, half)), at);
        at.gridx = 1;
        at.weightx = 0.9;
        at.insets = new Insets(0, 18, 0, 18);
        JPanel controls = transparentPanel(new GridBagLayout());
        controls.setBorder(BorderFactory.createLineBorder(HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION, 2));
        JPanel text = column(4);
        stack(text, line(getText("bindings.reconcile.welcome.controls"), HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT,
                HudPalette.HUD_FONT_MD, Font.BOLD));
        stack(text, line(getText("bindings.reconcile.welcome.controlsShared"), HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT,
                HudPalette.HUD_FONT_SM, Font.PLAIN));
        controls.add(text);
        row.add(controls, at);
        at.gridx = 2;
        at.weightx = 1;
        at.insets = new Insets(0, 0, 0, 0);
        row.add(copyStack(copies.subList(half, copies.size())), at);
        row.setPreferredSize(new Dimension(TEXT_WIDTH, Math.max(130, half * 130)));
        return row;
    }

    private JComponent copyStack(List<InstallationFiles> copies) {
        JPanel stack = transparentPanel(new GridLayout(0, 1, 0, 8));
        for (InstallationFiles copy : copies) {
            JPanel box = column(4);
            box.setOpaque(true);
            box.setBackground(HudPalette.HUD_COLOR_ROLE_PANEL_BACKGROUND);
            box.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(HudPalette.HUD_COLOR_ROLE_FRAME_BORDER),
                    new EmptyBorder(10, 12, 10, 12)));
            stack(box, badge(nameOf(copy.installId()), false));
            stack(box, line(getText("bindings.reconcile.welcome.ownList"), HudPalette.HUD_COLOR_ROLE_READOUT_LABEL,
                    HudPalette.HUD_FONT_SM, Font.PLAIN));
            List<String> aliases = aliasesOf(copy.installId());
            String shown = aliases.stream().limit(4).collect(Collectors.joining(" · "))
                    + (aliases.size() > 4 ? " …" : "");
            stack(box, ReconcileWidgets.alias(shown.isEmpty() ? "—" : shown,
                    HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT, HudPalette.HUD_FONT_SM));
            stack.add(box);
        }
        return stack;
    }

    private List<String> aliasesOf(long installId) {
        return reading.plan().installations().stream().filter(install -> install.installId() == installId)
                .flatMap(install -> install.elements().keySet().stream()).toList();
    }

    private JComponent count(int value, String key, Color color) {
        JPanel pill = transparentPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        pill.setOpaque(true);
        pill.setBackground(HudPalette.HUD_COLOR_ROLE_PANEL_BACKGROUND);
        pill.setBorder(BorderFactory.createLineBorder(HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER));
        pill.add(line(String.valueOf(value), color, HudPalette.HUD_FONT_LG, Font.BOLD));
        pill.add(line(getText(key, value), HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT, HudPalette.HUD_FONT_SM,
                Font.PLAIN));
        return pill;
    }

    private Optional<JComponent> unreachableNote() {
        if (reading.unreachable().isEmpty()) return Optional.empty();
        String names = reading.unreachable().stream().map(row -> nameOf(row.id(), row.storefront()))
                .collect(Collectors.joining(", "));
        return Optional.of(note(getText("bindings.reconcile.unreachable", names), HudPalette.HUD_COLOR_ROLE_WARNING));
    }

    // ------------------------------------------------------------------ 02 Plug in

    private JComponent plugIn() {
        JPanel column = page(14);
        stack(column, headline(getText("bindings.reconcile.plugIn.headline")));
        stack(column, wrapped(getText("bindings.reconcile.plugIn.body"), HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT,
                HudPalette.HUD_FONT_MD, TEXT_WIDTH));
        if (!live.available()) {
            stack(column, note(getText("bindings.reconcile.plugIn.cannotTell"), HudPalette.HUD_COLOR_ROLE_WARNING));
        }
        List<ReconcileQuestions.Controller> controllers = questions.controllers();
        DefaultTableModel model = new DefaultTableModel(new Object[]{
                getText("bindings.reconcile.plugIn.column.controller"),
                getText("bindings.reconcile.plugIn.column.names"),
                getText("bindings.reconcile.plugIn.column.knownTo"),
                getText("bindings.reconcile.plugIn.column.now")}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        int pressedRow = -1;
        List<Icon> icons = new ArrayList<>();
        for (ReconcileQuestions.Controller controller : controllers) {
            icons.add(ControllerIcons.icon(kindOf(controller.hardware(), controller.aliases()), ControllerIcons.TABLE));
            boolean on = live.isConnected(controller.hardware());
            boolean pressed = on && controller.hardware().equals(lastPressed);
            if (pressed) pressedRow = model.getRowCount();
            model.addRow(new Object[]{
                    controllerName(controller.hardware(), controller.aliases()),
                    String.join(" / ", controller.aliases()),
                    controller.installs().stream().map(this::nameOf).sorted().collect(Collectors.joining(", ")),
                    connectionText(on) + (pressed ? "   ← " + getText("bindings.reconcile.plugIn.pressed") : "")});
        }
        JTable table = new JTable(model);
        HudTable.style(table);
        table.putClientProperty(AppTheme.HUD_TABLE_STYLE_LOCKED, Boolean.TRUE);
        table.setDefaultRenderer(Object.class, new ConnectionRenderer(3, icons));
        table.setRowHeight(ICON_ROW_HEIGHT);
        table.setRowSelectionAllowed(true);
        if (pressedRow >= 0) table.setRowSelectionInterval(pressedRow, pressedRow);
        HudTable.fitColumnsToContent(table, 320);
        JScrollPane scroll = HudTable.dataPlaneScrollPane(table);
        scroll.setPreferredSize(new Dimension(TEXT_WIDTH, Math.min(420,
                HudPalette.HUD_TABLE_HEADER_HEIGHT + controllers.size() * ICON_ROW_HEIGHT + 8)));
        stack(column, scroll);
        stack(column, wrapped(getText("bindings.reconcile.plugIn.tip"), HudPalette.HUD_COLOR_ROLE_READOUT_LABEL,
                HudPalette.HUD_FONT_SM, TEXT_WIDTH));
        return column;
    }

    /**
     * Plugged-in state as the cell's text colour, never a fill - the HUD canon's rule for state - and each row's
     * controller picture in the first column.
     */
    private final class ConnectionRenderer extends HudTable.CellRenderer {
        private final int stateColumn;
        private final List<Icon> icons;

        /** @param icons the picture for each row, in row order */
        ConnectionRenderer(int stateColumn, List<Icon> icons) {
            this.stateColumn = stateColumn;
            this.icons = List.copyOf(icons.stream().map(icon -> icon == null ? EMPTY_ICON : icon).toList());
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setIcon(column == 0 && row < icons.size() ? icons.get(row) : null);
            setIconTextGap(12);
            if (column == stateColumn && !selected) {
                boolean on = live.available() && String.valueOf(value).startsWith(connectionText(true));
                cell.setForeground(on ? HudPalette.HUD_COLOR_ROLE_SUCCESS : HudPalette.HUD_COLOR_ROLE_READOUT_LABEL);
            }
            return cell;
        }
    }

    // ------------------------------------------------------------------ 03-05 Questions

    private JComponent questionPage(Row row) {
        JPanel column = page(12);
        if (mode.lead() != null) {
            stack(column, note(mode.lead(), HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION));
        }
        List<Row> needed = neededQuestions();
        stack(column, line(getText("bindings.reconcile.question.count", needed.indexOf(row) + 1, needed.size())
                .toUpperCase(), HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION, HudPalette.HUD_FONT_SM, Font.BOLD));
        switch (ReconcileQuestions.typeOf(row)) {
            case NAME -> nameQuestion(column, row);
            case CONTROLLER -> controllerQuestion(column, row);
            case BUTTON_NAMES -> buttonsQuestion(column, row);
        }
        return column;
    }

    /** One controller, two names: pick the name (03). The bindings follow the name picked. */
    private void nameQuestion(JPanel column, Row row) {
        HardwareId hardware = questions.hardwareOf(row).orElseThrow();
        List<String> names = row.options().stream().map(Option::key).toList();
        stack(column, headline(getText("bindings.reconcile.name.headline", controllerName(hardware, names))));
        stack(column, controllerLine(hardware, names, getText("bindings.reconcile.name.sub")));

        ButtonGroup group = new ButtonGroup();
        JPanel cards = cardRow(row.options().size());
        for (Option option : row.options()) {
            ChoiceCard card = new ChoiceCard();
            card.with(from(option.installs(), "bindings.reconcile.name.calls"));
            card.with(ReconcileWidgets.alias(option.key(), HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT,
                    HudPalette.HUD_FONT_LG + 4));
            int uses = questions.uses(option.key());
            card.with(consequence(uses > 0 ? getText("bindings.reconcile.name.uses", uses)
                    : getText("bindings.reconcile.name.unused"), uses > 0 ? Mark.OK : Mark.INFO));
            for (Option other : row.options()) {
                if (other == option) continue;
                int moving = questions.uses(other.key());
                if (moving > 0) {
                    card.with(consequence(getText("bindings.reconcile.name.moves", moving, other.key()), Mark.INFO));
                }
            }
            answerWith(card, group, row, option.key());
            cards.add(card);
        }
        stack(column, cards);
        boolean caseOnly = names.stream().map(String::toLowerCase).distinct().count() == 1;
        stack(column, note(getText(caseOnly ? "bindings.reconcile.name.noteCase" : "bindings.reconcile.name.note"),
                HudPalette.HUD_COLOR_ROLE_SUCCESS));
    }

    /** One name, two controllers: which one is it (04). The bindings stay on the name. */
    private void controllerQuestion(JPanel column, Row row) {
        String name = row.deviceName();
        int uses = questions.uses(name);
        stack(column, headline(getText("bindings.reconcile.controller.headline", name)));
        stack(column, wrapped(uses > 0 ? getText("bindings.reconcile.controller.sub", name, uses)
                        : getText("bindings.reconcile.controller.subUnused", name),
                HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT, HudPalette.HUD_FONT_MD, TEXT_WIDTH));

        ButtonGroup group = new ButtonGroup();
        JPanel cards = cardRow(row.options().size());
        for (Option option : row.options()) {
            HardwareId hardware = FirstSetupPlan.hardwareOf(option);
            boolean on = live.isConnected(hardware);
            ChoiceCard card = new ChoiceCard();
            card.with(from(option.installs(), "bindings.reconcile.controller.uses"));
            JPanel identity = transparentPanel(new BorderLayout(12, 0));
            identity.add(ControllerIcons.boxed(kindOf(hardware, List.of(name))), BorderLayout.WEST);
            JPanel named = column(2);
            stack(named, line(controllerName(hardware, List.of()), HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT,
                    HudPalette.HUD_FONT_MD, Font.BOLD));
            stack(named, connection(on, hardware));
            identity.add(named, BorderLayout.CENTER);
            card.with(identity);
            if (uses > 0) card.with(consequence(getText("bindings.reconcile.controller.works", name, uses), Mark.OK));
            if (!on && live.available()) {
                card.with(consequence(getText("bindings.reconcile.controller.unplugged"), Mark.WARN));
            }
            answerWith(card, group, row, option.key());
            if (hardware.equals(lastPressed)) card.requestFocusInWindow();
            cards.add(card);
        }
        stack(column, cards);
        if (clashes(row)) {
            stack(column, note(getText("bindings.reconcile.controller.clash"), HudPalette.HUD_COLOR_ROLE_WARNING));
        }
        stack(column, note(getText("bindings.reconcile.controller.note", name), HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION));
    }

    /** A controller's buttons named differently: one copy's names, or button by button (05). */
    private void buttonsQuestion(JPanel column, Row row) {
        String device = row.deviceName();
        List<String> aliases = reading.plan().namesOfLabelDevice(device);
        HardwareId hardware = questions.hardwareOf(row).orElse(null);
        String controller = hardware == null ? String.join(" / ", aliases) : controllerName(hardware, aliases);
        stack(column, headline(getText("bindings.reconcile.buttons.headline", controller)));
        stack(column, controllerLine(hardware, aliases, getText("bindings.reconcile.buttons.sub")));

        LabelMerge merge = reading.plan().labelMerge(device);
        ButtonGroup group = new ButtonGroup();
        JPanel cards = cardRow(merge.sides().size() + 1);
        for (String side : merge.sides()) {
            long installId = Long.parseLong(side);
            ChoiceCard card = new ChoiceCard();
            card.with(from(Set.of(installId), null));
            card.with(line(getText("bindings.reconcile.buttons.useCopy", nameOf(installId)),
                    HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT, HudPalette.HUD_FONT_MD, Font.BOLD));
            String examples = merge.collisions().stream().map(collision -> collision.bySide().get(side))
                    .filter(label -> label != null).limit(3).collect(Collectors.joining(", "));
            if (!examples.isEmpty()) {
                card.with(consequence(getText("bindings.reconcile.buttons.examples", examples), Mark.INFO));
            }
            Map<String, String> favouring = reading.plan().labelAnswersFavouring(device, installId);
            card.setSelected(!answeredByButton.contains(row.id()) && questions.isAnswered(row, answers)
                    && favouring.entrySet().stream().allMatch(e -> e.getValue().equals(answers.get(e.getKey()))));
            group.add(card);
            card.addActionListener(e -> {
                answeredByButton.remove(row.id());
                questions.keysOf(row).forEach(answers::remove);
                answers.putAll(favouring);
                answered();
            });
            cards.add(card);
        }
        ChoiceCard each = new ChoiceCard();
        each.with(line(getText("bindings.reconcile.buttons.each"), HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT,
                HudPalette.HUD_FONT_MD, Font.BOLD));
        each.with(consequence(getText("bindings.reconcile.buttons.chosen", questions.buttonsAnswered(row, answers),
                merge.collisions().size()), questions.isAnswered(row, answers) ? Mark.OK : Mark.INFO));
        JButton open = makeButtonSubtle(getText("bindings.reconcile.buttons.open"));
        open.addActionListener(e -> openButtonByButton(row, hardware));
        each.with(open);
        each.setSelected(answeredByButton.contains(row.id()));
        group.add(each);
        each.addActionListener(e -> openButtonByButton(row, hardware));
        cards.add(each);
        stack(column, cards);

        int onlyOne = onlyOneCopyNames(merge);
        stack(column, note(getText("bindings.reconcile.buttons.note", onlyOne), HudPalette.HUD_COLOR_ROLE_SUCCESS));
    }

    /** How many inputs only one copy names - kept whichever way the question is answered. */
    private static int onlyOneCopyNames(LabelMerge merge) {
        Map<String, Integer> holders = new LinkedHashMap<>();
        merge.sides().forEach(side -> merge.labelsOf(side).keySet()
                .forEach(input -> holders.merge(input, 1, Integer::sum)));
        return (int) holders.values().stream().filter(count -> count == 1).count();
    }

    private void openButtonByButton(Row row, HardwareId hardware) {
        String device = row.deviceName();
        LabelMerge merge = reading.plan().labelMerge(device);
        Map<String, String> sideNames = new LinkedHashMap<>();
        merge.sides().forEach(side -> sideNames.put(side, nameOf(Long.parseLong(side))));
        String title = hardware == null ? device : controllerName(hardware, reading.plan().namesOfLabelDevice(device));
        Optional<Map<String, String>> chosen = new LabelMergeDialog(this, title, merge, sideNames,
                reading.plan().labelAnswers(device, answers), live, hardware).showDialog();
        live.listen(liveListener());
        chosen.ifPresent(labels -> {
            questions.keysOf(row).forEach(answers::remove);
            labels.forEach((input, label) -> answers.put(labelAnswerKey(device, input), label));
            answeredByButton.add(row.id());
            answered();
        });
        show(Page.QUESTION);
    }

    private LiveControllers.Listener liveListener() {
        return new LiveControllers.Listener() {
            @Override
            public void changed() {
                show(current);
            }

            @Override
            public void pressed(HardwareId hardware, String token) {
                lastPressed = hardware;
                if (current == Page.PLUG_IN || current == Page.QUESTION) show(current);
            }
        };
    }

    /** A card answers {@code row} with {@code key} when clicked; it starts selected only if that is the answer now. */
    private void answerWith(ChoiceCard card, ButtonGroup group, Row row, String key) {
        group.add(card);
        card.setSelected(key.equals(answers.get(row.id())));
        card.addActionListener(e -> {
            answers.put(row.id(), key);
            answered();
            // WHY: answering can make a later question moot or bring a clash back, which the page says.
            show(Page.QUESTION);
        });
    }

    // ------------------------------------------------------------------ 09 Shortcut

    /** "Make everything like one copy" - and, at the check, "which setup is current?" (Q3). */
    private JComponent shortcut() {
        JPanel column = page(12);
        boolean atCheck = mode.shortcutFirst();
        if (atCheck && mode.lead() != null) stack(column, note(mode.lead(), HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION));
        stack(column, headline(getText(atCheck ? "bindings.reconcile.current.headline"
                : "bindings.reconcile.shortcut.headline")));
        stack(column, wrapped(getText(atCheck ? "bindings.reconcile.current.sub" : "bindings.reconcile.shortcut.sub"),
                HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT, HudPalette.HUD_FONT_MD, TEXT_WIDTH));

        ButtonGroup group = new ButtonGroup();
        List<Long> sides = questions.shortcutSides();
        JPanel cards = cardRow(sides.size());
        for (long side : sides) {
            boolean mine = mineSide != null && mineSide == side;
            ChoiceCard card = new ChoiceCard();
            card.with(badge(nameOf(side), mine));
            card.with(line(getText(mine ? "bindings.reconcile.current.mine" : "bindings.reconcile.shortcut.like",
                    nameOf(side)), HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT, HudPalette.HUD_FONT_MD, Font.BOLD));
            shortcutLines(side).forEach(text -> card.with(consequence(text, Mark.OK)));
            group.add(card);
            card.setSelected(favoured != null && favoured == side);
            card.addActionListener(e -> {
                answers.clear();
                answeredByButton.clear();
                answers.putAll(reading.plan().answersFavouring(side));
                favoured = side;
                updateFooter();
            });
            cards.add(card);
        }
        stack(column, cards);
        stack(column, note(getText("bindings.reconcile.shortcut.note"), HudPalette.HUD_COLOR_ROLE_SUCCESS));
        if (atCheck && !questions.questions().isEmpty()) {
            JButton oneByOne = ReconcileWidgets.link(getText("bindings.reconcile.current.oneByOne"));
            oneByOne.addActionListener(e -> {
                favoured = null;
                firstQuestionOrCheck();
            });
            stack(column, oneByOne);
        }
        return column;
    }

    /** What favouring one side decides, in words - the first few, then how many more. */
    private List<String> shortcutLines(long side) {
        Map<String, String> favouring = reading.plan().answersFavouring(side);
        List<String> lines = new ArrayList<>();
        for (Row row : questions.questions()) {
            switch (row.kind()) {
                case NAME_DIFFERS -> {
                    String name = favouring.get(row.id());
                    if (name == null) continue;
                    HardwareId hardware = reading.plan().hardwareOf(row);
                    lines.add(getText("bindings.reconcile.shortcut.calls",
                            controllerName(hardware, row.options().stream().map(Option::key).toList()), name));
                }
                case HARDWARE_DIFFERS -> {
                    String key = favouring.get(row.id());
                    if (key == null) continue;
                    HardwareId hardware = row.options().stream().filter(option -> option.key().equals(key))
                            .map(FirstSetupPlan::hardwareOf).findFirst().orElse(null);
                    if (hardware == null) continue;
                    lines.add(getText("bindings.reconcile.shortcut.means", row.deviceName(),
                            controllerName(hardware, List.of())));
                }
                case LABELS_DIFFER -> {
                    if (!reading.plan().labelMerge(row.deviceName()).sides().contains(String.valueOf(side))) continue;
                    HardwareId hardware = questions.hardwareOf(row).orElse(null);
                    List<String> aliases = reading.plan().namesOfLabelDevice(row.deviceName());
                    lines.add(getText("bindings.reconcile.shortcut.buttons", nameOf(side),
                            hardware == null ? String.join(" / ", aliases) : controllerName(hardware, aliases)));
                }
                default -> {
                }
            }
        }
        if (lines.size() > 4) {
            int more = lines.size() - 3;
            List<String> shown = new ArrayList<>(lines.subList(0, 3));
            shown.add(getText("bindings.reconcile.shortcut.more", more));
            return shown;
        }
        return lines;
    }

    // ------------------------------------------------------------------ 07 Check

    private JComponent check() {
        JPanel column = page(12);
        int copies = reading.installations().size();
        stack(column, headline(getText("bindings.reconcile.check.headline", copies)));
        stack(column, wrapped(getText("bindings.reconcile.check.sub"), HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT,
                HudPalette.HUD_FONT_MD, TEXT_WIDTH));

        FirstSetupPlan.Resolution resolution = reading.plan().resolve(answers);
        List<ReconcileQuestions.Outcome> outcomes = questions.outcomes(resolution, answers);
        DefaultTableModel model = new DefaultTableModel(new Object[]{
                getText("bindings.reconcile.plugIn.column.controller"),
                getText("bindings.reconcile.check.column.name"),
                getText("bindings.reconcile.check.column.buttons"),
                getText("bindings.reconcile.check.column.why"),
                getText("bindings.reconcile.plugIn.column.now")}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        List<Icon> icons = new ArrayList<>();
        for (ReconcileQuestions.Outcome outcome : outcomes) {
            FirstSetupPlan.Kept kept = outcome.kept();
            icons.add(ControllerIcons.icon(kindOf(kept.hardware(), List.of(kept.name())), ControllerIcons.TABLE));
            model.addRow(new Object[]{
                    controllerName(kept.hardware(), List.of(kept.name())),
                    kept.name(),
                    kept.labels().isEmpty() ? "—" : getText("bindings.reconcile.check.named", kept.labels().size()),
                    why(outcome),
                    connectionText(live.isConnected(kept.hardware()))});
        }
        JTable table = new JTable(model);
        HudTable.style(table);
        table.putClientProperty(AppTheme.HUD_TABLE_STYLE_LOCKED, Boolean.TRUE);
        table.setDefaultRenderer(Object.class, new ConnectionRenderer(4, icons));
        table.setRowHeight(ICON_ROW_HEIGHT);
        HudTable.fitColumnsToContent(table, 300);
        JScrollPane scroll = HudTable.dataPlaneScrollPane(table);
        scroll.setPreferredSize(new Dimension(TEXT_WIDTH, Math.min(340,
                HudPalette.HUD_TABLE_HEADER_HEIGHT + outcomes.size() * ICON_ROW_HEIGHT + 8)));
        stack(column, scroll);

        List<ReconcileQuestions.Tidied> tidied = questions.tidied(resolution, answers);
        if (!tidied.isEmpty()) {
            stack(column, line(getText("bindings.reconcile.check.tidied").toUpperCase(),
                    HudPalette.HUD_COLOR_ROLE_READOUT_LABEL, HudPalette.HUD_FONT_SM, Font.BOLD));
            for (ReconcileQuestions.Tidied one : tidied) {
                String text = one.dead()
                        ? one.movedTo() != null ? getText("bindings.reconcile.check.deadMoved", one.name(), one.movedTo())
                        : getText("bindings.reconcile.check.dead", one.name())
                        : getText("bindings.reconcile.check.lostName", controllerName(one.hardware(), List.of()),
                        one.name());
                stack(column, wrapped("• " + text, HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT, HudPalette.HUD_FONT_SM,
                        TEXT_WIDTH));
            }
        }
        List<FirstSetupPlan.Kept> unplugged = questions.unpluggedInUse(resolution, live.connected());
        if (live.available() && !unplugged.isEmpty()) {
            stack(column, note(getText("bindings.reconcile.check.unplugged", unplugged.size(),
                    unplugged.stream().map(FirstSetupPlan.Kept::name).collect(Collectors.joining(", "))),
                    HudPalette.HUD_COLOR_ROLE_WARNING));
        }
        if (mode.checkTrailer() != null) {
            stack(column, wrapped(mode.checkTrailer(), HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT, HudPalette.HUD_FONT_SM,
                    TEXT_WIDTH));
        }
        return column;
    }

    private String why(ReconcileQuestions.Outcome outcome) {
        String reason = switch (outcome.reason()) {
            case CHOSEN -> getText("bindings.reconcile.check.why.chosen");
            case ADDED -> getText("bindings.reconcile.check.why.added",
                    outcome.onlyIn().stream().map(this::nameOf).sorted().collect(Collectors.joining(", ")));
            case SAME -> getText("bindings.reconcile.check.why.same");
        };
        if (outcome.movedHere() > 0) {
            reason += " " + getText("bindings.reconcile.check.why.moved", outcome.movedHere());
        }
        return reason;
    }

    /** The last button: the working copy made off the EDT, then handed back. */
    private void finish() {
        if (finishing) return;
        finishing = true;
        setBusy(true);
        Map<String, String> given = Map.copyOf(answers);
        Long favouredSide = favoured;
        FirstSetupPlan.Resolution resolution = reading.plan().resolve(given);
        List<String> unplugged = live.available()
                ? questions.unpluggedInUse(resolution, live.connected()).stream()
                .map(kept -> controllerName(kept.hardware(), List.of(kept.name()))).toList()
                : List.of();
        // WHY: the merge parses and rewrites every device file and binds file, so it runs off the EDT, with the
        // dialog greyed until it is done.
        FlowSteps.inBackground("BindForge-Reconcile", log, "The reconcile's merge of the installations", () -> {
                    FirstSetupFiles.Merged merged = FirstSetupFiles.merge(reading, given);
                    ReconcileRecord.logDecision(mode.purpose(), reading, given, favouredSide, merged);
                    return () -> finished(new Choice(merged.files(), merged.bindingsMoved(), merged.moves(),
                            favouredSide, unplugged), null);
                },
                reason -> () -> finished(null, getText("bindings.firstTimeStartup.mergeFailed", reason)),
                reason -> () -> finished(null, getText("bindings.firstTimeStartup.stopped", reason)),
                FlowSteps.NOTHING);
    }

    private void finished(Choice made, String failure) {
        finishing = false;
        if (!isDisplayable()) return;
        setBusy(false);
        if (failure != null) {
            HudConfirmDialog.info(this, mode.title(), failure, getText("button.close"));
            return;
        }
        choice = made;
        dispose();
    }

    // ------------------------------------------------------------------ shared pieces

    private JPanel page(int gap) {
        JPanel column = column(gap);
        column.setBorder(new EmptyBorder(8, 4, 8, 4));
        return column;
    }

    private static JComponent headline(String text) {
        return wrapped(text, HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT, HudPalette.HUD_FONT_LG + 6, TEXT_WIDTH);
    }

    /** A note: a bar of {@code accent} on the left, the text on the panel colour. */
    private static JComponent note(String text, Color accent) {
        JPanel panel = transparentPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(HudPalette.HUD_COLOR_ROLE_PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
                new EmptyBorder(10, 14, 10, 14)));
        panel.add(wrapped(text, HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT, HudPalette.HUD_FONT_SM, TEXT_WIDTH - 40),
                BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    private enum Mark { OK, INFO, WARN }

    private JComponent consequence(String text, Mark mark) {
        JPanel row = transparentPanel(new BorderLayout(8, 0));
        String glyph = switch (mark) {
            case OK -> "✓";
            case INFO -> "•";
            case WARN -> "!";
        };
        Color color = switch (mark) {
            case OK -> HudPalette.HUD_COLOR_ROLE_SUCCESS;
            case INFO -> HudPalette.HUD_COLOR_ROLE_READOUT_LABEL;
            case WARN -> HudPalette.HUD_COLOR_ROLE_WARNING;
        };
        JLabel sign = line(glyph, color, HudPalette.HUD_FONT_SM, Font.BOLD);
        sign.setVerticalAlignment(SwingConstants.TOP);
        row.add(sign, BorderLayout.WEST);
        row.add(wrapped(text, mark == Mark.WARN ? color : HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT,
                HudPalette.HUD_FONT_SM, cardTextWidth - 20), BorderLayout.CENTER);
        return row;
    }

    /** Where a card's answer comes from: the copies' badges, then {@code key}'s words. */
    private JComponent from(Collection<Long> installs, String key) {
        JPanel row = transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        for (long install : installs.stream().sorted().toList()) {
            row.add(badge(nameOf(install), mineSide != null && mineSide == install));
            row.add(Box.createHorizontalStrut(6));
        }
        if (key != null) {
            row.add(line(getText(key), HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT, HudPalette.HUD_FONT_SM, Font.PLAIN));
        }
        return row;
    }

    /** The cards of one question, side by side - up to three; from four, one under another. */
    private JPanel cardRow(int cards) {
        int across = cards <= 3 ? Math.max(1, cards) : 1;
        cardTextWidth = (TEXT_WIDTH - 16 * (across - 1)) / across - 80;
        return transparentPanel(cards <= 3 ? new GridLayout(1, cards, 16, 0) : new GridLayout(0, 1, 0, 10));
    }

    /** A controller's product name and plugged-in state, with a line under it. */
    private JComponent controllerLine(HardwareId hardware, List<String> aliases, String sub) {
        JPanel line = transparentPanel(new BorderLayout(14, 0));
        line.add(ControllerIcons.boxed(kindOf(hardware, aliases)), BorderLayout.WEST);
        JPanel row = column(2);
        JPanel top = transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        top.add(line(hardware == null ? String.join(" / ", aliases) : controllerName(hardware, aliases),
                HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT, HudPalette.HUD_FONT_MD, Font.BOLD));
        if (hardware != null) {
            top.add(Box.createHorizontalStrut(14));
            top.add(connection(live.isConnected(hardware), hardware));
        }
        stack(row, top);
        stack(row, wrapped(sub, HudPalette.HUD_COLOR_ROLE_READOUT_LABEL, HudPalette.HUD_FONT_SM,
                TEXT_WIDTH - ControllerIcons.BOX - 14));
        line.add(row, BorderLayout.CENTER);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.max(ControllerIcons.BOX,
                row.getPreferredSize().height)));
        return line;
    }

    private JComponent connection(boolean on, HardwareId hardware) {
        String text = connectionText(on);
        if (on && hardware.equals(lastPressed)) text += "   ← " + getText("bindings.reconcile.plugIn.pressed");
        return line(text, on ? HudPalette.HUD_COLOR_ROLE_SUCCESS : HudPalette.HUD_COLOR_ROLE_READOUT_LABEL,
                HudPalette.HUD_FONT_SM, Font.PLAIN);
    }

    private String connectionText(boolean on) {
        if (!live.available()) return getText("bindings.reconcile.connection.unknown");
        return getText(on ? "bindings.reconcile.connection.on" : "bindings.reconcile.connection.off");
    }

    /**
     * What a controller is called: what Windows calls it when it is plugged in; otherwise its aliases - a controller
     * the copies hold an alias for "exists" even unplugged (Alan, 2026-11-18, Q2) - or its VID/PID.
     */
    private String controllerName(HardwareId hardware, Collection<String> aliases) {
        Optional<String> product = live.productName(hardware);
        if (product.isPresent()) return product.get();
        if (!aliases.isEmpty()) return String.join(" / ", aliases);
        if (hardware.vid().isEmpty() && hardware.pid().isEmpty()) return getText("bindings.reconcile.unknownController");
        return getText("bindings.reconcile.controllerById", hardware.vid() + ":" + hardware.pid());
    }

    /** The picture for a controller: from what Windows calls it, when plugged in, and its aliases. */
    private ControllerIcons.Kind kindOf(HardwareId hardware, Collection<String> aliases) {
        List<String> names = new ArrayList<>(aliases);
        if (hardware != null) live.productName(hardware).ifPresent(names::add);
        return ControllerIcons.kindOf(names);
    }

    private String nameOf(long installId) {
        return installNames.getOrDefault(String.valueOf(installId), String.valueOf(installId));
    }

    private String nameOf(long installId, String fallback) {
        return installNames.getOrDefault(String.valueOf(installId), fallback);
    }

    private void setBusy(boolean busy) {
        Component glass = getGlassPane();
        glass.setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
        glass.setVisible(busy);
    }

    private static JPanel clickBlocker() {
        JPanel blocker = new JPanel();
        blocker.setOpaque(false);
        // WHY: a glass pane with no mouse listener lets clicks through to the buttons beneath.
        blocker.addMouseListener(new java.awt.event.MouseAdapter() {
        });
        return blocker;
    }

    /** The steps across the top: Plug in, Choose, Check - with Welcome before them, unnumbered. */
    private static final class Stepper extends JPanel {
        private final List<JLabel> labels = new ArrayList<>();
        private final int offset;

        Stepper(boolean introduce) {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setOpaque(false);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER),
                    new EmptyBorder(0, 0, 10, 0)));
            List<String> names = new ArrayList<>();
            if (introduce) names.add(getText("bindings.reconcile.step.plugIn"));
            names.add(getText("bindings.reconcile.step.choose"));
            names.add(getText("bindings.reconcile.step.check"));
            offset = introduce ? 0 : 1;
            for (int i = 0; i < names.size(); i++) {
                JLabel label = line((i + 1) + "  " + names.get(i), HudPalette.HUD_COLOR_ROLE_DISABLED,
                        HudPalette.HUD_FONT_SM, Font.PLAIN);
                labels.add(label);
                add(label);
                if (i < names.size() - 1) {
                    add(line("   ———   ", HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER,
                            HudPalette.HUD_FONT_SM, Font.PLAIN));
                }
            }
        }

        /** @param step 0 Plug in, 1 Choose, 2 Check; below 0 for Welcome, before any of them */
        void at(int step) {
            for (int i = 0; i < labels.size(); i++) {
                int index = i + offset;
                JLabel label = labels.get(i);
                String text = label.getText().replaceFirst("^(✓|\\d+)", "");
                boolean done = index < step;
                boolean now = index == step;
                label.setText((done ? "✓" : String.valueOf(i + 1)) + text);
                label.setForeground(now ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION
                        : done ? HudPalette.HUD_COLOR_ROLE_SUCCESS : HudPalette.HUD_COLOR_ROLE_DISABLED);
                label.setFont(label.getFont().deriveFont(now ? Font.BOLD : Font.PLAIN));
            }
        }
    }
}
