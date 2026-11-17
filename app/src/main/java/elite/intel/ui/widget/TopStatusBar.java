package elite.intel.ui.widget;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.managers.ShipManager;
import elite.intel.eventbus.UiBus;
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.ActiveShipChangedEvent;
import elite.intel.ui.event.CommanderChangedEvent;
import elite.intel.ui.event.LocalModelLoadEvent;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Minimal identity strip at the top of the main window, which also serves as its title bar (the window has no
 * operating-system title bar; drag it to move the window, double-click it to maximize).
 * Left: app name and version, then "New version available." once an update is out. Middle: a progress bar while the
 * app's own LM Studio loads the model, hidden otherwise. Right: commander name and ship name, then the window's own
 * minimize, maximize/restore and close buttons.
 * Operational statuses (Bindings, Commands, Keymap, STT, LLM, TTS) live in the AI tab QUICK STATUS panel.
 * Call {@link #dispose()} when discarding to unregister event subscriptions.
 */
public class TopStatusBar extends HudPanel {

    private JLabel cmdrValue;
    private JLabel shipValue;
    private final JLabel updateNotice = new JLabel();
    private final JLabel modelLoadLabel = new JLabel();
    private final LoadBar modelLoadBar = new LoadBar();
    private JPanel modelLoadGroup;
    private final HudWindowButtons windowButtons;

    /**
     * Creates and registers the telemetry bar; reads initial state from application singletons.
     *
     * @param appName localized application name
     * @param version application version displayed as build metadata
     * @param window  the window this bar is the title bar of, which its window buttons act on
     * @param onClose the window's own close path, run by the close button
     */
    public TopStatusBar(String appName, String version, Frame window, Runnable onClose) {
        super(new BorderLayout(0, 0), HudPalette.HUD_COLOR_ROLE_INFORMATION, Variant.FLAT);
        setPreferredSize(new Dimension(0, HudPalette.HUD_TOP_BAR_HEIGHT));
        setMinimumSize(new Dimension(0, HudPalette.HUD_TOP_BAR_HEIGHT));
        setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        this.windowButtons = new HudWindowButtons(window, onClose);
        add(buildLeftGroup(appName, version), BorderLayout.WEST);
        add(buildModelLoadGroup(), BorderLayout.CENTER);
        add(buildRightGroup(), BorderLayout.EAST);

        UiBus.register(this);
    }

    /** Unregisters all event subscriptions, and the window buttons' listener on the window. Must be called when this component is discarded. */
    public void dispose() {
        UiBus.unregister(this);
        windowButtons.dispose();
    }

    /**
     * Shows "New version available." beside the version. WHY here: it used to be the window title, which no longer
     * shows now the main window has no title bar.
     */
    public void showUpdateAvailable() {
        updateNotice.setText(getText("app.updateAvailable.title"));
        updateNotice.setVisible(true);
    }

    // -- Event handlers --------------------------------------------------------

    @Subscribe
    public void onCommanderChanged(CommanderChangedEvent event) {
        SwingUtilities.invokeLater(() -> cmdrValue.setText(resolvedOrDash(event.commanderName())));
    }

    @Subscribe
    public void onActiveShipChanged(ActiveShipChangedEvent event) {
        SwingUtilities.invokeLater(() -> shipValue.setText(resolvedOrDash(event.shipName())));
    }

    @Subscribe
    public void onLocalModelLoad(LocalModelLoadEvent event) {
        SwingUtilities.invokeLater(() -> showModelLoad(event));
    }

    private void showModelLoad(LocalModelLoadEvent event) {
        if (event.loading()) {
            modelLoadLabel.setText(getText("hud.localModelLoading", event.percent()).toUpperCase(Locale.ROOT));
            modelLoadBar.setPercent(event.percent());
        }
        modelLoadGroup.setVisible(event.loading());
    }

    // -- Build -----------------------------------------------------------------

    /** App name and version, left-aligned, then the update notice (hidden until {@link #showUpdateAvailable()}). */
    private JPanel buildLeftGroup(String appName, String version) {
        JPanel row = opaqueRow();

        String name = appName == null ? "" : appName.toUpperCase();
        String ver  = version == null || version.isBlank() ? "UNKNOWN" : version;

        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_APP_TITLE));
        nameLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        row.add(nameLabel);

        row.add(Box.createHorizontalStrut(6));

        JLabel verLabel = new JLabel(ver);
        verLabel.setForeground(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        verLabel.setFont(verLabel.getFont().deriveFont(Font.PLAIN, HudPalette.HUD_FONT_APP_TITLE));
        verLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        row.add(verLabel);

        row.add(Box.createHorizontalStrut(HudPalette.HUD_GAP * 2));

        // A status, so it takes the accent colour (§11.1); no tooltip, so the header stays draggable over it.
        updateNotice.setForeground(HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
        updateNotice.setFont(updateNotice.getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_APP_TITLE));
        updateNotice.setAlignmentY(Component.CENTER_ALIGNMENT);
        updateNotice.setVisible(false);
        row.add(updateNotice);

        return row;
    }

    /**
     * The model-load label and bar, centred between the two groups and hidden until a load reports progress. No tooltip
     * and no mouse listener, so the header stays draggable over it.
     */
    private JPanel buildModelLoadGroup() {
        modelLoadLabel.setForeground(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        modelLoadLabel.setFont(modelLoadLabel.getFont().deriveFont(Font.PLAIN, HudPalette.HUD_FONT_APP_TITLE));
        modelLoadLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        modelLoadBar.setAlignmentY(Component.CENTER_ALIGNMENT);

        JPanel row = opaqueRow();
        row.add(modelLoadLabel);
        row.add(Box.createHorizontalStrut(HudPalette.HUD_GAP));
        row.add(modelLoadBar);

        modelLoadGroup = new JPanel(new GridBagLayout());
        modelLoadGroup.setOpaque(false);
        modelLoadGroup.add(row);
        modelLoadGroup.setVisible(false);
        return modelLoadGroup;
    }

    /** Commander name and ship name, then the window buttons, right-aligned. */
    private JPanel buildRightGroup() {
        JPanel row = opaqueRow();

        // Commander - label muted caps, value white caps
        row.add(keyLabel(getText("hud.cmdr")));
        row.add(hgap());
        cmdrValue = valueLabel(initCommanderName(), HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        row.add(cmdrValue);

        row.add(Box.createHorizontalStrut(20));

        // Ship - label muted caps, value carries the dedicated ship-name accent
        row.add(keyLabel(getText("hud.ship")));
        row.add(hgap());
        shipValue = valueLabel(initShipName(), HudPalette.HUD_COLOR_ROLE_SHIP_NAME_TEXT);
        row.add(shipValue);

        row.add(Box.createHorizontalStrut(HudPalette.HUD_GAP * 4));
        windowButtons.setAlignmentY(Component.CENTER_ALIGNMENT);
        row.add(windowButtons);

        return row;
    }

    // -- Initial state reads ---------------------------------------------------

    private static String initCommanderName() {
        try {
            String name = PlayerSession.getInstance().getConfiguredPlayerName();
            // getConfiguredPlayerName() returns "Commander" as the default fallback - treat as unknown
            return (name != null && !name.isBlank() && !"Commander".equals(name)) ? name : "—";
        } catch (Exception ignored) {
            return "—";
        }
    }

    private static String initShipName() {
        try {
            ShipDao.Ship ship = ShipManager.getInstance().getShip();
            return (ship != null && ship.getShipName() != null && !ship.getShipName().isBlank())
                    ? ship.getShipName() : "—";
        } catch (Exception ignored) {
            return "—";
        }
    }

    // -- Component factories ---------------------------------------------------

    private static JPanel opaqueRow() {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        return row;
    }

    private static JLabel keyLabel(String text) {
        JLabel l = new JLabel(text == null ? "" : text.toUpperCase(Locale.ROOT));
        l.setForeground(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, HudPalette.HUD_FONT_APP_TITLE));
        l.setAlignmentY(Component.CENTER_ALIGNMENT);
        return l;
    }

    private static JLabel valueLabel(String text, Color color) {
        JLabel l = new JLabel(text == null ? "—" : text.toUpperCase(Locale.ROOT));
        l.setForeground(color);
        l.setFont(l.getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_COMMANDER_NAME));
        l.setAlignmentY(Component.CENTER_ALIGNMENT);
        return l;
    }

    private static Component hgap() {
        return Box.createHorizontalStrut(4);
    }

    private static String resolvedOrDash(String value) {
        return (value != null && !value.isBlank()) ? value.toUpperCase(Locale.ROOT) : "—";
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /**
     * A thin flat bar in the header's own colours. WHY not {@link JProgressBar}: its look-and-feel styling does not
     * follow the HUD palette.
     */
    private static final class LoadBar extends JComponent {

        private static final Dimension SIZE = new Dimension(160, 6);
        private int percent;

        LoadBar() {
            setPreferredSize(SIZE);
            setMinimumSize(SIZE);
            setMaximumSize(SIZE);
        }

        void setPercent(int percent) {
            this.percent = percent;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            g.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER);
            g.fillRect(0, 0, w, h);
            g.setColor(HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
            g.fillRect(0, 0, w * percent / 100, h);
        }
    }
}
