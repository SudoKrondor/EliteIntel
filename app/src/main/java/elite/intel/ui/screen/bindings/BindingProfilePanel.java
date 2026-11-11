package elite.intel.ui.screen.bindings;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.io.BindingsMonitor;
import elite.intel.bindforge.io.BindsValues;
import elite.intel.bindforge.io.KeyBindingsParser;
import elite.intel.bindforge.model.GameKeyboardLayout;
import elite.intel.bindforge.storage.BindsEdits;
import elite.intel.bindforge.storage.ConfigurationFiles;
import elite.intel.bindforge.storage.EditedDeviceLabels;
import elite.intel.bindforge.storage.Edits;
import elite.intel.bindforge.storage.WorkingCopy;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.BindForgeEditsChangedEvent;
import elite.intel.ui.event.BindForgeReadOnlyChangedEvent;
import elite.intel.ui.event.BindingsSummaryChangedEvent;
import elite.intel.ui.event.BindingsUpdatedEvent;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.HudSearchToolbar;
import elite.intel.ui.widget.HudSlider;
import elite.intel.ui.widget.StatusBadge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static elite.intel.ui.dialog.HudConfirmDialog.info;
import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * The "Binding Profile" sub-tab of BIND FORGE - the Bind Editor's shell: the EDITING line naming the binds file shown,
 * the search field and the modes under it. The file is chosen in File Manager (Storage S15). Extracted from the former
 * monolithic {@code BindingsTabPanel}. One tab row holds the modes: the game's sections ({@link GameModePanel}, All to
 * On-Foot), Anomalies (its view of the rows in conflict) and Dead Zones &amp; Sensitivity ({@link SettingsModePanel}).
 * <p>
 * <strong>It shows and changes the edits not applied</strong> (Storage S5, 2026-10-28): any binds file in the working
 * copy loads, and capture SAVE, CLEAR and Auto-Assign are edits that light the one APPLY / DISCARD bar above
 * BindForge's tabs. Nothing here writes a file (storage.md rule 6). Reading and changing the edits touch the disk, so
 * both run off the EDT.
 */
public class BindingProfilePanel extends JPanel {
    private static final Logger log = LogManager.getLogger(BindingProfilePanel.class);

    /**
     * Pacing slider granularity: 10ms notches between the FAST and SLOW ends.
     */
    private static final int KEY_INPUT_PACING_STEP_MS = 10;

    /** Room above the hint and pacing row, and between it and the search, so neither crowds the grid. */
    private static final int BOTTOM_GAP = 8;

    private final KeyBindingsParser parser = KeyBindingsParser.getInstance();
    private final BindingsMonitor monitor = BindingsMonitor.getInstance();
    private final SystemSession systemSession = SystemSession.getInstance();
    private final RefreshCoalescer refreshes = new RefreshCoalescer();

    /** The binds file shown, on the EDITING line. */
    private JLabel editingValue;
    private JPanel keyboardOnlyBanner;
    private JTextField bindingSearchField;
    private final GameModePanel gameMode;
    private final SettingsModePanel settingsMode;
    private JTabbedPane modeTabs;
    /**
     * Where the Anomalies tab sits in {@link #modeTabs}, after the section tabs.
     */
    private int anomaliesTab;
    private static final String CARD_SECTIONS = "sections";
    private static final String CARD_ANOMALIES = "anomalies";
    private static final String CARD_SETTINGS = "settings";

    /** The binds file shown, by file name - {@code null} when none could be read. */
    private volatile String loadedFile;
    /** The file LOAD asked for, shown at the next read if the edits still hold it. */
    private volatile String wantedFile;
    /**
     * A change to the edits is running - SAVE, CLEAR or Auto-Assign; a second waits for it.
     */
    private boolean changeRunning;

