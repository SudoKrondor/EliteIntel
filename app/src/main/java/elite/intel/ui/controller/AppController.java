package elite.intel.ui.controller;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.ApiFactory;
import elite.intel.ai.brain.actions.customcommand.CustomCommandLoadAnnouncement;
import elite.intel.ai.ears.AudioCalibrator;
import elite.intel.ai.ears.AudioFormatDetector;
import elite.intel.ai.ears.EarsInterface;
import elite.intel.ai.hands.HandsService;
import elite.intel.ai.hands.KeyBindCheck;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.*;
import elite.intel.gameapi.journal.MissingMissionMonitor;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.*;
import elite.intel.util.StringUtls;
import elite.intel.util.Updater;
import elite.intel.ws.WebSocketBroadcaster;

import javax.swing.*;
import javax.swing.Timer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static elite.intel.ai.brain.commons.AiEndPoint.CONNECTION_CHECK_COMMAND;

public class AppController implements Runnable {

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final SystemSession systemSession = SystemSession.getInstance();

    /// NOTE Order of services is important
    private final Map<ServiceType, ServiceHolder> services = new LinkedHashMap<>();

    public AppController() {
        EventBusManager.register(this);
        this.isRunning.set(false);
        startIfWeHaveCredentials();
    }

    private void checkForUpdates() {
        SwingUtilities.invokeLater(() -> {
            CompletableFuture<Boolean> checkAsync = Updater.isUpdateAvailableAsync();
            try {
                Boolean updateAvailable = checkAsync.get();
                if (updateAvailable) {
                    EventBusManager.publish(new AiVoxResponseEvent("Newer version available"));
                    EventBusManager.publish(new UpdateAvailableEvent());
                }
            } catch (Exception e) {
                //kek
            }
        });
    }

    private void startIfWeHaveCredentials() {
        EventBusManager.publish(new ToggleServicesEvent(true));
    }

    @Subscribe
    public void onSpeechSpeedChangeEvent(SpeechSpeedChangeEvent event) {
        systemSession.setSpeechSpeed(event.getSpeed());
    }

    @Subscribe
    public void onBeepVolumeChangeEvent(NotificationVolumeChangedEvent event) {
        systemSession.setBeepVolume(event.getVolume());
    }

    @Subscribe
    public void onSttThreadsChangedEvent(SttThreadsChangedEvent event) {
        systemSession.setSttThreads(event.getNumThreads());
    }

    @Subscribe
    public void onSttVolumeChangedEvent(SttVolumeChangedEvent event) {
        systemSession.setVoiceVolume(event.getVolume());
    }

    @Subscribe
    public void onStreamModeToggle(VoiceInputModeToggleEvent event) {
        EventBusManager.publish(new ToggleWakeWordEvent(event.isStreaming()));
    }

    @Subscribe
    public void toggleStreamingMode(ToggleWakeWordEvent event) {
        appendToLog("Voice input mode toggle");
        systemSession.stopStartListening(event.isOn());
        EventBusManager.publish(new SleepWakeStateChangedEvent(event.isOn()));
        EventBusManager.publish(new AiVoxResponseEvent(event.isOn() ? ignoreModeOnMessage() : ignoreModeOffMessage()));
    }

    private String ignoreModeOffMessage() {
        return StringUtls.localizedSpeech("speech.ignoreModeOff");
    }

    private String ignoreModeOnMessage() {
        return StringUtls.localizedSpeech("speech.ignoreModeOn");
    }

    @Subscribe
    private void recalibrateAudio(RecalibrateAudioEvent event) {
        SwingUtilities.invokeLater(() -> {
            appendToLog("Starting audio calibration...");
            EarsInterface ears = services.get(ServiceType.EARS).get();
            if (ears == null) return;
            ears.stop();
            new Thread(() -> {
                try {
                    AudioFormatDetector.Format format = AudioFormatDetector.detectSupportedFormat();
                    AudioCalibrator.calibrateRMS(format);
                    SwingUtilities.invokeLater(() -> {
                        ears.start();
                        EventBusManager.publish(new MissionCriticalAnnouncementEvent("Audio calibration complete"));
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        ears.start();
                        appendToLog("Calibration failed: " + ex.getMessage());
                        EventBusManager.publish(new MissionCriticalAnnouncementEvent("Audio calibration failed"));
                    });
                }
            }, "AudioCalibrator-Thread").start();
        });
    }

    @Subscribe
    void onToggleServiceEvent(ToggleServicesEvent event) {
        new Thread(() -> {
            if (event.isStartSercice()) {
                try {
                    startServices();
                } catch (Exception stop) {
                    stopServices();
                    EventBusManager.publish(new ServicesStateEvent(false));
                }
            } else {
                stopServices();
            }
        }).start();
    }

