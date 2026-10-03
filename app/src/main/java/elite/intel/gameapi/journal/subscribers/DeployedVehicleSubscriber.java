package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.journal.events.*;
import elite.intel.session.DeployedVehicle;

/**
 * Keeps {@link DeployedVehicle} level with the journal, so the Nomad - which Status.json reports as an SRV -
 * can be told apart from a wheeled SRV.
 * <p>
 * Registered on the pre-scan bus as well as the live one: a commander who starts the app while already in the
 * Nomad gets no launch event on the live bus, and only today's {@code LoadGame} (or an earlier
 * {@code LaunchVessel}) says what they are sitting in. One who logs in ON FOOT needs the slot ids the earlier
 * journals tied to each vehicle, to know what their next {@code Embark} climbs into. A single assignment, so it
 * runs on the bus thread.
 */
public class DeployedVehicleSubscriber {

    private final DeployedVehicle deployedVehicle = DeployedVehicle.getInstance();

    @Subscribe
    public void onLaunchVessel(LaunchVesselEvent event) {
        if (event == null) return;
        deployedVehicle.launched(event.getId(), event.getVesselType());
    }

    @Subscribe
    public void onLaunchSRV(LaunchSRVEvent event) {
        if (event == null) return;
        deployedVehicle.launched(event.getId(), event.getSrvType());
    }

    @Subscribe
    public void onDockSRV(DockSRVEvent event) {
        if (event == null) return;
        deployedVehicle.stowed(event.getId(), event.getSrvType());
    }

    /**
     * Back in from on foot. Only an SRV boarding says anything - the ship is not a deployed vehicle.
     */
    @Subscribe
    public void onEmbark(EmbarkEvent event) {
        if (event == null || !event.isSrv()) return;
        deployedVehicle.reboarded(event.getId());
    }

    /**
     * {@code Ship} names whatever the commander is sitting in at login - the Nomad as {@code Lander01}.
     */
    @Subscribe
    public void onLoadGame(LoadGameEvent event) {
        if (event == null) return;
        deployedVehicle.boarded(event.getShip());
    }
}
