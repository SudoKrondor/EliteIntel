package elite.intel.ui.controller;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.ApiFactory;
import elite.intel.ai.brain.LocalLlmModelCheck;
import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandLoadAnnouncement;
import elite.intel.ai.brain.vega.input.CompanionSubsystemGate;
import elite.intel.ai.ears.*;
import elite.intel.ai.hands.HandsService;
import elite.intel.ai.hands.KeyBindCheck;
import elite.intel.ai.mouth.kokoro.KokoroTTS;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.devices.DeviceService;
import elite.intel.diagnostics.*;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.AuxiliaryFilesMonitor;
import elite.intel.gameapi.DeferredNotificationMonitor;
import elite.intel.gameapi.JournalParser;
import elite.intel.gameapi.journal.MissingMissionMonitor;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.setup.SetupCheck;
import elite.intel.tools.ws.WebSocketBroadcaster;
import elite.intel.ui.event.*;
import elite.intel.util.StringUtls;
import elite.intel.util.Updater;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.Mixer;
import javax.swing.*;
import javax.swing.Timer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class AppController {

    private static final Logger log = LogManager.getLogger(AppController.class);

    // WHY: intentionally process-lifetime and never shut down. Connection checks run on every service
    // start (and on brain restart), so the executor must outlive stopServices()/restart cycles. Its single
    // worker is a daemon thread, so it never blocks JVM exit and needs no explicit teardown.
    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ConnectionCheck-Worker");
        t.setDaemon(true);
        return t;
    });

    private final Object lifecycleLock = new Object();
    // Written only under lifecycleLock; volatile so the off-lock reads (e.g. onLlmConnectionStatus on the
    // EDT) observe the latest transition.
    private volatile ServicesStateEvent.State serviceState = ServicesStateEvent.State.STOPPED;
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final SystemSession systemSession = SystemSession.getInstance();

    /// NOTE Order of services is important
    private final Map<ServiceType, ServiceHolder> services = new LinkedHashMap<>();
    private Timer retryConnectionTimer;
    // Written on the EDT (retry Timer callback, stopRetryTimer), read on the ConnectionCheck-Worker
    // thread in onLlmConnectionStatus; volatile to make that cross-thread read safe.
    private volatile int retryAttemptNumber = 0;
    // Silences the spoken "connection failed" message during silent retries (first failure still speaks).
    // Written on the EDT in onLlmConnectionStatus, read on the ConnectionCheck-Worker thread; volatile.
    private volatile boolean suppressConnectionFailSpeech = false;

    public AppController() {
        UiBus.register(this);
        startIfWeHaveCredentials();
    }

    private void checkForUpdates() {
        SwingUtilities.invokeLater(() -> {
            CompletableFuture<Boolean> checkAsync = Updater.isUpdateAvailableAsync();
            try {
                Boolean updateAvailable = checkAsync.get();
                if (updateAvailable) {
                    GameEventBus.publish(new AiVoxResponseEvent("Newer version available"));
                    UiBus.publish(new UpdateAvailableEvent());
                }
            } catch (Exception e) {
                log.warn("Update check failed", e);
            }
        });
    }

    private void startIfWeHaveCredentials() {
        UiBus.publish(new ToggleServicesEvent(true));
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
    private void recalibrateAudio(RecalibrateAudioEvent event) {
        SwingUtilities.invokeLater(() -> {
            appendToLog(StringUtls.localizedSpeech("log.audioCalibrationStarting"));
            EarsInterface ears = services.get(ServiceType.EARS).get();
            if (ears == null) return;
            ears.stop();
            new Thread(() -> {
                try {
                    Mixer.Info inputMixerInfo = AudioDeviceEnumerator.resolveInputDevice(systemSession.getAudioInputDevice());
                    AudioFormatDetector.Format format = AudioFormatDetector.detectSupportedFormat(inputMixerInfo);
                    AudioCalibrator.calibrateRMS(format, inputMixerInfo);
                    SwingUtilities.invokeLater(() -> {
                        ears.start();
                        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedResponse("speech.audioCalibrationComplete")));
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        ears.start();
                        appendToLog(StringUtls.localizedSpeech("log.audioCalibrationFailed", String.valueOf(ex.getMessage())));
                        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedResponse("speech.audioCalibrationFailed")));
                    });
                }
            }, "AudioCalibrator-Thread").start();
        });
    }

    @Subscribe
    void onToggleServiceEvent(ToggleServicesEvent event) {
        new Thread(() -> {
            if (event.isStartService()) {
                try {
                    startServices();
                } catch (Exception e) {
                    log.error("Failed to start services, stopping", e);
                    // Surface the reason to the user (e.g. an unsupported LLM provider), not just the log file.
                    appendToLog(StringUtls.localizedSpeech("log.serviceStartFailed", String.valueOf(e.getMessage())));
                    stopServices();
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

    @Subscribe
    void onRestartServicesEvent(RestartServicesEvent event) {
        new Thread(this::restartAllServices, "ServicesRestart-Thread").start();
    }

    /**
     * Full stop/start of every service. Used when a setting change alters the service registry
     * itself rather than the config of an existing service (e.g. a change that adds or removes a
     * {@link ServiceType}). A granular {@link RestartBrainEvent} cannot do that, so the whole set is
     * rebuilt. No-op when services are stopped (the change takes effect on the next Start).
     */
    private void restartAllServices() {
        synchronized (lifecycleLock) {
            if (!isServiceRunning()) return;
            stopServices();
            try {
                startServices();
            } catch (Exception e) {
                // WHY: broad catch at the worker-thread boundary - a startup failure must be surfaced to the
                // user (console log) and leave services cleanly stopped, not kill the thread half-running.
                log.error("Failed to restart services", e);
                appendToLog(StringUtls.localizedSpeech("log.serviceStartFailed", String.valueOf(e.getMessage())));
                stopServices();
            }
        }
    }

    private void restartBrainService() {
        synchronized (lifecycleLock) {
            if (!isServiceRunning()) return;
            // The companion subsystem is the LLM service; restart it so an LLM source (local/cloud) change is
            // picked up.
            ServiceHolder brain = services.get(ServiceType.COMPANION);
            if (brain == null) return;
            appendToLog("Restarting LLM service...");
            brain.stop();
            brain.start();
            appendToLog("LLM service restarted");
            Timer checkTimer = new Timer(2000, e -> connectionCheck());
            checkTimer.setRepeats(false);
            checkTimer.start();
        }
    }

    @Subscribe
    void onLanguageChangedEvent(LanguageChangedEvent event) {
        new Thread(this::restartEarsService, "EarsRestart-Lang-Thread").start();
        // Delay so the language-change announcement can finish playing before TTS rebuilds
        new Thread(() -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            restartMouthService();
        }, "MouthRestart-Lang-Thread").start();
    }

    @Subscribe
    void onRestartMouthEvent(RestartMouthEvent event) {
        new Thread(this::restartMouthService, "MouthRestart-Thread").start();
    }

    @Subscribe
    void onRestartEarsEvent(RestartEarsEvent event) {
        new Thread(this::restartEarsService, "EarsRestart-Thread").start();
    }

    private void restartEarsService() {
        synchronized (lifecycleLock) {
            if (!isServiceRunning()) return;
            ServiceHolder ears = services.get(ServiceType.EARS);
            if (ears == null) return;
            appendToLog("Restarting STT service...");
            ears.stop();
            try {
                ears.start();
                appendToLog("STT service restarted");
            } catch (Exception e) {
                log.error("Failed to restart STT service", e);
                appendToLog(StringUtls.localizedSpeech("log.sttRestartFailed", String.valueOf(e.getMessage())));
            }
        }
    }

    private void restartMouthService() {
        synchronized (lifecycleLock) {
            if (!isServiceRunning()) return;
            ServiceHolder mouth = services.get(ServiceType.MOUTH);
            if (mouth == null) return;
            appendToLog("Restarting TTS service...");

            // Radio is always voiced by Kokoro, so a dedicated radio engine is needed only when the main
            // mouth is not Kokoro. The radio engine and a Kokoro main mouth share the Kokoro singleton, so
            // ordering matters: retire an unneeded radio engine BEFORE (re)starting the main mouth, and
            // leave an already-running radio engine untouched on a main-mouth-only change (no model reload).
            boolean needRadio = !systemSession.useLocalTTS();
            ServiceHolder radio = services.get(ServiceType.RADIO_MOUTH);
            if (radio != null && !needRadio) {
                radio.stop();
                services.remove(ServiceType.RADIO_MOUTH);
                radio = null;
            }

            mouth.stop();
            mouth.start();

            if (needRadio) {
                if (radio == null) {
                    radio = new ServiceHolder(() -> {
                        KokoroTTS r = KokoroTTS.getInstance();
                        r.setRole(KokoroTTS.Role.RADIO);
                        return r;
                    });
                    services.put(ServiceType.RADIO_MOUTH, radio);
                }
                radio.start(); // no-op if the radio engine is already running
            }
            appendToLog("TTS service restarted");
        }
    }

    private void appendToLog(String data) {
        UiBus.publish(new AppLogEvent(data));
    }

    private void startServices() {
        synchronized (lifecycleLock) {
            if (serviceState != ServicesStateEvent.State.STOPPED) {
                log.info("Ignoring service start request while lifecycle state is {}", serviceState);
                return;
            }
            transitionTo(ServicesStateEvent.State.STARTING);

            try {
                checkForUpdates();
                UiBus.publish(new ClearConsoleEvent());
                initServices();

                for (ServiceHolder service : services.values()) {
                    service.start();
                }

                transitionTo(ServicesStateEvent.State.RUNNING);

                Timer connectionCheckTimer = new Timer(2000, e -> {
                    GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedSpeech("speech.connectingToLlm")));
                    connectionCheck();
                });
                connectionCheckTimer.setRepeats(false);
                connectionCheckTimer.start();

                // Setup first: it speaks about what is missing entirely, and the checks below assume a
                // configured app (KeyBindCheck reads a bindings profile; LocalLlmModelCheck judges a model).
                SetupCheck.getInstance().check();
                KeyBindCheck.getInstance().check();
                CustomCommandLoadAnnouncement.getInstance().announce();
                LocalLlmModelCheck.getInstance().check();
            } catch (RuntimeException | Error e) {
                log.error("Service startup failed while lifecycle was STARTING", e);
                stopServiceRegistry();
                transitionTo(ServicesStateEvent.State.STOPPED);
                throw e;
            }
        }
    }

    @Subscribe
    public void onLlmConnectionStatus(LlmConnectionStatusEvent event) {
        suppressConnectionFailSpeech = !event.connected() && retryAttemptNumber > 0;
        SwingUtilities.invokeLater(() -> {
            if (event.connected()) {
                stopRetryTimer();
            } else if (isServiceRunning()) {
                startRetryTimer();
            }
        });
    }

    private void connectionCheck() {
        bgExecutor.submit(() -> {
            try {
                // Probe the live analysis endpoint directly (companion and query handlers share the same
                // provider config). Publishing LlmConnectionStatusEvent drives the UI + retry timer; the
                // spoken result is silenced during silent retries via suppressConnectionFailSpeech.
                boolean reachable = ApiFactory.getInstance().getAnalysisEndpoint().verifyConnection();
                UiBus.publish(new LlmConnectionStatusEvent(reachable));
                if (!suppressConnectionFailSpeech) {
                    String key = reachable ? "speech.connectionSuccessful" : "speech.connectionFailed";
                    GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedResponse(key)));
                }
            } catch (Exception e) {
                log.warn("Connection check failed", e);
            }
        });
    }

    private void startRetryTimer() {
        if (retryConnectionTimer != null && retryConnectionTimer.isRunning()) return;
        retryConnectionTimer = new Timer(30_000, e -> {
            retryAttemptNumber++;
            connectionCheck();
        });
        retryConnectionTimer.setRepeats(true);
        retryConnectionTimer.start();
        log.info("LLM connection retry timer started (30s interval)");
    }

    private void stopRetryTimer() {
        retryAttemptNumber = 0;
        if (retryConnectionTimer != null) {
            retryConnectionTimer.stop();
            retryConnectionTimer = null;
        }
    }

    private void stopServices() {
        synchronized (lifecycleLock) {
            if (serviceState == ServicesStateEvent.State.STOPPED
                    || serviceState == ServicesStateEvent.State.STOPPING) {
                return;
            }
            transitionTo(ServicesStateEvent.State.STOPPING);
            stopRetryTimer();
            stopServiceRegistry();
            transitionTo(ServicesStateEvent.State.STOPPED);
            UiBus.publish(new AppLogEvent("All services are stopped\n\n"));
        }
    }

    private void initServices() {
        this.services.clear();
        this.services.putAll(buildServices(systemSession.useLocalTTS()));
    }

    private void stopServiceRegistry() {
        List<ServiceType> reverseOrder = new ArrayList<>(services.keySet());
        Collections.reverse(reverseOrder);
        for (ServiceType type : reverseOrder) {
            ServiceHolder holder = services.get(type);
            if (holder != null) {
                holder.stop();
            }
        }
        services.clear();
    }

    private boolean isServiceRunning() {
        return serviceState == ServicesStateEvent.State.RUNNING;
    }

    // WHY: transitions are published while lifecycleLock is held so a state change and the work it guards
    // cannot interleave with another lifecycle call. UiBus delivery is synchronous, so ServicesStateEvent
    // subscribers must not block or synchronously re-enter a lifecycle method; current ones only
    // invokeLater or spawn their own threads.
    private void transitionTo(ServicesStateEvent.State target) {
        serviceState = target;
        UiBus.publish(new ServicesStateEvent(target));
    }

    /**
     * Builds the ordered service registry. Static and side-effect-free (it only wires lazy suppliers,
     * nothing is started here) so the registration can be verified in tests without standing up the
     * controller. Order matters: audio and command execution come up first, then the COMPANION subsystem
     * (the sole LLM service since the legacy command pipeline was removed). Live game-file monitors start
     * last so journal/status subscriber cascades cannot publish into a half-started speech/companion layer.
     * <p>
     * Radio transmissions are always voiced by Kokoro. When Kokoro is also the main mouth
     * ({@code useLocalTts}) it handles radio through its own queue and no extra service is needed.
     * When the main mouth is a cloud provider, a dedicated {@link ServiceType#RADIO_MOUTH} runs the
     * Kokoro engine in {@link KokoroTTS.Role#RADIO} alongside it.
     */
    static LinkedHashMap<ServiceType, ServiceHolder> buildServices(boolean useLocalTts) {
        LinkedHashMap<ServiceType, ServiceHolder> services = new LinkedHashMap<>();
        // Diagnostics swaps in a no-op EARS: the microphone/STT is never used (input comes from the file), and
        // skipping the heavy STT model load is a large startup win with no effect on the run. TTS is NOT
        // suppressed - the DiagnosticsSpeechGateway (wired below) delegates to the real speech path, so the
        // companion voice stays audible and the chat panel shows replies, matching a live session.
        boolean diagnostics = DiagnosticsMode.isEnabled();
        services.put(ServiceType.MOUTH, new ServiceHolder(ApiFactory.getInstance()::getMouthImpl));
        if (!useLocalTts) {
            services.put(ServiceType.RADIO_MOUTH, new ServiceHolder(() -> {
                KokoroTTS radio = KokoroTTS.getInstance();
                radio.setRole(KokoroTTS.Role.RADIO);
                return radio;
            }));
        }
        services.put(ServiceType.EARS, new ServiceHolder(
                diagnostics ? DiagnosticsEars::new : ApiFactory.getInstance()::getEarsImpl));
        services.put(ServiceType.HANDS, new ServiceHolder(HandsService::new));
        services.put(ServiceType.DEVICE, new ServiceHolder(() -> new ManagedService() {
            public void start() {
                DeviceService.getInstance().start();
            }

            public void stop() {
                DeviceService.getInstance().stop();
            }
        }));
        // After DEVICE, whose poll loop feeds it the button transitions, and after EARS, because the gate it
        // arms is the microphone's and there must be a microphone to gate.
        services.put(ServiceType.PUSH_TO_TALK, new ServiceHolder(PushToTalkService::getInstance));
        // The companion subsystem is the LLM service (the legacy command pipeline was removed). In diagnostics
        // mode it is wired with a recording execution gateway (file-fed phrases exercise the real routing path
        // without pressing keys into the game or calling third-party REST APIs) and a speech gateway that
        // delegates to the real TTS path (audible voice + chat-panel replies, indistinguishable from normal).
        services.put(ServiceType.COMPANION, new ServiceHolder(diagnostics
                ? () -> new CompanionSubsystemGate(null, new DiagnosticsExecutionGateway(), new DiagnosticsSpeechGateway())
                : CompanionSubsystemGate::new));
        services.put(ServiceType.NOTIFICATION_MONITOR, new ServiceHolder(DeferredNotificationMonitor::getInstance));
        services.put(ServiceType.MISSING_MISSION_MONITOR, new ServiceHolder(MissingMissionMonitor::getInstance));
        services.put(ServiceType.WEB_SOCKET, new ServiceHolder(WebSocketBroadcaster::getInstance));
        // Diagnostics also stubs the live game-file monitors: AuxiliaryFilesMonitor re-reads the game's stale
        // Status.json every 120 ms and JournalParser tails the real journal, both of which would overwrite the
        // Status/session the harness sets via @visible/@status/@event - defeating deterministic context control
        // and silently dropping any command gated on isVisibleForLLM. The harness owns game state instead.
        services.put(ServiceType.JOURNAL_PARSER, new ServiceHolder(
                diagnostics ? NoOpService::new : JournalParser::new));
        services.put(ServiceType.AUXILIARY_FILES_MONITOR, new ServiceHolder(
                diagnostics ? NoOpService::new : AuxiliaryFilesMonitor::new));
        return services;
    }

    static class ServiceHolder {
        private final Supplier<? extends ManagedService> creator;
        private ManagedService instance;

        ServiceHolder(Supplier<? extends ManagedService> creator) {
            this.creator = Objects.requireNonNull(creator);
        }

        synchronized void start() {
            if (instance != null) return;
            ManagedService created = creator.get();
            if (created == null) return;
            instance = created;
            try {
                created.start();
            } catch (RuntimeException | Error e) {
                instance = null;
                try {
                    created.stop();
                } catch (RuntimeException | Error stopFailure) {
                    e.addSuppressed(stopFailure);
                }
                throw e;
            }
        }

        synchronized void stop() {
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

    enum ServiceType {
        MOUTH, RADIO_MOUTH, EARS, HANDS, DEVICE, PUSH_TO_TALK, COMPANION, NOTIFICATION_MONITOR,
        MISSING_MISSION_MONITOR, WEB_SOCKET, JOURNAL_PARSER, AUXILIARY_FILES_MONITOR
    }
}
