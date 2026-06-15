package elite.intel.ui.view.settings;

import elite.intel.ai.brain.LocalLlmProvider;
import elite.intel.ai.mouth.google.GoogleVoices;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.RestartBrainEvent;
import elite.intel.ui.event.RestartMouthEvent;
import elite.intel.ui.event.TTSProviderChangedEvent;
import elite.intel.ui.view.HudBanner;
import elite.intel.ui.view.HudConfirmDialog;
import elite.intel.ui.view.HudSection;
import elite.intel.ui.view.HudSegmentedControl;
import elite.intel.ui.view.HudTwoColumns;
import elite.intel.ui.view.HudUnsavedHint;
import elite.intel.ui.view.StatusBadge;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.Objects;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.view.AppTheme.*;

/**
 * Unified AI services tab: routes the language model (LLM) and speech (TTS) each between a
 * LOCAL and a CLOUD source via {@link HudSegmentedControl} switches, with the active source's
 * configuration highlighted and the unused one dimmed (§0.6).
 * <p>
 * Persistence is transactional: no control writes to {@link SystemSession} on its own. All edits
 * live in an in-memory working copy and are committed atomically by {@link #save()} (the only point
 * that fires {@link RestartBrainEvent}/{@link RestartMouthEvent}). This removes the previous
 * cross-panel "Use" checkbox synchronisation and the risk of half-saved, inconsistent state.
 */
public class AiServicesSettingsPanel extends JPanel {

    private static final String DEFAULT_MODEL = "matrixportalx/tulu-3.1-8b-supernova";

    private static final int SRC_LOCAL = 0;
    private static final int SRC_CLOUD = 1;
    private static final int PROV_OLLAMA = 0;
    private static final int PROV_LMSTUDIO = 1;

    private final SystemSession systemSession = SystemSession.getInstance();

    // -- Controls --------------------------------------------------------------
    private HudSegmentedControl llmSourceControl;
    private HudSegmentedControl providerControl;
    private JTextField addressField;
    private JTextField commandModelField;
    private JTextField queryModelField;
    private JPasswordField apiKeyField;
    private JCheckBox llmLockCheck;

    private HudSegmentedControl ttsSourceControl;
    private JPasswordField ttsKeyField;
    private JCheckBox ttsLockCheck;

    private JPanel localCol;
    private JPanel rightCol;
    private JPanel ttsRightCol;
    private JButton saveButton;
    private JLabel unsavedLabel;

    // -- Working copy (in memory, committed only by save) ----------------------
    private String ollamaAddress = "", ollamaCommand = "", ollamaQuery = "";
    private String lmAddress = "", lmCommand = "", lmQuery = "";
    /** Which provider's values are currently shown in the address/model fields. */
    private LocalLlmProvider shownProvider = LocalLlmProvider.LMSTUDIO;

    // Snapshot of the last saved state; the dirty flag tracks whether edits differ from it,
    // so reverting values back to saved clears the "unsaved changes" banner.
    private boolean savedLlmLocal;
    private LocalLlmProvider savedProvider = LocalLlmProvider.LMSTUDIO;
    private String savedOllamaAddress = "", savedOllamaCommand = "", savedOllamaQuery = "";
    private String savedLmAddress = "", savedLmCommand = "", savedLmQuery = "";
    private String savedAiKey = "", savedTtsKey = "";
    private boolean savedTtsLocal;

    /** Suppresses dirty-marking while controls are populated programmatically. */
    private boolean loading = false;
    private boolean dirty = false;

    public AiServicesSettingsPanel() {
        buildUi();
        wireListeners();
    }

    // -------------------------------------------------------------------------
    // UI
    // -------------------------------------------------------------------------

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(HUD_BG);

        // ----- Section: LANGUAGE MODEL (flat working zone, §9) -----
        HudSection llmSection = HudSection.flat(getText("settings.ai.section.llm"), new BorderLayout(0, HUD_GAP));
        JPanel llm = llmSection.body();

        // Full-width source switch, no label: the segment captions name each setup.
        llmSourceControl = new HudSegmentedControl(
                new String[]{getText("settings.ai.localSetup"), getText("settings.ai.cloudSetup")}, SRC_LOCAL);
        llm.add(llmSourceControl, BorderLayout.NORTH);

        // Left column — LOCAL SETUP.
        localCol = transparentPanel(new GridBagLayout());
        GridBagConstraints gc = baseGbc();
        providerControl = new HudSegmentedControl(
                new String[]{getText("settings.localLlm.ollama"), getText("settings.localLlm.lmStudio")}, PROV_LMSTUDIO);
        addLabel(localCol, getText("settings.ai.host"), gc);
        addField(localCol, providerControl, gc, 1, 1.0);

