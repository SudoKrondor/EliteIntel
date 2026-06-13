package elite.intel.gameapi.gamestate.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.gamestate.status_events.BeingInterdictedEvent;
import elite.intel.gameapi.gamestate.status_events.InGlideEvent;
import elite.intel.gameapi.gamestate.status_events.PlayerMovedEvent;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags.GuiFocus;
import elite.intel.session.ui.PanelStateTracker;

import static elite.intel.util.StringUtls.localizedEvent;

public class StatusEventSubscriber {

    private boolean lowFuelAnnounced = false;
    private boolean lowOxygenAnnounced = false;
    private boolean lowHealthAnnounced = false;
    private boolean glideAnnounced = false;
    private String lastAnnouncedLegalState = null;

    // Track previous GuiFocus to detect transitions, not just current state
    private GuiFocus previousGuiFocus = GuiFocus.NO_FOCUS;

    @Subscribe
    public void onStatusChangedEvent(GameEvents.StatusEvent event) {
        Status status = Status.getInstance();

        // --------------------------------------------------------------------------------------
        // GuiFocus transition - must run before setStatus() so we compare against the previous state.
        GuiFocus currentGuiFocus = GuiFocus.fromValue(event.getGuiFocus());
        if (currentGuiFocus != previousGuiFocus) {
            PanelStateTracker.getInstance().onGuiFocusChanged(currentGuiFocus);
            previousGuiFocus = currentGuiFocus;
        }

        // --------------------------------------------------------------------------------------
        // Legal state change alert - suppress Speeding (transient) and deduplicate against
        // the last state we actually announced so rapid oscillation doesn't repeat the alert.
        String legalState = event.getLegalState();
        if (legalState != null
                && !"Speeding".equalsIgnoreCase(legalState)
                && !"Clean".equalsIgnoreCase(legalState)
                && !legalState.equalsIgnoreCase(lastAnnouncedLegalState)) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(localizedEvent("event.status.legalStatus", legalState)));
            lastAnnouncedLegalState = legalState;
        }

        status.setStatus(event);
        EventBusManager.publish(new PlayerMovedEvent(event.getLatitude(), event.getLongitude(), event.getPlanetRadius(), event.getAltitude()));

        // --------------------------------------------------------------------------------------
        //TODO: Can throw custom events. like BeingInterdictedEvent if(status.isBeingInterdicted()){ publish event...}

        EventBusManager.publish(new InGlideEvent(status.isGlideMode()));

        if (status.isBeingInterdicted()) {
            EventBusManager.publish(new BeingInterdictedEvent());
        }

        // --------------------------------------------------------------------------------------
        // Mission-critical alerts
        if (status.isLowFuel() && !lowFuelAnnounced) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(localizedEvent("event.status.lowFuel")));
            lowFuelAnnounced = true;
        }

        if (status.isLowOxygen() && !lowOxygenAnnounced) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(localizedEvent("event.status.lowOxygen")));
            lowOxygenAnnounced = true;
        }

        if (status.isLowHealth() && !lowHealthAnnounced) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(localizedEvent("event.status.lowHealth")));
            lowHealthAnnounced = true;
        }

        if (status.isGlideMode() && !glideAnnounced) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(localizedEvent("event.status.glideEngaged")));
            glideAnnounced = true;
        } else if (!status.isGlideMode() && glideAnnounced) {
            glideAnnounced = false;
        }
    }
}