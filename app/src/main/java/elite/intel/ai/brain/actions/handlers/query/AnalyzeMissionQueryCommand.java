package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.QueryIds;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.MissionType;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@RegisterQuery
public class AnalyzeMissionQueryCommand extends BaseQueryAnalyzer implements IntelQuery {

    @Override public String id() { return QueryIds.ANALYZE_MISSIONS; }


    private final MissionManager missionManager = MissionManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        MissionType[] availableMissionTypes = missionManager.getAvailableMissionTypes();
        if (availableMissionTypes.length == 0) return process(StringUtls.localizedLlm("query.missions.none"));

        Map<MissionType, Collection<MissionDto>> missions = Arrays.stream(availableMissionTypes).collect(
                Collectors.toMap(
                        missionType -> missionType,
                        missionType -> missionManager.getMissions(missionType).values(),
                        (a, b) -> b
                )
        );

        LocationDto playerLocation = locationManager.findByLocationData(playerSession.getLocationData());
        String instructions = """
                Answer the user's question about active missions.
                
                Data fields:
                - currentStarSystem: the player's current star system
                - missions: map of mission type to list of missions. Per mission:
                  - faction: issuing faction
                  - missionDescription: what the mission requires
                  - missionType: category of mission
                  - reward: credit reward
                  - destinationSystem: system where the mission must be completed
                  - destinationStation: specific station destination (if applicable)
                  - commodity / commodityName / count: cargo details for delivery missions
                  - killCount / target / missionTargetFaction: combat mission details
                  - expiry: when the mission expires
                  - isWing: whether this is a wing mission
                
                Rules:
                - If the user does not mention a specific system: list all active missions with faction, description, and destination.
                - If the user refers to the current system ("here", "this system", "current location"): show only missions where destinationSystem or origin matches currentStarSystem.
                - If the user names a specific system: show only missions relevant to that system.
                - Do not filter by current system unless the user explicitly asks about it.
                """;
        return process(new AiDataStruct(instructions, new DataDto(missions, playerLocation.getStarName())), originalUserInput);
    }

    record DataDto(Map<MissionType, Collection<MissionDto>> missions, String currentStarSystem) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}