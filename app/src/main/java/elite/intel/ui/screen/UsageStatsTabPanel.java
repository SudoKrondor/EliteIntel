package elite.intel.ui.screen;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudStatCell;
import elite.intel.ui.widget.StatusBadge;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.LlmSessionStatsChangedEvent;
import elite.intel.ui.event.RestartBrainEvent;
import elite.intel.ui.event.ServicesStateEvent;
import elite.intel.ui.telemetry.LlmSessionStatsSnapshot;
import elite.intel.ui.telemetry.LlmSessionStatsTracker;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

public class UsageStatsTabPanel extends JPanel {

    private final SystemSession systemSession = SystemSession.getInstance();
    private final LlmSessionStatsTracker statsTracker = LlmSessionStatsTracker.getInstance();

    private JLabel providerLabel;
    private JLabel sessionTimeLabel;
    private JLabel totalLabel;
    private JLabel savedLabel;
    private JLabel tphLabel;
    private HudStatCell cellPrompt;
    private HudStatCell cellCompletion;
    private HudStatCell cellHits;
    private HudStatCell cellWritten;
    private HudStatCell cellSpeed;

    // Token bars fill relative to the last request's total tokens (composition). Speed has no fixed
    // ceiling, so its bar fills relative to the observed session peak.
    private double peakSpeed;

    @SuppressWarnings("unused")
    private final Timer clockTimer;

    public UsageStatsTabPanel() {
        EventBusManager.register(this);
        buildUi();
        clockTimer = new Timer(1_000, e -> tickClock());
        clockTimer.start();
    }

    public void dispose() {
        clockTimer.stop();
        EventBusManager.unregister(this);
    }

    @Subscribe
    public void onServicesState(ServicesStateEvent event) {
        // Tracker handles state reset; panel rebuilds UI because usingLocalLLMs may change.
        if (event.isRunning()) {
            SwingUtilities.invokeLater(this::reset);
        }
    }

    @Subscribe
    public void onRestartBrain(RestartBrainEvent event) {
        SwingUtilities.invokeLater(this::reset);
    }

    @Subscribe
    public void onStatsChanged(LlmSessionStatsChangedEvent event) {
        SwingUtilities.invokeLater(() -> refreshFromSnapshot(event.snapshot()));
    }

    private void reset() {
        removeAll();
        buildUi();
        revalidate();
        repaint();
    }

