package elite.intel.ui.screen;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.dao.CommunitySyncDao.State;
import elite.intel.db.managers.CommunityManager;
import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.CommunitySyncedEvent;
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
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.HudPalette.HUD_GAP;

/**
 * The Massacre Sets page of the Commander tab: the commander's choice to share the pirate massacre sets this app
 * learned and use the ones other commanders shared (give to get), and what is known on each side.
 * <p>
 * Sharing is off until the commander turns it on. The upload and download themselves happen in the background
 * ({@code CommunitySync}), so the page reports when they last succeeded, and re-reads after every sync pass.
 */
public class MassacreSetsTabPanel extends JPanel {

    private static final Logger log = LogManager.getLogger(MassacreSetsTabPanel.class);

    private final CommunityManager community = CommunityManager.getInstance();
    private final GlobalSettingsManager settings = GlobalSettingsManager.getInstance();

    private JButton toggleButton;
    private JLabel statusLabel;
    private HudStatCell sharedCell;
    private HudStatCell lastSharedCell;
    private HudStatCell receivedCell;
    private HudStatCell usableCell;
    private HudStatCell lastUpdatedCell;
    private HudStatCell contributorsCell;

    /**
     * Everything the page shows, read together off the Swing thread.
     */
    private record Figures(boolean sharing, int shareable, int received, int usable, State state) {
    }

    public MassacreSetsTabPanel() {
        buildUi();
        initData();
        UiBus.register(this);
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel page = AppTheme.transparentPanel(null);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));

        HudBanner intro = HudBanner.multiline(getText("massacreSets.intro"), StatusBadge.State.INFO);
        intro.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(intro);
        page.add(Box.createVerticalStrut(HUD_GAP));

        // WHY a button and not a checkbox: the same on/off control the Exo-Mastery page has, one tab over.
        JPanel controls = AppTheme.transparentPanel(new FlowLayout(FlowLayout.LEFT, HUD_GAP, 0));
        toggleButton = AppTheme.makeButton(getText("massacreSets.enable"));
        toggleButton.addActionListener(e -> {
            settings.setShareMassacreSets(!settings.getShareMassacreSets());
            initData();
        });
        statusLabel = AppTheme.hudReadoutValue("", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        controls.add(toggleButton);
        controls.add(statusLabel);
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(controls);
        page.add(Box.createVerticalStrut(HUD_GAP));

        sharedCell = cell("massacreSets.stat.shared", "/images/shared-sets.png", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        lastSharedCell = cell("massacreSets.stat.lastShared", "/images/clock-five.png", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        page.add(section(getText("massacreSets.section.yours"), List.of(sharedCell, lastSharedCell)));

        receivedCell = cell("massacreSets.stat.received", "/images/sets-from-contributors.png", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        usableCell = cell("massacreSets.stat.usable", "/images/usable-here.png", HudPalette.HUD_COLOR_ROLE_SUCCESS);
        lastUpdatedCell = cell("massacreSets.stat.lastUpdated", "/images/update.png", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        contributorsCell = cell("massacreSets.stat.contributors", "/images/commander.png", HudPalette.HUD_COLOR_ROLE_INFORMATION);
        page.add(section(getText("massacreSets.section.others"),
                List.of(receivedCell, usableCell, lastUpdatedCell, contributorsCell)));

        add(page, BorderLayout.NORTH);
    }

    private static HudStatCell cell(String labelKey, String icon, Color color) {
        return new HudStatCell(getText(labelKey), icon, color, null);
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
     * Reads the figures off the Swing thread - counting what this install can share opens every commander's file -
     * and shows them back on it.
     */
    public void initData() {
        Thread.ofVirtual().start(() -> {
            Figures figures = read();
            SwingUtilities.invokeLater(() -> show(figures));
        });
    }

    @Subscribe
    public void onCommunitySynced(CommunitySyncedEvent event) {
        initData();
    }

    private Figures read() {
        int shareable;
        try {
            shareable = community.shareableSetCount();
        } catch (IOException e) {
            // WHY: the count is only shown, and the rest of the page is still worth showing without it.
            log.warn("Could not count the shareable massacre sets: {}", e.getMessage());
            shareable = -1;
        }
        return new Figures(settings.getShareMassacreSets(), shareable, community.crowdSetCount(),
                community.usableCrowdSetCount(), community.state());
    }

    private void show(Figures f) {
        toggleButton.setText(getText(f.sharing() ? "massacreSets.disable" : "massacreSets.enable"));
        statusLabel.setText(getText(f.sharing() ? "massacreSets.status.on" : "massacreSets.status.off"));

        sharedCell.setValue(f.shareable() < 0 ? null : LocalizedNumbers.grouped(f.shareable()), 0, "");
        lastSharedCell.setValue(localDate(f.state().lastReportAt()), 0, "");

        boolean downloaded = f.state().lastDownloadAt() != null;
        receivedCell.setValue(downloaded ? LocalizedNumbers.grouped(f.received()) : null, 0, "");
        double usableShare = f.received() == 0 ? 0 : (double) f.usable() / f.received();
        usableCell.setValue(downloaded ? LocalizedNumbers.grouped(f.usable()) : null, usableShare,
                downloaded && f.received() > 0 ? Math.round(usableShare * 100) + "%" : "");
        lastUpdatedCell.setValue(localDate(f.state().lastDownloadAt()), 0, "");
        Integer contributors = f.state().contributors();
        contributorsCell.setValue(contributors == null ? null : LocalizedNumbers.grouped(contributors), 0, "");
        revalidate();
        repaint();
    }

    /**
     * The local date of an ISO instant, or null (shown as the placeholder) when it never happened.
     */
    private static String localDate(String instant) {
        if (instant == null) return null;
        return Instant.parse(instant).atZone(ZoneId.systemDefault()).toLocalDate().toString();
    }
}