        addressField = makeTextField();
        nextRow(gc);
        addLabel(localCol, getText("settings.ai.address"), gc);
        addField(localCol, addressField, gc, 1, 1.0);

        commandModelField = makeTextField();
        nextRow(gc);
        addLabel(localCol, getText("settings.ai.commandModel"), gc);
        addField(localCol, commandModelField, gc, 1, 1.0);

        queryModelField = makeTextField();
        nextRow(gc);
        addLabel(localCol, getText("settings.ai.queryModel"), gc);
        addField(localCol, queryModelField, gc, 1, 1.0);

        // Right column — CLOUD SETUP.
        rightCol = transparentPanel(new GridBagLayout());
        GridBagConstraints rgc = baseGbc();
        apiKeyField = makePasswordField();
        llmLockCheck = makeCheckBox(getText("settings.cloud.locked"), true);
        // Field + lock in one BorderLayout cell: EAST reserves the checkbox's full width
        // (incl. "LOCKED" text) regardless of GridBag column sizing; field takes the rest.
        JPanel apiKeyRow = transparentPanel(new BorderLayout(HUD_SEP_W, 0));
        apiKeyRow.add(apiKeyField, BorderLayout.CENTER);
        apiKeyRow.add(llmLockCheck, BorderLayout.EAST);
        addLabel(rightCol, getText("settings.ai.apiKey"), rgc);
        addField(rightCol, apiKeyRow, rgc, 1, 1.0);

        nextRow(rgc);
        HudBanner supported = HudBanner.multiline(
                getText("settings.cloud.supportedLlms") + " "
                        + getText("settings.cloud.supportedLlms.names") + " "
                        + getText("settings.cloud.modelAutoSelected"),
                StatusBadge.State.INFO);
        addSpanComponent(rightCol, supported, rgc);

        JPanel llmLeftWrap = transparentPanel(new BorderLayout());
        llmLeftWrap.add(localCol, BorderLayout.NORTH);
        JPanel llmRightWrap = transparentPanel(new BorderLayout());
        llmRightWrap.add(rightCol, BorderLayout.NORTH);
        llm.add(new HudTwoColumns(llmLeftWrap, llmRightWrap), BorderLayout.CENTER);

        // ----- Section: SPEECH (TTS) (flat working zone, §9) -----
        HudSection speechSection = HudSection.flat(getText("settings.ai.section.speech"), new BorderLayout(0, HUD_GAP));
        JPanel tts = speechSection.body();

        // Full-width source switch, no label.
        ttsSourceControl = new HudSegmentedControl(
                new String[]{getText("settings.ai.voice.local"), getText("settings.ai.voice.cloud")}, SRC_CLOUD);
        tts.add(ttsSourceControl, BorderLayout.NORTH);

        // Left column — LOCAL (Kokoro has no configuration). Right column — CLOUD Google TTS key.
        JPanel ttsLeftCol = transparentPanel(new BorderLayout());
        ttsRightCol = transparentPanel(new GridBagLayout());
        GridBagConstraints tgc = baseGbc();
        ttsKeyField = makePasswordField();
        ttsLockCheck = makeCheckBox(getText("settings.cloud.locked"), true);
        JPanel ttsKeyRow = transparentPanel(new BorderLayout(HUD_SEP_W, 0));
        ttsKeyRow.add(ttsKeyField, BorderLayout.CENTER);
        ttsKeyRow.add(ttsLockCheck, BorderLayout.EAST);
        addLabel(ttsRightCol, getText("settings.ai.googleTtsKey"), tgc);
        addField(ttsRightCol, ttsKeyRow, tgc, 1, 1.0);

        JPanel ttsRightWrap = transparentPanel(new BorderLayout());
        ttsRightWrap.add(ttsRightCol, BorderLayout.NORTH);
        tts.add(new HudTwoColumns(ttsLeftCol, ttsRightWrap), BorderLayout.CENTER);

        // ----- Footer controls -----
        saveButton = makeButton(getText("button.save"));
        saveButton.addActionListener(e -> save());
        saveButton.setEnabled(false); // nothing to save until something changes
        JButton restoreButton = makeButtonSubtle(getText("button.restoreDefaults"));
        restoreButton.addActionListener(e -> SwingUtilities.invokeLater(this::restoreDefaults));

        // Unsaved-changes hint as a footer status (§10): shared HudUnsavedHint, just left of SAVE.
        unsavedLabel = new HudUnsavedHint();

