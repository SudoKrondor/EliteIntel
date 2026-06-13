package elite.intel.ui.view;

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
                AppTheme.SHELL_GAP / 2,
                AppTheme.SHELL_GAP / 2,
                AppTheme.SHELL_GAP / 2,
                AppTheme.SHELL_GAP / 2
        ));
        root.setBackground(AppTheme.HUD_SHELL_BACKGROUND);
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
        shell.setBackground(AppTheme.HUD_SHELL_BACKGROUND);
        shell.add(statusBar, BorderLayout.CENTER);
        return shell;
    }

    private ImageIcon scaledIcon(String resource) {
        return AppTheme.scaledIcon(getClass(), resource, AppTheme.HUD_ICON_NAV);
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
        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 20);
        // Use platform sans-serif for UI labels because the previous custom font did not cover Cyrillic glyphs reliably.
        UIManager.put("defaultFont", new FontUIResource(new Font(UI_FONT_FAMILY, Font.PLAIN, 18)));
        try {
            mono = Font.createFont(Font.TRUETYPE_FONT,
                            Objects.requireNonNull(getClass().getResourceAsStream("/fonts/UbuntuSansMono-Regular.ttf")))
                    .deriveFont(20f);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(mono);
            UIManager.put("monospaceFont", new FontUIResource(mono));
            SwingUtilities.updateComponentTreeUI(this);
        } catch (FontFormatException | IOException e) {
            log.error("Failed to load custom font: {}", e.getMessage());
        }
        return mono;
    }

    private void installDarkDefaults() {
        UIManager.put("Panel.background", AppTheme.BG);
        UIManager.put("OptionPane.background", AppTheme.BG);
        UIManager.put("TabbedPane.background", AppTheme.HUD_CONTENT_BACKGROUND);
        UIManager.put("TabbedPane.foreground", AppTheme.FG);
        UIManager.put("TabbedPane.contentAreaColor", AppTheme.HUD_CONTENT_BACKGROUND);
        UIManager.put("Label.foreground", AppTheme.FG);
        UIManager.put("CheckBox.foreground", AppTheme.FG);
        UIManager.put("RadioButton.foreground", AppTheme.BUTTON_BG);
        UIManager.put("Button.foreground", AppTheme.BUTTON_FG);
        UIManager.put("Button.background", AppTheme.BUTTON_BG);
        UIManager.put("Button.disabledText",       AppTheme.HUD_DISABLED);
        UIManager.put("Button.disabledForeground", AppTheme.HUD_DISABLED);
        UIManager.put("ScrollPane.background", AppTheme.BG);
        UIManager.put("Viewport.background", AppTheme.BG);
        UIManager.put("TextField.background", AppTheme.BG_PANEL);
        UIManager.put("PasswordField.background", AppTheme.BG_PANEL);
        UIManager.put("TextArea.background", AppTheme.BG_PANEL);
        UIManager.put("EditorPane.background", AppTheme.BG_PANEL);
        UIManager.put("TextField.foreground", AppTheme.FG);
        UIManager.put("PasswordField.foreground", AppTheme.FG);
        UIManager.put("TextArea.foreground", AppTheme.FG);
        UIManager.put("EditorPane.foreground", AppTheme.FG);
        UIManager.put("TextField.inactiveForeground", AppTheme.FG_MUTED);
        UIManager.put("PasswordField.inactiveForeground", AppTheme.FG_MUTED);
        UIManager.put("TextArea.inactiveForeground", AppTheme.FG_MUTED);
        UIManager.put("EditorPane.inactiveForeground", AppTheme.FG_MUTED);
        UIManager.put("Table.background", AppTheme.HUD_PANEL_BG);
        UIManager.put("Table.foreground", AppTheme.FG);
        UIManager.put("Table.selectionBackground", AppTheme.ACCENT);
        UIManager.put("Table.selectionForeground", AppTheme.SEL_FG);
        UIManager.put("ComboBox.background",              AppTheme.HUD_TABLE_ROW);
        UIManager.put("ComboBox.foreground",              AppTheme.FG);
        UIManager.put("ComboBox.disabledBackground",       AppTheme.HUD_TABLE_ROW);
        UIManager.put("ComboBox.disabledForeground",       AppTheme.HUD_DISABLED);
        UIManager.put("ComboBox.buttonDisabledBackground", AppTheme.HUD_TABLE_ROW);
        UIManager.put("ComboBox.maximumRowCount",          8);
        // Width=0 suppresses the vertical separator between value and arrow button.
        // FlatLaf re-installs buttonSeparatorColor from the theme after per-instance nulling,
        // so zeroing the width is the only reliable suppression (update() skips it when width <= 0).
        UIManager.put("ComboBox.buttonSeparatorWidth",     0);
        // Editable combo editor calls selectAll() on setSelectedItem; override FlatLaf's
        // cold-blue theme default so selection follows the warm HUD palette.
        UIManager.put("ComboBox.selectionBackground", AppTheme.ACCENT);
        UIManager.put("ComboBox.selectionForeground", AppTheme.SEL_FG);
    }
}
