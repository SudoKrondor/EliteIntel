package elite.intel.ui.screen.settings;

import elite.intel.ai.ProviderEnum;
import elite.intel.ai.mouth.TtsProvider;
import elite.intel.db.managers.ShipManager;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.RestartBrainEvent;
import elite.intel.ui.event.RestartMouthEvent;
import elite.intel.ui.event.TTSProviderChangedEvent;
import elite.intel.ui.widget.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.Objects;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.*;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * Unified AI services tab: routes the language model (LLM) and speech (TTS) each between a
 * LOCAL and a CLOUD source via {@link HudSegmentedControl} switches, with the active source's
 * configuration highlighted and the unused one dimmed (section 0.6). Speech has three engines for
 * those two slots: LOCAL is Kokoro or Supertonic, chosen with a switch of its own, and CLOUD is Google
 * (API key).
 * <p>
 * Persistence is transactional: no control writes to {@link SystemSession} on its own. All edits
 * live in an in-memory working copy and are committed atomically by {@link #save()} (the only point
 * that fires {@link RestartBrainEvent}/{@link RestartMouthEvent}). This removes the previous
 * cross-panel "Use" checkbox synchronisation and the risk of half-saved, inconsistent state.
 */
public class AiServicesSettingsPanel extends JPanel {

    private static final String DEFAULT_MODEL = "google/gemma-4-e4b";
    /**
     * LM Studio's out-of-the-box OpenAI-compatible endpoint; the only local host this app talks to.
     */
    private static final String DEFAULT_LOCAL_ADDRESS = "http://localhost:1234/v1/chat/completions";

    private static final int SRC_LOCAL = 0;
    private static final int SRC_CLOUD = 1;
    private static final int ENGINE_KOKORO = 0;
    private static final int ENGINE_SUPERTONIC = 1;

    private final SystemSession systemSession = SystemSession.getInstance();

    // -- Controls --------------------------------------------------------------
    private HudSegmentedControl llmSourceControl;
    private JTextField addressField;
    private JTextField commandModelField;
    private HudComboBox<ProviderOption> providerCombo;
    private JPasswordField apiKeyField;
    /**
     * Why SAVE is greyed out while the cloud setup is half-filled: no provider picked, or no key for it.
     */
    private HudBanner cloudIncompleteHint;
    private JCheckBox llmLockCheck;

    private HudSegmentedControl ttsSourceControl;
    private HudSegmentedControl ttsLocalEngineControl;
    private JLabel ttsKeyLabel;
    private JPasswordField ttsKeyField;
    private JCheckBox ttsLockCheck;
    /**
     * Google WaveNet is the only engine that reads a pitch, so the slider lives in Google's segment alone.
     */
    private JLabel googleWaveNetPitchLabel;
    private HudSlider googleWaveNetPitchSlider;
    /**
     * Supertonic's output boost: Supertonic alone reads it, so the slider lives in Supertonic's segment.
     */
    private JLabel supertonicBoostLabel;
    private HudSlider supertonicBoostSlider;

    private JPanel localCol;
    private JPanel rightCol;
    private HudBanner ttsKokoroHint;
    private HudBanner ttsSupertonicHint;
    /**
     * Supertonic's boost row and banner together: shown while Supertonic is the local engine, hidden for Kokoro.
     */
    private JPanel ttsSupertonicConfig;
    private HudBanner ttsGoogleHint;
    private JButton saveButton;
    private JLabel unsavedLabel;

    // -- Working copy (in memory, committed only by save) ----------------------
    private String lmAddress = "", lmCommand = "";

    // Snapshot of the last saved state; the dirty flag tracks whether edits differ from it,
    // so reverting values back to saved clears the "unsaved changes" banner.
    private boolean savedLlmLocal;
    private String savedLmAddress = "", savedLmCommand = "";
    private String savedAiKey = "", savedTtsKey = "";
    /**
     * The stored provider, {@code null} for none.
     */
    private ProviderEnum savedProvider;
    private TtsProvider savedTtsProvider = TtsProvider.KOKORO;
    private int savedGoogleWaveNetPitch;
    private int savedSupertonicBoost;

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
        setBackground(HUD_COLOR_ROLE_APPLICATION_BACKGROUND);

