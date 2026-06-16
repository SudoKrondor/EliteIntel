package elite.intel.ui.screen;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.HudSection;
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
    private BarChart chart;

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

        chart = new BarChart(usingLocalLLMs);
        HudSection tokenSection = HudSection.flat(getText("stats.section.tokenUsage"), new BorderLayout());
        tokenSection.body().add(chart, BorderLayout.CENTER);

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
        int written = snap.totalCacheWritten();
        int total = snap.totalPromptTokens() + snap.totalCompletionTokens();

        chart.update(snap.lastPromptTokens(), snap.lastCompletionTokens(), hits, written, snap.lastTps());
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

    private static final class BarChart extends JPanel {

        private static final String[] LABELS = {
                getText("stats.chart.lastPrompt"),
                getText("stats.chart.lastCompletion"),
                getText("stats.chart.cacheHitsTotal"),
                getText("stats.chart.cacheWrittenTotal")
        };
        private static final String TPS_LABEL = getText("stats.chart.lastSpeed");
        private static final Color[] COLORS = {
                HudPalette.HUD_COLOR_ROLE_INFORMATION,
                HudPalette.HUD_COLOR_ROLE_SUCCESS,
                HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION,
                HudPalette.HUD_COLOR_ROLE_DISABLED
        };
        private static final Color TPS_COLOR = HudPalette.HUD_COLOR_ROLE_INFORMATION;

        private final boolean localMode;
        private int[] values = new int[4];
        private double lastTps = 0.0;
        private double maxTps = 1.0;

        BarChart(boolean localMode) {
            this.localMode = localMode;
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(super.getPreferredSize().width, 220);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 220);
        }

        void update(int prompt, int completion, int hits, int written, double tps) {
            values = new int[]{prompt, completion, hits, written};
            lastTps = tps;
            if (tps > maxTps) maxTps = tps;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int tokenBars = localMode ? 2 : values.length;
                int totalBars = tokenBars + 1; // +1 for TPS
                int labelW = 260;
                int valueW = 80;
                int barAreaW = getWidth() - labelW - valueW - 24;
                if (barAreaW <= 0) return;

                int rowH = Math.min(42, Math.max(20, (getHeight() - 20) / totalBars));
                int barH = rowH - Math.max(4, rowH / 5);
                int totalH = totalBars * rowH - (rowH - barH);
                int startY = (getHeight() - totalH) / 2;

                int maxVal = 1;
                for (int i = 0; i < tokenBars; i++) maxVal = Math.max(maxVal, values[i]);

                Font font = g2.getFont().deriveFont(HudPalette.HUD_FONT_LG);
                g2.setFont(font);
                FontMetrics fm = g2.getFontMetrics(font);
                int baseline = barH / 2 + fm.getAscent() / 2 - 1;

                // Token bars
                for (int i = 0; i < tokenBars; i++) {
                    int y = startY + i * rowH;

                    g2.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
                    g2.drawString(LABELS[i], 0, y + baseline);

                    g2.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND);
                    g2.fillRoundRect(labelW, y, barAreaW, barH, 6, 6);

                    if (values[i] > 0) {
                        int fillW = Math.max(6, (int) ((long) values[i] * barAreaW / maxVal));
                        g2.setColor(COLORS[i]);
                        g2.fillRoundRect(labelW, y, fillW, barH, 6, 6);
                    }

                    g2.setColor(HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
                    g2.drawString(formatTokens(values[i]), labelW + barAreaW + 8, y + baseline);
                }

                // TPS bar uses its own observed scale so token volume cannot flatten speed changes.
                int tpsY = startY + tokenBars * rowH;
                g2.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
                g2.drawString(TPS_LABEL, 0, tpsY + baseline);

                g2.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND);
                g2.fillRoundRect(labelW, tpsY, barAreaW, barH, 6, 6);

                if (lastTps > 0) {
                    int fillW = Math.max(6, (int) (lastTps / maxTps * barAreaW));
                    g2.setColor(TPS_COLOR);
                    g2.fillRoundRect(labelW, tpsY, fillW, barH, 6, 6);
                }

                g2.setColor(HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
                g2.drawString(String.format("%.1f t/s", lastTps), labelW + barAreaW + 8, tpsY + baseline);

            } finally {
                g2.dispose();
            }
        }

        private static String formatTokens(int v) {
            if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
            if (v >= 1_000) return String.format("%.1fK", v / 1_000.0);
            return String.valueOf(v);
        }
    }
}