    public BindingProfilePanel() {
        GameModePanel.EditRunner runner = new GameModePanel.EditRunner() {
            @Override
            public boolean running() {
                return changeRunning;
            }

            @Override
            public void run(String title, GameModePanel.Change change, Runnable onEdt, Runnable afterFailure) {
                offEdt(title, change, onEdt, afterFailure);
            }
        };
        gameMode = new GameModePanel(this, runner, count -> {
            if (modeTabs != null) modeTabs.setTitleAt(anomaliesTab, getText("bindings.mode.anomalies", count));
        });
        settingsMode = new SettingsModePanel(runner);
        buildUi();
        // WHY: what the grid shows also comes from outside the binds file - which controls custom commands tap - and
        // nothing announces a change to it. Coming back to this tab is when the commander looks, so that is when it
        // is read again.
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !isShowing()) return;
            initData();
        });
        UiBus.register(this);
    }

    public void dispose() {
        UiBus.unregister(this);
    }

    /**
     * The one way to open the capture dialog for a control, for whichever mode names one. On the EDT.
     */
    void openCapture(String bindingId) {
        gameMode.openCapture(bindingId);
    }

    /**
     * The edits changed - an edit here or in the Alias Designer, DISCARD, APPLY, RESTORE or first-time startup - so the
     * binds file shown may have.
     */
    @Subscribe
    public void onEditsChanged(BindForgeEditsChangedEvent event) {
        SwingUtilities.invokeLater(this::initData);
    }

    /**
     * BindForge went read only, or stopped being: Game Mode, Anomalies and Settings follow (Storage S14b).
     */
    @Subscribe
    public void onReadOnlyChanged(BindForgeReadOnlyChangedEvent event) {
        SwingUtilities.invokeLater(() -> {
            gameMode.readOnlyChanged();
            settingsMode.readOnlyChanged();
        });
    }

    /**
     * The live bindings changed - {@code BindingsMonitor} saw the bindings folder change. Only the AI tab's counts
     * read them; the grid shows the edits.
     */
    @Subscribe
    public void onBindingsUpdated(BindingsUpdatedEvent event) {
        new Thread(this::publishLiveSummary, "BindForge-BindingsSummary").start();
    }

    private void buildUi() {
        setLayout(new BorderLayout(2, SCREEN_TOP_GAP));
        setBorder(hudSubtabContentBorder());
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);

        add(compactProfilePanel(), BorderLayout.NORTH);
        add(modeArea(), BorderLayout.CENTER);

        // WHY at the foot: the hint and the pacing are VEGA's, not the binds file's, so they sit with the other
        // controls under the grid and the tab row comes straight after the file's name (Alan, 2026-11-10).
        JPanel bottom = transparentPanel(new BorderLayout(0, BOTTOM_GAP));
        bottom.add(hintAndPacingRow(), BorderLayout.NORTH);
        bottom.add(footer(), BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }

    /**
     * The one tab row - the sections, then Anomalies and Dead Zones &amp; Sensitivity - over what the chosen tab shows.
     * <p>
     * WHY the tabs are empty and a card panel holds the views: a component sits on one tab only, and all five section
     * tabs show the same tables, narrowed.
     */
    private JPanel modeArea() {
        modeTabs = AppTheme.makeSectionTabs();
        gameMode.addSectionTabs(modeTabs);
        anomaliesTab = modeTabs.getTabCount();
        modeTabs.addTab(getText("bindings.mode.anomalies", 0), transparentPanel(null));
        modeTabs.addTab(getText("bindings.mode.settings"), transparentPanel(null));

        CardLayout cards = new CardLayout();
        JPanel views = transparentPanel(cards);
        views.add(gameMode, CARD_SECTIONS);
        views.add(gameMode.anomaliesView(), CARD_ANOMALIES);
        views.add(settingsMode, CARD_SETTINGS);
        modeTabs.addChangeListener(e -> {
            int tab = modeTabs.getSelectedIndex();
            cards.show(views, sectionTabChosen() ? CARD_SECTIONS : tab == anomaliesTab ? CARD_ANOMALIES : CARD_SETTINGS);
        });

        JPanel area = transparentPanel(new BorderLayout());
        area.add(modeTabs, BorderLayout.NORTH);
        area.add(views, BorderLayout.CENTER);
        return area;
    }

    /**
     * The section tabs come first in the row, so everything before Anomalies is one.
     */
    private boolean sectionTabChosen() {
        return modeTabs.getSelectedIndex() < anomaliesTab;
    }

    /**
     * The search, then what the mode on screen adds to it.
     * <p>
     * WHY the shell's and not Game Mode's: every mode is searched, and only Game Mode has filters. In Game Mode's own
     * footer the search would vanish with it on the other tabs.
     */
    private JPanel footer() {
        JComponent controls = gameMode.footerControls();
        HudSearchToolbar footer = HudSearchToolbar.tableFilter(
                getText("bindings.search.placeholder"),
                getText("actions.commands.search.clearTooltip"),
                controls);
        footer.setBorder(hudFooterBorder());
        wireSearch(footer.textField());
        modeTabs.addChangeListener(e -> controls.setVisible(sectionTabChosen()));
        return footer;
    }

    /**
     * The one read-only line naming the binds file shown: {@code EDITING  DualVirpilDawnTreader.4.2.binds}. Choosing
     * another is File Manager's Bindings File section and its LOAD - the folder and the dropdown moved there, because
     * only File Manager shows folders and files (principle 12; Storage S15, Alan, 2026-11-04).
     */
    private JPanel compactProfilePanel() {
        JPanel details = transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        details.setBorder(BorderFactory.createEmptyBorder(HUD_PADDING_SMALL, HUD_PADDING_SMALL, 6, HUD_PADDING_SMALL));
        details.add(hudReadoutLabel(getText("bindings.editing")));
        details.add(Box.createHorizontalStrut(7));
        editingValue = hudReadoutValue(getText("bindings.editing.none"), HUD_COLOR_ROLE_PRIMARY_TEXT);
        details.add(editingValue);
        return details;
    }

    /**
     * Reads the edits and the binds file to show, off the EDT, then shows them - the file LOAD asked for, else the one
     * shown, else the one the preset file's General line names, else the first.
     * <p>
     * A read already in flight is left to finish, and one more follows it - see {@link RefreshCoalescer}.
     */
    public void initData() {
        if (!refreshes.tryStart()) return;
        String wanted = wantedFile != null ? wantedFile : loadedFile;
        // WHY: the grid shows nothing when the read fails. A file the read could not use is logged as a warning - a
        // damaged working copy is offered a restore on the BindForge tab. Before first-time startup there is none to
        // read, which is no failure: the grid shows nothing and nothing is logged. A bug is logged as an error
        // (Krondor's branch review, 2026-10-31). Neither is a dialog: a read runs at every refresh.
        // WHY: the AI tab's counts read the live bindings, not the edits, so they run on their own thread - a read
        // that fails does not skip them, and a failure in them does not blank a read that worked (Storage S8R3).
        new Thread(this::publishLiveSummary, "BindForge-BindingsSummary").start();
        FlowSteps.inBackground("BindForge-BindEditorRead", log, "The Bind Editor's read", () -> {
                    Loaded read = read(wanted);
                    return () -> shown(read);
                }, reason -> () -> shown(Loaded.nothing(wanted)), reason -> () -> shown(Loaded.nothing(wanted)),
                FlowSteps.NOTHING);
    }

    /** {@link #show} what a read found, then the read asked for while it ran, if any. On the EDT. */
    private void shown(Loaded read) {
        show(read);
        if (refreshes.finish()) initData();
    }

    /**
     * File Manager's LOAD, or a rename there of the file shown: shows {@code file} at the next read, if the edits still
     * hold it. On the EDT.
     */
    public void load(String file) {
        wantedFile = file;
        initData();
    }

    /** The binds file shown, by file name - {@code null} when none could be read. */
    public String shownFile() {
        return loadedFile;
    }

    /**
     * What one read found. {@code file} is {@code null} when nothing could be shown - the log says why.
     *
     * @param asked the file this read was asked for, LOAD's or the one shown - {@code null} for neither
     */
    private record Loaded(String asked, String file, byte[] content,
                          Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots, GameKeyboardLayout layout,
                          EditedDeviceLabels labels, BindsValues values) {
        static Loaded nothing(String asked) {
            return new Loaded(asked, null, null, Map.of(), GameKeyboardLayout.OTHER, EditedDeviceLabels.NONE, BindsValues.NONE);
        }
    }

    /**
     * Reads the edits and parses the file to show. Off the EDT.
     *
     * @throws IOException if the edits, the file or the device files cannot be read - none when there is no working
     *                     copy yet, which shows nothing
     */
    private Loaded read(String asked) throws IOException {
        ConfigurationFiles current;
        try {
            current = Edits.stored().snapshot().current();
        } catch (WorkingCopy.NoWorkingCopyException e) {
            return Loaded.nothing(asked);
        }
        List<String> files = BindsEdits.bindsFiles(current);
        if (files.isEmpty()) return Loaded.nothing(asked);
        String file = asked != null && files.contains(asked)
                ? asked
                : BindsEdits.generalFile(current).orElse(files.getFirst());
        byte[] content = current.bindings().get(file);
        return new Loaded(asked, file, content, parser.parseReadOnlyBindingSlots(content),
                parser.parseKeyboardLayout(content), EditedDeviceLabels.of(current), BindsValues.of(content));
    }

    /** Shows what {@link #read} found. On the EDT. */
    private void show(Loaded read) {
        editingValue.setText(read.file() != null ? read.file() : getText("bindings.editing.none"));
        // WHY only when this read was LOAD's: a LOAD pressed while an older read ran is read next, not forgotten.
        if (Objects.equals(wantedFile, read.asked())) wantedFile = null;
        loadedFile = read.file();
        gameMode.show(new GameModePanel.Shown(read.file(), read.content(), read.slots(), read.layout(), read.labels(), read.values()));
        settingsMode.show(read.file(), read.values());
    }

    /**
     * The AI tab's counts, from the live bindings {@code BindingsMonitor} reads - what Vega presses, not what is on
     * screen (Alan, 2026-10-28, Storage S5). Only the slot EliteIntel would press counts, not both slots.
     */
    private void publishLiveSummary() {
        Map<String, KeyBindingsParser.KeyBinding> live = monitor.getBindings();
        if (live == null) {
            UiBus.publish(new BindingsSummaryChangedEvent(0, 0));
            return;
        }
        UiBus.publish(new BindingsSummaryChangedEvent(
                monitor.findMissingGameBindings(live).size(),
                monitor.findFoundGameBindings(live).size()));
    }

    private JPanel keyboardOnlyWarningStrip() {
        // Centralised on HudBanner (section 7.3) with the leading warning glyph - no hand-rolled strip.
        keyboardOnlyBanner = new HudBanner(getText("bindings.keyboardOnlyHint"),
                StatusBadge.State.STANDBY, true);
        return keyboardOnlyBanner;
    }

    /**
     * The row above the search: what VEGA presses with, and how fast - the pacing governs exactly what the hint
     * describes.
     * <p>
     * Pacing of the keystrokes this app sends to the game. The slider sets the floor of the pause held after each
     * keystroke of a sequence; the executor adds its own randomised spread on top. FAST is the pacing every build so
     * far has used and stays the default - SLOW is for machines where the game drops keystrokes out of a sequence it
     * cannot keep up with.
     */
    private JPanel hintAndPacingRow() {
        JPanel row = transparentPanel(new GridBagLayout());
        row.setBorder(new EmptyBorder(BOTTOM_GAP, 6, 0, 6));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.insets = new Insets(0, 0, 0, 16);

        gbc.gridx = 0;
        row.add(keyboardOnlyWarningStrip(), gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(0, 0, 0, 7);
        row.add(sliderRowLabel(getText("bindings.keyInputPacing")), gbc);

        gbc.gridx = 2;
        row.add(sliderRowLabel(getText("bindings.keyInputPacing.fast")), gbc);

        HudSlider slider = new HudSlider(
                SystemSession.KEY_INPUT_DELAY_MIN_MS,
                SystemSession.KEY_INPUT_DELAY_MAX_MS,
                KEY_INPUT_PACING_STEP_MS,
                systemSession.getKeyInputDelayMs());
        slider.setToolTipText(getText("bindings.keyInputPacing.tooltip"));
        slider.addChangeListener(e -> systemSession.setKeyInputDelayMs(slider.getValue()));
        gbc.gridx = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        row.add(slider, gbc);

        gbc.gridx = 4;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 0);
        row.add(sliderRowLabel(getText("bindings.keyInputPacing.slow")), gbc);

        return row;
    }

    /**
     * A label on the pacing row, dropped by the slider's value area so it sits level with the track rather
     * than with the value printed above the thumb.
     */
    private static JLabel sliderRowLabel(String text) {
        JLabel label = hudReadoutLabel(text);
        label.setBorder(new EmptyBorder(HUD_SLIDER_VALUE_AREA, 0, 0, 0));
        return label;
    }

    /**
     * Search-as-you-type over what the modes show. Every keystroke re-renders; the tables are rebuilt from an in-memory
     * map, so there is nothing to debounce.
     */
    private void wireSearch(JTextField field) {
        bindingSearchField = field;
        bindingSearchField.setToolTipText(getText("bindings.search.tooltip"));
        bindingSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                searchModes();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                searchModes();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                searchModes();
            }
        });
    }

    private void searchModes() {
        gameMode.search(bindingSearchField.getText());
        settingsMode.search(bindingSearchField.getText());
    }

    /**
     * Changes the edits off the EDT, one change at a time, then runs {@code onEdt} on it. A failure is shown under
     * {@code title}, not only logged - the user pressed a button and must be told - and {@code afterFailure} runs
     * instead. A file the change could not use is {@code bindings.change.failed}; a bug is logged as an error and said
     * to have stopped unexpectedly (Krondor's branch review, 2026-10-31). The screen reads the edits again either way.
     */
    private void offEdt(String title, GameModePanel.Change change, Runnable onEdt, Runnable afterFailure) {
        changeRunning = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        gameMode.changeStarted();
        FlowSteps.inBackground("BindForge-BindEditorChange", log, title, () -> {
                    try {
                        change.run();
                    } catch (Edits.RefusedException e) {
                        // WHY: refused - a write running, or read only - said in the user's language.
                        log.info("{} not done: {}", title, e.getMessage());
                        return () -> changed(title, getText(e.messageKey()), onEdt, afterFailure);
                    }
                    return () -> changed(title, null, onEdt, afterFailure);
                }, reason -> () -> changed(title, getText("bindings.change.failed"), onEdt, afterFailure),
                reason -> () -> changed(title, getText("bindings.change.stopped", reason), onEdt, afterFailure),
                FlowSteps.NOTHING);
    }

    /** After {@link #offEdt}: {@code failure} shown, or {@code onEdt} run. On the EDT. */
    private void changed(String title, String failure, Runnable onEdt, Runnable afterFailure) {
        changeRunning = false;
        setCursor(Cursor.getDefaultCursor());
        initData();
        if (failure != null) {
            info(this, title, failure, getText("button.close"));
            afterFailure.run();
            return;
        }
        onEdt.run();
    }
}
