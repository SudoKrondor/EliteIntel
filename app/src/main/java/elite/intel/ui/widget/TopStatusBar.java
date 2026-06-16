package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.ActiveShipChangedEvent;
import elite.intel.ui.event.CommanderChangedEvent;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Minimal identity strip at the top of the main window.
 * Left: app name and version. Right: commander name and ship name.
 * Operational statuses (Bindings, Commands, Keymap, STT, LLM, TTS) live in the AI tab QUICK STATUS panel.
 * Call {@link #dispose()} when discarding to unregister event subscriptions.
 */
public class TopStatusBar extends HudPanel {

    private JLabel cmdrValue;
    private JLabel shipValue;

    /**
     * Creates and registers the telemetry bar; reads initial state from application singletons.
     *
     * @param appName localized application name
     * @param version application version displayed as build metadata
     */
    public TopStatusBar(String appName, String version) {
        super(new BorderLayout(0, 0), AppTheme.HUD_CYAN, Variant.FLAT);
        setPreferredSize(new Dimension(0, AppTheme.HUD_TOP_BAR_HEIGHT));
        setMinimumSize(new Dimension(0, AppTheme.HUD_TOP_BAR_HEIGHT));
        setBackground(AppTheme.HUD_SHELL_BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.HUD_BORDER_DIM),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        add(buildLeftGroup(appName, version), BorderLayout.WEST);
        add(buildRightGroup(), BorderLayout.EAST);

        EventBusManager.register(this);
    }

    /** Unregisters all event subscriptions. Must be called when this component is discarded. */
    public void dispose() {
        EventBusManager.unregister(this);
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    @Subscribe
    public void onCommanderChanged(CommanderChangedEvent event) {
        SwingUtilities.invokeLater(() -> cmdrValue.setText(resolvedOrDash(event.commanderName())));
    }

    @Subscribe
    public void onActiveShipChanged(ActiveShipChangedEvent event) {
        SwingUtilities.invokeLater(() -> shipValue.setText(resolvedOrDash(event.shipName())));
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    /** App name and version, left-aligned. */
    private static JPanel buildLeftGroup(String appName, String version) {
        JPanel row = opaqueRow();

        String name = appName == null ? "" : appName.toUpperCase();
        String ver  = version == null || version.isBlank() ? "UNKNOWN" : version;

        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(AppTheme.FG);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, AppTheme.HUD_FONT_APP_TITLE));
        nameLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        row.add(nameLabel);

        row.add(Box.createHorizontalStrut(6));

        JLabel verLabel = new JLabel(ver);
        verLabel.setForeground(AppTheme.FG_MUTED);
        verLabel.setFont(verLabel.getFont().deriveFont(Font.PLAIN, AppTheme.HUD_FONT_READOUT_KEY));
        verLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        row.add(verLabel);

        return row;
    }

    /** Commander name and ship name, right-aligned. */
    private JPanel buildRightGroup() {
        JPanel row = opaqueRow();

        // Commander — label muted caps, value accent
        row.add(keyLabel(getText("hud.cmdr")));
        row.add(hgap());
        cmdrValue = valueLabel(initCommanderName());
        row.add(cmdrValue);

        row.add(Box.createHorizontalStrut(20));

        // Ship — label muted caps, value white caps
        row.add(keyLabel(getText("hud.ship")));
        row.add(hgap());
        shipValue = valueLabel(initShipName());
        row.add(shipValue);

        return row;
    }

    // ── Initial state reads ───────────────────────────────────────────────────

    private static String initCommanderName() {
        try {
            String name = PlayerSession.getInstance().getConfiguredPlayerName();
            // getConfiguredPlayerName() returns "Commander" as the default fallback — treat as unknown
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

    // ── Component factories ───────────────────────────────────────────────────

    private static JPanel opaqueRow() {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        return row;
    }

    private static JLabel keyLabel(String text) {
        JLabel l = new JLabel(text == null ? "" : text.toUpperCase(Locale.ROOT));
        l.setForeground(AppTheme.FG_MUTED);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, AppTheme.HUD_FONT_READOUT_KEY));
        l.setAlignmentY(Component.CENTER_ALIGNMENT);
        return l;
    }

    private static JLabel valueLabel(String text) {
        JLabel l = new JLabel(text == null ? "—" : text.toUpperCase(Locale.ROOT));
        l.setForeground(AppTheme.FG);
        l.setFont(l.getFont().deriveFont(Font.BOLD, AppTheme.HUD_FONT_COMMANDER_NAME));
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
        g.setColor(AppTheme.HUD_SHELL_BACKGROUND);
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}
