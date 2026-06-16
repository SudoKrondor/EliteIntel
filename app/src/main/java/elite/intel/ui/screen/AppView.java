package elite.intel.ui.screen;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.widget.TopStatusBar;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.SystemSession;
import elite.intel.starvizion.StarVizionTabPanel;
import elite.intel.ui.controller.AiTabController;
import elite.intel.ui.event.LanguageChangedEvent;
import elite.intel.ui.event.ServicesStateEvent;
import elite.intel.ui.event.ShipProfileChangedEvent;
import elite.intel.ui.event.SystemShutDownEvent;
import elite.intel.ui.event.UpdateAvailableEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Objects;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.HudGlyphs.*;

public class AppView extends JFrame implements AppViewInterface {

    private static final Logger log = LoggerFactory.getLogger(AppView.class);
    private static final String UI_FONT_FAMILY = Font.SANS_SERIF;
    private static final String ICON_AI = "/images/ai.png";
    private static final String ICON_PLAYER = "/images/controller.png";
    private static final String ICON_ACTIONS = "/images/keys-binding.png";
    private static final String ICON_SETTINGS = "/images/settings.png";
    private static final String ICON_STATS = "/images/stats.png";
    private static final String CREDITS_ICON = "/images/release.png";
    private static final String MANUAL_ICON = "/images/manual.png";

    private final SystemSession systemSession = SystemSession.getInstance();
    private Font monoFont;
    private AiTabPanel aiTabPanel;
    private PlayerTabPanel playerTabPanel;
    private ActionsTabPanel actionsTabPanel;
    private SettingsTabPanel settingsTabPanel;
    private UsageStatsTabPanel usageStatsTabPanel;
    private MarkdownViewPanel creditsPanel;
    private MarkdownViewPanel userManualPanel;
    private StarVizionTabPanel starVizionTabPanel;
    private AiTabController aiTabController;
    private TopStatusBar topStatusBar;
    private boolean servicesRunning;

