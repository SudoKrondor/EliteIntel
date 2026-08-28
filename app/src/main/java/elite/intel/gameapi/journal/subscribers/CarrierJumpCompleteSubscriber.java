package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.ai.mouth.EventNarrator;
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

            // WHY here as well as from CarrierLocation: CarrierJump proves the carrier is in this system,
            // and CarrierLocation is absent from older journals. Both events hand the same arrival to the
            // same owner, which charges it once and serialises the two threads - without that, whichever
            // wrote the carrier's system first left the other believing it had never moved, and the jump
            // went uncharged, unannounced at its true fuel level and, off route, never re-plotted.
            //
            // WHY the coordinates go in with it: CarrierJump carries the destination StarPos, which is
            // authoritative and free, where CarrierLocation has none and would resolve them over the
            // network. Placeholder coordinates are withheld so they are resolved properly instead.
            CarrierArrival.recordFleetArrival(starSystem, event.getSystemAddress(),
                    coordsArePlaceholder ? null : starPos);

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

            // WHY through the arrival owner: the level quoted below must be the one this jump left behind,
            // never the one the depot held before it.
            CarrierDataDto postJumpCarrierData = CarrierArrival.settledFleetCarrierData();
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

            // WHY the figure arrives pre-worded: whether the depot level is known or merely worked out is
            // ours to decide, not the model's, and it must not quietly firm up an estimate into a fact.
            // WHY the payload is worded rather than keyed: the model reads it as prose and will happily
            // read a field name back out loud - a commander was told his carrier held "1000 tons
            // fuelSupply". Every label here is therefore a phrase a human would say.
            String instructions = """
                        Notify user about new carrier location.
                        Example: Carrier jump complete!. New location <system>, remaining fuel supply <tons>. Fuel in reserve <tons> tons.
                        Quote the fuel supply exactly as given, keeping the word "approximately" when it is there.
                        Never read a label out as written; say it the way a person would.
                    """;
            CompanionRuntime.narrator().narrate(
                    "Carrier arrived in " + event.getStarSystem()
                            + ". Fuel supply: " + CarrierFuelPhrase.of(postJumpCarrierData)
                            + ". Fuel in reserve: " + postJumpCarrierData.getFuelReserve() + " tons."
                            + remainingRoute,
                            instructions
                    );
            DeferredNotificationManager.getInstance().scheduleNotification(localizedEvent("event.carrier.jumpCooldownComplete"), FOUR_MINUTES);
        });
    }

}
