package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.QueryIds;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.search.edsm.dto.ShipyardDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;


@RegisterQuery
public class AnalyzeShipyardQueryCommand extends BaseQueryAnalyzer implements IntelQuery {

    @Override public String id() { return QueryIds.LOCAL_SHIPYARD; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //EventBusManager.publish(new AiVoxResponseEvent("Analyzing shipyard data. Stand by."));

        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        ShipyardDto shipyard = currentLocation.getShipyard();

        if (shipyard == null || shipyard.getData() == null) {
            return process(StringUtls.localizedLlm("query.shipyard.noData"));
        }

        String instructions = """
                Answer the user's question about the shipyard at the current station.
                
                Data fields:
                - shipyard.data.name: station name
                - shipyard.data.sName: star system name
                - shipyard.data.ships: list of ships available for purchase at this shipyard
                
                Rules:
                - If asked what ships are available: list names from shipyard.data.ships.
                - If asked whether a specific ship is available: check the ships list and reply yes or no.
                - If ships list is empty or null, say no ships are currently listed at this shipyard.
                """;

        return process(new AiDataStruct(instructions, new DataDto(shipyard)), originalUserInput);
    }

    private record DataDto(ToYamlConvertable shipyard) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
