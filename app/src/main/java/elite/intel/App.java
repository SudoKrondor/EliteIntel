package elite.intel;

import com.formdev.flatlaf.FlatLightLaf;
import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.ai.brain.vega.memory.facts.MemoryFactSourceRegistry;
import elite.intel.db.managers.ShipMakeManager;
import elite.intel.db.util.Database;
import elite.intel.diagnostics.*;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.JournalPreScanner;
import elite.intel.gameapi.SubscriberRegistration;
import elite.intel.session.LoadSessionEvent;
import elite.intel.session.PlayerSession;
import elite.intel.ui.controller.AppController;
import elite.intel.ui.screen.AppView;
import elite.intel.ui.theme.HudPalette;
import elite.intel.util.AudioPlayer;
import elite.intel.util.Cypher;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import javax.swing.*;


public class App {

    private static final Logger log = LogManager.getLogger(App.class);

    // NOTE: do NOT set sun.java2d.opengl here. The HUD overlay needs the GL
    // pipeline to stop per-pixel translucent windows flickering, but the
    // property is JVM-wide and corrupts the FlatLaf main window (missing tab
    // bar, blank section frames). The overlay has to obtain that pipeline
    // without imposing it on the rest of the UI.

    public static void main(String[] args) {

        // init kry and db first!
        Cypher.initializeKey();
        Database.init();
        CustomCommandRegistry.getInstance().load();
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        MemoryFactSourceRegistry.getInstance().load();
        // Warm the ship-make cache off the EDT so the fleet table never triggers the first DB read while painting.
        ShipMakeManager.getInstance();

        // File-driven diagnostics harness: when a phrase input file is present, feed it as commander input and
        // mirror the SYSTEM LOG to a session log so an automated tester can drive and observe the companion.
        if (DiagnosticsMode.isEnabled()) {
            DiagnosticsLog.open();
            // Must precede service start: the companion's semantic reducer freezes the language at construction.
            DiagnosticsMode.applyBootLanguage();
            DiagnosticsPacer.getInstance().start();
            new DiagnosticsLogWriter().start();
            new DiagnosticsInputTailer().start();
            log.info("Diagnostics mode enabled");
        }

        // change the debug log level when we have version 1.0
        Configurator.setRootLevel(Level.ALL);

        // Seed DB from previous journal sessions so first-run queries have data.
        // Runs concurrently on its own thread; uses a private EventBus.
        // No live subscribers are triggered, no TTS/EDSM/game-input side effects.
        Thread.ofVirtual().name("journal-pre-scan").start(
                () -> JournalPreScanner.scan(PlayerSession.getInstance().getJournalPath())
        );

        // Event subscribers
        SubscriberRegistration.registerSubscribers();
        AudioPlayer.getInstance();

        // spin up the session
        GameEventBus.publish(new LoadSessionEvent());

        // init UI
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
                // FlatLaf paints hover/pressed over the renderer; neutralise it here.
                UIManager.put("TableHeader.hoverBackground", HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
                UIManager.put("TableHeader.hoverForeground", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
                UIManager.put("TableHeader.pressedBackground", HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
                UIManager.put("TableHeader.pressedForeground", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
            } catch (Exception e) {
                e.printStackTrace();
            }
            AppView view = new AppView();
            new AppController();
            view.getUiComponent().setVisible(true);
        });
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            log.error("Uncaught exception on thread {}", thread.getName(), throwable);
        });
    }
}