package elite.intel.ui.view;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.actions.customcommand.CustomCommandRegistry;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.LoadGameEvent;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.*;
import elite.intel.ui.telemetry.LlmSessionStatsSnapshot;
import elite.intel.ui.telemetry.LlmSessionStatsTracker;
import elite.intel.util.SleepNoThrow;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.view.AppTheme.*;

public class AiTabPanel extends JPanel {

    private JButton wakeWordButton;
    private JButton obsOverlayButton;
    private boolean obsOverlayVisible;
    private final OBSOverlayWindow[] obsOverlay = {null};

    private JButton startStopServicesButton;
    private JButton recalibrateAudioButton;
    private JButton audioDevicesButton;
    private HudUpdateButton updateAppButton;
    private HudCommanderBlock commanderBlock;
    private final AtomicBoolean isServiceRunning = new AtomicBoolean(false);

    private HudLogArea userPanel;
    private HudLogArea aiPanel;
    private HudLogArea systemPanel;

    // QUICK STATUS readouts
    private HudStatusReadout sttBadge;
    private HudStatusReadout llmBadge;
    private HudStatusReadout ttsBadge;
    private HudStatusReadout bindingsBadge;
    private HudStatusReadout commandsBadge;
    private HudStatusReadout keymapBadge;
    private boolean sleeping;
    private String lastLlmProvider;

    // SYSTEM SUMMARY telemetry blocks
    private HudTelemetryBlock modelBlock;
    private HudTelemetryBlock sessionTimeBlock;
    private HudTelemetryBlock tokensBlock;
    private HudTelemetryBlock tphBlock;
    private HudTelemetryBlock cacheBlock;
    private HudTelemetryBlock speedBlock;

    @SuppressWarnings("unused")
    private final Timer summaryClockTimer;
    private final Font monoFont;

    public AiTabPanel(Font monoFont) {
        this.monoFont = monoFont;
        LlmSessionStatsTracker.getInstance(); // ensure tracker is registered before events flow
        sleeping = SystemSession.getInstance().isSleepingModeOn();
        EventBusManager.register(this);
        buildUi();
        summaryClockTimer = new Timer(1_000, e -> tickSummaryClock());
        summaryClockTimer.start();
    }

    public void dispose() {
        summaryClockTimer.stop();
        EventBusManager.unregister(this);
        if (updateAppButton != null) updateAppButton.dispose();
    }

    private static final int SIDEBAR_WIDTH = 220;

    private void buildUi() {
        setLayout(new BorderLayout(HUD_GAP, HUD_GAP));
        setBackground(HUD_BG);
        setBorder(hudDenseScreenBorder());

        // --- Controls wired up, placed in right sidebar SHORTCUTS ---
        startStopServicesButton = makeButtonSubtle(getText("button.startServices"));
        startStopServicesButton.addActionListener(e -> {
            EventBusManager.publish(new ToggleServicesEvent(!isServiceRunning.get()));
            startStopServicesButton.setEnabled(false);
        });

        wakeWordButton = makeButtonSubtle(wakeWordText());
        wakeWordButton.addActionListener(e ->
                EventBusManager.publish(new ToggleWakeWordEvent(!sleeping)));
        wakeWordButton.setEnabled(false);

        obsOverlayButton = makeButtonSubtle(obsOverlayText());
        obsOverlayButton.setToolTipText(getText("ai.obsOverlay.tooltip"));
        obsOverlayButton.addActionListener(e -> SwingUtilities.invokeLater(() -> {
            obsOverlayVisible = !obsOverlayVisible;
            if (obsOverlayVisible) {
                if (obsOverlay[0] == null) obsOverlay[0] = new OBSOverlayWindow();
                obsOverlay[0].setVisible(true);
            } else if (obsOverlay[0] != null) {
                obsOverlay[0].setVisible(false);
            }
            obsOverlayButton.setText(obsOverlayText());
        }));

        recalibrateAudioButton = makeButtonSubtle(getText("button.calibrateAudio"));
        recalibrateAudioButton.setEnabled(false);
        recalibrateAudioButton.addActionListener(e -> EventBusManager.publish(new RecalibrateAudioEvent()));

        audioDevicesButton = makeButtonSubtle(getText("button.audioDevices"));
        audioDevicesButton.addActionListener(e ->
                new AudioInterfaceDialog(AiTabPanel.this).setVisible(true));

        updateAppButton = new HudUpdateButton(false);

        // --- Log panels ---
        userPanel = new HudLogArea(30, HudLogArea.Style.USER_INPUT);

        aiPanel = new HudLogArea(25, HudLogArea.Style.AI_RESPONSE);

        systemPanel = new HudLogArea(12, HudLogArea.Style.SYSTEM_LOG);

        // --- Main log area (user/ai top, system below) ---
        HudSplitPane topSplit = new HudSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                logSection(getText("ai.section.userInput"), userPanel),
                logSection(getText("ai.section.aiResponse"), aiPanel)
        );
        topSplit.setResizeWeight(0.38);

