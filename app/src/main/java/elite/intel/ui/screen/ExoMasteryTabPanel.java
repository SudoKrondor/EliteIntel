package elite.intel.ui.screen;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.dao.ExoMasteryDao.Stats;
import elite.intel.db.managers.ExoMasteryManager;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.exomastery.ExoMasteryCatalog;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.event.ExoMasteryChangedEvent;
import elite.intel.ui.i18n.LocalizedNumbers;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudStatCell;
import elite.intel.ui.widget.StatusBadge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.HudPalette.HUD_GAP;

/**
 * The Exo-Mastery page of the Commander tab: one button that loads or unloads the exobiology
 * catalogue, and the commander's progress through it once it is loaded.
 * <p>
 * Off, the page is the button and a line of explanation. Enable downloads the published catalogue
 * and imports it, each half with its own progress bar, and the page fills in with the totals.
 * Disable asks first, because it throws the catalogue away - though not the bodies already sampled,
 * which the confirmation says out loud.
 * <p>
 * The totals re-read whenever the ledger changes behind the page (a body sampled out in play), on
 * a UI-bus event, so a commander watching the page sees the harvested figure move.
 */
public class ExoMasteryTabPanel extends JPanel {

    private static final Logger log = LogManager.getLogger(ExoMasteryTabPanel.class);

    private final ExoMasteryManager exoMastery = ExoMasteryManager.getInstance();

    private JButton toggleButton;
    private JLabel statusLabel;
    private JPanel progressSection;
    private HudStatCell downloadCell;
    private HudStatCell importCell;
    private JPanel statsSection;
    private HudStatCell systemsCell;
    private HudStatCell bodiesCell;
    private HudStatCell valueCell;
    private HudStatCell harvestedCell;
    /**
     * True while a download/import or a purge is running, so the button cannot start a second one.
     */
    private boolean busy;

