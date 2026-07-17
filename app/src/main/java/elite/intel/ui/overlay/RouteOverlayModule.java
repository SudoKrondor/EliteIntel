package elite.intel.ui.overlay;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudStatusReadout;
import elite.intel.ui.widget.HudTwoColumns;
import elite.intel.ui.widget.StatusBadge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Live route card for the companion overlay. It refreshes data asynchronously, presents a trade route when one is
 * active, and otherwise falls back to the ordinary navigation route without blocking Swing's event thread.
 */
final class RouteOverlayModule implements OverlayModule {

    private static final Logger log = LogManager.getLogger(RouteOverlayModule.class);
    private static final int REFRESH_INTERVAL_MS = 1_250;
    private static final String NAVIGATION_CARD = "navigation";
    private static final String TRADE_CARD = "trade";

    private final OverlayRouteSnapshotProvider snapshotProvider;
    private final HudSection section;
    private final CardLayout cardsLayout = new CardLayout();
    private final JPanel cards = AppTheme.transparentPanel(cardsLayout);
    private final Timer refreshTimer = new Timer(REFRESH_INTERVAL_MS, event -> requestRefresh());

    private final HudStatusReadout nextJump = readout("overlay.navigation.next");
    private final HudStatusReadout destination = readout("overlay.navigation.destination");
    private final HudStatusReadout jumpsLeft = readout("overlay.navigation.jumps");
    private final HudStatusReadout starClass = readout("overlay.navigation.star");
    private final HudStatusReadout fuelStar = readout("overlay.navigation.fuel");
    private final TradeRouteOverlayView tradeRouteView = new TradeRouteOverlayView();

    private boolean running;
    private long activeGeneration;
    private boolean refreshPending;
    private SwingWorker<OverlayRouteSnapshot, Void> refreshWorker;
    private OverlayRouteSnapshot currentSnapshot;

    /** Creates the production route module backed by the existing route managers. */
    RouteOverlayModule() {
        this(new OverlayRouteSnapshotProvider());
    }

    /** Test seam for a deterministic snapshot provider. */
    RouteOverlayModule(OverlayRouteSnapshotProvider snapshotProvider) {
        this.snapshotProvider = Objects.requireNonNull(snapshotProvider, "snapshotProvider");
        section = HudSection.compactCard(null, new BorderLayout());
        section.setHeaderVisible(false);
        section.setSurfaceBackground(HudPalette.HUD_COLOR_ROLE_OVERLAY_CARD_BACKGROUND);
        cards.add(new HudTwoColumns(
                readoutColumn(nextJump, destination, jumpsLeft),
                readoutColumn(starClass, fuelStar)), NAVIGATION_CARD);
        cards.add(tradeRouteView, TRADE_CARD);
        section.body().add(cards, BorderLayout.CENTER);
        section.setVisible(false);
        refreshTimer.setRepeats(true);
    }

    @Override
    public JComponent component() {
        return section;
    }

    @Override
    public void start() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::start);
            return;
        }
        if (running) {
            return;
        }
        running = true;
        activeGeneration++;
        requestRefresh();
        refreshTimer.start();
    }

    @Override
    public void stop() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::stop);
            return;
        }
        if (!running) {
            return;
        }
        running = false;
        activeGeneration++;
        refreshTimer.stop();
        refreshPending = false;
        if (refreshWorker != null) {
            refreshWorker.cancel(true);
            refreshWorker = null;
        }
    }

    private void requestRefresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::requestRefresh);
            return;
        }
        if (!running) {
            return;
        }
        if (refreshWorker != null) {
            refreshPending = true;
            return;
        }
        long generation = activeGeneration;
        refreshWorker = new SwingWorker<>() {
            @Override
            protected OverlayRouteSnapshot doInBackground() {
                return snapshotProvider.load();
            }

            @Override
            protected void done() {
                if (generation != activeGeneration || !running) {
                    return;
                }
                try {
                    if (!isCancelled()) {
                        OverlayRouteSnapshot snapshot = get();
                        if (!snapshot.equals(currentSnapshot)) {
                            applySnapshot(snapshot);
                        }
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    log.debug("Unable to refresh the companion overlay route", exception.getCause());
                } finally {
                    if (generation == activeGeneration) {
                        refreshWorker = null;
                        if (refreshPending && running) {
                            refreshPending = false;
                            requestRefresh();
                        }
                    }
                }
            }
        };
        refreshWorker.execute();
    }

    private void applySnapshot(OverlayRouteSnapshot snapshot) {
        currentSnapshot = snapshot;
        if (snapshot instanceof NoRouteSnapshot) {
            section.setVisible(false);
            repackOwningWindow();
            return;
        }
        section.setVisible(true);
        if (snapshot instanceof NavigationRouteSnapshot navigation) {
            applyNavigation(navigation, nextJump, destination, jumpsLeft, starClass, fuelStar);
            cardsLayout.show(cards, NAVIGATION_CARD);
        } else if (snapshot instanceof TradeRouteSnapshot trade) {
            applyTrade(trade);
            cardsLayout.show(cards, TRADE_CARD);
        }
        section.revalidate();
        section.repaint();
        repackOwningWindow();
    }

    private void applyTrade(TradeRouteSnapshot trade) {
        tradeRouteView.render(trade);
    }

    private static void applyNavigation(
            NavigationRouteSnapshot route,
            HudStatusReadout next,
            HudStatusReadout destination,
            HudStatusReadout jumps,
            HudStatusReadout star,
            HudStatusReadout fuel
    ) {
        next.setValue(valueOrUnknown(route.nextSystem()), StatusBadge.State.INFO);
        destination.setValue(valueOrUnknown(route.destinationSystem()), StatusBadge.State.INFO);
        jumps.setValue(Integer.toString(route.remainingJumps()), StatusBadge.State.INFO);
        StatusBadge.State starState = route.scoopable() ? StatusBadge.State.OK : StatusBadge.State.STANDBY;
        star.setValue(valueOrUnknown(route.starClass()), starState);
        fuel.setValue(getText(route.scoopable()
                ? "overlay.value.scoopable"
                : "overlay.value.unscoopable"), starState);
    }

    private static JPanel readoutColumn(HudStatusReadout... rows) {
        JPanel column = AppTheme.transparentPanel(null);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        for (int index = 0; index < rows.length; index++) {
            HudStatusReadout row = rows[index];
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            column.add(row);
            if (index < rows.length - 1) {
                column.add(Box.createVerticalStrut(HudPalette.HUD_GAP_TIGHT));
            }
        }
        return column;
    }

    private static HudStatusReadout readout(String labelKey) {
        return new HudStatusReadout(getText(labelKey), "", StatusBadge.State.IDLE);
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? getText("help.trade.value.unknown") : value;
    }

    private void repackOwningWindow() {
        Window window = SwingUtilities.getWindowAncestor(section);
        if (window != null && window.isVisible()) {
            window.pack();
        }
    }
}
