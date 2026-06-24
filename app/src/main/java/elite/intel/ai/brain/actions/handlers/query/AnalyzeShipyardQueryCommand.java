package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.search.edsm.dto.ShipyardDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;


@RegisterQuery
public class AnalyzeShipyardQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_local_shipyard";

    @Override public String llmDescription() { return "Report the ships available at the current station's shipyard."; }


    @Override public String id() { return ID; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //GameEventBus.publish(new AiVoxResponseEvent("Analyzing shipyard data. Stand by."));

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
