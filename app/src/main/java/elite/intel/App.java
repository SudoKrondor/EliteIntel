package elite.intel;

import com.formdev.flatlaf.FlatLightLaf;
import elite.intel.ai.brain.actions.customcommand.CustomCommandRegistry;
import elite.intel.ui.view.AppTheme;
import elite.intel.db.util.Database;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.JournalPreScanner;
import elite.intel.gameapi.SubscriberRegistration;
import elite.intel.session.LoadSessionEvent;
import elite.intel.session.PlayerSession;
import elite.intel.ui.controller.AppController;
import elite.intel.ui.view.AppView;
import elite.intel.util.Cypher;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import javax.swing.*;


public class App {

    public static void main(String[] args) {

        // init kry and db first!
        Cypher.initializeKey();
        Database.init();
        CustomCommandRegistry.getInstance().load();

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

        // spin up the session
        EventBusManager.publish(new LoadSessionEvent());

        // init UI
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
                // FlatLaf paints hover/pressed over the renderer; neutralise it here.
                UIManager.put("TableHeader.hoverBackground", AppTheme.HUD_BG);
                UIManager.put("TableHeader.hoverForeground", AppTheme.FG_MUTED);
                UIManager.put("TableHeader.pressedBackground", AppTheme.HUD_BG);
                UIManager.put("TableHeader.pressedForeground", AppTheme.FG_MUTED);
            } catch (Exception e) {
                e.printStackTrace();
            }
            AppView view = new AppView();
            new AppController();
            view.getUiComponent().setVisible(true);
        });
    }
}