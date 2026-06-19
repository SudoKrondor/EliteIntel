package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.search.edsm.EdsmApiClient;
import elite.intel.search.edsm.dto.DeathsDto;
import elite.intel.search.edsm.dto.TrafficDto;
import elite.intel.search.edsm.dto.data.DeathsStats;
import elite.intel.search.edsm.dto.data.TrafficStats;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

@RegisterQuery
public class AnalyzeCurrentLocationQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_current_location";


    @Override public String id() { return ID; }


    public static final double DAY = 86400.0;
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        Status status = Status.getInstance();

        LocationDto location = locationManager.findByLocationData(playerSession.getLocationData());
        DeathsDto deathsDto = EdsmApiClient.searchDeaths(location.getStarName());
        TrafficDto trafficDto = EdsmApiClient.searchTraffic(location.getStarName());

        String station = "none";
        if (status.isDocked() && location.getStationName() != null || location.getStationName() != null) {
            station = "Docked at " + location.getStationName() + " " + location.getStationType();
        }

        String flightStatus;
        if (status.isDocked()) {
            flightStatus = station;
        } else if (status.isLanded()) {
            flightStatus = "Landed on surface";
        } else {
            flightStatus = "In flight";
        }

        String instructions = """
                Answer the user's questions about current location. Answer each question individually using only the provided data.
                
                Data fields:
                - flightStatus: current state (docked/landed/in flight)
                - starSystemName: current star system
                - planetName: current planet or body (if applicable)
                - securityLevel: system security level
                - controllingFaction: faction controlling current location
                - localPowers: powers active in this system
                - deathsData: EDSM historical death statistics for this system
                - trafficData: EDSM historical traffic statistics for this system
                - planetRadius: radius of current planet in kilometers
                - surfaceTemperatureInCelsius: surface temperature of current planet
                - dayLength: pre-formatted solar day length for current planet
                
                Rules:
                - If asked about docking or flight state: use flightStatus directly.
                - If asked about temperature: state surfaceTemperatureInCelsius in degrees Celsius.
                - If asked about day length: use dayLength directly. Do not recalculate.
                - If any requested data is missing, say you do not have enough information.
                """;

        double surfaceTemperatureInKelvin = Math.round(location.getSurfaceTemperature() * 100.0) / 100.0;
        return process(
                new AiDataStruct(
                        instructions,
                        new DataDto(
                                flightStatus,
                                playerSession.getPrimaryStarName(),
                                location.getPlanetShortName(),
                                location.getSecurity(),
                                location.getStationFaction(),
                                location.getPowers() == null ? null : location.getPowers().toArray(String[]::new),
                                deathsDto.getData() == null ? null : deathsDto.getData().getDeaths(),
                                trafficDto.getData() == null ? null : trafficDto.getData().getTraffic(),
                                (int) location.getRadius(),
                                (surfaceTemperatureInKelvin - 273),
                                computeSolarDayLength(location)
                        )
                ),
                originalUserInput
        );
    }

    private String computeSolarDayLength(LocationDto location) {
        // Moons tidally locked to a planet: their solar day equals the parent planet's orbital period around the star,
        // not the moon's own rotation/orbital period around the planet.
        if (location.isTidalLocked()
                && LocationDto.LocationType.MOON.equals(location.getLocationType())
                && location.getParentBodyId() > 0) {
            LocationDto parentPlanet = locationManager.getLocation(location.getStarName(), location.getParentBodyId());
            double parentOrbitalPeriod = parentPlanet.getOrbitalPeriod();
            if (parentOrbitalPeriod > 0) {
                return formatSecondsToHoursMinutes(parentOrbitalPeriod);
            }
        }
        return getFormattedSolarDayLength(location.getRotationPeriod(), location.getOrbitalPeriod(), location.isTidalLocked());
    }

    private String getFormattedSolarDayLength(double rotationPeriodSeconds, double orbitalPeriodSeconds, boolean isTidallyLocked) {

        if (isTidallyLocked) {
            // For tidal lock: solar day = sidereal day (rotation period)
            double siderealAbs = Math.abs(rotationPeriodSeconds);
            return formatSecondsToHoursMinutes(siderealAbs);
        }

        if (orbitalPeriodSeconds <= 0) {
            // No orbit data → fallback to sidereal
            return formatSecondsToHoursMinutes(Math.abs(rotationPeriodSeconds));
        }

        double siderealAbs = Math.abs(rotationPeriodSeconds);
        if (siderealAbs < 60) {
            return "Unknown";
        }

        double siderealDays = siderealAbs / DAY;
        double orbitalDays = orbitalPeriodSeconds / DAY;

        double relativeSpeed;
        if (rotationPeriodSeconds < 0) {
            // retrograde: apparent day is shorter
            relativeSpeed = 1.0 / siderealDays + 1.0 / orbitalDays;
        } else {
            // prograde
            double diff = 1.0 / orbitalDays - 1.0 / siderealDays;
            relativeSpeed = Math.abs(diff);  // avoid negative
        }

        if (relativeSpeed < 1e-9) {
            // synchronous / near-lock
            return formatSecondsToHoursMinutes(siderealAbs);
        }

        double solarSeconds = DAY / relativeSpeed;

        // Safety cap: prevent absurd values (e.g. orbital period bug)
        if (solarSeconds > 1e10 || solarSeconds < 60) {
            return formatSecondsToHoursMinutes(siderealAbs);
        }

        return formatSecondsToHoursMinutes(solarSeconds);
    }

    // Helper – keeps code clean
    private String formatSecondsToHoursMinutes(double seconds) {
        if (seconds <= 0) return "Unknown";

        long totalSec = Math.round(seconds);
        long hours = totalSec / 3600;
        long minutes = (totalSec % 3600) / 60;

        String hourPart = hours + (hours == 1 ? " hour" : " hours");
        String minutePart = minutes + (minutes == 1 ? " minute" : " minutes");
        return hourPart + " and " + minutePart;
    }

    record DataDto(
            String flightStatus,
            String starSystemName,
            String planetName,
            String securityLevel,
            String controllingFaction,
            String[] localPowers,
            DeathsStats deathsData,
            TrafficStats trafficData,
            double planetRadius,
            double surfaceTemperatureInCelsius,
            String dayLength
    ) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