    public AppView() {
        super("--");
        monoFont = loadCustomFont();
        installDarkDefaults();
        EventBusManager.register(this);

        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/elite-logo.png")));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (actionsTabPanel != null) {
                    actionsTabPanel.promptCloseWithDraft();
                }
                System.exit(0);
            }
        });
        setMinimumSize(new Dimension(600, 500));
        setSize(new Dimension(1200, 900));
        setLocationRelativeTo(null);

        buildUi();
        initData();
    }

    private void buildUi() {
        setTitle(getText("app.title", systemSession.readVersionFromResources()));

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(
                HudPalette.SHELL_GAP / 2,
                HudPalette.SHELL_GAP / 2,
                HudPalette.SHELL_GAP / 2,
                HudPalette.SHELL_GAP / 2
        ));
        root.setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        setContentPane(root);

        JTabbedPane tabs = AppTheme.makeMainNavTabs();

        ImageIcon aiIcon = scaledIcon(ICON_AI);
        ImageIcon playerIcon = scaledIcon(ICON_PLAYER);
        ImageIcon actionsIcon = scaledIcon(ICON_ACTIONS);
        ImageIcon settingsIcon = scaledIcon(ICON_SETTINGS);
        ImageIcon statsIcon = scaledIcon(ICON_STATS);
        ImageIcon creditsIcon = scaledIcon(CREDITS_ICON);
        ImageIcon manualIcon = scaledIcon(MANUAL_ICON);

        aiTabPanel = new AiTabPanel(monoFont);
        playerTabPanel = new PlayerTabPanel();
        actionsTabPanel = new ActionsTabPanel();
        settingsTabPanel = new SettingsTabPanel();
        usageStatsTabPanel = new UsageStatsTabPanel();
        creditsPanel = new MarkdownViewPanel("credits.md");
        userManualPanel = new MarkdownViewPanel("user-manual.md");
        starVizionTabPanel = new StarVizionTabPanel();

        tabs.addTab(getText("tab.ai"), aiIcon, aiTabPanel);
        tabs.addTab(getText("tab.player"), playerIcon, playerTabPanel);
        tabs.addTab(getText("tab.actions"), actionsIcon, actionsTabPanel);
        tabs.addTab(getText("tab.settings"), settingsIcon, settingsTabPanel);
        tabs.addTab(getText("tab.stats"), statsIcon, usageStatsTabPanel);
        tabs.addTab(getText("tab.manual"), manualIcon, userManualPanel);
        //tabs.addTab("Credits", creditsIcon, creditsPanel);

        topStatusBar = new TopStatusBar(
                getText("app.name"),
                systemSession.readVersionFromResources()
        );
        root.add(buildTopShell(topStatusBar), BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        AppTheme.applyDarkPalette(getContentPane());

        aiTabController = new AiTabController(aiTabPanel);
    }

    private JComponent buildTopShell(TopStatusBar statusBar) {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setOpaque(true);
        shell.setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        shell.add(statusBar, BorderLayout.CENTER);
        return shell;
    }

    private ImageIcon scaledIcon(String resource) {
        return HudGlyphs.scaledIcon(getClass(), resource, HudPalette.HUD_ICON_NAV);
    }

    @Override
    public void initData() {
        settingsTabPanel.initData();
        playerTabPanel.initData();
        actionsTabPanel.initData();
        aiTabPanel.initData(systemSession.isSleepingModeOn(), servicesRunning);
    }

    @Subscribe
    public void onServiceStatusEvent(ServicesStateEvent event) {
        servicesRunning = event.isRunning();
        // TopStatusBar handles ServicesStateEvent directly via its own subscription.
    }

    @Subscribe
    public void onShipProfileChangedEvent(ShipProfileChangedEvent event) {
        SwingUtilities.invokeLater(this::initData);
    }

    @Subscribe
    public void onLanguageChangedEvent(LanguageChangedEvent event) {
        // Most Swing labels are constructed once, so changing language rebuilds the tree instead of chasing component references.
        SwingUtilities.invokeLater(this::rebuildUi);
    }

    private void rebuildUi() {
        // Rebuilt panels/controllers register with EventBus; dispose old instances first to avoid duplicate subscribers.
        if (topStatusBar != null) topStatusBar.dispose();
        if (aiTabController != null) aiTabController.dispose();
        if (aiTabPanel != null) aiTabPanel.dispose();
        if (actionsTabPanel != null) actionsTabPanel.dispose();
        if (settingsTabPanel != null) settingsTabPanel.dispose();
        if (usageStatsTabPanel != null) usageStatsTabPanel.dispose();
        if (starVizionTabPanel != null) starVizionTabPanel.dispose();
        buildUi();
        initData();
        revalidate();
        repaint();
    }

    @Override
    public JFrame getUiComponent() {
        return this;
    }

    @Subscribe
    public void onUpdateAvailableEvent(UpdateAvailableEvent event) {
        SwingUtilities.invokeLater(() -> setTitle(getText("app.updateAvailable.title")));
    }

    @Subscribe
    public void onSystemShutdownEvent(SystemShutDownEvent event) {
        SwingUtilities.invokeLater(() -> {
            setVisible(false);
            System.exit(0);
        });
    }

    private Font loadCustomFont() {
        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, (int) HudPalette.HUD_FONT_MONO_BASE);
        // Use platform sans-serif for UI labels because the previous custom font did not cover Cyrillic glyphs reliably.
        UIManager.put("defaultFont", new FontUIResource(new Font(UI_FONT_FAMILY, Font.PLAIN, (int) HudPalette.HUD_FONT_UI_DEFAULT)));
        try {
            mono = Font.createFont(Font.TRUETYPE_FONT,
                            Objects.requireNonNull(getClass().getResourceAsStream("/fonts/UbuntuSansMono-Regular.ttf")))
                    .deriveFont(HudPalette.HUD_FONT_MONO_BASE);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(mono);
            UIManager.put("monospaceFont", new FontUIResource(mono));
            SwingUtilities.updateComponentTreeUI(this);
        } catch (FontFormatException | IOException e) {
            log.error("Failed to load custom font: {}", e.getMessage());
        }
        return mono;
    }

    private void installDarkDefaults() {
        UIManager.put("Panel.background", HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        UIManager.put("OptionPane.background", HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        UIManager.put("TabbedPane.background", HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        UIManager.put("TabbedPane.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("TabbedPane.contentAreaColor", HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        UIManager.put("Label.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("CheckBox.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("RadioButton.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
        UIManager.put("Button.foreground", HudPalette.HUD_COLOR_ROLE_BUTTON_TEXT);
        UIManager.put("Button.background", HudPalette.HUD_COLOR_ROLE_PRIMARY_BUTTON_BACKGROUND);
        UIManager.put("Button.disabledText",       HudPalette.HUD_COLOR_ROLE_DISABLED);
        UIManager.put("Button.disabledForeground", HudPalette.HUD_COLOR_ROLE_DISABLED);
        UIManager.put("ScrollPane.background", HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        UIManager.put("Viewport.background", HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        UIManager.put("TextField.background", HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        UIManager.put("PasswordField.background", HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        UIManager.put("TextArea.background", HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        UIManager.put("EditorPane.background", HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        UIManager.put("TextField.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("PasswordField.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("TextArea.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("EditorPane.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("TextField.inactiveForeground", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        UIManager.put("PasswordField.inactiveForeground", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        UIManager.put("TextArea.inactiveForeground", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        UIManager.put("EditorPane.inactiveForeground", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        UIManager.put("Table.background", HudPalette.HUD_COLOR_ROLE_PANEL_BACKGROUND);
        UIManager.put("Table.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("Table.selectionBackground", HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
        UIManager.put("Table.selectionForeground", HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT);
        UIManager.put("ComboBox.background",              HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        UIManager.put("ComboBox.foreground",              HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("ComboBox.disabledBackground",       HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        UIManager.put("ComboBox.disabledForeground",       HudPalette.HUD_COLOR_ROLE_DISABLED);
        UIManager.put("ComboBox.buttonDisabledBackground", HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        UIManager.put("ComboBox.maximumRowCount",          8);
        // Width=0 suppresses the vertical separator between value and arrow button.
        // FlatLaf re-installs buttonSeparatorColor from the theme after per-instance nulling,
        // so zeroing the width is the only reliable suppression (update() skips it when width <= 0).
        UIManager.put("ComboBox.buttonSeparatorWidth",     0);
        // Editable combo editor calls selectAll() on setSelectedItem; override FlatLaf's
        // cold-blue theme default so selection follows the warm HUD palette.
        UIManager.put("ComboBox.selectionBackground", HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
        UIManager.put("ComboBox.selectionForeground", HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT);
    }
}