    public ExoMasteryTabPanel() {
        buildUi();
        UiBus.register(this);
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel page = AppTheme.transparentPanel(null);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));

        HudBanner intro = HudBanner.multiline(getText("exoMastery.intro"), StatusBadge.State.INFO);
        intro.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(intro);
        page.add(Box.createVerticalStrut(HUD_GAP));

        JPanel controls = AppTheme.transparentPanel(new FlowLayout(FlowLayout.LEFT, HUD_GAP, 0));
        toggleButton = AppTheme.makeButton(getText("exoMastery.enable"));
        toggleButton.addActionListener(e -> onToggle());
        statusLabel = AppTheme.hudReadoutValue("", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        controls.add(toggleButton);
        controls.add(statusLabel);
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(controls);
        page.add(Box.createVerticalStrut(HUD_GAP));

        downloadCell = new HudStatCell(getText("exoMastery.progress.download"), "/images/cloud.png",
                HudPalette.HUD_COLOR_ROLE_INFORMATION, null);
        importCell = new HudStatCell(getText("exoMastery.progress.import"), "/images/file-recycle.png",
                HudPalette.HUD_COLOR_ROLE_INFORMATION, null);
        progressSection = section(getText("exoMastery.section.progress"), List.of(downloadCell, importCell));
        progressSection.setVisible(false);
        page.add(progressSection);

        systemsCell = new HudStatCell(getText("exoMastery.stat.systems"), "/images/stats.png",
                HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT, null);
        bodiesCell = new HudStatCell(getText("exoMastery.stat.bodies"), "/images/stats.png",
                HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT, null);
        valueCell = new HudStatCell(getText("exoMastery.stat.value"), "/images/coins.png",
                HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION, getText("exoMastery.unit.credits"));
        harvestedCell = new HudStatCell(getText("exoMastery.stat.harvested"), "/images/coins.png",
                HudPalette.HUD_COLOR_ROLE_SUCCESS, getText("exoMastery.unit.credits"));
        statsSection = section(getText("exoMastery.section.stats"), List.of(systemsCell, bodiesCell, valueCell, harvestedCell));
        statsSection.setVisible(false);
        page.add(statsSection);

        add(page, BorderLayout.NORTH);
    }

    private static JPanel section(String title, List<HudStatCell> cells) {
        JPanel column = AppTheme.transparentPanel(null);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        for (HudStatCell cell : cells) {
            cell.setAlignmentX(Component.LEFT_ALIGNMENT);
            column.add(cell);
        }
        HudSection section = HudSection.flat(title, new BorderLayout());
        section.body().add(column, BorderLayout.CENTER);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        return section;
    }

    /**
     * Reads the ledger and shows it: the button says what pressing it would do, the totals show
     * only while the catalogue is loaded. Off the EDT for the read, back on it for the paint.
     */
    public void initData() {
        Thread.ofVirtual().start(() -> {
            boolean enabled = exoMastery.isEnabled();
            Stats stats = enabled ? exoMastery.stats() : null;
            SwingUtilities.invokeLater(() -> show(enabled, stats));
        });
    }

    @Subscribe
    public void onExoMasteryChanged(ExoMasteryChangedEvent event) {
        if (!busy) initData();
    }

    private void show(boolean enabled, Stats stats) {
        toggleButton.setText(getText(enabled ? "exoMastery.disable" : "exoMastery.enable"));
        toggleButton.setEnabled(!busy);
        statsSection.setVisible(enabled && stats != null);
        if (enabled && stats != null) {
            systemsCell.setValue(LocalizedNumbers.grouped(stats.systems()), 0, "");
            bodiesCell.setValue(LocalizedNumbers.grouped(stats.bodies()), 0, "");
            valueCell.setValue(LocalizedNumbers.grouped(stats.totalValue()), 0, "");
            // Clamped: a body kept through a purge and absent from the catalogue re-imported after it
            // is harvested value with no catalogue value under it.
            double fraction = stats.totalValue() == 0 ? 0 : Math.min(1.0, (double) stats.harvestedValue() / stats.totalValue());
            harvestedCell.setValue(LocalizedNumbers.grouped(stats.harvestedValue()), fraction,
                    Math.round(fraction * 100) + "%");
        }
        revalidate();
        repaint();
    }

    private void onToggle() {
        if (busy) return;
        if (exoMastery.isEnabled()) {
            unloadCatalog();
        } else {
            loadCatalog();
        }
    }

    private void loadCatalog() {
        busy = true;
        toggleButton.setEnabled(false);
        statusLabel.setText(getText("exoMastery.status.downloading"));
        downloadCell.setValue("0%", 0, "");
        importCell.setValue("0%", 0, "");
        progressSection.setVisible(true);
        revalidate();

        new SwingWorker<Stats, Runnable>() {
            @Override
            protected Stats doInBackground() throws Exception {
                ExoMasteryCatalog catalog = ExoMasteryCatalog.download(percent ->
                        publish(() -> downloadCell.setValue(percent + "%", percent / 100.0, "")));
                publish(() -> statusLabel.setText(getText("exoMastery.status.importing")));
                exoMastery.importCatalog(catalog, percent ->
                        publish(() -> importCell.setValue(percent + "%", percent / 100.0, "")));
                return exoMastery.stats();
            }

            @Override
            protected void process(List<Runnable> updates) {
                updates.forEach(Runnable::run);
            }

            @Override
            protected void done() {
                busy = false;
                progressSection.setVisible(false);
                try {
                    Stats stats = get();
                    statusLabel.setText(getText("exoMastery.status.enabled"));
                    show(true, stats);
                } catch (InterruptedException | ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    log.warn("Exo-Mastery catalogue could not be loaded: {}", cause.getMessage());
                    statusLabel.setText(getText("exoMastery.status.failed", cause.getMessage()));
                    show(false, null);
                }
            }
        }.execute();
    }

    private void unloadCatalog() {
        boolean confirmed = HudConfirmDialog.confirm(this,
                getText("exoMastery.confirm.title"),
                getText("exoMastery.confirm.message"),
                getText("exoMastery.confirm.yes"),
                getText("exoMastery.confirm.cancel"));
        if (!confirmed) return;
        busy = true;
        toggleButton.setEnabled(false);
        statusLabel.setText("");
        Thread.ofVirtual().start(() -> {
            try {
                exoMastery.purge();
            } finally {
                SwingUtilities.invokeLater(() -> {
                    busy = false;
                    show(false, null);
                });
            }
        });
    }
}