        // Restore on the left; the status + Save (primary) grouped at the right edge (§10).
        JPanel rightGroup = transparentPanel(new FlowLayout(FlowLayout.RIGHT, HUD_GAP, 0));
        rightGroup.add(unsavedLabel);
        rightGroup.add(saveButton);
        JPanel buttons = transparentPanel(new BorderLayout());
        buttons.add(restoreButton, BorderLayout.WEST);
        buttons.add(rightGroup, BorderLayout.EAST);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, saveButton.getPreferredSize().height));

        JPanel content = transparentPanel(null);
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(HUD_PADDING, HUD_PADDING, HUD_PADDING, HUD_PADDING));
        content.add(llmSection);
        content.add(Box.createVerticalStrut(HUD_GAP));
        content.add(speechSection);
        content.add(Box.createVerticalStrut(HUD_GAP));
        content.add(buttons);

        add(content, BorderLayout.NORTH);
    }

    private void addSpanComponent(JPanel panel, JComponent comp, GridBagConstraints gc) {
        gc.gridx = 0;
        gc.weightx = 1.0;
        gc.gridwidth = 3;
        gc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(comp, gc);
        gc.gridwidth = 1;
    }

    private void wireListeners() {
        llmSourceControl.addChangeListener(e -> {
            recomputeDirty();
            updateEnablement();
        });
        ttsSourceControl.addChangeListener(e -> {
            recomputeDirty();
            updateEnablement();
        });
        providerControl.addChangeListener(e -> onProviderSwitched());
        llmLockCheck.addItemListener(e -> updateEnablement());
        ttsLockCheck.addItemListener(e -> updateEnablement());

        onTextChange(addressField);
        onTextChange(commandModelField);
        onTextChange(queryModelField);
        onTextChange(apiKeyField);
        onTextChange(ttsKeyField);
    }

    private void onTextChange(JTextComponent c) {
        c.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { recomputeDirty(); }
            public void removeUpdate(DocumentEvent e) { recomputeDirty(); }
            public void changedUpdate(DocumentEvent e) { recomputeDirty(); }
        });
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** Re-seeds the working copy and all controls from the session, clearing the dirty flag. */
    public void initData() {
        loading = true;
        try {
            boolean local = systemSession.useLocalCommandLlm() && systemSession.useLocalQueryLlm();
            llmSourceControl.setSelectedIndex(local ? SRC_LOCAL : SRC_CLOUD);

            LocalLlmProvider provider = systemSession.getLocalLlmProvider();
            providerControl.setSelectedIndex(provider == LocalLlmProvider.OLLAMA ? PROV_OLLAMA : PROV_LMSTUDIO);
            shownProvider = provider;

            ollamaAddress = nz(systemSession.getOllamaAddress(), LocalLlmProvider.OLLAMA.getDefaultUrl());
            ollamaCommand = nz(systemSession.getOllamaCommandModel(), "");
            ollamaQuery = nz(systemSession.getOllamaQueryModel(), "");
            lmAddress = nz(systemSession.getLmStudioAddress(), LocalLlmProvider.LMSTUDIO.getDefaultUrl());
            lmCommand = nz(systemSession.getLmStudioCommandModel(), "");
            lmQuery = nz(systemSession.getLmStudioQueryModel(), "");
            loadFields(shownProvider);

            apiKeyField.setText(nz(systemSession.getAiApiKey(), ""));
            ttsKeyField.setText(nz(systemSession.getTtsApiKey(), ""));
            ttsSourceControl.setSelectedIndex(systemSession.useLocalTTS() ? SRC_LOCAL : SRC_CLOUD);

            savedLlmLocal = local;
            savedProvider = provider;
            savedOllamaAddress = ollamaAddress;
            savedOllamaCommand = ollamaCommand;
            savedOllamaQuery = ollamaQuery;
            savedLmAddress = lmAddress;
            savedLmCommand = lmCommand;
            savedLmQuery = lmQuery;
            savedAiKey = nz(systemSession.getAiApiKey(), "");
            savedTtsKey = nz(systemSession.getTtsApiKey(), "");
            savedTtsLocal = systemSession.useLocalTTS();

            updateEnablement();
        } finally {
            loading = false;
        }
        clearDirty();
    }

    /** Discards unsaved edits by reloading from the session. */
    public void reload() {
        initData();
    }

    public boolean isDirty() {
        return dirty;
    }

    private void onProviderSwitched() {
        captureFields(shownProvider);
        shownProvider = providerControl.getSelectedIndex() == PROV_OLLAMA
                ? LocalLlmProvider.OLLAMA : LocalLlmProvider.LMSTUDIO;
        loading = true;
        try {
            loadFields(shownProvider);
        } finally {
            loading = false;
        }
        recomputeDirty();
    }

    private void captureFields(LocalLlmProvider provider) {
        if (provider == LocalLlmProvider.OLLAMA) {
            ollamaAddress = addressField.getText();
            ollamaCommand = commandModelField.getText();
            ollamaQuery = queryModelField.getText();
        } else {
            lmAddress = addressField.getText();
            lmCommand = commandModelField.getText();
            lmQuery = queryModelField.getText();
        }
    }

    private void loadFields(LocalLlmProvider provider) {
        boolean ollama = provider == LocalLlmProvider.OLLAMA;
        addressField.setText(ollama ? ollamaAddress : lmAddress);
        commandModelField.setText(ollama ? ollamaCommand : lmCommand);
        queryModelField.setText(ollama ? ollamaQuery : lmQuery);
    }

    /** Enables the active source's controls and dims the unused source (§0.6). */
    private void updateEnablement() {
        // Whole left column dims together (labels, provider switch, fields) via each control's
        // own §0.6 disabled rendering.
        boolean local = llmSourceControl.getSelectedIndex() == SRC_LOCAL;
        for (Component c : localCol.getComponents()) {
            c.setEnabled(local);
        }

        // Right column: dim its labels and the info banner; the API-key field also respects the lock.
        boolean cloud = !local;
        for (Component c : rightCol.getComponents()) {
            if (c instanceof JLabel || c instanceof HudBanner) c.setEnabled(cloud);
        }
        apiKeyField.setEnabled(cloud && !llmLockCheck.isSelected());
        llmLockCheck.setEnabled(cloud);

        boolean ttsCloud = ttsSourceControl.getSelectedIndex() == SRC_CLOUD;
        for (Component c : ttsRightCol.getComponents()) {
            if (c instanceof JLabel) c.setEnabled(ttsCloud);
        }
        ttsKeyField.setEnabled(ttsCloud && !ttsLockCheck.isSelected());
        ttsLockCheck.setEnabled(ttsCloud);
    }

    /** Re-evaluates whether the working copy differs from the last saved snapshot. */
    private void recomputeDirty() {
        if (loading) return;
        setDirty(isModified());
    }

    private boolean isModified() {
        captureFields(shownProvider);
        boolean newLocal = llmSourceControl.getSelectedIndex() == SRC_LOCAL;
        LocalLlmProvider newProvider = providerControl.getSelectedIndex() == PROV_OLLAMA
                ? LocalLlmProvider.OLLAMA : LocalLlmProvider.LMSTUDIO;
        boolean newTtsLocal = ttsSourceControl.getSelectedIndex() == SRC_LOCAL;
        String newAiKey = new String(apiKeyField.getPassword());
        String newTtsKey = new String(ttsKeyField.getPassword());
        return newLocal != savedLlmLocal
                || newProvider != savedProvider
                || newTtsLocal != savedTtsLocal
                || !Objects.equals(newAiKey, savedAiKey)
                || !Objects.equals(newTtsKey, savedTtsKey)
                || !Objects.equals(ollamaAddress, savedOllamaAddress)
                || !Objects.equals(ollamaCommand, savedOllamaCommand)
                || !Objects.equals(ollamaQuery, savedOllamaQuery)
                || !Objects.equals(lmAddress, savedLmAddress)
                || !Objects.equals(lmCommand, savedLmCommand)
                || !Objects.equals(lmQuery, savedLmQuery);
    }

    private void setDirty(boolean modified) {
        if (dirty == modified) return;
        dirty = modified;
        unsavedLabel.setVisible(modified);
        saveButton.setEnabled(modified); // SAVE is only meaningful when there are unsaved edits
        revalidate();
        repaint();
    }

    private void clearDirty() {
        setDirty(false);
    }

    // -------------------------------------------------------------------------
    // Commit
    // -------------------------------------------------------------------------

    /**
     * Atomically persists the whole working copy. If the TTS source changed, a confirmation
     * dialog (voice reset) is shown first; declining it aborts the entire save and leaves the
     * panel in its edited state.
     *
     * @return {@code true} if the configuration was saved, {@code false} if the user aborted
     */
    public boolean save() {
        captureFields(shownProvider);

        boolean newLocal = llmSourceControl.getSelectedIndex() == SRC_LOCAL;
        LocalLlmProvider newProvider = providerControl.getSelectedIndex() == PROV_OLLAMA
                ? LocalLlmProvider.OLLAMA : LocalLlmProvider.LMSTUDIO;
        boolean newTtsLocal = ttsSourceControl.getSelectedIndex() == SRC_LOCAL;
        String newAiKey = new String(apiKeyField.getPassword());
        String newTtsKey = new String(ttsKeyField.getPassword());

        boolean oldTtsLocal = systemSession.useLocalTTS();
        if (newTtsLocal != oldTtsLocal) {
            boolean confirmed = HudConfirmDialog.confirm(
                    this,
                    getText("settings.audio.switchTts.title"),
                    getText("settings.audio.switchTts.message"),
                    getText("button.continue"),
                    getText("button.cancel"));
            if (!confirmed) {
                return false; // abort entire save, stay in editing state
            }
            String defaultVoice = newTtsLocal ? KokoroVoices.BELLA.name() : GoogleVoices.EMMA.name();
            ShipManager.getInstance().resetAllVoicesToDefault(defaultVoice);
        }

        // Restart side-effects fire only when the relevant config actually changed.
        boolean oldLocal = systemSession.useLocalCommandLlm() && systemSession.useLocalQueryLlm();
        LocalLlmProvider oldProvider = systemSession.getLocalLlmProvider();
        String oldAiKey = systemSession.getAiApiKey();
        String oldTtsKey = systemSession.getTtsApiKey();
        boolean providerCfgChanged =
                !Objects.equals(systemSession.getOllamaAddress(), ollamaAddress)
                        || !Objects.equals(systemSession.getOllamaCommandModel(), ollamaCommand)
                        || !Objects.equals(systemSession.getOllamaQueryModel(), ollamaQuery)
                        || !Objects.equals(systemSession.getLmStudioAddress(), lmAddress)
                        || !Objects.equals(systemSession.getLmStudioCommandModel(), lmCommand)
                        || !Objects.equals(systemSession.getLmStudioQueryModel(), lmQuery);
        boolean brainChanged = newLocal != oldLocal || newProvider != oldProvider
                || providerCfgChanged || !Objects.equals(oldAiKey, newAiKey);
        boolean mouthChanged = newTtsLocal != oldTtsLocal || !Objects.equals(oldTtsKey, newTtsKey);

        systemSession.setOllamaSettings(ollamaAddress, ollamaCommand, ollamaQuery);
        systemSession.setLmStudioSettings(lmAddress, lmCommand, lmQuery);
        systemSession.setLocalLlmProvider(newProvider);
        systemSession.setUseLocalCommandLlm(newLocal);
        systemSession.setUseLocalQueryLlm(newLocal);
        systemSession.setAiApiKey(newAiKey);
        systemSession.setUseLocalTTS(newTtsLocal);
        systemSession.setTtsApiKey(newTtsKey);

        EventBusManager.publish(new AppLogEvent("AI services config saved"));
        if (brainChanged) EventBusManager.publish(new RestartBrainEvent());
        if (mouthChanged) {
            EventBusManager.publish(new TTSProviderChangedEvent());
            EventBusManager.publish(new RestartMouthEvent());
        }

        savedLlmLocal = newLocal;
        savedProvider = newProvider;
        savedOllamaAddress = ollamaAddress;
        savedOllamaCommand = ollamaCommand;
        savedOllamaQuery = ollamaQuery;
        savedLmAddress = lmAddress;
        savedLmCommand = lmCommand;
        savedLmQuery = lmQuery;
        savedAiKey = newAiKey;
        savedTtsKey = newTtsKey;
        savedTtsLocal = newTtsLocal;

        clearDirty();
        return true;
    }

    /** Resets the LLM configuration to defaults and commits immediately. */
    private void restoreDefaults() {
        loading = true;
        try {
            ollamaAddress = LocalLlmProvider.OLLAMA.getDefaultUrl();
            ollamaCommand = DEFAULT_MODEL;
            ollamaQuery = DEFAULT_MODEL;
            lmAddress = LocalLlmProvider.LMSTUDIO.getDefaultUrl();
            lmCommand = DEFAULT_MODEL;
            lmQuery = DEFAULT_MODEL;
            providerControl.setSelectedIndex(PROV_LMSTUDIO);
            shownProvider = LocalLlmProvider.LMSTUDIO;
            llmSourceControl.setSelectedIndex(SRC_LOCAL);
            loadFields(LocalLlmProvider.LMSTUDIO);
            updateEnablement();
        } finally {
            loading = false;
        }
        save();
    }

    private static String nz(String value, String fallback) {
        return value != null && !value.isEmpty() ? value : fallback;
    }
}