        // ----- Section: LANGUAGE MODEL (flat working zone, section 9) -----
        HudSection llmSection = HudSection.flat(getText("settings.ai.section.llm"), new BorderLayout(0, HUD_GAP));
        JPanel llm = llmSection.body();

        // Full-width source switch, no label: the segment captions name each setup.
        llmSourceControl = new HudSegmentedControl(
                new String[]{getText("settings.ai.localSetup"), getText("settings.ai.cloudSetup")}, SRC_LOCAL);
        llm.add(llmSourceControl, BorderLayout.NORTH);

        // Left column - LOCAL SETUP (LM Studio: address + served model).
        localCol = transparentPanel(new GridBagLayout());
        GridBagConstraints gc = baseGbc();
        addressField = makeTextField();
        addLabel(localCol, getText("settings.ai.address"), gc, 0);
        addField(localCol, addressField, gc, 1, 1.0);

        commandModelField = makeTextField();
        nextRow(gc);
        addLabel(localCol, getText("settings.ai.model"), gc, 0);
        addField(localCol, commandModelField, gc, 1, 1.0);

        // Right column - CLOUD SETUP.
        rightCol = transparentPanel(new GridBagLayout());
        GridBagConstraints rgc = baseGbc();
        providerCombo = new HudComboBox<>(providerOptions(),
                option -> option.provider() == null
                        ? getText("settings.ai.provider.select")
                        : option.provider().displayName(),
                option -> option.provider() == null);
        addLabel(rightCol, getText("settings.ai.provider"), rgc, 0);
        addField(rightCol, providerCombo, rgc, 1, 1.0);

        nextRow(rgc);
        apiKeyField = makePasswordField();
        llmLockCheck = makeCheckBox(getText("settings.cloud.locked"), true);
        // Field + lock in one BorderLayout cell: EAST reserves the checkbox's full width
        // (incl. "LOCKED" text) regardless of GridBag column sizing; field takes the rest.
        JPanel apiKeyRow = transparentPanel(new BorderLayout(HUD_SEP_W, 0));
        apiKeyRow.add(apiKeyField, BorderLayout.CENTER);
        apiKeyRow.add(llmLockCheck, BorderLayout.EAST);
        addLabel(rightCol, getText("settings.ai.apiKey"), rgc, 0);
        addField(rightCol, apiKeyRow, rgc, 1, 1.0);

        nextRow(rgc);
        cloudIncompleteHint = HudBanner.multiline(getText("settings.ai.provider.incomplete"), StatusBadge.State.STANDBY);
        addSpanComponent(rightCol, cloudIncompleteHint, rgc);

        nextRow(rgc);
        // The dropdown lists the supported providers; what it cannot say is that the model follows from the choice.
        HudBanner modelAutoSelected = HudBanner.multiline(getText("settings.cloud.modelAutoSelected"), StatusBadge.State.INFO);
        addSpanComponent(rightCol, modelAutoSelected, rgc);

        JPanel llmLeftWrap = transparentPanel(new BorderLayout());
        llmLeftWrap.add(localCol, BorderLayout.NORTH);
        JPanel llmRightWrap = transparentPanel(new BorderLayout());
        llmRightWrap.add(rightCol, BorderLayout.NORTH);
        llm.add(new HudTwoColumns(llmLeftWrap, llmRightWrap), BorderLayout.CENTER);

        // ----- Section: SPEECH (TTS) (flat working zone, section 9) -----
        HudSection speechSection = HudSection.flat(getText("settings.ai.section.speech"), new BorderLayout(0, HUD_GAP));
        JPanel tts = speechSection.body();

        // Full-width source switch, no label.
        ttsSourceControl = new HudSegmentedControl(
                new String[]{getText("settings.ai.voice.local"), getText("settings.ai.voice.cloud")}, SRC_CLOUD);
        tts.add(ttsSourceControl, BorderLayout.NORTH);

