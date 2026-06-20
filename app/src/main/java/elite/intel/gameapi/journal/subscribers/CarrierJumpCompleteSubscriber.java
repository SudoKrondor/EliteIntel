package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.DeferredNotificationManager;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.journal.events.CarrierJumpEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.search.spansh.carrierroute.CarrierJump;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.ClipboardUtils;
import elite.intel.util.FleetCarrierRouteCalculator;

import java.util.Objects;

import static elite.intel.util.StringUtls.localizedEvent;
import static elite.intel.util.StringUtls.localizedEventPlural;

@SuppressWarnings("unused")
public class CarrierJumpCompleteSubscriber {
    private static final Long FOUR_MINUTES = (long) (1000 * 60 * 4);
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Subscribe
    public void onCarrierJumpCompleteEvent(CarrierJumpEvent event) {
        Thread.ofVirtual().start(() -> {
            String starSystem = event.getStarSystem();
            double[] starPos = event.getStarPos();
            playerSession.setLastKnownCarrierLocation(starSystem);

            if (starPos.length == 3 && starPos[0] == 0.0 && starPos[1] == 0.0 && starPos[2] == 0 && !"sol".equalsIgnoreCase(starSystem)) {
                UiBus.publish(new AppLogEvent(localizedEvent("event.carrier.jumpCompleteStarWarning")));
                GameEventBus.publish(new MissionCriticalAnnouncementEvent(localizedEvent("event.carrier.jumpCompleteNoStar")));
            }


            FleetCarrierRouteManager fleetCarrierRouteManager = FleetCarrierRouteManager.getInstance();
            CarrierDataDto carrierData = playerSession.getFleetCarrierData();
            carrierData.setSystemAddress(event.getSystemAddress());
            carrierData.setStarName(event.getStarSystem());
            CarrierJump currentLocationLeg = fleetCarrierRouteManager.findByPrimaryStar(event.getStarSystem());
            boolean currentLegIsNotPresent = currentLocationLeg == null;
            boolean routePlotted = !fleetCarrierRouteManager.getFleetCarrierRoute().isEmpty();

            if (currentLegIsNotPresent && routePlotted) {
                String systemName = fleetCarrierRouteManager
                        .getFleetCarrierRoute()
                        .get(fleetCarrierRouteManager.getFleetCarrierRoute().size() - 1)
                        .getSystemName();
                ClipboardUtils.setClipboardText(systemName);
                FleetCarrierRouteCalculator.calculate();
            }

            int fuelUsed = currentLocationLeg == null ? 0 : currentLocationLeg.getFuelUsed();
            carrierData.setFuelLevel(carrierData.getFuelLevel() - fuelUsed);
            playerSession.setFleetCarrierData(carrierData);

            fleetCarrierRouteManager.removeLeg(event.getStarSystem());

            playerSession.setCarrierDepartureTime(null);

            Status status = Status.getInstance();
            CarrierDataDto carrierInfo = playerSession.getFleetCarrierData();

            LocationDto location = toLocationDto(event);
            if (status.isDocked()) {
                location.setX(starPos[0]);
                location.setY(starPos[1]);
                location.setZ(starPos[2]);
                playerSession.setCurrentLocationId(event.getBodyId(), event.getSystemAddress());
                playerSession.setCurrentPrimaryStarName(event.getStarSystem());
                locationManager.save(location);
            }

            if (starPos[0] > 0) {
                carrierData.setX(starPos[0]);
                carrierData.setY(starPos[1]);
                carrierData.setZ(starPos[2]);
                playerSession.setFleetCarrierData(carrierData);
            }

            CarrierDataDto postJumpCarrierData = playerSession.getFleetCarrierData();
            int numJumpsRemaining = fleetCarrierRouteManager.getFleetCarrierRoute().size();
            int estimatedTimeToFinal = numJumpsRemaining * 20;
            String timeString;
            if (estimatedTimeToFinal > 59) {
                int hours = estimatedTimeToFinal / 60;
                int minutes = estimatedTimeToFinal % 60;
                timeString = localizedEvent("event.time.hoursAndMinutes",
                        localizedEventPlural(hours, "event.time.hours"),
                        localizedEventPlural(minutes, "event.time.minutes"));
            } else {
                timeString = localizedEventPlural(estimatedTimeToFinal, "event.time.minutes");
            }
            String remainingRoute = numJumpsRemaining == 0
                    ? " " + localizedEvent("event.carrier.jump.finalDest")
                    : " " + localizedEvent("event.carrier.jump.remaining",
                    localizedEventPlural(numJumpsRemaining, "event.carrier.jump.count"), timeString);

            String instructions = """
                        Notify user about new carrier location.
                        Example: Carrier jump complete!. New location <starSystem>, remaining fuel supply <fuelSupply> tons. Fuel in reserve <fuelReserve> tons.
                    """;
            GameEventBus.publish(
                    new SensorDataEvent(
                            "Carrier Location: " + event.getStarSystem() + " fuelSupply " + postJumpCarrierData.getFuelLevel() + " fuelReserve:" + postJumpCarrierData.getFuelReserve() + remainingRoute,
                            instructions
                    )
            );
            DeferredNotificationManager.getInstance().scheduleNotification(localizedEvent("event.carrier.jumpCooldownComplete"), FOUR_MINUTES);
        });
    }

    private LocationDto toLocationDto(CarrierJumpEvent event) {
        LocationManager locationData = LocationManager.getInstance();
        LocationDto location = locationData.getLocation(event.getStarSystem(), event.getBodyId());
        return fillInWhatWeCan(event, Objects.requireNonNullElseGet(location, () -> new LocationDto(event.getBodyId(), event.getStarSystem())));
    }

    private LocationDto fillInWhatWeCan(CarrierJumpEvent event, LocationDto location) {
        location.setAllegiance(event.getSystemAllegiance());
        location.setX(event.getStarPos()[0]);
        location.setY(event.getStarPos()[1]);
        location.setZ(event.getStarPos()[2]);
        location.setStationGovernment(event.getSystemGovernmentLocalised());
        location.setPlanetName(event.getBody());
        location.setAllegiance(event.getSystemAllegiance());
        location.setControllingPower(event.getControllingPower());
        //...
        return location;
    }
}
