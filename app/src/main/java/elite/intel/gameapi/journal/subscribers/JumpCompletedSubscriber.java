package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.CompanionRuntime;
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
import elite.intel.gameapi.journal.events.dto.MaterialDto;
import elite.intel.gameapi.search.edsm.EdsmApiClient;
import elite.intel.gameapi.search.edsm.dto.DeathsDto;
import elite.intel.gameapi.search.edsm.dto.SystemBodiesDto;
import elite.intel.gameapi.search.edsm.dto.TrafficDto;
import elite.intel.gameapi.search.edsm.dto.data.BodyData;
import elite.intel.gameapi.search.edsm.dto.data.ParentBody;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private final GlobalSettingsManager globalSettings = GlobalSettingsManager.getInstance();
    private final HighGradeEmissionsAdvisor hgeAdvisor = HighGradeEmissionsAdvisor.getInstance();

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

            if (finalDestination != null && finalDestination.equalsIgnoreCase(event.getStarSystem())) {
                shipRoute.clearRoute();
                if (reminderText != null && !reminderText.isBlank()) {
                    EventNarrator.say(localizedEvent("event.route.reminder", reminderText));
                } else {
                    sb.append(localizedEvent("event.route.arrivedFinal", finalDestination));
                }
                TrafficDto trafficDto = EdsmApiClient.searchTraffic(finalDestination);
                DeathsDto deathsDto = EdsmApiClient.searchDeaths(finalDestination);
                primaryStar.setTrafficDto(trafficDto);
                primaryStar.setDeathsDto(deathsDto);

            } else if (roueSet) {
                if (reminderText != null && !reminderText.isBlank() && reminderText.toLowerCase().contains(event.getStarSystem().toLowerCase(Locale.ROOT))) {
                    EventNarrator.say(localizedEvent("event.route.reminder", reminderText));
                }

                sb.append(localizedEvent("event.route.arrived", event.getStarSystem()));
                List<NavRouteDto> route = shipRoute.getOrderedRoute();
                int remainingJump = route.size();
                if (remainingJump > 0 && globalSettings.getAnnounceRemainingJumps()) {
                    // The toggle alone is not enough: a ship with no fuel scoop cannot use a scoopable
                    // star, and telling it "refuel possible" is worse than saying nothing. See FuelScoop.
                    boolean announceFuel = FuelScoop.announceFuelStars();
                    route.stream().findFirst().ifPresent(nextStop -> {
                        sb.append(" ").append(localizedEvent("event.route.waypoint", nextStop.getName(), nextStop.getStarClass()));
                        if (announceFuel) {
                            sb.append(isFuelStarClause(nextStop.getStarClass()));
                        }
                    });
                    sb.append(" ").append(localizedEventPlural(remainingJump, "event.route.jumpsLeft"));
                }
            }

            locationManager.save(primaryStar);

            if (!event.isReplay()) {
                if (playerSession.isRouteAnnouncementOn()) {
                    CompanionRuntime.narrator().narrate(sb.toString(), "Announce this route information.");
                }
                if (isSellerSystem && station != null) {
                    CompanionRuntime.narrator().narrate(
                            "Head to " + station.getSourceStationName() + " buy " + station.getSourceCommodity(),
                            "Remind the commander of their active trade route: state the station name and the commodity to buy.");
                }
                if (isBuyerSystem && station != null) {
                    CompanionRuntime.narrator().narrate(
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
            LocationDto.LocationType bodyType = classifyEdsmBody(data);
            if (bodyType != null) stellarObject.setLocationType(bodyType);
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
            applyBodyClass(stellarObject, data);
            stellarObject.setStarName(starSystem);
            locationManager.save(stellarObject);
        }
    }


    /**
     * Classifies an EDSM body, or returns null when EDSM says nothing useful about it.
     *
     * <p>WHY: {@link LocationDto#determineType} matches descriptive words ("body", "giant", "world",
     * "star"), which EDSM carries in {@code subType}. It used to be passed {@code type}, which is only
     * ever "Star" or "Planet", so every planet fell through to null. Since a null was then stored, each
     * jump wiped the type of every non-star body in the system, including bodies the commander's own
     * scan had classified correctly. Callers must skip a null rather than persist it.
     *
     * <p>EDSM has no distinct moon type, so a body whose parent is a planet is a moon. That is the same
     * rule {@link elite.intel.gameapi.journal.ScanBodyClassifier} applies to journal scans, and it keeps
     * this path from demoting a known moon to a planet.
     */
    static LocationDto.LocationType classifyEdsmBody(BodyData data) {
        String subType = data.getSubType();
        if (subType == null || subType.isBlank()) return null;

        LocationDto.LocationType type = LocationDto.determineType(subType, data.getDistanceToArrival() == 0);
        if (type != LocationDto.LocationType.PLANET) return type;
        return orbitsAPlanet(data) ? LocationDto.LocationType.MOON : LocationDto.LocationType.PLANET;
    }

    private static boolean orbitsAPlanet(BodyData data) {
        List<ParentBody> parents = data.getParents();
        if (parents == null) return false;
        for (ParentBody parent : parents) {
            if (parent.getStar() != null) return false;
            if (parent.getPlanet() != null) return true;
        }
        return false;
    }

    /**
     * Records an EDSM body's class in the field that matches what it actually is.
     *
     * <p>WHY: EDSM reports a star's class in {@code spectralClass} ("M5") and a planet's in
     * {@code subType} ("High metal content world"); {@code spectralClass} is absent for planets. This
     * used to write {@code spectralClass} into {@code planetClass}, so every EDSM star was stored as a
     * planet whose type was a spectral code, and its star class was lost. Downstream that reads as a
     * planet: BiomeAnalyzer treats "no star class but a planet class" as a planet.
     */
    static void applyBodyClass(LocationDto stellarObject, BodyData data) {
        if ("Star".equalsIgnoreCase(data.getType())) {
            stellarObject.setStarClass(data.getSpectralClass());
        } else {
            stellarObject.setPlanetClass(data.getSubType());
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