        // Left column - LOCAL: two engines that speak on this machine and have nothing to configure, chosen
        // with a switch of their own. Kokoro is the default. Right column - CLOUD: Google, the one cloud engine, so no switch - just its
        // key row and WaveNet pitch above its banner.
        JPanel ttsLeftCol = transparentPanel(new BorderLayout(0, HUD_GAP));
        ttsLocalEngineControl = new HudSegmentedControl(
                new String[]{getText("settings.ai.voice.kokoro"), getText("settings.ai.voice.supertonic")},
                ENGINE_KOKORO);
        ttsLeftCol.add(ttsLocalEngineControl, BorderLayout.NORTH);
        // One banner is shown at a time: the selected engine's, or the "why Kokoro is greyed out" one.
        ttsKokoroHint = HudBanner.multiline(getText("settings.ai.voice.local.hint"), StatusBadge.State.INFO);
        ttsSupertonicHint = HudBanner.multiline(getText("settings.ai.voice.supertonic.hint"), StatusBadge.State.INFO);
        // Supertonic: its boost row above its banner, mirroring Google's pitch row on the cloud side.
        JPanel supertonicForm = transparentPanel(new GridBagLayout());
        GridBagConstraints sgc = baseGbc();
        supertonicBoostSlider = new HudSlider(
                SystemSession.SUPERTONIC_BOOST_MIN, SystemSession.SUPERTONIC_BOOST_MAX, 1,
                SystemSession.SUPERTONIC_BOOST_MIN);
        supertonicBoostLabel = addLabel(supertonicForm, getText("settings.ai.supertonicBoost"), sgc, 0);
        supertonicBoostLabel.setPreferredSize(
                new Dimension(supertonicBoostLabel.getPreferredSize().width, HUD_SLIDER_HEIGHT));
        supertonicBoostLabel.setBorder(BorderFactory.createEmptyBorder(HUD_SLIDER_VALUE_AREA, 0, 0, 0));
        addField(supertonicForm, supertonicBoostSlider, sgc, 1, 1.0);
        ttsSupertonicConfig = transparentPanel(new BorderLayout(0, HUD_GAP));
        ttsSupertonicConfig.add(supertonicForm, BorderLayout.NORTH);
        ttsSupertonicConfig.add(ttsSupertonicHint, BorderLayout.CENTER);
        JPanel ttsLocalHints = transparentPanel(new BorderLayout());
        ttsLocalHints.add(ttsKokoroHint, BorderLayout.NORTH);
        ttsLocalHints.add(ttsSupertonicConfig, BorderLayout.CENTER);
        ttsLeftCol.add(ttsLocalHints, BorderLayout.CENTER);

        JPanel ttsRightCol = transparentPanel(new BorderLayout(0, HUD_GAP));
        JPanel ttsKeyForm = transparentPanel(new GridBagLayout());
        GridBagConstraints tgc = baseGbc();
        ttsKeyField = makePasswordField();
        ttsLockCheck = makeCheckBox(getText("settings.cloud.locked"), true);
        JPanel ttsKeyRow = transparentPanel(new BorderLayout(HUD_SEP_W, 0));
        ttsKeyRow.add(ttsKeyField, BorderLayout.CENTER);
        ttsKeyRow.add(ttsLockCheck, BorderLayout.EAST);
        ttsKeyLabel = addLabel(ttsKeyForm, getText("settings.ai.googleTtsKey"), tgc, 0);
        addField(ttsKeyForm, ttsKeyRow, tgc, 1, 1.0);
        nextRow(tgc);
        googleWaveNetPitchSlider = new HudSlider(
                SystemSession.GOOGLE_WAVENET_PITCH_MIN, SystemSession.GOOGLE_WAVENET_PITCH_MAX, 1,
                SystemSession.GOOGLE_WAVENET_PITCH_MIN);
        googleWaveNetPitchLabel = addLabel(ttsKeyForm, getText("settings.ai.googleWaveNetPitch"), tgc, 0);
        // Drop the label from the row top to the track, the slider's own layout being value above, track below.
        googleWaveNetPitchLabel.setPreferredSize(
                new Dimension(googleWaveNetPitchLabel.getPreferredSize().width, HUD_SLIDER_HEIGHT));
        googleWaveNetPitchLabel.setBorder(BorderFactory.createEmptyBorder(HUD_SLIDER_VALUE_AREA, 0, 0, 0));
        addField(ttsKeyForm, googleWaveNetPitchSlider, tgc, 1, 1.0);
        ttsGoogleHint = HudBanner.multiline(getText("settings.ai.voice.google.hint"), StatusBadge.State.INFO);
        ttsRightCol.add(ttsKeyForm, BorderLayout.NORTH);
        ttsRightCol.add(ttsGoogleHint, BorderLayout.CENTER);

