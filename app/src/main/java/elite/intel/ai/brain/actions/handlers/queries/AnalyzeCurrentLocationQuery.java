package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.queries.struct.AiDataStruct;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.search.edsm.EdsmApiClient;
import elite.intel.gameapi.search.edsm.dto.DeathsDto;
import elite.intel.gameapi.search.edsm.dto.TrafficDto;
import elite.intel.gameapi.search.edsm.dto.data.DeathsStats;
import elite.intel.gameapi.search.edsm.dto.data.TrafficStats;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.SolarDayCalculator;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

@RegisterQuery
public class AnalyzeCurrentLocationQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_current_location";

    @Override
    public String llmDescription() {
        return "Report the commander's current location and its details: system, body, docked/landed status, security, controlling faction and powers, gravity, atmosphere, day length, planet radius and temperature.";
    }


    @Override public String id() { return ID; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        Status status = Status.getInstance();

        LocationDto location = locationManager.findByLocationData(playerSession.getLocationData());
        DeathsDto deathsDto = EdsmApiClient.searchDeaths(location.getStarName());
        TrafficDto trafficDto = EdsmApiClient.searchTraffic(location.getStarName());

        // The station is looked up separately: it is not the body this location record describes.
        LocationDto dockedAt = locationManager.findCurrentStation();
        String station = "none";
        if (dockedAt.getStationName() != null) {
            station = "Docked at " + dockedAt.getStationName() + " " + dockedAt.getStationType();
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
                - controllingFaction: the faction that controls the STAR SYSTEM (null when not known)
                - stationFaction: the faction running the station we are docked at, which is often a different
                  one, and is null at a fleet carrier because a carrier has no faction
                - localPowers: powers active in this system
                - deathsData: EDSM historical death statistics for this system
                - trafficData: EDSM historical traffic statistics for this system
                - planetRadius: radius of current planet in kilometers
                - surfaceTemperatureInCelsius: surface temperature of current planet (absent when unknown)
                - dayLength: pre-formatted solar day length for current planet
                
                Rules:
                - If asked about docking or flight state: use flightStatus directly.
                - If asked about temperature: state surfaceTemperatureInCelsius in degrees Celsius.
                - If asked about day length: use dayLength directly. Do not recalculate.
                - If any requested data is missing, say you do not have enough information. Never name a faction
                  that is not in the data, and never offer stationFaction as the system's controlling faction.
                """;

        double surfaceTemperatureInKelvin = Math.round(location.getSurfaceTemperature() * 100.0) / 100.0;
        // Below absolute zero means we never recorded a temperature for this body; omit it rather than speak nonsense.
        Double surfaceTemperatureInCelsius = surfaceTemperatureInKelvin > 0 ? surfaceTemperatureInKelvin - 273 : null;
        return process(
                new AiDataStruct(
                        instructions,
                        new DataDto(
                                flightStatus,
                                playerSession.getPrimaryStarName(),
                                location.getPlanetShortName(),
                                location.getSecurity(),
                                location.getSystemFaction(),
                                location.getStationFaction(),
                                location.getPowers() == null ? null : location.getPowers().toArray(String[]::new),
                                deathsDto.getData() == null ? null : deathsDto.getData().getDeaths(),
                                trafficDto.getData() == null ? null : trafficDto.getData().getTraffic(),
                                (int) Math.round(location.getRadius() / 1000.0), // stored in metres, prompt asks for km
                                surfaceTemperatureInCelsius,
                                computeSolarDayLength(location)
                        )
                ),
                originalUserInput
        );
    }

    private String computeSolarDayLength(LocationDto location) {
        return formatSecondsToHoursMinutes(SolarDayCalculator.solarDaySeconds(location));
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
            String stationFaction,
            String[] localPowers,
            DeathsStats deathsData,
            TrafficStats trafficData,
            double planetRadius,
            Double surfaceTemperatureInCelsius,
            String dayLength
    ) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
