package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.journal.events.ApproachBodyEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.MaterialDto;
import elite.intel.search.edsm.EdsmApiClient;
import elite.intel.search.edsm.dto.SystemBodiesDto;
import elite.intel.search.edsm.dto.data.BodyData;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.LocationUtils;

import java.util.List;
import java.util.Map;

import static elite.intel.util.StringUtls.localizedEvent;

public class ApproachBodySubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    private static double formatDouble(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Subscribe
    public void onApproachBodyEvent(ApproachBodyEvent event) {
        Thread.ofVirtual().start(() -> {
            Status status = Status.getInstance();
            double orbitalCruiseEntryAltitude = status.getStatus().getAltitude();
            StringBuilder sb = new StringBuilder();
            LocationDto location = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBodyID());

            if (location.getPlanetName() == null || location.getPlanetName().isEmpty()) {
                location.setPlanetName(event.getBody());
                location.setPlanetShortName(event.getBody());
            }

            String locationType = location.getLocationType() == null ? "" : location.getLocationType().name();
            sb.append(localizedEvent("event.approach.body.entering", locationType, location.getPlanetName()));

            String currentSystem = event.getStarSystem();

            location.setOrbitalCruiseEntryAltitude(orbitalCruiseEntryAltitude);

            playerSession.setCurrentLocationId(event.getBodyID(), event.getSystemAddress());
            playerSession.setCurrentPrimaryStarName(event.getStarSystem());

            if (playerSession.getTracking().isEnabled()) return;

            SystemBodiesDto systemBodiesDto = EdsmApiClient.searchSystemBodies(currentSystem);

            boolean weHaveOurOwnData = location.getGravity() > 0;

            if (weHaveOurOwnData) {
                useOurData(location, sb);
            } else {
                //try EDSM data
                boolean edsmHasData = systemBodiesDto.getData() != null
                        && systemBodiesDto.getData().getBodies() != null
                        && !systemBodiesDto.getData().getBodies().isEmpty();

                if (edsmHasData) {
                    List<BodyData> edsmData = systemBodiesDto.getData().getBodies();
                    //we found the planet in EDSM
                    edsmData.stream().filter(edsmDataItem ->
                            location.getPlanetName().equalsIgnoreCase(edsmDataItem.getName())).findFirst().ifPresent(bodyData ->
                            extractDataFromEdsm(bodyData, location, sb)
                    );
                } else { // no data available
                    sb.append(" ").append(localizedEvent("event.approach.body.noData", event.getBody()));
                    sb.append(" ").append(localizedEvent("event.approach.body.checkData"));
                }
            }

            locationManager.save(location);
            if (!event.isReplay() && playerSession.isRouteAnnouncementOn()) {
                String instructions = """
                            We are approaching planet/moon. Warn/Notify user with this data.
                            Temperature data is provided in C (Celsius)
                            Gravity around equal to or less than 1G is safe. Issue a gravity warning if gravity is higher than 1G.
                        """;
                EventBusManager.publish(new SensorDataEvent(sb.toString(), instructions));
            }
        }); // end virtual thread
    }


    private void useOurData(LocationDto location, StringBuilder sb) {
        double surfaceGravity = formatDouble(location.getGravity());

        if (surfaceGravity > 1000) {
            sb.append(" ").append(localizedEvent("event.approach.body.gravityAnomaly"));
            /// 9.80665 * massEM / Math.pow(radiusKm / 6371.0, 2);
            double g = LocationUtils.gravityFix(location.getMassEM(), location.getRadius());
            sb.append(" ").append(localizedEvent("event.approach.body.calculatedGravity", formatDouble(g)));
        } else {
            sb.append(" ").append(localizedEvent("event.approach.body.surfaceGravity", surfaceGravity));
            if (surfaceGravity > 1) {
                sb.append(" ").append(localizedEvent("event.approach.body.gravityWarning"));
            }
        }


        if (!"None".equalsIgnoreCase(location.getAtmosphere())) {
            sb.append(" ").append(localizedEvent("event.approach.body.atmosphere", location.getAtmosphere()));
        }
        List<MaterialDto> materials = location.getMaterials();
        if (materials != null && !materials.isEmpty()) {
            sb.append(" ").append(localizedEvent("event.approach.body.materials", materials.size()));
        }
        double surfaceTemperature = location.getSurfaceTemperature();
        double temperatureInC = surfaceTemperature - 273;
        location.setSurfaceTemperature(temperatureInC);
        sb.append(" ").append(localizedEvent("event.approach.body.temperature", (int) temperatureInC));
        if (location.isTidalLocked()) sb.append(" ").append(localizedEvent("event.approach.body.tidalLocked"));
    }


    private void extractDataFromEdsm(BodyData bodyData, LocationDto location, StringBuilder sb) {
        double gravity = formatDouble(bodyData.getGravity());
        location.setGravity(gravity);
        sb.append(" ").append(localizedEvent("event.approach.body.surfaceGravity", gravity));
        if (gravity > 1) {
            sb.append(" ").append(localizedEvent("event.approach.body.gravityWarning"));
        }
        double surfaceTemperatureKelvin = bodyData.getSurfaceTemperature();
        location.setSurfaceTemperature(surfaceTemperatureKelvin);
        sb.append(" ").append(localizedEvent("event.approach.body.temperature", (int) (surfaceTemperatureKelvin - 273)));
        if (bodyData.getAtmosphereType() != null && !bodyData.getAtmosphereType().isEmpty()) {
            sb.append(" ").append(localizedEvent("event.approach.body.atmosphere", bodyData.getAtmosphereType()));
        }
        Map<String, Double> materials = bodyData.getMaterials();
        if (materials != null && !materials.isEmpty()) {
            sb.append(" ").append(localizedEvent("event.approach.body.materials", materials.size()));
            for (Map.Entry<String, Double> material : materials.entrySet()) {
                location.addMaterial(new MaterialDto(material.getKey(), material.getValue()));
            }
        }
        location.setPlanetData(bodyData);
    }
}