    @Subscribe
    void onRestartBrainEvent(RestartBrainEvent event) {
        new Thread(this::restartBrainService, "BrainRestart-Thread").start();
    }

    private void restartBrainService() {
        if (!isRunning.get()) return;
        ServiceHolder brain = services.get(ServiceType.BRAIN);
        if (brain == null) return;
        appendToLog("Restarting LLM service...");
        brain.stop();
        brain.start();
        appendToLog("LLM service restarted");
    }

    @Subscribe
    void onRestartMouthEvent(RestartMouthEvent event) {
        new Thread(this::restartMouthService, "MouthRestart-Thread").start();
    }

    private void restartMouthService() {
        if (!isRunning.get()) return;
        ServiceHolder mouth = services.get(ServiceType.MOUTH);
        if (mouth == null) return;
        appendToLog("Restarting TTS service...");
        mouth.stop();
        mouth.start();
        appendToLog("TTS service restarted");
    }

    private void appendToLog(String data) {
        EventBusManager.publish(new AppLogEvent(data));
    }

    @Override
    public void run() {
        while (isRunning.get()) {
            try {
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void startServices() {
        if (isRunning.get()) return;
        checkForUpdates();
        EventBusManager.publish(new ClearConsoleEvent());
        initServices();

        for (ServiceType type : ServiceType.values()) {
            ServiceHolder service = services.get(type);
            if (service != null) service.start();
        }

        String mission_statement = playerSession.getPlayerMissionStatement();
        playerSession.setPlayerMissionStatement(mission_statement);

        isRunning.set(true);
        EventBusManager.publish(new ServicesStateEvent(true));

        Timer connectionCheckTimer = new Timer(2000, e -> {
            EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedSpeech("speech.connectingToLlm")));
            EventBusManager.publish(new SensorDataEvent(CONNECTION_CHECK_COMMAND, "Acknowledge connection, let use know we are on line and ready to go."));
        });
        connectionCheckTimer.setRepeats(false);
        connectionCheckTimer.start();

        KeyBindCheck.getInstance().check();
        CustomCommandLoadAnnouncement.getInstance().announce();
    }

    private void stopServices() {
        if (!isRunning.get()) return;
        List<ServiceType> reverseOrder = new ArrayList<>(services.keySet());
        Collections.reverse(reverseOrder);
        for (ServiceType type : reverseOrder) {
            ServiceHolder holder = services.get(type);
            if (holder != null) {
                holder.stop();
                services.remove(type);
            }
        }
        this.services.clear();
        EventBusManager.publish(new ServicesStateEvent(false));
        isRunning.set(false);
        EventBusManager.publish(new AppLogEvent("All services are stopped\n\n"));
    }

    private void initServices() {
        stopServices();
        this.services.clear();
        services.put(ServiceType.JOURNAL_PARSER, new ServiceHolder(JournalParser::new));
        services.put(ServiceType.AUXILIARY_FILES_MONITOR, new ServiceHolder(AuxiliaryFilesMonitor::new));
        services.put(ServiceType.HANDS, new ServiceHolder(HandsService::new));
        services.put(ServiceType.MOUTH, new ServiceHolder(ApiFactory.getInstance()::getMouthImpl));
        services.put(ServiceType.EARS, new ServiceHolder(ApiFactory.getInstance()::getEarsImpl));
        services.put(ServiceType.BRAIN, new ServiceHolder(ApiFactory.getInstance()::getCommandEndpoint));
        services.put(ServiceType.NOTIFICATION_MONITOR, new ServiceHolder(DeferredNotificationMonitor::getInstance));
        services.put(ServiceType.MISSING_MISSION_MONITOR, new ServiceHolder(MissingMissionMonitor::getInstance));
        services.put(ServiceType.WEB_SOCKET, new ServiceHolder(WebSocketBroadcaster::getInstance));
    }

    private static class ServiceHolder {
        private final Supplier<? extends ManagedService> creator;
        private ManagedService instance;

        ServiceHolder(Supplier<? extends ManagedService> creator) {
            this.creator = Objects.requireNonNull(creator);
        }

        void start() {
            if (instance == null) instance = creator.get();
            if (instance != null) instance.start();
        }

        void stop() {
            if (instance != null) {
                instance.stop();
                instance = null;
            }
        }

        @SuppressWarnings("unchecked")
        <T extends ManagedService> T get() {
            return (T) instance;
        }
    }

    private enum ServiceType {
        JOURNAL_PARSER, AUXILIARY_FILES_MONITOR, HANDS, MOUTH, EARS, BRAIN,
        NOTIFICATION_MONITOR, MISSING_MISSION_MONITOR, WEB_SOCKET
    }
}
