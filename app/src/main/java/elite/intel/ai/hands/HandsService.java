package elite.intel.ai.hands;

import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.ui.controller.ManagedService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class HandsService implements ManagedService {
    private static final Logger log = LogManager.getLogger(HandsService.class);
    private final BindingsMonitor monitor;
    private final InputSequenceExecutor inputSequenceExecutor;
    private Thread processingThread;

    public HandsService() {
        this.monitor = BindingsMonitor.getInstance();
        this.inputSequenceExecutor = new InputSequenceExecutor();
        log.info("HandsService initialized");
    }

    @Override
    public synchronized void start() {
        if (processingThread != null && processingThread.isAlive()) {
            log.warn("HandsService is already running");
            return;
        }
        processingThread = new Thread(this::run, "HandsServiceThread");
        processingThread.start();
        log.info("HandsService started");
    }

    @Override
    public synchronized void stop() {
        inputSequenceExecutor.shutdown();
        if (processingThread == null || !processingThread.isAlive()) {
            log.warn("HandsService is not running");
            return;
        }
        processingThread.interrupt();
        try {
            monitor.stopMonitoring();
        } catch (Exception e) {
            log.error("Error stopping monitoring: {}", e.getMessage(), e);
        }

        try {
            processingThread.join(5000);
            log.info("HandsService stopped");
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for HandsService to stop", e);
            Thread.currentThread().interrupt();
        }
        processingThread = null;
    }

    private void run() {
        try {
            monitor.startMonitoring();
        } catch (IOException e) {
            log.info("HandsService interrupted, shutting down");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error in HandsService: {}", e.getMessage(), e);
            GameEventBus.publish(new AiVoxResponseEvent("Error in command handler: " + e.getMessage()));
        }
    }
}
