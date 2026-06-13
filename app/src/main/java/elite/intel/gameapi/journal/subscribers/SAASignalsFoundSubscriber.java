package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.data.BioForms;
import elite.intel.gameapi.journal.events.SAASignalsFoundEvent;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.GenusDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.MaterialDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.PLANETARY_RING;
import static elite.intel.util.StringUtls.localizedEvent;

public class SAASignalsFoundSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    private static void announce(String sb) {
        Status status = Status.getInstance();
        if (status.isInMainShip() && !status.isLanded() && !status.isDocked()) {
            String instructions = """
                        Report the signals detected on this body. List each signal type briefly.
                        If biological signals are present, name each genus and state the average projected payout.
                        If this is our first discovery, include the first-discovery bonus.
                    """;
            EventBusManager.publish(new SensorDataEvent(sb, instructions));
        }
    }

    @Subscribe
    public void onSAASignalsFound(SAASignalsFoundEvent event) {
        Thread.ofVirtual().start(() -> {
            StringBuilder sb = new StringBuilder();
            LocationDto location = LocationManager.getInstance().findBySystemAddress(event.getSystemAddress(), event.getBodyID());
            LocationDto primaryStarLocation = locationManager.findPrimaryStar(playerSession.getPrimaryStarName());
            location.setPlanetName(event.getBodyName());
            location.setBodyId(event.getBodyID());
            location.setStarName(primaryStarLocation.getStarName());
            location.setX(primaryStarLocation.getX());
            location.setY(primaryStarLocation.getY());
            location.setZ(primaryStarLocation.getZ());

            location.addSaaSignals(event.getSignals());
            locationManager.save(location);

            List<SAASignalsFoundEvent.Signal> signals = event.getSignals();
            int signalsFound = signals != null ? signals.size() : 0;

            if (signalsFound > 0) {
                int liveSignals = event.getGenuses() != null ? event.getGenuses().size() : 0;
                sb.append(" ").append(localizedEvent("event.signals.found")).append(" ");
                for (SAASignalsFoundEvent.Signal signal : signals) {
                    sb.append(" ").append(localizedEvent("event.signals.type", signal.getType()));
                }

                if (liveSignals > 0) {
                    location.setBioSignals(liveSignals);
                    location.setGenus(toGenusDto(event.getGenuses(), location.isOurDiscovery(), location.getPlanetName()));
                    boolean hasBeenScanned = scanBioCompleted(event, playerSession);

                    if (!hasBeenScanned) sb.append(" ").append(localizedEvent("event.signals.exobio", liveSignals));

                    long averageProjectedPayment = 0;
                    long averageFirstDiscoveryBonus = 0;
                    for (SAASignalsFoundEvent.Genus genus : event.getGenuses()) {
                        BioForms.ProjectedPayment averagePayment = BioForms.getAverageProjectedPayment(genus.getGenusLocalised());
                        if (averagePayment != null) {
                            averageProjectedPayment = averageProjectedPayment + averagePayment.payment();
                            averageFirstDiscoveryBonus = averageFirstDiscoveryBonus + averagePayment.firstDiscoveryBonus();
                        }

                        if (!hasBeenScanned) {
                            sb.append(" ");
                            sb.append(genus.getGenusLocalised());
                            sb.append(", ");
                        }
                    }
                    if (!hasBeenScanned) {
                        sb.append(localizedEvent("event.signals.avgPayment", averageProjectedPayment));
                        if (location.isOurDiscovery()) {
                            sb.append(" ").append(localizedEvent("event.signals.firstDiscoveryBonus", averageFirstDiscoveryBonus));
                        }
                    }

                } else if (event.getBodyName().contains("Ring")) {
                    //Rings are bodies
                    LocationDto ring = new LocationDto(event.getBodyID());
                    ring.setSystemAddress(event.getSystemAddress());
                    ring.setBodyId(event.getBodyID());
                    ring.setPlanetName(event.getBodyName());
                    ring.setMaterials(toMaterials(event.getSignals()));
                    ring.setLocationType(PLANETARY_RING);

                    String parentBodyName = event.getBodyName().replaceAll(" [A-Z] Ring$", "");
                    ring.setParentBodyName(parentBodyName);
                    LocationDto parent = locationManager.getLocation(
                            playerSession.getPrimaryStarName(),
                            findParentId(
                                    parentBodyName,
                                    locationManager.findAllBySystemAddress(event.getSystemAddress()
                                    )
                            )
                    );
                    if (parent != null) parent.setHasRings(true);
                    if (event.getSignals() != null) {
                        ring.setSaaSignals(event.getSignals());
                        ring.setGeoSignals(event.getSignals().size());
                        if (parent != null) parent.setSaaSignals(event.getSignals());
                    }
                    locationManager.save(ring);
                    if (parent != null) locationManager.save(parent);
                }

                if (playerSession.isDiscoveryAnnouncementOn()) {
                    announce(sb.toString());
                }
            } else {
                if (playerSession.isDiscoveryAnnouncementOn()) {
                    announce(localizedEvent("event.signals.none"));
                }
            }

            locationManager.save(location);
        });
    }


    private long findParentId(String parentBodyName, Collection<LocationDto> allLocationsInStarSystem) {
        for (LocationDto dto : allLocationsInStarSystem) {
            if (dto.getPlanetName().equalsIgnoreCase(parentBodyName)) {
                return dto.getBodyId();
            }
        }
        return 0;
    }

    private boolean scanBioCompleted(SAASignalsFoundEvent event, PlayerSession playerSession) {
        List<BioSampleDto> bioSamples = playerSession.getBioCompletedSamples();
        for (SAASignalsFoundEvent.Genus genus : event.getGenuses()) {
            for (BioSampleDto bioSampleDto : bioSamples) {
                boolean matchingGenus = bioSampleDto.getGenus().equalsIgnoreCase(genus.getGenusLocalised());
                boolean samePlanet = bioSampleDto.getPlanetName().equalsIgnoreCase(event.getBodyName());
                if (matchingGenus && samePlanet) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<MaterialDto> toMaterials(List<SAASignalsFoundEvent.Signal> signals) {
        ArrayList<MaterialDto> materialDtos = new ArrayList<>();
        for (SAASignalsFoundEvent.Signal signal : signals) {
            materialDtos.add(new MaterialDto(signal.getType(), 100, true));
        }
        return materialDtos;
    }

    private List<GenusDto> toGenusDto(List<SAASignalsFoundEvent.Genus> organics, boolean isOurDiscovery, String planetName) {
        ArrayList<GenusDto> result = new ArrayList<>();
        for (SAASignalsFoundEvent.Genus genus : organics) {
            GenusDto dto = new GenusDto();
            dto.setSpecies(genus.getGenusLocalised());
            dto.setPlanetName(planetName);
            BioForms.ProjectedPayment projectedPayment = BioForms.getAverageProjectedPayment(genus.getGenusLocalised());
            if (projectedPayment != null && projectedPayment.payment() != null) {
                dto.setRewardInCredits(projectedPayment.payment());
                if (isOurDiscovery) {
                    dto.setBonusCreditsForFirstDiscovery(projectedPayment.firstDiscoveryBonus());
                }
            }
            result.add(dto);
        }
        return result;
    }
}
