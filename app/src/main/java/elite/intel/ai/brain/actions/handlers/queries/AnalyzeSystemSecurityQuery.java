package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.queries.struct.AiDataStruct;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.search.edsm.dto.DeathsDto;
import elite.intel.gameapi.search.edsm.dto.TrafficDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

@RegisterQuery
public class AnalyzeSystemSecurityQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_system_security";

    @Override
    public String llmDescription() {
        return "Report the current system's security level, controlling faction, allegiance and power-play control.";
    }


    @Override public String id() { return ID; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //GameEventBus.publish(new AiVoxResponseEvent("Analyzing system security. Stand by."));

        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        DeathsDto deathsDto = currentLocation.getDeathsDto();
        TrafficDto trafficDto = currentLocation.getTrafficDto();

        String instructions = """
                Answer the user's question about system security and political status.
                
                Data fields:
                - starSystem: name of the star system
                - security: security level (High, Medium, Low, Anarchy)
                - controllingFaction: the faction that controls the STAR SYSTEM. Null when we have not been told.
                - stationFaction: the faction running the station we are docked at, which is often a different
                  one, and is null at a fleet carrier because a carrier has no faction
                - government: government type
                - economy: secondary economy type
                - population: system population
                - controllingPower: major power controlling this system (if any)
                - powerPlayState: current powerplay state for this system
                - powerplayStateControlProgress: control progress (none = not under powerplay)
                - powerplayStateReinforcement: powerplay reinforcement points
                - powerplayStateUndermining: powerplay undermining points
                - deathsDto: historical death statistics from EDSM
                - trafficDto: historical traffic statistics from EDSM
                - discoveredBy: EDSM discovery attribution data
                
                Rules:
                - A system may have a controlling faction and government even without major powerplay presence.
                - Answer only what the user asked.
                - If a field is null or none, state that data is not available. Never name a faction that is
                  not in the data, and never offer stationFaction as the system's controlling faction.
                """;
        double pscp = currentLocation.getPowerplayStateControlProgress();
        String powerPlayStateControlProgress = pscp == 0 ? "none" : String.valueOf(pscp);
        return process(
                new AiDataStruct(instructions,
                        new DataDto(
                                currentLocation.getStarName(),
                                trafficDto == null ? null : trafficDto.getData().getDiscovery(),
                                currentLocation.getSecurity(),
                                currentLocation.getSystemFaction(),
                                currentLocation.getStationFaction(),
                                currentLocation.getPopulation(),
                                currentLocation.getSecondEconomy(),
                                currentLocation.getGovernment(),
                                currentLocation.getPowerplayState(),
                                currentLocation.getControllingPower(),
                                powerPlayStateControlProgress,
                                currentLocation.getPowerplayStateReinforcement(),
                                currentLocation.getPowerplayStateUndermining(),
                                deathsDto == null ? null : deathsDto.getData().getDeaths(),
                                trafficDto == null ? null : trafficDto.getData().getTraffic()
                        )
                ),
                originalUserInput
        );
    }

    record DataDto(
            String starSystem,
            ToYamlConvertable discoveredBy,
            String security,
            String controllingFaction,
            String stationFaction,
            Long population,
            String economy,
            String government,
            String powerPlayState,
            String controllingPower,
            String powerplayStateControlProgress,
            Integer powerplayStateReinforcement,
            Integer powerplayStateUndermining,
            ToYamlConvertable deathsDto,
            ToYamlConvertable trafficDto
    ) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
