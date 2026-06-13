package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.search.edsm.dto.OutfittingDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

public class AnalyzeLocalOutfittingHandler extends BaseQueryAnalyzer implements QueryHandler {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //EventBusManager.publish(new AiVoxResponseEvent("Analyzing outfitting data. Stand by."));
        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        OutfittingDto outfitting = currentLocation.getOutfitting();
        if (outfitting == null || outfitting.getData() == null) {
            return process(StringUtls.localizedLlm("query.outfitting.noData"));
        }

        String instructions = """
                Answer the user's question about outfitting modules available at the current station.

                Data fields:
                - outfitting.data.name: station name
                - outfitting.data.sName: star system name
                - outfitting.data.outfitting: list of modules available for purchase at this station
                
                Rules:
                - If asked whether a specific module is available: search the outfitting list by name and reply yes or no.
                - If asked what modules are available: list items from outfitting.data.outfitting.
                - If the outfitting list is empty or null, say no modules are currently listed at this station.
                - Answer only what was asked.
                """;
        return process(new AiDataStruct(instructions, new DataDto(outfitting)), originalUserInput);
    }

    private record DataDto(OutfittingDto outfitting) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