        tts.add(new HudTwoColumns(ttsLeftCol, ttsRightCol), BorderLayout.CENTER);

        // ----- Footer controls -----
        saveButton = makeButton(getText("button.save"));
        saveButton.addActionListener(e -> save());
        saveButton.setEnabled(false); // nothing to save until something changes
        JButton restoreButton = makeButtonSubtle(getText("button.restoreDefaults"));
        restoreButton.addActionListener(e -> SwingUtilities.invokeLater(this::restoreDefaults));

        // Unsaved-changes hint as a footer status (section 10): shared HudUnsavedHint, just left of SAVE.
        unsavedLabel = new HudUnsavedHint();

        // Restore on the left; the status + Save (primary) grouped at the right edge (section 10).
        JPanel rightGroup = transparentPanel(new FlowLayout(FlowLayout.RIGHT, HUD_GAP, 0));
        rightGroup.add(unsavedLabel);
        rightGroup.add(saveButton);
        JPanel buttons = transparentPanel(new BorderLayout());
        buttons.add(restoreButton, BorderLayout.WEST);
        buttons.add(rightGroup, BorderLayout.EAST);
        // Footer buttons anchor to the bottom of the tab, just above the SettingsTabPanel footer rail,
        // with the standard padding: HUD_PADDING sides (aligns with content) + a gap above the rail.
        buttons.setBorder(BorderFactory.createEmptyBorder(0, HUD_PADDING, HUD_PADDING, HUD_PADDING));

