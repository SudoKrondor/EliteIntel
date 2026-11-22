package elite.intel.ui.screen;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.google.common.eventbus.Subscribe;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.controller.AiTabController;
import elite.intel.ui.event.*;
import elite.intel.ui.inputmonitor.InputMonitorTabPanel;
import elite.intel.ui.screen.bindings.LeavePrompt;
import elite.intel.ui.support.GameClockWarning;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.theme.HudLookAndFeel;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudWindowButtons;
import elite.intel.ui.widget.TopStatusBar;
import elite.intel.util.AppRelauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

public class AppView extends JFrame implements AppViewInterface {

    private static final Logger log = LoggerFactory.getLogger(AppView.class);
    private static final String ICON_AI = "/images/ai.png";
    private static final String ICON_PLAYER = "/images/commander.png";
    private static final String ICON_ACTIONS = "/images/actions.png";
    private static final String ICON_BINDINGS = "/images/keyboard.png";
    private static final String ICON_SERVICES = "/images/services.png";
    private static final String ICON_JUKEBOX = "/images/speaker.png";
    private static final String ICON_STATS = "/images/stats.png";
    private static final String CREDITS_ICON = "/images/release.png";

    private final SystemSession systemSession = SystemSession.getInstance();
    /** Big enough to show the Color Themes tab's editor whole, preview included. */
    private static final Dimension DEFAULT_SIZE = new Dimension(1720, 1080);
    private static final Dimension MINIMUM_SIZE = new Dimension(1280, 720);

    private Font monoFont;
    private AiUiState uiState;
    private AiTabPanel aiTabPanel;
    private CommanderTabPanel commanderTabPanel;
    private ActionsTabPanel actionsTabPanel;
    private BindingsTabPanel bindingsTabPanel;
    private ServicesTabPanel servicesTabPanel;
    private JukeboxTabPanel jukeboxTabPanel;
    private UsageStatsTabPanel usageStatsTabPanel;
    private MarkdownViewPanel creditsPanel;
    private InputMonitorTabPanel inputMonitorTabPanel;
    private AiTabController aiTabController;
    private TopStatusBar topStatusBar;
    private volatile ServicesStateEvent.State servicesState = ServicesStateEvent.State.STOPPED;
    /** An update is out; touched on the event thread only. */
    private boolean updateAvailable;
    /** The content pane, which carries the shell inset (and the frame, with native decorations); set by buildUi. */
    private JPanel shellRoot;

