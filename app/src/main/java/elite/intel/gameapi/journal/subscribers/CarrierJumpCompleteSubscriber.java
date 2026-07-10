package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.EventNarrator;
import elite.intel.companion.CompanionRuntime;
import elite.intel.db.managers.DeferredNotificationManager;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.journal.events.CarrierJumpEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.AppLogEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.intel.util.StringUtls.localizedEvent;
import static elite.intel.util.StringUtls.localizedEventPlural;

@SuppressWarnings("unused")
public class CarrierJumpCompleteSubscriber {
    private static final Logger log = LogManager.getLogger(CarrierJumpCompleteSubscriber.class);
    private static final Long FOUR_MINUTES = (long) (1000 * 60 * 4);
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Subscribe
    public void onCarrierJumpCompleteEvent(CarrierJumpEvent event) {
        Thread.ofVirtual().start(() -> {
            String starSystem = event.getStarSystem();
            double[] starPos = event.getStarPos();

            if (starPos == null || starPos.length != 3) {
                log.warn("CarrierJump for {} carries a malformed StarPos ({}); skipping carrier jump handling",
                        starSystem, starPos == null ? "absent" : starPos.length + " values");
                return;
            }

            boolean coordsArePlaceholder = starPos[0] == 0.0 && starPos[1] == 0.0 && starPos[2] == 0.0
                    && !"sol".equalsIgnoreCase(starSystem);

            if (coordsArePlaceholder) {
                UiBus.publish(new AppLogEvent(localizedEvent("event.carrier.jumpCompleteStarWarning")));
                EventNarrator.critical(localizedEvent("event.carrier.jumpCompleteNoStar"));
            }

            FleetCarrierRouteManager fleetCarrierRouteManager = FleetCarrierRouteManager.getInstance();

            // WHY: the system we are sitting in is never part of the remaining route. CarrierLocation
            // normally clears the leg (and decrements fuel from it) a minute earlier, but that event
            // is absent from older journals, so removing it here too keeps the route correct. The
            // call is idempotent, and fuel is decremented in CarrierLocationSubscriber alone.
            fleetCarrierRouteManager.removeLeg(starSystem);

            // WHY: CarrierJump is only written when the commander is aboard, but the DOCKED status
            // flag is set only while he is in his ship. On foot in the concourse it is clear, so the
            // live Status singleton cannot answer "was I aboard". The event's own fields can.
            boolean commanderAboard = event.isDocked() || event.isOnFoot();
            if (commanderAboard) {
                playerSession.setCurrentPrimaryStarName(starSystem);
                if (event.getBodyId() == null) {
                    log.warn("CarrierJump for {} carries no BodyID; current location row not saved", starSystem);
                } else {
                    LocationDto arrival = CarrierJumpLocationMapper.toArrivalLocation(event, locationManager);
                    locationManager.save(arrival);
                    // WHY: point at the id the row actually holds, not the one the event reported.
                    // LocationDto.setBodyId ignores a lower id, so an event reporting BodyID 0 for an
                    // already identified body would otherwise leave the pointer aimed at nothing.
                    playerSession.setCurrentLocationId(arrival.getBodyId(), event.getSystemAddress());
                }
            }

            if (!coordsArePlaceholder) {
                // WHY: CarrierJump carries the destination StarPos, which is authoritative and free.
                // CarrierLocation has no coordinates and has to resolve them from the route leg or
                // over the network, so refine them here whenever the commander was aboard to see it.
                CarrierDataDto carrierData = playerSession.getFleetCarrierData();
                carrierData.setStarName(starSystem);
                carrierData.setSystemAddress(event.getSystemAddress());
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
            CompanionRuntime.narrator().narrate(
                            "Carrier Location: " + event.getStarSystem() + " fuelSupply " + postJumpCarrierData.getFuelLevel() + " fuelReserve:" + postJumpCarrierData.getFuelReserve() + remainingRoute,
                            instructions
                    );
            DeferredNotificationManager.getInstance().scheduleNotification(localizedEvent("event.carrier.jumpCooldownComplete"), FOUR_MINUTES);
        });
    }

}