        JPanel content = transparentPanel(null);
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(HUD_PADDING, HUD_PADDING, HUD_PADDING, HUD_PADDING));
        content.add(llmSection);
        content.add(Box.createVerticalStrut(HUD_GAP));
        content.add(speechSection);

        add(content, BorderLayout.NORTH);
        add(buttons, BorderLayout.SOUTH);
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
        ttsLocalEngineControl.addChangeListener(e -> {
            recomputeDirty();
            updateEnablement();
        });
        llmLockCheck.addItemListener(e -> updateEnablement());
        providerCombo.addActionListener(e -> onProviderPicked());
        ttsLockCheck.addItemListener(e -> updateEnablement());
        googleWaveNetPitchSlider.addChangeListener(e -> recomputeDirty());
        supertonicBoostSlider.addChangeListener(e -> recomputeDirty());

        onTextChange(addressField);
        onTextChange(commandModelField);
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

            lmAddress = nz(systemSession.getLmStudioAddress(), DEFAULT_LOCAL_ADDRESS);
            lmCommand = nz(systemSession.getLmStudioCommandModel(), "");
            loadFields();

            savedProvider = systemSession.getLlmProvider().orElse(null);
            selectProvider(savedProvider);
            apiKeyField.setText(nz(systemSession.getAiApiKey(), ""));
            String storedTtsKey = nz(systemSession.getTtsApiKey(), "");
            ttsKeyField.setText(storedTtsKey);
            int storedPitch = systemSession.getGoogleWaveNetPitch();
            googleWaveNetPitchSlider.setValue(storedPitch);
            int storedBoost = systemSession.getSupertonicBoostPercent();
            supertonicBoostSlider.setValue(storedBoost);
            TtsProvider ttsProvider = systemSession.getTtsProvider();
            ttsSourceControl.setSelectedIndex(ttsProvider.isLocal() ? SRC_LOCAL : SRC_CLOUD);
            if (ttsProvider.isLocal()) {
                ttsLocalEngineControl.setSelectedIndex(
                        ttsProvider == TtsProvider.SUPERTONIC ? ENGINE_SUPERTONIC : ENGINE_KOKORO);
            }

            savedLlmLocal = local;
            savedLmAddress = lmAddress;
            savedLmCommand = lmCommand;
            savedAiKey = nz(systemSession.getAiApiKey(), "");
            savedTtsKey = storedTtsKey;
            savedTtsProvider = ttsProvider;
            savedGoogleWaveNetPitch = storedPitch;
            savedSupertonicBoost = storedBoost;

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

    private void captureFields() {
        lmAddress = addressField.getText();
        lmCommand = commandModelField.getText();
    }

    private void loadFields() {
        addressField.setText(lmAddress);
        commandModelField.setText(lmCommand);
    }

    /** Enables the active source's controls and dims the unused source (section 0.6). */
    private void updateEnablement() {
        // Whole left column dims together (labels and fields) via each control's own
        // section 0.6 disabled rendering.
        boolean local = llmSourceControl.getSelectedIndex() == SRC_LOCAL;
        for (Component c : localCol.getComponents()) {
            c.setEnabled(local);
        }

        // Right column: dim its labels and the info banner; the API-key field also respects the lock.
        boolean cloud = !local;
        for (Component c : rightCol.getComponents()) {
            if (c instanceof JLabel || c instanceof HudBanner) c.setEnabled(cloud);
        }
        providerCombo.setEnabled(cloud);
        apiKeyField.setEnabled(cloud && !llmLockCheck.isSelected());
        cloudIncompleteHint.setVisible(isCloudSetupIncomplete());
        llmLockCheck.setEnabled(cloud);

        // Each TTS column lives or dies with the source switch. On the local side its engine switch picks
        // which engine's hint is shown; the other engine's is not dimmed but absent.
        boolean ttsCloud = ttsSourceControl.getSelectedIndex() == SRC_CLOUD;
        boolean supertonic = ttsLocalEngineControl.getSelectedIndex() == ENGINE_SUPERTONIC;
        ttsLocalEngineControl.setEnabled(!ttsCloud);
        ttsKokoroHint.setVisible(!supertonic);
        ttsSupertonicConfig.setVisible(supertonic);
        ttsKokoroHint.setEnabled(!ttsCloud);
        ttsSupertonicHint.setEnabled(!ttsCloud);
        supertonicBoostLabel.setEnabled(!ttsCloud);
        supertonicBoostSlider.setEnabled(!ttsCloud);
        ttsGoogleHint.setEnabled(ttsCloud);
        ttsKeyLabel.setEnabled(ttsCloud);
        ttsKeyField.setEnabled(ttsCloud && !ttsLockCheck.isSelected());
        ttsLockCheck.setEnabled(ttsCloud);
        googleWaveNetPitchLabel.setEnabled(ttsCloud);
        googleWaveNetPitchSlider.setEnabled(ttsCloud);
    }

    /** Re-evaluates whether the working copy differs from the last saved snapshot. */
    private void recomputeDirty() {
        if (loading) return;
        setDirty(isModified());
        updateSaveEnablement();
        cloudIncompleteHint.setVisible(isCloudSetupIncomplete());
    }

    private boolean isModified() {
        captureFields();
        boolean newLocal = llmSourceControl.getSelectedIndex() == SRC_LOCAL;
        String newAiKey = new String(apiKeyField.getPassword());
        String newTtsKey = new String(ttsKeyField.getPassword());
        return newLocal != savedLlmLocal
                || selectedProvider() != savedProvider
                || selectedTtsProvider() != savedTtsProvider
                || googleWaveNetPitchSlider.getValue() != savedGoogleWaveNetPitch
                || supertonicBoostSlider.getValue() != savedSupertonicBoost
                || !Objects.equals(newAiKey, savedAiKey)
                || !Objects.equals(newTtsKey, savedTtsKey)
                || !Objects.equals(lmAddress, savedLmAddress)
                || !Objects.equals(lmCommand, savedLmCommand);
    }

    private void setDirty(boolean modified) {
        if (dirty == modified) return;
        dirty = modified;
        unsavedLabel.setVisible(modified);
        updateSaveEnablement();
        revalidate();
        repaint();
    }

    private void clearDirty() {
        setDirty(false);
    }

    /**
     * SAVE is only meaningful when there are unsaved edits, and only possible when they are complete.
     */
    private void updateSaveEnablement() {
        saveButton.setEnabled(dirty && !isCloudSetupIncomplete());
    }

    /**
     * True while the cloud source is selected without a provider or without a key: a setup that could not
     * answer the commander, so it is not saved. The local source does not need either.
     */
    private boolean isCloudSetupIncomplete() {
        return llmSourceControl.getSelectedIndex() == SRC_CLOUD
                && (selectedProvider() == null || apiKeyField.getPassword().length == 0);
    }

    // -------------------------------------------------------------------------
    // Commit
    // -------------------------------------------------------------------------

    /**
     * Atomically persists the whole working copy. If the TTS engine changed, a confirmation
     * dialog (voice reset) is shown first; declining it aborts the entire save and leaves the
     * panel in its edited state.
     *
     * @return {@code true} if the configuration was saved, {@code false} if the user aborted
     */
    public boolean save() {
        captureFields();

        if (isCloudSetupIncomplete()) {
            return false; // SAVE is greyed out for this; the tab-switch prompt can still ask
        }
        boolean newLocal = llmSourceControl.getSelectedIndex() == SRC_LOCAL;
        ProviderEnum newProvider = selectedProvider();
        TtsProvider newTtsProvider = selectedTtsProvider();
        String newAiKey = new String(apiKeyField.getPassword());
        String newTtsKey = new String(ttsKeyField.getPassword());
        int newPitch = googleWaveNetPitchSlider.getValue();
        int newBoost = supertonicBoostSlider.getValue();

        // Every engine names its voices differently, so any engine change - not just local/cloud - invalidates
        // the fleet's stored voices.
        TtsProvider oldTtsProvider = systemSession.getTtsProvider();
        if (newTtsProvider != oldTtsProvider) {
            boolean confirmed = HudConfirmDialog.confirm(
                    this,
                    getText("settings.audio.switchTts.title"),
                    getText("settings.audio.switchTts.message"),
                    getText("button.continue"),
                    getText("button.cancel"));
            if (!confirmed) {
                return false; // abort entire save, stay in editing state
            }
            ShipManager.getInstance().resetAllVoicesToDefault(newTtsProvider.defaultVoiceName());
        }

        // Restart side-effects fire only when the relevant config actually changed.
        boolean oldLocal = systemSession.useLocalCommandLlm() && systemSession.useLocalQueryLlm();
        String oldAiKey = systemSession.getAiApiKey();
        String oldTtsKey = systemSession.getTtsApiKey();
        boolean localCfgChanged =
                !Objects.equals(systemSession.getLmStudioAddress(), lmAddress)
                        || !Objects.equals(systemSession.getLmStudioCommandModel(), lmCommand);
        boolean brainChanged = newLocal != oldLocal
                || localCfgChanged || !Objects.equals(oldAiKey, newAiKey)
                || newProvider != systemSession.getLlmProvider().orElse(null);
        boolean mouthChanged = newTtsProvider != oldTtsProvider || !Objects.equals(oldTtsKey, newTtsKey);

        systemSession.setLmStudioSettings(lmAddress, lmCommand);
        systemSession.setUseLocalCommandLlm(newLocal);
        systemSession.setUseLocalQueryLlm(newLocal);
        systemSession.setAiApiKey(newAiKey);
        systemSession.setLlmProvider(newProvider);
        systemSession.setTtsProvider(newTtsProvider);
        systemSession.setTtsApiKey(newTtsKey);
        // Google reads the pitch per request, so a new value needs no restart - just the save.
        systemSession.setGoogleWaveNetPitch(newPitch);
        // Supertonic reads its boost per utterance, so the same holds.
        systemSession.setSupertonicBoostPercent(newBoost);

        UiBus.publish(new AppLogEvent("AI services config saved"));
        if (brainChanged) UiBus.publish(new RestartBrainEvent());
        if (mouthChanged) {
            UiBus.publish(new TTSProviderChangedEvent());
            UiBus.publish(new RestartMouthEvent());
        }

        savedLlmLocal = newLocal;
        savedLmAddress = lmAddress;
        savedLmCommand = lmCommand;
        savedAiKey = newAiKey;
        savedProvider = newProvider;
        savedTtsKey = newTtsKey;
        savedTtsProvider = newTtsProvider;
        savedGoogleWaveNetPitch = newPitch;
        savedSupertonicBoost = newBoost;

        clearDirty();
        return true;
    }

    /** Resets the LLM configuration to defaults and commits immediately. */
    private void restoreDefaults() {
        loading = true;
        try {
            lmAddress = DEFAULT_LOCAL_ADDRESS;
            lmCommand = DEFAULT_MODEL;
            llmSourceControl.setSelectedIndex(SRC_LOCAL);
            loadFields();
            updateEnablement();
        } finally {
            loading = false;
        }
        save();
    }

    /**
     * The engine the controls currently describe: LOCAL is Kokoro unless its engine switch says Supertonic,
     * and CLOUD is Google.
     */
    private TtsProvider selectedTtsProvider() {
        return ttsSourceControl.getSelectedIndex() == SRC_LOCAL
                ? (ttsLocalEngineControl.getSelectedIndex() == ENGINE_SUPERTONIC ? TtsProvider.SUPERTONIC : TtsProvider.KOKORO)
                : TtsProvider.GOOGLE;
    }

    /**
     * A key belongs to one provider, so picking a different one than the stored provider empties the key field
     * (and unlocks it) for that provider's key, and picking the stored one back restores its key: an accidental
     * pick costs nothing. An install with a key but no stored provider - one whose key format named none - keeps
     * its key, because picking the provider is all it needs.
     */
    private void onProviderPicked() {
        if (loading) return;
        if (savedProvider != null) {
            boolean backToSaved = selectedProvider() == savedProvider;
            apiKeyField.setText(backToSaved ? savedAiKey : "");
            if (!backToSaved) {
                llmLockCheck.setSelected(false);
            }
        }
        recomputeDirty();
        updateEnablement();
    }

    /**
     * The provider the dropdown shows, {@code null} while it shows the "select a provider" placeholder.
     */
    private ProviderEnum selectedProvider() {
        ProviderOption option = (ProviderOption) providerCombo.getSelectedItem();
        return option == null ? null : option.provider();
    }

    private void selectProvider(ProviderEnum provider) {
        for (int i = 0; i < providerCombo.getItemCount(); i++) {
            if (providerCombo.getItemAt(i).provider() == provider) {
                providerCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * The placeholder (no provider) first, then every provider.
     */
    private static ProviderOption[] providerOptions() {
        ProviderEnum[] providers = ProviderEnum.values();
        ProviderOption[] options = new ProviderOption[providers.length + 1];
        options[0] = new ProviderOption(null);
        for (int i = 0; i < providers.length; i++) {
            options[i + 1] = new ProviderOption(providers[i]);
        }
        return options;
    }

    /**
     * One dropdown entry; {@code provider} is {@code null} for the "select a provider" placeholder.
     */
    private record ProviderOption(ProviderEnum provider) {
    }

    private static String nz(String value, String fallback) {
        return value != null && !value.isEmpty() ? value : fallback;
    }
}
