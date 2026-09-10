package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.VegaRuntime;
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
    /**
     * Cooldown plus lockdown, whatever the distance jumped.
     */
    private static final int MINUTES_PER_JUMP = 20;
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
            // went uncharged, unannounced at its true fuel level and, off route, never voided the route.
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
            String remainingRoute = voyageReport(starSystem, fleetCarrierRouteManager);

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
            VegaRuntime.narrator().narrate(
                    "Carrier arrived in " + event.getStarSystem()
                            + ". Fuel supply: " + CarrierFuelPhrase.of(postJumpCarrierData)
                            + ". Fuel in reserve: " + postJumpCarrierData.getFuelReserve() + " tons."
                            + remainingRoute,
                            instructions
                    );
            DeferredNotificationManager.getInstance().scheduleNotification(localizedEvent("event.carrier.jumpCooldownComplete"), FOUR_MINUTES);
        });
    }

    /**
     * What this arrival means for the plotted voyage, as a phrase to append to the announcement.
     *
     * <p>WHY it asks the arrival owner instead of counting the legs left in the table: a leg count is
     * only about this jump when the carrier is actually on the route it came from. It used to be read
     * unconditionally, so a commander who scheduled a single jump of his own was told "one jump
     * remaining" - the leg of a route he had stopped following months ago, which nothing consumed and
     * nothing cleared - or, with no route at all, "final destination reached".
     *
     * <p>Empty for a jump that has nothing to do with a route, which is most of them.
     */
    private static String voyageReport(String starSystem, FleetCarrierRouteManager route) {
        return switch (CarrierArrival.voyageStatusAt(starSystem)) {
            case NO_ROUTE -> "";
            case ROUTE_ABANDONED -> " " + localizedEvent("event.carrier.jump.routeAbandoned");
            case DESTINATION_REACHED -> " " + localizedEvent("event.carrier.jump.finalDest");
            case EN_ROUTE -> {
                int jumpsRemaining = route.getFleetCarrierRoute().size();
                yield " " + localizedEvent("event.carrier.jump.remaining",
                        localizedEventPlural(jumpsRemaining, "event.carrier.jump.count"),
                        travelTime(jumpsRemaining * MINUTES_PER_JUMP));
            }
        };
    }

    /**
     * A leg count spoken as a duration. A carrier jump costs the same twenty minutes whatever its length.
     */
    private static String travelTime(int totalMinutes) {
        if (totalMinutes <= 59) {
            return localizedEventPlural(totalMinutes, "event.time.minutes");
        }
        return localizedEvent("event.time.hoursAndMinutes",
                localizedEventPlural(totalMinutes / 60, "event.time.hours"),
                localizedEventPlural(totalMinutes % 60, "event.time.minutes"));
    }
}
