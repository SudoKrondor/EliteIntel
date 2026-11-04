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
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.DataDirectoryValidator;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.BindForgeEditsChangedEvent;
import elite.intel.ui.event.BindingsSummaryChangedEvent;
import elite.intel.ui.event.BindingsUpdatedEvent;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static elite.intel.ui.dialog.HudConfirmDialog.info;
import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudGlyphs.verticalEllipsisIcon;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * The "Binding Profile" sub-tab of BIND FORGE - the Bind Editor's shell: the binds file chosen from the working copy, its
 * name, the search field and the modes under it. Extracted from the former monolithic {@code BindingsTabPanel}. The modes
 * are Game Mode ({@link GameModePanel}), Anomalies (its view of the rows in conflict) and Settings
 * ({@link SettingsModePanel}).
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

    private final KeyBindingsParser parser = KeyBindingsParser.getInstance();
    private final BindingsMonitor monitor = BindingsMonitor.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final SystemSession systemSession = SystemSession.getInstance();
    private final RefreshCoalescer refreshes = new RefreshCoalescer();

    private HudComboBox<String> bindsFileBox;
    private JButton loadButton;
    private JTextField bindingsDirField;
    private JPanel keyboardOnlyBanner;
    private JTextField bindingSearchField;
    private final GameModePanel gameMode;
    private final SettingsModePanel settingsMode;
    private JTabbedPane modeTabs;
    private static final int ANOMALIES_TAB = 1;

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
            if (modeTabs != null) modeTabs.setTitleAt(ANOMALIES_TAB, getText("bindings.mode.anomalies", count));
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

        JPanel details = compactProfilePanel();
        JPanel header = transparentPanel(new BorderLayout(0, 2));
        header.add(bindingProfileCard(details), BorderLayout.NORTH);
        header.add(hintAndPacingRow(), BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        modeTabs = AppTheme.makeSectionTabs();
        modeTabs.addTab(getText("bindings.mode.gameMode"), gameMode);
        modeTabs.addTab(getText("bindings.mode.anomalies", 0), gameMode.anomaliesView());
        modeTabs.addTab(getText("bindings.mode.settings"), settingsMode);
        add(modeTabs, BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);
    }

    /**
     * The search, then what the mode on screen adds to it.
     * <p>
     * WHY the shell's and not Game Mode's: every mode is searched, and only Game Mode has filters. In Game Mode's own
     * footer the search would vanish with it on the other tabs.
     */
    private JPanel footer() {
        JPanel search = transparentPanel(new BorderLayout(7, 0));
        search.add(hudReadoutLabel(getText("bindings.search.label")), BorderLayout.WEST);
        search.add(bindingSearchField(), BorderLayout.CENTER);
        // WHY a wrapper: the field keeps its own height when the buttons beside it make the line taller.
        JPanel searchLine = transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        searchLine.add(search, gbc);

        JComponent controls = gameMode.footerControls();
        // WHY the buttons' height: the footer keeps it on every tab, so the grid above does not shift between modes.
        searchLine.setPreferredSize(new Dimension(0, controls.getPreferredSize().height));
        searchLine.setBorder(new EmptyBorder(0, 6, 0, 0));
        modeTabs.addChangeListener(e -> controls.setVisible(modeTabs.getSelectedComponent() == gameMode));

        JPanel footer = transparentPanel(new BorderLayout(HUD_GAP, 0));
        footer.setBorder(hudFooterBorder());
        footer.add(searchLine, BorderLayout.CENTER);
        footer.add(controls, BorderLayout.EAST);
        return footer;
    }

    private JPanel compactProfilePanel() {
        JPanel profileCardBody = transparentPanel(new BorderLayout(0, 0));
        JPanel details = transparentPanel(new GridBagLayout());
        details.setBorder(BorderFactory.createEmptyBorder(2, 0, 6, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(2, 0, 6, 7);
        gbc.anchor = GridBagConstraints.WEST;

        // One row, to leave the grid the height: [label | folder | picker | label | file | LOAD].
        // WHY the folder gets the larger share: a path runs far longer than a file name.
        addProfileLabel(details, getText("player.bindingsDirectory"), gbc, 0);
        bindingsDirField = readOnlyField();
        bindingsDirField.setToolTipText(getText("player.bindingsDirectory.tooltip"));
        addProfileField(details, bindingsDirField, gbc, 1, DIRECTORY_SHARE);

        JButton selectBindingsDirButton = compactDirectoryChooserButton();
        selectBindingsDirButton.addActionListener(e -> selectBindingsDirectory());
        addProfileButton(details, selectBindingsDirButton, gbc, 2);

        // The binds file, from the working copy with the edits on it, and LOAD (bind-editor.md, Shell)
        addProfileLabel(details, getText("bindings.bindsFile"), gbc, 3);
        bindsFileBox = new HudComboBox<>(new DefaultComboBoxModel<>());
        bindsFileBox.setToolTipText(getText("bindings.bindsFile.tooltip"));
        addProfileField(details, bindsFileBox, gbc, 4, 1.0 - DIRECTORY_SHARE);

        loadButton = makeButtonSubtle(getText("bindings.bindsFile.load"));
        loadButton.setToolTipText(getText("bindings.bindsFile.load.tooltip"));
        loadButton.addActionListener(e -> loadSelected());
        addProfileButton(details, loadButton, gbc, 5);

        profileCardBody.add(details, BorderLayout.CENTER);
        return profileCardBody;
    }

    private JComponent bindingProfileCard(JPanel body) {
        // Working zone of the tab -> FLAT section (HUD section 9), not a framed accent box.
        HudSection card = new HudSection(
                getText("bindings.section.profile"),
                new BorderLayout(),
                HudPanel.Variant.FLAT,
                6);
        card.body().add(body, BorderLayout.CENTER);

        JPanel wrapper = transparentPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * Reads the edits and the binds file to show, off the EDT, then shows them - the file LOAD asked for, else the one
     * shown, else the one the preset file's General line names, else the first.
     * <p>
     * A read already in flight is left to finish, and one more follows it - see {@link RefreshCoalescer}.
     */
    public void initData() {
        bindingsDirField.setText(playerSession.getBindingsDir().toString());
        if (!refreshes.tryStart()) return;
        String wanted = wantedFile != null ? wantedFile : loadedFile;
        // WHY: the grid shows nothing when the read fails. A file the read could not use is logged as a warning -
        // before first-time startup there is no working copy, and the BindForge tab says so; a damaged one is offered a
        // restore there too. A bug is logged as an error (Krondor's branch review, 2026-10-31). Neither is a dialog: a
        // read runs at every refresh.
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

    /** LOAD: the file chosen in the list. */
    private void loadSelected() {
        Object chosen = bindsFileBox.getSelectedItem();
        if (chosen == null) return;
        wantedFile = chosen.toString();
        initData();
    }

    /**
     * What one read found. {@code file} is {@code null} when nothing could be shown - the log says why.
     *
     * @param asked the file this read was asked for, LOAD's or the one shown - {@code null} for neither
     */
    private record Loaded(String asked, List<String> files, String file, byte[] content,
                          Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots, GameKeyboardLayout layout,
                          EditedDeviceLabels labels, BindsValues values) {
        static Loaded nothing(String asked) {
            return new Loaded(asked, List.of(), null, null, Map.of(), GameKeyboardLayout.OTHER, EditedDeviceLabels.NONE, BindsValues.NONE);
        }
    }

    /**
     * Reads the edits and parses the file to show. Off the EDT.
     *
     * @throws IOException if the edits, the file or the device files cannot be read
     */
    private Loaded read(String asked) throws IOException {
        ConfigurationFiles current = Edits.stored().snapshot().current();
        List<String> files = BindsEdits.bindsFiles(current);
        if (files.isEmpty()) return Loaded.nothing(asked);
        String file = asked != null && files.contains(asked)
                ? asked
                : BindsEdits.generalFile(current).orElse(files.getFirst());
        byte[] content = current.bindings().get(file);
        return new Loaded(asked, files, file, content, parser.parseReadOnlyBindingSlots(content),
                parser.parseKeyboardLayout(content), EditedDeviceLabels.of(current), BindsValues.of(content));
    }

    /** Shows what {@link #read} found. On the EDT. */
    private void show(Loaded read) {
        bindsFileBox.setModel(new DefaultComboBoxModel<>(read.files().toArray(String[]::new)));
        bindsFileBox.setSelectedItem(read.file());
        boolean any = read.file() != null;
        bindsFileBox.setEnabled(any);
        loadButton.setEnabled(any);
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

    private void selectBindingsDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(getText("player.bindingsDirectory.dialog"));
        String current = playerSession.getBindingsDir().toString();
        if (!current.isBlank())
            chooser.setCurrentDirectory(new File(current).getParentFile());
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            if (!playerSession.setBindingsDir(path)) {
                DataDirectoryValidator.warnUnusable(DataDirectoryValidator.DirectoryKind.BINDINGS);
                return;
            }
            bindingsDirField.setText(playerSession.getBindingsDir().toString());
            DataDirectoryValidator.validateAndWarn(playerSession.getBindingsDir(), DataDirectoryValidator.DirectoryKind.BINDINGS);
            initData();
        }
    }

    private void addProfileLabel(JPanel panel, String text, GridBagConstraints gbc, int column) {
        gbc.gridx = column;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(2, 0, 6, 7);
        panel.add(hudReadoutLabel(text), gbc);
    }

    private void addProfileField(JPanel panel, JComponent component, GridBagConstraints gbc, int column, double weightX) {
        gbc.gridx = column;
        gbc.gridwidth = 1;
        gbc.weightx = weightX;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 0, 6, 7);
        component.setPreferredSize(new Dimension(0, component.getPreferredSize().height));
        panel.add(component, gbc);
    }

    private void addProfileButton(JPanel panel, JComponent button, GridBagConstraints gbc, int column) {
        gbc.gridx = column;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(2, 0, 6, 7);
        panel.add(button, gbc);
    }

    /**
     * The folder's share of the width the profile row's two fields divide between them.
     */
    private static final double DIRECTORY_SHARE = 0.6;

    private JTextField readOnlyField() {
        JTextField field = makeTextField();
        field.setEditable(false);
        return field;
    }

    private JButton compactDirectoryChooserButton() {
        JButton button = makeFieldButton(verticalEllipsisIcon(HUD_FIELD_HEIGHT), HUD_FIELD_HEIGHT);
        button.setToolTipText(getText("player.bindingsDirectory.select.tooltip"));
        return button;
    }

    private JPanel keyboardOnlyWarningStrip() {
        // Centralised on HudBanner (section 7.3) with the leading warning glyph - no hand-rolled strip.
        keyboardOnlyBanner = new HudBanner(getText("bindings.keyboardOnlyHint"),
                StatusBadge.State.STANDBY, true);
        return keyboardOnlyBanner;
    }

    /**
     * The row under the profile: what VEGA presses with, and how fast - the pacing governs exactly what the hint
     * describes.
     * <p>
     * Pacing of the keystrokes this app sends to the game. The slider sets the floor of the pause held after each
     * keystroke of a sequence; the executor adds its own randomised spread on top. FAST is the pacing every build so
     * far has used and stays the default - SLOW is for machines where the game drops keystrokes out of a sequence it
     * cannot keep up with.
     */
    private JPanel hintAndPacingRow() {
        JPanel row = transparentPanel(new GridBagLayout());
        row.setBorder(new EmptyBorder(0, 6, 2, 6));

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
    private JTextField bindingSearchField() {
        bindingSearchField = makeTextField();
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
        return bindingSearchField;
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
                    } catch (Edits.WriteRunningException e) {
                        // WHY: a write is running - said in the user's language, not the exception's.
                        log.info("{} not done: {}", title, e.getMessage());
                        return () -> changed(title, getText("bindings.writeRunning"), onEdt, afterFailure);
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
