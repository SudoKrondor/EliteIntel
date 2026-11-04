package elite.intel.ui.screen.bindings;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.devices.DeviceNames;
import elite.intel.bindforge.io.BindingsMonitor;
import elite.intel.bindforge.io.KeyBindingsParser;
import elite.intel.bindforge.model.GameKeyboardLayout;
import elite.intel.bindforge.storage.*;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.DataDirectoryValidator;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.BindForgeEditsChangedEvent;
import elite.intel.ui.event.BindingsSummaryChangedEvent;
import elite.intel.ui.event.BindingsUpdatedEvent;
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
import java.util.Optional;

import static elite.intel.ui.dialog.HudConfirmDialog.info;
import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.sizeFieldLabel;
import static elite.intel.ui.theme.HudGlyphs.verticalEllipsisIcon;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * The "Binding Profile" sub-tab of BIND FORGE - the Bind Editor's shell: the binds file chosen from the working copy, its
 * name, the search field and the place a mode sits. Extracted from the former monolithic {@code BindingsTabPanel}. The
 * one mode so far, the grid, is {@link GameModePanel}; no mode bar is shown until there is a second.
 * <p>
 * <strong>It shows and changes the edits not applied</strong> (Storage S5, 2026-10-28): any binds file in the working
 * copy loads, and capture SAVE, CLEAR, Auto-Assign and a rename are edits that light the one APPLY / DISCARD bar above
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
    /** The shown file's BindName, editable - renaming it (Storage S12). */
    private HudTextField nameField;
    /** The shown file's version and extension, fixed text after the Name box. */
    private JLabel nameSuffix;
    /** Why a typed name is refused, or that the file is named like a factory one - warning text colour. */
    private JLabel nameProblem;
    /** Set while {@link #showName} fills the box, so filling it is not judged as typing. */
    private boolean showingName;
    /** What a typed name is checked against, read with the binds file. */
    private Rename.Names names = Rename.Names.NONE;
    private JTextField bindingsDirField;
    private JPanel keyboardOnlyBanner;
    private JTextField bindingSearchField;
    private final GameModePanel gameMode;

    /** The binds file shown, by file name - {@code null} when none could be read. */
    private volatile String loadedFile;
    /** The file LOAD asked for, shown at the next read if the edits still hold it. */
    private volatile String wantedFile;
    /**
     * A change to the edits is running - SAVE, CLEAR, Auto-Assign or a rename; a second waits for it.
     */
    private boolean changeRunning;

    public BindingProfilePanel() {
        gameMode = new GameModePanel(this, new GameModePanel.EditRunner() {
            @Override
            public boolean running() {
                return changeRunning;
            }

            @Override
            public void run(String title, GameModePanel.Change change, Runnable onEdt, Runnable afterFailure) {
                offEdt(title, change, onEdt, afterFailure);
            }
        });
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
        header.add(keyInputPacingRow(), BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // WHY the search is the shell's and the footer the mode's: a second mode shares the search, and brings its own
        // filters. The mode sits under the search, so it fills the same space it did when the footer was the shell's.
        JPanel modeArea = transparentPanel(new BorderLayout(0, 2));
        modeArea.add(bindingSearchRow(), BorderLayout.NORTH);
        modeArea.add(gameMode, BorderLayout.CENTER);
        add(modeArea, BorderLayout.CENTER);
    }

    private JPanel compactProfilePanel() {
        JPanel profileCardBody = transparentPanel(new BorderLayout(0, 0));
        JPanel details = transparentPanel(new GridBagLayout());
        details.setBorder(BorderFactory.createEmptyBorder(2, 0, 6, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(2, 0, 6, 7);
        gbc.anchor = GridBagConstraints.WEST;

        // Single-column grid: [label | field | affordance]. Both rows share the label column width and the field
        // right edge; the vertical-ellipsis and LOAD line up on the right.
        // Row 0 - Bindings Directory + picker
        addProfileLabel(details, getText("player.bindingsDirectory"), gbc, 0, LABEL_COL_WIDTH);
        bindingsDirField = readOnlyField();
        bindingsDirField.setToolTipText(getText("player.bindingsDirectory.tooltip"));
        addProfileField(details, bindingsDirField, gbc, 1, 1, 1.0);

        JButton selectBindingsDirButton = compactDirectoryChooserButton();
        selectBindingsDirButton.addActionListener(e -> selectBindingsDirectory());
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(2, 0, 6, 7);
        details.add(selectBindingsDirButton, gbc);

        // Row 1 - the binds file, from the working copy with the edits on it, and LOAD (bind-editor.md, Shell)
        gbc.gridy = 1;
        addProfileLabel(details, getText("bindings.bindsFile"), gbc, 0, LABEL_COL_WIDTH);
        bindsFileBox = new HudComboBox<>(new DefaultComboBoxModel<>());
        bindsFileBox.setToolTipText(getText("bindings.bindsFile.tooltip"));
        // WHY: the Name box sits beside the dropdown, the version and extension after it as fixed text (bind-editor.md,
        // Shell; Storage S12). A text box, so picking a file can never rename one.
        JPanel fileAndName = transparentPanel(new BorderLayout(7, 0));
        fileAndName.add(bindsFileBox, BorderLayout.CENTER);
        fileAndName.add(nameRow(), BorderLayout.EAST);
        addProfileField(details, fileAndName, gbc, 1, 1, 1.0);

        loadButton = makeButtonSubtle(getText("bindings.bindsFile.load"));
        loadButton.setToolTipText(getText("bindings.bindsFile.load.tooltip"));
        loadButton.addActionListener(e -> loadSelected());
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(2, 0, 6, 7);
        details.add(loadButton, gbc);

        // Row 2 - why a typed name is refused, or that the file is named like a factory one; empty otherwise
        gbc.gridy = 2;
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 4, 7);
        nameProblem = hudReadoutValue(" ", HUD_COLOR_ROLE_WARNING);
        details.add(nameProblem, gbc);

        profileCardBody.add(details, BorderLayout.CENTER);
        return profileCardBody;
    }

    /** The Name box, its label and the fixed version text (Storage S12). Enter renames; Esc puts the name back. */
    private JPanel nameRow() {
        nameField = new HudTextField();
        nameField.setColumns(16);
        nameField.setToolTipText(getText("bindings.bindsFile.name.tooltip"));
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                judgeName();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                judgeName();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                judgeName();
            }
        });
        nameField.addActionListener(e -> renameLoaded());
        nameField.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "bindforge.name.revert");
        nameField.getActionMap().put("bindforge.name.revert", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showName();
            }
        });
        nameSuffix = hudReadoutLabel("");
        JPanel row = transparentPanel(new BorderLayout(4, 0));
        row.add(hudReadoutLabel(getText("bindings.bindsFile.name")), BorderLayout.WEST);
        row.add(nameField, BorderLayout.CENTER);
        row.add(nameSuffix, BorderLayout.EAST);
        return row;
    }

    /** The loaded file's BindName in the box, and the version after it. On the EDT. */
    private void showName() {
        String file = loadedFile;
        showingName = true;
        try {
            nameField.setText(file == null ? "" : Rename.bindName(file).orElse(""));
        } finally {
            showingName = false;
        }
        nameSuffix.setText(file == null ? "" : Rename.suffix(file));
        nameField.setEnabled(file != null);
        judgeName();
    }

    /**
     * The typed name, judged: the controller name's rule against the set's own BindNames, loosely, the file's own
     * included - and a factory BindName refused whatever its case (bind-editor.md, Shell; Alan, 2026-11-03). Empty when
     * it may be used or is the file's own name.
     */
    private Optional<String> nameRefusal(String typed) {
        String file = loadedFile;
        if (file == null || typed.equals(Rename.bindName(file).orElse(null))) return Optional.empty();
        DeviceNames.Verdict verdict = DeviceNames.validate(typed, names.taken(typed));
        if (!verdict.valid()) return Optional.of(OnboardingDialog.problemText(verdict));
        return Rename.factoryClash(typed, names.factory())
                .map(factory -> OnboardingDialog.problemText(
                        new DeviceNames.Verdict(DeviceNames.Problem.CLASHES, factory)));
    }

    /** Shows why the typed name is refused, else the factory-name warning for the file shown, else nothing. */
    private void judgeName() {
        if (showingName) return;
        String file = loadedFile;
        Optional<String> refusal = nameRefusal(nameField.getText().strip());
        if (refusal.isPresent()) {
            nameProblem.setText(refusal.get());
        } else if (file != null && Rename.factoryNamed(file, names.factory())) {
            nameProblem.setText(getText("bindings.bindsFile.name.factoryNamed"));
        } else {
            nameProblem.setText(" ");
        }
    }

    /** Enter: the rename, as an edit not applied, when the typed name may be used and differs from the file's. */
    private void renameLoaded() {
        String file = loadedFile;
        String typed = nameField.getText().strip();
        if (file == null || changeRunning || nameRefusal(typed).isPresent()) return;
        if (typed.equals(Rename.bindName(file).orElse(null))) return;
        String renamed = typed + Rename.suffix(file);
        offEdt(getText("bindings.bindsFile.name"), () -> {
            Edits.stored().change((current, applied) -> Rename.rename(current, applied, file, typed));
            // WHY: before the screen reads again, so the renamed file stays the one shown.
            wantedFile = renamed;
        }, () -> {
        }, this::showName);
    }

    private JComponent bindingProfileCard(JPanel body) {
        // Working zone of the tab -> FLAT section (HUD section 9), not a framed accent box.
        HudSection card = new HudSection(
                getText("bindings.section.profile"),
                new BorderLayout(),
                HudPanel.Variant.FLAT,
                6);
        card.body().add(body, BorderLayout.CENTER);
        card.setFooter(keyboardOnlyWarningStrip(), HUD_COLOR_ROLE_WARNING_PANEL_BACKGROUND);

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
                          EditedDeviceLabels labels,
                          Rename.Names names) {
        static Loaded nothing(String asked) {
            return new Loaded(asked, List.of(), null, null, Map.of(), GameKeyboardLayout.OTHER, EditedDeviceLabels.NONE, Rename.Names.NONE);
        }
    }

    /**
     * Reads the edits and parses the file to show. Off the EDT.
     *
     * @throws IOException if the edits, the file or the device files cannot be read
     */
    private Loaded read(String asked) throws IOException {
        Edits.Snapshot snapshot = Edits.stored().snapshot();
        ConfigurationFiles current = snapshot.current();
        List<String> files = BindsEdits.bindsFiles(current);
        if (files.isEmpty()) return Loaded.nothing(asked);
        String file = asked != null && files.contains(asked)
                ? asked
                : BindsEdits.generalFile(current).orElse(files.getFirst());
        byte[] content = current.bindings().get(file);
        return new Loaded(asked, files, file, content, parser.parseReadOnlyBindingSlots(content),
                parser.parseKeyboardLayout(content), EditedDeviceLabels.of(current), Rename.names(snapshot));
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
        names = read.names();
        showName();
        gameMode.show(new GameModePanel.Shown(read.file(), read.content(), read.slots(), read.layout(), read.labels()));
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

    private void addProfileLabel(JPanel panel, String text, GridBagConstraints gbc, int column, int width) {
        gbc.gridx = column;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(2, 0, 6, 7);
        JLabel label = hudReadoutLabel(text);
        sizeFieldLabel(label, width);
        panel.add(label, gbc);
    }

    private void addProfileField(
            JPanel panel,
            JComponent component,
            GridBagConstraints gbc,
            int column,
            int gridWidth,
            double weightX
    ) {
        gbc.gridx = column;
        gbc.gridwidth = gridWidth;
        gbc.weightx = weightX;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 0, 6, 7);
        component.setPreferredSize(new Dimension(0, component.getPreferredSize().height));
        panel.add(component, gbc);
    }

    /** Shared width of the left label column so all rows align (fits the longest label). */
    private static final int LABEL_COL_WIDTH = 180;

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
     * Pacing of the keystrokes this app sends to the game, sitting directly under the keyboard-only
     * banner because it governs exactly what that banner describes.
     * <p>
     * The slider sets the floor of the pause held after each keystroke of a sequence; the executor
     * adds its own randomised spread on top. FAST is the pacing every build so far has used and stays
     * the default - SLOW is for machines where the game drops keystrokes out of a sequence it cannot
     * keep up with.
     */
    private JPanel keyInputPacingRow() {
        JPanel row = transparentPanel(new GridBagLayout());
        row.setBorder(new EmptyBorder(0, 6, 0, 6));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 7);

        gbc.gridx = 0;
        row.add(sliderRowLabel(getText("bindings.keyInputPacing"), LABEL_COL_WIDTH), gbc);

        gbc.gridx = 1;
        row.add(sliderRowLabel(getText("bindings.keyInputPacing.fast"), 0), gbc);

        HudSlider slider = new HudSlider(
                SystemSession.KEY_INPUT_DELAY_MIN_MS,
                SystemSession.KEY_INPUT_DELAY_MAX_MS,
                KEY_INPUT_PACING_STEP_MS,
                systemSession.getKeyInputDelayMs());
        slider.setToolTipText(getText("bindings.keyInputPacing.tooltip"));
        slider.addChangeListener(e -> systemSession.setKeyInputDelayMs(slider.getValue()));
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        row.add(slider, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 0);
        row.add(sliderRowLabel(getText("bindings.keyInputPacing.slow"), 0), gbc);

        return row;
    }

    /**
     * Label for the pacing row, dropped by the slider's value area so it sits level with the track
     * rather than with the value printed above the thumb. A {@code width} of 0 leaves the label at
     * its natural width.
     */
    private static JLabel sliderRowLabel(String text, int width) {
        JLabel label = hudReadoutLabel(text);
        label.setBorder(new EmptyBorder(HUD_SLIDER_VALUE_AREA, 0, 0, 0));
        if (width > 0) {
            sizeFieldLabel(label, width);
        }
        return label;
    }

    /**
     * Search-as-you-type over both tables, sitting directly above them because it narrows exactly
     * what they show. Every keystroke re-renders; the tables are rebuilt from an in-memory map, so
     * there is nothing to debounce.
     */
    private JPanel bindingSearchRow() {
        JPanel row = transparentPanel(new GridBagLayout());
        row.setBorder(new EmptyBorder(0, 6, 2, 6));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 7);

        JLabel searchLabel = hudReadoutLabel(getText("bindings.search.label"));
        sizeFieldLabel(searchLabel, LABEL_COL_WIDTH);
        gbc.gridx = 0;
        row.add(searchLabel, gbc);

        bindingSearchField = makeTextField();
        bindingSearchField.setToolTipText(getText("bindings.search.tooltip"));
        bindingSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                gameMode.search(bindingSearchField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                gameMode.search(bindingSearchField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                gameMode.search(bindingSearchField.getText());
            }
        });
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        row.add(bindingSearchField, gbc);

        return row;
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