    /**
     * @param monoFont the app's monospace font, as {@link HudLookAndFeel#install()} loaded it; the look and feel
     *                 must be installed before the window is built
     */
    public AppView(Font monoFont) {
        super("--");
        this.monoFont = monoFont;
        UiBus.register(this);

        AppTheme.applyAppIcon(this);
        installWindowChrome();
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                requestClose();
            }
        });
        setMinimumSize(MINIMUM_SIZE);
        placeOnScreen();

        buildUi();
        initData();
        // WHY after buildUi: applyFrame dresses the content pane buildUi makes.
        addWindowStateListener(e -> applyFrame());
    }

    /**
     * The one close path, for the header's close button and for every close request from outside (Alt+F4, the
     * taskbar). WHY: closing with BindForge's edits not applied asks first; Go back cancels the close (storage.md
     * rule 9, Storage S7).
     */
    private void requestClose() {
        bindingsTabPanel.leave(LeavePrompt.Leaving.CLOSE, () -> System.exit(0));
    }

    /**
     * Takes away the operating system's title bar and lets the app header ({@link TopStatusBar}) be the title bar,
     * with our own window buttons in it (ED_HUD_REFERENCE §11.1). WHY FlatLaf's full-window-content mode rather than
     * {@code setUndecorated} as the dialogs do: the window stays a real system window underneath, so dragging the
     * header, double-clicking it, edge resizing and Windows Snap all keep working. FlatLaf's own title-bar buttons are
     * hidden; the header's {@link HudWindowButtons} replace them. Where FlatLaf cannot use the native decorations (Linux),
     * it draws the window frame itself, which it only does for an undecorated frame.
     */
    private void installWindowChrome() {
        if (!usesNativeDecorations()) {
            setUndecorated(true);
            getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
        }
        JRootPane rootPane = getRootPane();
        rootPane.putClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT, true);
        rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_ICON, false);
        rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_TITLE, false);
        rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_ICONIFFY, false);
        rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_MAXIMIZE, false);
        rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_CLOSE, false);
    }

    /**
     * Whether Windows draws the window, through FlatLaf's native decorations (Windows 10 and later). WHY not
     * {@code FlatLaf.isUseNativeWindowDecorations()} alone: that only reads the {@code TitlePane.useWindowDecorations}
     * setting, which is true on Linux too, so on Linux the window kept the window manager's title bar and frame and
     * FlatLaf never took over moving, resizing or double-click (found on KDE, 2026-11-15).
     */
    private static boolean usesNativeDecorations() {
        return FlatLaf.supportsNativeWindowDecorations() && FlatLaf.isUseNativeWindowDecorations();
    }

    /**
     * Draws the dialogs' window frame round the main window (§10.1), except when maximized, where a frame would only
     * sit against the screen edge; and tells FlatLaf how tall the title bar is - the frame, the shell inset and the
     * header - so that a drag or double-click anywhere on the header acts on the window.
     * <p>
     * Where FlatLaf draws the frame itself (Linux), our frame takes the place of FlatLaf's own window border on the
     * root pane rather than sitting inside it, so the window has one frame, not two (review, 2026-11-14). FlatLaf's
     * title bar then starts inside that border, so the frame is not part of its height. Edge resizing does not depend
     * on the border: FlatLaf's resizer handles it on its own.
     */
    private void applyFrame() {
        boolean maximized = (getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH;
        int frame = maximized ? 0 : HudPalette.HUD_BORDER_THICKNESS_ACCENT;
        int inset = HudPalette.SHELL_GAP / 2;
        Border frameBorder = BorderFactory.createMatteBorder(
                frame, frame, frame, frame, HudPalette.HUD_COLOR_ROLE_DIALOG_FRAME_BORDER);
        Border shellInset = new EmptyBorder(inset, inset, inset, inset);
        int titleBarHeight = inset + HudPalette.HUD_TOP_BAR_HEIGHT;
        if (usesNativeDecorations()) {
            shellRoot.setBorder(BorderFactory.createCompoundBorder(frameBorder, shellInset));
            titleBarHeight += frame;
        } else {
            getRootPane().setBorder(frameBorder);
            shellRoot.setBorder(shellInset);
        }
        getRootPane().putClientProperty(FlatClientProperties.TITLE_BAR_HEIGHT, titleBarHeight);
        getRootPane().revalidate();
        getRootPane().repaint();
    }

    /**
     * Opens at {@link #DEFAULT_SIZE}, centred - or maximized when the screen's usable area (the screen less its
     * taskbars and panels) is smaller than that, as on a 1920x1080 screen. WHY the size is clamped as well:
     * un-maximizing returns the window to its own size, which must then fit the screen rather than reach past it.
     */
    private void placeOnScreen() {
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        boolean fits = DEFAULT_SIZE.width <= usable.width && DEFAULT_SIZE.height <= usable.height;
        setSize(Math.min(DEFAULT_SIZE.width, usable.width), Math.min(DEFAULT_SIZE.height, usable.height));
        setLocationRelativeTo(null);
        if (!fits) setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
    }

    private void buildUi() {
        // WHY the flag: the title shows on the taskbar and in Alt+Tab, and a language change rebuilds through here.
        setTitle(updateAvailable
                ? getText("app.updateAvailable.title")
                : getText("app.title", systemSession.readVersionFromResources()));

        JPanel root = new JPanel(new BorderLayout());
        shellRoot = root;
        root.setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        setContentPane(root);
        applyFrame();

        JTabbedPane tabs = AppTheme.makeMainNavTabs();
        tabs.setModel(new LeavingBindForge(tabs));

        ImageIcon aiIcon = scaledIcon(ICON_AI);
        ImageIcon commanderIcon = scaledIcon(ICON_PLAYER);
        ImageIcon actionsIcon = scaledIcon(ICON_ACTIONS);
        ImageIcon bindingsIcon = scaledIcon(ICON_BINDINGS);
        ImageIcon servicesIcon = scaledIcon(ICON_SERVICES);
        ImageIcon jukeboxIcon = scaledIcon(ICON_JUKEBOX);
        ImageIcon statsIcon = scaledIcon(ICON_STATS);
        ImageIcon creditsIcon = scaledIcon(CREDITS_ICON);
        if (uiState == null) {
            uiState = new AiUiState();
        }
        aiTabPanel = new AiTabPanel(monoFont, uiState);
        commanderTabPanel = new CommanderTabPanel();
        actionsTabPanel = new ActionsTabPanel();
        bindingsTabPanel = new BindingsTabPanel();
        servicesTabPanel = new ServicesTabPanel();
        jukeboxTabPanel = new JukeboxTabPanel();
        usageStatsTabPanel = new UsageStatsTabPanel();
        creditsPanel = new MarkdownViewPanel("credits.md");
        inputMonitorTabPanel = new InputMonitorTabPanel();

        tabs.addTab(getText("tab.ai"), aiIcon, aiTabPanel);
        tabs.addTab(getText("tab.commander"), commanderIcon, commanderTabPanel);
        tabs.addTab(getText("tab.actions"), actionsIcon, actionsTabPanel);
        tabs.addTab(getText("tab.bindings"), bindingsIcon, bindingsTabPanel);
        tabs.addTab(getText("tab.services"), servicesIcon, servicesTabPanel);
        tabs.addTab(getText("tab.jukebox"), jukeboxIcon, jukeboxTabPanel);
        tabs.addTab(getText("tab.stats"), statsIcon, usageStatsTabPanel);
        //tabs.addTab("Credits", creditsIcon, creditsPanel);

        topStatusBar = new TopStatusBar(
                getText("app.name"),
                systemSession.readVersionFromResources(),
                this,
                this::requestClose
        );
        if (updateAvailable) topStatusBar.showUpdateAvailable();
        root.add(buildTopShell(topStatusBar), BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        AppTheme.applyDarkPalette(getContentPane());

        aiTabController = new AiTabController(aiTabPanel);
    }

    /**
     * The main tabs' selection, asking before it leaves the BindForge tab with edits not applied (storage.md rule 9,
     * Storage S7). A switch the user turns down is not made; one agreed to later - after Apply - is made then.
     */
    private final class LeavingBindForge extends DefaultSingleSelectionModel {

        private final JTabbedPane tabs;

        LeavingBindForge(JTabbedPane tabs) {
            this.tabs = tabs;
        }

        @Override
        public void setSelectedIndex(int index) {
            int from = getSelectedIndex();
            if (from == index || from < 0 || bindingsTabPanel == null
                    || from != tabs.indexOfComponent(bindingsTabPanel)) {
                super.setSelectedIndex(index);
                return;
            }
            bindingsTabPanel.leave(LeavePrompt.Leaving.TAB, () -> {
                // WHY: Apply ran in between - the tab asked for must still be there.
                if (index < tabs.getTabCount()) super.setSelectedIndex(index);
            });
        }
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
        servicesTabPanel.initData();
        commanderTabPanel.initData();
        actionsTabPanel.initData();
        bindingsTabPanel.initData();
        aiTabPanel.initData(systemSession.isSleeping(), servicesState);
    }

    @Subscribe
    public void onServiceStatusEvent(ServicesStateEvent event) {
        // Cache the latest lifecycle state so a later tab (re)build can seed AiTabPanel; AiTabPanel also
        // updates its own controls through its own ServicesStateEvent subscription.
        servicesState = event.state();
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
        if (bindingsTabPanel != null) bindingsTabPanel.dispose();
        if (servicesTabPanel != null) servicesTabPanel.dispose();
        if (usageStatsTabPanel != null) usageStatsTabPanel.dispose();
        if (inputMonitorTabPanel != null) inputMonitorTabPanel.dispose();
        if (actionsTabPanel != null) actionsTabPanel.dispose();
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
        SwingUtilities.invokeLater(() -> {
            // WHY the flag: the event comes once, and a language change rebuilds the header.
            updateAvailable = true;
            setTitle(getText("app.updateAvailable.title"));
            topStatusBar.showUpdateAvailable();
        });
    }

    @Subscribe
    public void onGameClockDisagreement(GameClockDisagreementEvent event) {
        SwingUtilities.invokeLater(() -> GameClockWarning.show(this, event));
    }

    @Subscribe
    public void onRelaunchRequested(RelaunchRequestedEvent event) {
        SwingUtilities.invokeLater(() -> bindingsTabPanel.leave(LeavePrompt.Leaving.CLOSE, () -> {
            if (!AppRelauncher.relaunch()) {
                event.relaunchFailed().run();
                return;
            }
            setVisible(false);
            System.exit(0);
        }));
    }

    @Subscribe
    public void onSystemShutdownEvent(SystemShutDownEvent event) {
        SwingUtilities.invokeLater(() -> {
            setVisible(false);
            System.exit(0);
        });
    }
}
