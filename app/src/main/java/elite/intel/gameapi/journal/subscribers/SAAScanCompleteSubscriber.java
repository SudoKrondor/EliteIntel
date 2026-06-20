package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.journal.events.SAAScanCompleteEvent;
import elite.intel.session.PlayerSession;

public class SAAScanCompleteSubscriber {

    @Subscribe
    public void onSAAScanComplete(SAAScanCompleteEvent event) {

        if (PlayerSession.getInstance().isDiscoveryAnnouncementOn()) {
            String message = "Surface scan complete " + efficiency(event);
            String instructions = """
                        Report the surface scan result.
                        State the efficiency outcome clearly: whether the target was beaten, met, or missed, and by how many probes.
                    """;
            GameEventBus.publish(new SensorDataEvent(message, instructions));
        }
    }

    private String efficiency(SAAScanCompleteEvent event) {
        if (event.getEfficiencyTarget() > event.getProbesUsed()) {
            return " Efficiency Excellent: Only " + event.getProbesUsed() + " probes used out of " + event.getEfficiencyTarget() + ". ";
        } else if (event.getEfficiencyTarget() == event.getProbesUsed() || event.getBodyName().contains("Ring")) {
            return " Efficiency Target Met. ";
        } else {
            return " Efficiency Poor: " + event.getProbesUsed() + " exceeding efficiency target of " + event.getEfficiencyTarget() + " probes. ";
        }
    }
}
