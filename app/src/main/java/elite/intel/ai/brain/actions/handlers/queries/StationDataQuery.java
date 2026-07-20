package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.queries.struct.AiDataStruct;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.List;


@RegisterQuery
public class StationDataQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_station_details";

    @Override
    public String llmDescription() {
        return "Report the services available at the current/docked station, or whether a specific service is offered here.";
    }


    @Override public String id() { return ID; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {

        LocationDto location = locationManager.findByLocationData(playerSession.getLocationData());

        String instructions = """
                Answer the user's question about the current station.
                
                Data fields:
                - stationServices: list of services available at this station
                
                Rules:
                - If asked what services are available: list all items from stationServices.
                - If asked whether a specific service is available: check the list and reply yes or no.
                - If the list is empty or null, say no service data is available.
                """;

        return process(new AiDataStruct(instructions, new DataDto(location.getStationServices())), originalUserInput);
    }

    record DataDto(List<String> stationServices) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
