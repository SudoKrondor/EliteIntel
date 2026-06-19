package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.QueryIds;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.search.edsm.dto.DeathsDto;
import elite.intel.search.edsm.dto.TrafficDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

@RegisterQuery
public class AnalyzeSystemSecurityQueryCommand extends BaseQueryAnalyzer implements IntelQuery {

    @Override public String id() { return QueryIds.SYSTEM_SECURITY_ANALYSIS; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //EventBusManager.publish(new AiVoxResponseEvent("Analyzing system security. Stand by."));

        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        DeathsDto deathsDto = currentLocation.getDeathsDto();
        TrafficDto trafficDto = currentLocation.getTrafficDto();

        String instructions = """
                Answer the user's question about system security and political status.
                
                Data fields:
                - starSystem: name of the star system
                - security: security level (High, Medium, Low, Anarchy)
                - faction: controlling faction at current location
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
                - If a field is null or none, state that data is not available.
                """;
        double pscp = currentLocation.getPowerplayStateControlProgress();
        String powerPlayStateControlProgress = pscp == 0 ? "none" : String.valueOf(pscp);
        return process(
                new AiDataStruct(instructions,
                        new DataDto(
                                currentLocation.getStarName(),
                                trafficDto == null ? null : trafficDto.getData().getDiscovery(),
                                currentLocation.getSecurity(),
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
            String faction,
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