    private void buildUi() {
        setLayout(new BorderLayout(HudPalette.HUD_GAP, HudPalette.HUD_GAP));
        setBorder(AppTheme.hudScreenBorder());
        setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        boolean usingLocalLLMs = systemSession.useLocalCommandLlm() && systemSession.useLocalQueryLlm();

        JPanel dashboard = AppTheme.transparentPanel(null);
        dashboard.setLayout(new BoxLayout(dashboard, BoxLayout.Y_AXIS));

        HudSection telemetrySection = HudSection.flat(getText("stats.section.llmTelemetry"), new BorderLayout());
        JPanel header = AppTheme.transparentPanel(null);
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));

        providerLabel = new JLabel(getText("stats.llm.na"));
        providerLabel.setFont(providerLabel.getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_STAT_LG));
        providerLabel.setForeground(HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);

        sessionTimeLabel = new JLabel(getText("stats.sessionTime.initial"));
        sessionTimeLabel.setForeground(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);

        header.add(providerLabel);
        header.add(Box.createHorizontalGlue());
        header.add(sessionTimeLabel);
        telemetrySection.body().add(header, BorderLayout.CENTER);

        resetPeaks();
        JPanel cells = AppTheme.transparentPanel(null);
        cells.setLayout(new BoxLayout(cells, BoxLayout.Y_AXIS));
        cellPrompt = new HudStatCell(getText("stats.chart.lastPrompt"), "/images/microchip-ai.png",
                HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION, null);
        cellCompletion = new HudStatCell(getText("stats.chart.lastCompletion"), "/images/ai.png",
                HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT, null);
        cellHits = new HudStatCell(getText("stats.chart.cacheHits"), "/images/file-recycle.png",
                HudPalette.HUD_COLOR_ROLE_SUCCESS, null);
        cellWritten = new HudStatCell(getText("stats.chart.cacheWritten"), "/images/file-recycle.png",
                HudPalette.HUD_COLOR_ROLE_SUCCESS, null);
        cellSpeed = new HudStatCell(getText("stats.chart.lastSpeed"), "/images/tachometer-fast.png",
                HudPalette.HUD_COLOR_ROLE_INFORMATION, "t/s");
        for (HudStatCell cell : new HudStatCell[]{cellPrompt, cellCompletion, cellHits, cellWritten, cellSpeed}) {
            cell.setAlignmentX(Component.LEFT_ALIGNMENT);
            cells.add(cell);
        }
        HudSection tokenSection = HudSection.flat(getText("stats.section.tokenUsage"), new BorderLayout());
        tokenSection.body().add(cells, BorderLayout.CENTER);

        JPanel footer = AppTheme.transparentPanel(null);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));

        if (usingLocalLLMs) {
            totalLabel = new JLabel(getText("stats.total.free", 0));
        } else {
            totalLabel = new JLabel(getText("stats.total.chargeable", 0));
        }
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_STAT_LG));
        totalLabel.setForeground(HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);

        savedLabel = new JLabel(getText("stats.cacheSaved", 0));
        savedLabel.setFont(savedLabel.getFont().deriveFont(HudPalette.HUD_FONT_MD));
        savedLabel.setForeground(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);

        tphLabel = new JLabel(getText("stats.tokensPerHour"));
        tphLabel.setFont(tphLabel.getFont().deriveFont(HudPalette.HUD_FONT_MD));
        tphLabel.setForeground(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);

        totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        savedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tphLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        footer.add(totalLabel);
        footer.add(Box.createVerticalStrut(6));
        if (!usingLocalLLMs) footer.add(savedLabel);
        if (!usingLocalLLMs) footer.add(Box.createVerticalStrut(6));
        footer.add(tphLabel);

        HudSection summarySection = HudSection.flat(getText("stats.section.sessionSummary"), new BorderLayout());
        summarySection.body().add(footer, BorderLayout.CENTER);

        dashboard.add(telemetrySection);
        dashboard.add(Box.createVerticalStrut(HudPalette.HUD_GAP));
        dashboard.add(tokenSection);
        if (usingLocalLLMs) {
            dashboard.add(Box.createVerticalStrut(HudPalette.HUD_GAP));
            dashboard.add(new HudBanner(getText("stats.localCacheNote"), StatusBadge.State.INFO));
        }
        dashboard.add(Box.createVerticalStrut(HudPalette.HUD_GAP));
        dashboard.add(summarySection);
        dashboard.add(Box.createVerticalGlue());

        add(dashboard, BorderLayout.CENTER);
    }

    private void refreshFromSnapshot(LlmSessionStatsSnapshot snap) {
        if (snap.hasData()) {
            providerLabel.setText(getText("stats.llm", snap.modelDisplay()));
        }
        int hits = snap.totalCachedHits();
        int total = snap.totalPromptTokens() + snap.totalCompletionTokens();

        // Token cells are scoped to the last request and fill as a share of that request's total tokens.
        int prompt = snap.lastPromptTokens();
        int completion = snap.lastCompletionTokens();
        int lastCached = snap.lastCachedTokens();
        int lastWritten = snap.lastCacheWrittenTokens();
        int callTotal = prompt + completion + lastCached + lastWritten;
        double tps = snap.lastTps();
        peakSpeed = Math.max(peakSpeed, tps);
        cellPrompt.setValue(formatTokens(prompt), ratio(prompt, callTotal), pct(prompt, callTotal));
        cellCompletion.setValue(formatTokens(completion), ratio(completion, callTotal), pct(completion, callTotal));
        cellHits.setValue(formatTokens(lastCached), ratio(lastCached, callTotal), pct(lastCached, callTotal));
        cellWritten.setValue(formatTokens(lastWritten), ratio(lastWritten, callTotal), pct(lastWritten, callTotal));
        cellSpeed.setValue(String.format("%.1f", tps), ratio(tps, peakSpeed), pct(tps, peakSpeed));
        if (systemSession.useLocalCommandLlm() && systemSession.useLocalQueryLlm()) {
            totalLabel.setText(getText("stats.total.free.upper", total));
        } else {
            totalLabel.setText(getText("stats.total.chargeable", total));
        }
        savedLabel.setText(hits > 0
                ? getText("stats.cacheSavedReduced", hits)
                : getText("stats.cacheSaved", 0));
        refreshTph(snap);
    }

    private void tickClock() {
        LlmSessionStatsSnapshot snap = statsTracker.getSnapshot();
        Duration d = Duration.between(snap.sessionStart(), Instant.now());
        sessionTimeLabel.setText(getText("stats.sessionTime", String.format("%02d:%02d:%02d",
                d.toHours(), d.toMinutesPart(), d.toSecondsPart())));
        refreshTph(snap);
    }

    private void refreshTph(LlmSessionStatsSnapshot snap) {
        // promptTokens = API input_tokens (excludes cache reads), so add all three buckets
        int total = snap.totalPromptTokens() + snap.totalCompletionTokens() + snap.totalCachedHits();
        long elapsedSeconds = Duration.between(snap.sessionStart(), Instant.now()).toSeconds();
        if (elapsedSeconds < 600) {
            tphLabel.setText(getText("stats.tokensPerHour.collecting"));
        } else if (total > 0) {
            long tph = Math.round(total / (elapsedSeconds / 3600.0));
            tphLabel.setText(getText("stats.tokensPerHour.cached", tph));
        } else {
            tphLabel.setText(getText("stats.tokensPerHour"));
        }
    }

    // -------------------------------------------------------------------------

    private void resetPeaks() {
        peakSpeed = 0;
    }

    /** Bar fill as a fraction of {@code total} (clamped to 0..1). */
    private static double ratio(double value, double total) {
        return total > 0 ? Math.min(1.0, value / total) : 0;
    }

    /** Percentage of {@code total}, formatted for the right edge. */
    private static String pct(double value, double total) {
        return (total > 0 ? Math.round(value / total * 100) : 0) + "%";
    }

    private static String formatTokens(int v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000) return String.format("%.1fK", v / 1_000.0);
        return String.valueOf(v);
    }
}
