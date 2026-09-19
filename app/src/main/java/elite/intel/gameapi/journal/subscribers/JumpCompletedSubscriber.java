package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.ai.mouth.EventNarrator;
import elite.intel.db.dao.DestinationReminderDao;
import elite.intel.db.dao.RouteMonetisationDao.MonetisationTransaction;
import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.db.managers.*;
import elite.intel.gameapi.DiscoveryScanner;
import elite.intel.gameapi.FuelScoop;
import elite.intel.gameapi.gamestate.dtos.NavRouteDto;
import elite.intel.gameapi.hge.HighGradeEmissionsAdvisor;
import elite.intel.gameapi.journal.events.FSDJumpEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.search.edsm.EdsmApiClient;
import elite.intel.gameapi.search.edsm.dto.DeathsDto;
import elite.intel.gameapi.search.edsm.dto.TrafficDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static elite.intel.util.StringUtls.*;

@SuppressWarnings("unused")
public class JumpCompletedSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final SystemSession systemSession = SystemSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final ShipRouteManager shipRoute = ShipRouteManager.getInstance();
    private final MonetizeRouteManager monetizeRouteManager = MonetizeRouteManager.getInstance();
    private final ReminderManager destinationReminderManager = ReminderManager.getInstance();
    private final ShipSettingsManager shipSettingsManager = ShipSettingsManager.getInstance();
    private final NeutronStarRouteManager neutronStarRouteManager = NeutronStarRouteManager.getInstance();
    private final GlobalSettingsManager globalSettings = GlobalSettingsManager.getInstance();
    private final HighGradeEmissionsAdvisor hgeAdvisor = HighGradeEmissionsAdvisor.getInstance();

    @Subscribe
    public void onFSDJumpEvent(FSDJumpEvent event) {
        Thread.ofVirtual().start(() -> {
            neutronStarRouteManager.removeLeg(event.getSystemAddress());

            EdsmSystemBodies.fetchAndRecord(event.getStarSystem(), event.getSystemAddress(), event.getStarPos());

            boolean isSellerSystem = monetizeRouteManager.isSeller(event.getStarSystem());
            boolean isBuyerSystem = monetizeRouteManager.isBuyer(event.getStarSystem());
            MonetisationTransaction station = monetizeRouteManager.getTransaction();

            LocationDto primaryStar = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBodyId());
            primaryStar.setBodyId(event.getBodyId());
            primaryStar.setSystemAddress(event.getSystemAddress());
            primaryStar.setStationGovernment(event.getSystemGovernmentLocalised());
            primaryStar.setAllegiance(event.getSystemAllegiance());
            primaryStar.setSecurity(event.getSystemSecurityLocalised());
            if (event.getSystemFaction() != null) primaryStar.setSystemFaction(event.getSystemFaction().getName());
            primaryStar.setStarName(event.getStarSystem());
            primaryStar.setPlanetName(event.getBody());
            primaryStar.setLocationType(LocationDto.LocationType.PRIMARY_STAR);
            primaryStar.setX(event.getStarPos()[0]);
            primaryStar.setY(event.getStarPos()[1]);
            primaryStar.setZ(event.getStarPos()[2]);
            primaryStar.setPopulation(event.getPopulation());
            primaryStar.setPowerplayState(event.getPowerplayState());
            primaryStar.setPowerplayStateControlProgress(event.getPowerplayStateControlProgress());
            primaryStar.setPowerplayStateReinforcement(event.getPowerplayStateReinforcement());
            primaryStar.setPowerplayStateUndermining(event.getPowerplayStateUndermining());
            playerSession.setCurrentLocationId(primaryStar.getBodyId(), event.getSystemAddress());
            playerSession.setCurrentPrimaryStarName(primaryStar.getStarName());


            String finalDestination = playerSession.getFinalDestination();

            StringBuilder sb = new StringBuilder();
            List<NavRouteDto> orderedRoute = shipRoute.getOrderedRoute();
            boolean roueSet = !orderedRoute.isEmpty();
            DestinationReminderDao.Reminder reminder = destinationReminderManager.getReminder();
            String reminderText = null;
            if (reminder != null && event.getStarSystem().equals(reminder.getStarSystem())) {
                reminderText = reminder.getReminder() == null ? "" : reminder.getReminder();
            }

            // Reminders are spoken whatever the arrival toggle says: the commander left a note for this
            // system precisely so they would hear it here, and silencing "Arrived at" is not a request
            // to lose that.
            if (finalDestination != null && finalDestination.equalsIgnoreCase(event.getStarSystem())) {
                shipRoute.clearRoute();
                if (reminderText != null && !reminderText.isBlank()) {
                    EventNarrator.say(localizedEvent("event.route.reminder", reminderText));
                } else if (globalSettings.getAnnounceArrival()) {
                    sb.append(localizedEvent("event.route.arrivedFinal", finalDestination));
                }
                TrafficDto trafficDto = EdsmApiClient.searchTraffic(finalDestination);
                DeathsDto deathsDto = EdsmApiClient.searchDeaths(finalDestination);
                primaryStar.setTrafficDto(trafficDto);
                primaryStar.setDeathsDto(deathsDto);

            } else if (roueSet) {
                // Matched on the reminder's system column above; the text itself no longer names the system.
                if (reminderText != null && !reminderText.isBlank()) {
                    EventNarrator.say(localizedEvent("event.route.reminder", reminderText));
                }

                sb.append(waypointArrival(event.getStarSystem(), shipRoute.getOrderedRoute(),
                        globalSettings.getAnnounceArrival(), globalSettings.getAnnounceRemainingJumps(), FuelScoop::announceFuelStars));
            }

            locationManager.save(primaryStar);

            if (!event.isReplay()) {
                if (playerSession.isRouteAnnouncementOn() && !sb.isEmpty()) {
                    VegaRuntime.narrator().narrate(sb.toString(), "Announce this route information.");
                }
                if (isSellerSystem && station != null) {
                    VegaRuntime.narrator().narrate(
                            "Head to " + station.getSourceStationName() + " buy " + station.getSourceCommodity(),
                            "Remind the commander of their active trade route: state the station name and the commodity to buy.");
                }
                if (isBuyerSystem && station != null) {
                    VegaRuntime.narrator().narrate(
                            "Head to " + station.getDestinationStationName() + " sell " + station.getDestinationCommodity(),
                            "Remind the commander of their active trade route: state the station name and the commodity to sell.");
                }
            }

            if (!event.isReplay()) {
                hgeAdvisor.onSystemEntered(
                        event.getSystemAllegiance(),
                        event.getPopulation(),
                        factionStates(event));
            }

            ShipSettingsDao.ShipSettings shipSettings = shipSettingsManager.getSettings(playerSession.getShipLoadout().getShipId());
            if (!event.isReplay() && shipSettings.isHonkOnJump()) {
                DiscoveryScanner.honk(shipSettings);
            }

        }); // end virtual thread
    }


    /**
     * What is said on reaching a waypoint that is not the end of the route: the arrival, then the road
     * ahead. They are two toggles because they answer different needs - a commander on a long haul may
     * want only the count of jumps left, and one who knows the road may want nothing at all - so each
     * part is dropped on its own and the line is empty when both are off.
     *
     * @param remainingRoute    the legs still to fly, the next one first
     * @param announceFuelStars whether a scoopable next star is worth mentioning, asked only when the
     *                          road ahead is spoken at all - the answer costs a loadout lookup
     * @return the line to narrate, empty when there is nothing the commander asked to hear
     */
    static String waypointArrival(String starSystem, List<NavRouteDto> remainingRoute,
                                  boolean announceArrival, boolean announceRemainingJumps, BooleanSupplier announceFuelStars) {
        StringBuilder sb = new StringBuilder();
        if (announceArrival) {
            sb.append(localizedEvent("event.route.arrived", starSystem));
        }
        int remainingJump = remainingRoute.size();
        if (remainingJump > 0 && announceRemainingJumps) {
            // The toggle alone is not enough: a ship with no fuel scoop cannot use a scoopable
            // star, and telling it "refuel possible" is worse than saying nothing. See FuelScoop.
            boolean announceFuel = announceFuelStars.getAsBoolean();
            NavRouteDto nextStop = remainingRoute.getFirst();
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(localizedEvent("event.route.waypoint", nextStop.getName(), nextStop.getStarClass()));
            if (announceFuel) {
                sb.append(isFuelStarClause(nextStop.getStarClass()));
            }
            sb.append(" ").append(localizedEventPlural(remainingJump, "event.route.jumpsLeft"));
        }
        return sb.toString();
    }

    /**
     * Every state running on any faction in the system.
     *
     * <p>Both fields are read because they answer different questions: {@code FactionState} is the
     * faction's dominant state, while {@code ActiveStates} lists everything else it is also running.
     * A faction in Boom that is also in War reports War only in {@code ActiveStates}, and the war
     * materials are on offer all the same.
     */
    static List<String> factionStates(FSDJumpEvent event) {
        List<String> states = new ArrayList<>();
        if (event.getFactions() == null) return states;
        for (FSDJumpEvent.Faction faction : event.getFactions()) {
            if (faction.getFactionState() != null) {
                states.add(faction.getFactionState());
            }
            if (faction.getActiveStates() == null) continue;
            for (FSDJumpEvent.ActiveState active : faction.getActiveStates()) {
                if (active.getState() != null) {
                    states.add(active.getState());
                }
            }
        }
        return states;
    }
}