        HudSplitPane mainSplit = new HudSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                topSplit,
                logSection(getText("ai.section.systemMessages"), systemPanel)
        );
        mainSplit.setResizeWeight(0.65);

        // --- Right sidebar ---
        JPanel sidebar = transparentPanel(new BorderLayout(0, HUD_GAP));
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));

        HudSection quickStatusSection = HudSection.compactCard(getText("ai.section.quickStatus"), new BorderLayout());
        quickStatusSection.body().add(buildQuickStatusPanel(), BorderLayout.NORTH);

        HudSection shortcutsSection = new HudSection(getText("ai.section.shortcuts"), new BorderLayout());
        shortcutsSection.body().add(buildShortcutsPanel(), BorderLayout.CENTER);
        commanderBlock.setCredits(PlayerSession.getInstance().getPersonalCredits());
        commanderBlock.tickClock();

        sidebar.add(quickStatusSection, BorderLayout.NORTH);
        sidebar.add(shortcutsSection, BorderLayout.CENTER);

        // --- Center: main logs + sidebar ---
        JPanel centerPanel = transparentPanel(new BorderLayout(HUD_GAP, 0));
        centerPanel.add(mainSplit, BorderLayout.CENTER);
        centerPanel.add(sidebar, BorderLayout.EAST);
        add(centerPanel, BorderLayout.CENTER);

        // --- Bottom summary strip ---
        HudSection summarySection = HudSection.compactCard(getText("ai.section.systemSummary"), new BorderLayout());

        modelBlock       = new HudTelemetryBlock(getText("ai.summary.llmModel"),      tryLoadIcon("/images/microchip-ai.png"));
        sessionTimeBlock = new HudTelemetryBlock(getText("ai.summary.sessionTime"),   tryLoadIcon("/images/clock-five.png"));
        tokensBlock      = new HudTelemetryBlock(getText("ai.summary.tokensUsed"),    tryLoadIcon("/images/coins.png"));
        tphBlock         = new HudTelemetryBlock(getText("ai.summary.tokensPerHour"), tryLoadIcon("/images/tachometer-fast.png"));
        cacheBlock       = new HudTelemetryBlock(getText("ai.summary.cacheSaved"),    tryLoadIcon("/images/file-recycle.png"));
        speedBlock       = new HudTelemetryBlock(getText("ai.summary.lastSpeed"),     tryLoadIcon("/images/bolt.png"));

        HudTelemetryStrip strip = new HudTelemetryStrip();
        strip.addBlock(modelBlock);
        strip.addBlock(sessionTimeBlock);
        strip.addBlock(tokensBlock);
        strip.addBlock(tphBlock);
        strip.addBlock(cacheBlock);
        strip.addBlock(speedBlock);

        summarySection.body().add(strip, BorderLayout.CENTER);
        add(summarySection, BorderLayout.SOUTH);
    }

    /**
     * Builds the three-zone SHORTCUTS panel: top runtime buttons, centered commander block,
     * bottom configuration buttons.
     */
    private JPanel buildShortcutsPanel() {
        JPanel root = transparentPanel(new BorderLayout(0, HUD_GAP));

        JPanel top = transparentPanel(null);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JPanel bottom = transparentPanel(null);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

        for (JButton b : new JButton[]{startStopServicesButton, wakeWordButton,
                obsOverlayButton, audioDevicesButton, recalibrateAudioButton,
                updateAppButton}) {
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, AppTheme.HUD_BUTTON_HEIGHT));
        }

        // top: runtime controls
        top.add(startStopServicesButton);
        top.add(Box.createRigidArea(new Dimension(0, HUD_GAP)));
        top.add(wakeWordButton);
        top.add(Box.createRigidArea(new Dimension(0, HUD_GAP)));
        top.add(obsOverlayButton);

        // bottom: audio + update
        bottom.add(audioDevicesButton);
        bottom.add(Box.createRigidArea(new Dimension(0, HUD_GAP)));
        bottom.add(recalibrateAudioButton);
        bottom.add(Box.createRigidArea(new Dimension(0, HUD_GAP)));
        bottom.add(updateAppButton);

        // center: commander identity block, centred in available space
        commanderBlock = new HudCommanderBlock(monoFont);
        JPanel centerWrap = transparentPanel(new GridBagLayout());
        centerWrap.add(commanderBlock, new GridBagConstraints());

        root.add(top, BorderLayout.NORTH);
        root.add(centerWrap, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        return root;
    }

    private HudSection logSection(String title, JComponent content) {
        HudSection section = new HudSection(title, new BorderLayout());
        section.body().add(content, BorderLayout.CENTER);
        return section;
    }

    public void initData(boolean sleepingModeOn, boolean servicesRunning) {
        this.sleeping = sleepingModeOn;
        wakeWordButton.setText(wakeWordText());
        applyServiceState(servicesRunning);
    }

    public void addUserMessage(String text) {
        SwingUtilities.invokeLater(() -> userPanel.addMessage(text));
    }

    public void addAiMessage(String text) {
        SwingUtilities.invokeLater(() -> aiPanel.addMessage(text));
    }

    /** Renders a structured SYSTEM_LOG entry; timestamp is formatted as {@code HH:mm:ss}. */
    public void addSystemMessage(LocalTime timestamp, String text) {
        SwingUtilities.invokeLater(() -> systemPanel.addSystemLogEntry(timestamp, text));
    }

    @Subscribe
    public void onServiceStatusEvent(ServicesStateEvent event) {
        SwingUtilities.invokeLater(() -> {
            SleepNoThrow.sleep(1000);
            applyServiceState(event.isRunning());
        });
    }

    @Subscribe
    public void onStatsChanged(LlmSessionStatsChangedEvent event) {
        SwingUtilities.invokeLater(() -> refreshSummary(event.snapshot()));
    }

    private void applyServiceState(boolean running) {
        isServiceRunning.set(running);
        startStopServicesButton.setText(running ? getText("button.stopServices") : getText("button.startServices"));
        startStopServicesButton.setEnabled(true);
        recalibrateAudioButton.setEnabled(running);
        wakeWordButton.setEnabled(running);
        refreshSttBadge();
        refreshLlmBadge();
        refreshTtsBadge();
    }

    /** Updates summary telemetry blocks from the latest stats snapshot. */
    private void refreshSummary(LlmSessionStatsSnapshot snap) {
        modelBlock.setValue(snap.lastModel());

        int totalTokens = snap.totalPromptTokens() + snap.totalCompletionTokens() + snap.totalCachedHits();
        tokensBlock.setValue(totalTokens > 0 ? fmtTokens(totalTokens) : null);

        int hits = snap.totalCachedHits();
        // Always show numeric value for cache (0 is informative, not "no data")
        cacheBlock.setValue(fmtTokens(hits));

        double tps = snap.lastTps();
        speedBlock.setValue(tps > 0 ? String.format("%.1f t/s", tps) : null);

        updateTph(snap);
    }

    /** Ticks the session-time, tokens-per-hour, and commander clock blocks every second. */
    private void tickSummaryClock() {
        LlmSessionStatsSnapshot snap = LlmSessionStatsTracker.getInstance().getSnapshot();
        Duration d = Duration.between(snap.sessionStart(), java.time.Instant.now());
        sessionTimeBlock.setValue(String.format("%02d:%02d:%02d",
                d.toHours(), d.toMinutesPart(), d.toSecondsPart()));
        updateTph(snap);
        if (commanderBlock != null) commanderBlock.tickClock();
    }

    private void updateTph(LlmSessionStatsSnapshot snap) {
        // promptTokens = API input_tokens (excludes cache reads), so add all three buckets
        long elapsedSeconds = Duration.between(snap.sessionStart(), java.time.Instant.now()).toSeconds();
        if (elapsedSeconds < 600) {
            tphBlock.setValue(null); // collecting data
            return;
        }
        int total = snap.totalPromptTokens() + snap.totalCompletionTokens() + snap.totalCachedHits();
        if (total > 0) {
            long tph = Math.round(total / (elapsedSeconds / 3600.0));
            tphBlock.setValue(fmtTokens(tph) + "/hr");
        } else {
            tphBlock.setValue(null);
        }
    }

    private static String fmtTokens(long v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000) return String.format("%.1fK", v / 1_000.0);
        return String.valueOf(v);
    }

    @Subscribe
    public void onSleepWakeStateChanged(SleepWakeStateChangedEvent event) {
        SwingUtilities.invokeLater(() -> {
            sleeping = event.sleeping();
            wakeWordButton.setText(wakeWordText());
            refreshSttBadge();
        });
    }

    @Subscribe
    public void onLlmUsage(LlmUsageEvent event) {
        SwingUtilities.invokeLater(() -> {
            lastLlmProvider = event.provider();
            refreshLlmBadge();
        });
    }

    @Subscribe
    public void onTtsProviderChanged(TTSProviderChangedEvent event) {
        SwingUtilities.invokeLater(this::refreshTtsBadge);
    }

    // ── QUICK STATUS badge build and refresh ──────────────────────────────────

    /**
     * Builds the QUICK STATUS panel with live STT / LLM / TTS status rows.
     * Badges are initialised from current state and updated via event-driven refresh methods.
     */
    private JPanel buildQuickStatusPanel() {
        boolean running = isServiceRunning.get();

        String sttText = !running ? getText("hud.state.standby")
                : sleeping     ? getText("hud.state.sleeping")
                               : getText("hud.state.listening");
        StatusBadge.State sttState = (running && !sleeping) ? StatusBadge.State.OK : StatusBadge.State.IDLE;
        sttBadge = new HudStatusReadout(getText("hud.stt"), sttText, sttState);

        llmBadge = new HudStatusReadout(getText("hud.llm"),
                running ? getText("hud.state.active") : getText("hud.state.standby"),
                running ? StatusBadge.State.OK : StatusBadge.State.IDLE);

        String ttsText;
        StatusBadge.State ttsState;
        if (!running) {
            ttsText = getText("hud.state.standby");
            ttsState = StatusBadge.State.IDLE;
        } else {
            boolean local = SystemSession.getInstance().useLocalTTS();
            ttsText = local ? getText("hud.tts.local") : getText("hud.tts.cloud");
            ttsState = StatusBadge.State.OK;
        }
        ttsBadge = new HudStatusReadout(getText("hud.tts"), ttsText, ttsState);

        int initCmdCount = CustomCommandRegistry.getInstance().getCustomCommands().size();
        bindingsBadge = new HudStatusReadout(getText("hud.bindings"), "—", StatusBadge.State.STANDBY);
        commandsBadge = new HudStatusReadout(getText("hud.commands"),
                getText("hud.commands.summary", initCmdCount), StatusBadge.State.INFO);
        keymapBadge   = new HudStatusReadout(getText("hud.keymap"), "—", StatusBadge.State.STANDBY);

        JPanel panel = transparentPanel(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(sttBadge);
        panel.add(Box.createRigidArea(new Dimension(0, 3)));
        panel.add(llmBadge);
        panel.add(Box.createRigidArea(new Dimension(0, 3)));
        panel.add(ttsBadge);
        panel.add(Box.createRigidArea(new Dimension(0, 3)));
        panel.add(bindingsBadge);
        panel.add(Box.createRigidArea(new Dimension(0, 3)));
        panel.add(commandsBadge);
        panel.add(Box.createRigidArea(new Dimension(0, 3)));
        panel.add(keymapBadge);
        return panel;
    }

    private void refreshSttBadge() {
        boolean running = isServiceRunning.get();
        if (!running) {
            sttBadge.setValue(getText("hud.state.standby"), StatusBadge.State.IDLE);
        } else if (sleeping) {
            sttBadge.setValue(getText("hud.state.sleeping"), StatusBadge.State.IDLE);
        } else {
            sttBadge.setValue(getText("hud.state.listening"), StatusBadge.State.OK);
        }
    }

    private void refreshLlmBadge() {
        boolean running = isServiceRunning.get();
        if (!running) {
            llmBadge.setValue(getText("hud.state.standby"), StatusBadge.State.IDLE);
        } else if (lastLlmProvider != null && !lastLlmProvider.isBlank()) {
            llmBadge.setValue(lastLlmProvider, StatusBadge.State.OK);
        } else {
            llmBadge.setValue(getText("hud.state.active"), StatusBadge.State.OK);
        }
    }

    private void refreshTtsBadge() {
        boolean running = isServiceRunning.get();
        if (!running) {
            ttsBadge.setValue(getText("hud.state.standby"), StatusBadge.State.IDLE);
        } else {
            boolean local = SystemSession.getInstance().useLocalTTS();
            ttsBadge.setValue(
                    local ? getText("hud.tts.local") : getText("hud.tts.cloud"),
                    StatusBadge.State.OK);
        }
    }

    @Subscribe
    public void onBindingsSummaryChanged(BindingsSummaryChangedEvent event) {
        SwingUtilities.invokeLater(() -> {
            if (event.missing() > 0) {
                bindingsBadge.setValue(getText("hud.bindings.badge.warn", event.missing()), StatusBadge.State.STANDBY);
            } else {
                bindingsBadge.setValue(getText("hud.bindings.badge.ok"), StatusBadge.State.OK);
            }
        });
    }

    @Subscribe
    public void onCustomCommandsSummaryChanged(CustomCommandsSummaryChangedEvent event) {
        SwingUtilities.invokeLater(() ->
                commandsBadge.setValue(getText("hud.commands.summary", event.count()), StatusBadge.State.INFO));
    }

    @Subscribe
    public void onKeymapSyncStateChanged(KeymapSyncStateChangedEvent event) {
        SwingUtilities.invokeLater(() -> {
            StatusBadge.State state = event.inSync() ? StatusBadge.State.OK : StatusBadge.State.STANDBY;
            String text = event.inSync() ? getText("hud.keymap.inSync") : getText("hud.keymap.modified");
            keymapBadge.setValue(text, state);
        });
    }

    private String wakeWordText() {
        // sleeping → offer to wake up; listening → offer to sleep
        return getText(sleeping ? "ai.action.wake" : "ai.action.sleep");
    }

    private String obsOverlayText() {
        return getText(obsOverlayVisible ? "ai.action.hideObs" : "ai.action.showObs");
    }

    @Subscribe
    public void onClearConsoleEvent(ClearConsoleEvent event) {
        SwingUtilities.invokeLater(() -> {
            userPanel.clear();
            aiPanel.clear();
            systemPanel.clear();
        });
    }

    @Subscribe
    public void onLoadGame(LoadGameEvent event) {
        SwingUtilities.invokeLater(() -> commanderBlock.setCredits(event.getCredits()));
    }

    /**
     * Loads a telemetry PNG via ImageIO into a decoded BufferedImage, then scales
     * to {@link HudTelemetryBlock#ICON_SIZE} with high-quality Graphics2D hints if the
     * source size differs. Returns {@code null} on failure so the block falls back to
     * its diamond marker. Result is constructed once and cached by the caller.
     */
    private static ImageIcon tryLoadIcon(String resource) {
        try {
            var url = AiTabPanel.class.getResource(resource);
            if (url == null) return null;
            BufferedImage src = ImageIO.read(url);
            if (src == null) return null;
            int target = HudTelemetryBlock.ICON_SIZE;
            if (src.getWidth() == target && src.getHeight() == target) {
                return new ImageIcon(src);
            }
            // High-quality one-time scale; not needed for current 32×32 PNGs but kept as a safety net.
            BufferedImage scaled = new BufferedImage(target, target, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            try {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
                g2.drawImage(src, 0, 0, target, target, null);
            } finally {
                g2.dispose();
            }
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }
}
