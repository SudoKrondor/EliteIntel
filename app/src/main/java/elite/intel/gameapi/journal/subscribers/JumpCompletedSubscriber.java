package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.dao.DestinationReminderDao;
import elite.intel.db.dao.RouteMonetisationDao.MonetisationTransaction;
import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.db.managers.*;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.FireGroups;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.gamestate.dtos.NavRouteDto;
import elite.intel.gameapi.journal.events.FSDJumpEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.MaterialDto;
import elite.intel.search.edsm.EdsmApiClient;
import elite.intel.search.edsm.dto.DeathsDto;
import elite.intel.search.edsm.dto.SystemBodiesDto;
import elite.intel.search.edsm.dto.TrafficDto;
import elite.intel.search.edsm.dto.data.BodyData;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static elite.intel.ai.hands.Bindings.GameCommand.*;
import static elite.intel.gameapi.FireGroups.fireGroupInSettings;
import static elite.intel.util.GravityCalculator.calculateSurfaceGravity;
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
    private final Status status = Status.getInstance();

    @Subscribe
    public void onFSDJumpEvent(FSDJumpEvent event) {
        Thread.ofVirtual().start(() -> {
            neutronStarRouteManager.removeLeg(event.getSystemAddress());

            SystemBodiesDto systemBodiesDto = EdsmApiClient.searchSystemBodies(event.getStarSystem());
            processEdsmData(systemBodiesDto, event.getSystemAddress(), event.getStarPos(), event.getStarSystem());

            boolean isSellerSystem = monetizeRouteManager.isSeller(event.getStarSystem());
            boolean isBuyerSystem = monetizeRouteManager.isBuyer(event.getStarSystem());
            MonetisationTransaction station = monetizeRouteManager.getTransaction();

            LocationDto primaryStar = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBodyId());
            primaryStar.setBodyId(event.getBodyId());
            primaryStar.setSystemAddress(event.getSystemAddress());
            primaryStar.setStationGovernment(event.getSystemGovernmentLocalised());
            primaryStar.setAllegiance(event.getSystemAllegiance());
            primaryStar.setSecurity(event.getSystemSecurityLocalised());
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
            if (reminder != null && reminder.getStarSystem().equals(event.getStarSystem())) {
                reminderText = reminder.getReminder() == null ? "" : reminder.getReminder();
            }

            if (finalDestination != null && finalDestination.equalsIgnoreCase(event.getStarSystem())) {
                shipRoute.clearRoute();
                if (reminderText != null && !reminderText.isBlank()) {
                    GameEventBus.publish(new AiVoxResponseEvent(localizedEvent("event.route.reminder", reminderText)));
                } else {
                    sb.append(localizedEvent("event.route.arrivedFinal", finalDestination));
                }
                TrafficDto trafficDto = EdsmApiClient.searchTraffic(finalDestination);
                DeathsDto deathsDto = EdsmApiClient.searchDeaths(finalDestination);
                primaryStar.setTrafficDto(trafficDto);
                primaryStar.setDeathsDto(deathsDto);

            } else if (roueSet) {
                if (reminderText != null && !reminderText.isBlank() && reminderText.toLowerCase().contains(event.getStarSystem().toLowerCase(Locale.ROOT))) {
                    GameEventBus.publish(new AiVoxResponseEvent(localizedEvent("event.route.reminder", reminderText)));
                }

                sb.append(localizedEvent("event.route.arrived", event.getStarSystem()));
                List<NavRouteDto> route = shipRoute.getOrderedRoute();
                int remainingJump = route.size();
                if (remainingJump > 0) {
                    route.stream().findFirst().ifPresent(
                            nextStop -> sb
                                    .append(" ")
                                    .append(localizedEvent("event.route.waypoint", nextStop.getName(), nextStop.getStarClass()))
                                    .append(isFuelStarClause(nextStop.getStarClass()))
                    );
                    sb.append(" ").append(localizedEventPlural(remainingJump, "event.route.jumpsLeft"));
                }
            }

            locationManager.save(primaryStar);

            if (!event.isReplay()) {
                if (playerSession.isRouteAnnouncementOn()) {
                    //GameEventBus.publish(new RouteAnnouncementEvent(sb.toString()));
                    GameEventBus.publish(new SensorDataEvent(sb.toString(), "Announce this route information."));
                }
                if (isSellerSystem && station != null) {
                    GameEventBus.publish(new SensorDataEvent("Head to " + station.getSourceStationName() + " buy " + station.getSourceCommodity(), "Remind the commander of their active trade route: state the station name and the commodity to buy."));
                }
                if (isBuyerSystem && station != null) {
                    GameEventBus.publish(new SensorDataEvent("Head to " + station.getDestinationStationName() + " sell " + station.getDestinationCommodity(), "Remind the commander of their active trade route: state the station name and the commodity to sell."));
                }
            }

            ShipSettingsDao.ShipSettings shipSettings = shipSettingsManager.getSettings(playerSession.getShipLoadout().getShipId());
            if (!event.isReplay() && shipSettings.isHonkOnJump()) {
                boolean isInCombatMode = !status.isAnalysisMode();
                /// Change to analysis mode
                if (isInCombatMode) {
                    GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_ACTIVATE_ANALYSIS_MODE.getGameBinding())));
                }

                /// Switch fire-group
                FireGroups.cycleToGroup(fireGroupInSettings(shipSettings));

                /// Scan
                int honkTrigger = shipSettings.getHonkTrigger(); /// 1 primary, 2 secondary
                if (honkTrigger == 1) {
                    GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingHold(BINDING_PRIMARY_FIRE.getGameBinding(), 6000)));
                } else {
                    GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingHold(BINDING_SECONDARY_FIRE.getGameBinding(), 6000)));
                }

                /// change back to combat mode - if the user was in combat mode
                if (isInCombatMode) {
                    GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_ACTIVATE_COMBAT_MODE.getGameBinding())));
                }
            }

        }); // end virtual thread
    }


    private void processEdsmData(SystemBodiesDto systemBodiesDto, long systemAddress, double[] starPos, String starSystem) {
        if (systemBodiesDto == null) return;
        if (systemBodiesDto.getData() == null) return;
        List<BodyData> bodies = systemBodiesDto.getData().getBodies();
        if (bodies == null || bodies.isEmpty()) return;

        for (BodyData data : bodies) {
            LocationDto stellarObject = locationManager.findBySystemAddress(systemAddress, data.getBodyId());
            stellarObject.setSystemAddress(systemAddress);
            stellarObject.setAtmosphere(data.getAtmosphereType());
            stellarObject.setBodyId(data.getBodyId());
            stellarObject.setHasRings(data.getRings() != null && !data.getRings().isEmpty());
            stellarObject.setTerraformable("Terraformable".equalsIgnoreCase(data.getTerraformingState()));
            stellarObject.setLandable(data.isLandable());
            stellarObject.setMaterials(toMaterials(data.getMaterials()));
            stellarObject.setPlanetName(data.getName());
            stellarObject.setMassEM(data.getEarthMasses());
            stellarObject.setRadius(data.getRadius());
            Double surfaceGravity = calculateSurfaceGravity(data.getEarthMasses(), data.getRadius());
            stellarObject.setGravity(surfaceGravity == null ? 0 : surfaceGravity);
            stellarObject.setSurfaceTemperature(data.getSurfaceTemperature()); // Keep Kelvin
            stellarObject.setTidalLocked(data.isRotationalPeriodTidallyLocked());
            stellarObject.setLocationType(LocationDto.determineType(data.getType(), data.getDistanceToArrival() == 0));
            if (starPos != null) {
                stellarObject.setX(starPos[0]);
                stellarObject.setY(starPos[1]);
                stellarObject.setZ(starPos[2]);
            }
            if (data.getDiscovery() != null) {
                stellarObject.setOurDiscovery(data.getDiscovery().getCommander() == null);
                stellarObject.setDiscoveredBy(data.getDiscovery().getCommander());
                stellarObject.setDiscoveredOn(data.getDiscovery().getDate());
            }
            stellarObject.setOrbitalPeriod(data.getOrbitalPeriod());
            stellarObject.setAxialTilt(data.getAxialTilt());
            stellarObject.setRotationPeriod(data.getRotationalPeriod());
            stellarObject.setVolcanism(data.getVolcanismType());
            stellarObject.setPlanetClass(data.getSpectralClass());
            stellarObject.setStarName(starSystem);
            locationManager.save(stellarObject);
        }
    }


    private List<MaterialDto> toMaterials(Map<String, Double> materials) {
        if (materials == null) return new ArrayList<>();
        ArrayList<MaterialDto> materialDtos = new ArrayList<>();
        for (Map.Entry<String, Double> entry : materials.entrySet()) {
            materialDtos.add(new MaterialDto(entry.getKey(), entry.getValue()));
        }
        return materialDtos;
    }
}
