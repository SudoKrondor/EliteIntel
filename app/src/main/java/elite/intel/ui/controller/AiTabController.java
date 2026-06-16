package elite.intel.ui.controller;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.EventBusManager;
import elite.intel.ui.event.*;
import elite.intel.ui.screen.AiTabPanel;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicBoolean;

public class AiTabController {

    private final AiTabPanel view;
    private final AtomicBoolean showDetailedLog = new AtomicBoolean(false);

    public AiTabController(AiTabPanel view) {
        this.view = view;
        EventBusManager.register(this);
    }

    public void dispose() {
        EventBusManager.unregister(this);
    }

    @Subscribe
    public void onToggleDetailedLogEvent(ToggleDetailedLogEvent event) {
        showDetailedLog.set(event.isDetailed());
    }

    @Subscribe
    public void onAppLogDebugEvent(AppLogDebugEvent event) {
        if (!showDetailedLog.get()) return;
        view.addSystemMessage(event.getTimestamp(), event.getData());
    }

    @Subscribe
    public void onAppLogEvent(AppLogEvent event) {
        if (event.getData() == null || event.getData().isBlank()) return;
        view.addSystemMessage(event.getTimestamp(), event.getData());
    }

    @Subscribe
    public void onUserInputEvent(NormalizedUserInputEvent event) {
        view.addUserMessage(event.getText());
    }

    @Subscribe
    public void onAiResponseLogEvent(AiResponseLogEvent event) {
        view.addAiMessage(event.getData());
    }
}
