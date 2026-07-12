package elite.intel.diagnostics;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.ears.IsSpeakingEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.NormalizedUserInputEvent;
import elite.intel.ui.controller.ManagedService;
import elite.intel.ui.event.AiResponseLogEvent;
import elite.intel.ui.event.AppLogDebugEvent;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.LlmConnectionStatusEvent;

/**
 * Mirrors the GUI SYSTEM LOG stream to the diagnostics {@link DiagnosticsLog session.log}, so an automated
 * tester reads the companion's reactions from a file instead of the screen. Subscribes to the same two buses
 * as {@code AiTabController}: UI log events ride {@link UiBus}, while user-input and speech events ride
 * {@link GameEventBus}. Each line is prefixed by kind ({@code USER}/{@code AI}/{@code LOG}/{@code DBG}/
 * {@code DIAG}) so the log is easy to grep. Active only in diagnostics mode.
 */
public final class DiagnosticsLogWriter implements ManagedService {

    // DIAG ready is emitted once, on the first confirmed LLM connection - the true "all services up and the
    // LLM endpoint is reachable" signal (fires after ServicesStateEvent + the connection probe), so the tester
    // never feeds phrases while the stack is still coming up.
    private volatile boolean readyEmitted;

    @Override
    public void start() {
        UiBus.register(this);
        GameEventBus.register(this);
    }

    @Override
    public void stop() {
        UiBus.unregister(this);
        GameEventBus.unregister(this);
    }

    @Subscribe
    public void onAppLog(AppLogEvent event) {
        writeIfPresent("LOG", event.getData());
    }

    @Subscribe
    public void onAppLogDebug(AppLogDebugEvent event) {
        writeIfPresent("DBG", event.getData());
    }

    @Subscribe
    public void onAiResponse(AiResponseLogEvent event) {
        writeIfPresent("AI", event.getData());
    }

    /** Mirrors AiTabController: blank log payloads (e.g. spacer AppLogEvent("")) are dropped, not logged. */
    private void writeIfPresent(String prefix, String data) {
        if (data == null || data.isBlank()) {
            return;
        }
        DiagnosticsLog.write(prefix + " " + data);
    }

    @Subscribe
    public void onUserInput(NormalizedUserInputEvent event) {
        DiagnosticsLog.write("USER " + event.getText());
    }

    @Subscribe
    public void onSpeaking(IsSpeakingEvent event) {
        DiagnosticsLog.write("DIAG speaking=" + event.isSpeaking());
    }

    @Subscribe
    public void onLlmConnectionStatus(LlmConnectionStatusEvent event) {
        if (event.connected()) {
            if (!readyEmitted) {
                readyEmitted = true;
                DiagnosticsLog.write("DIAG ready");
            }
        } else {
            DiagnosticsLog.write("DIAG llm-not-connected");
        }
    }
}
