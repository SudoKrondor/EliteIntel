package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.FSSBodySignalsEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RegisterQuery
public class AnalyzeGeologyInStarSystemQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_geo_signals";


    @Override public String id() { return ID; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.geology.searching")));
        Map<String, Integer> planetsWithGeoSignals = planetsWithGeoSignals();
        String instructions = """
                Report geological signals detected in this star system.
                
                Data fields:
                - planetsWithGeoSignals: map of planet short name to geological signal count
                
                Rules:
                - List each planet and its signal count.
                - If the map is empty, say no geological signals are on record.
                """;
        return process(new AiDataStruct(instructions, new DataDto(planetsWithGeoSignals)), originalUserInput);
    }

    private Map<String, Integer> planetsWithGeoSignals() {
        Map<String, Integer> result = new HashMap<>();
        Collection<LocationDto> locations = locationManager.findAllBySystemAddress(playerSession.getLocationData().getSystemAddress());
        for (LocationDto location : locations) {
            if (location.getGeoSignals() > 0) {
                result.put(location.getPlanetShortName(), location.getGeoSignals());
            }
            if(location.getFssSignals() == null) continue;
            List<FSSBodySignalsEvent.Signal> fssSignals = location.getFssSignals();
            for(FSSBodySignalsEvent.Signal signal :fssSignals){
                if(signal.getTypeLocalised().toLowerCase().contains("geo")){
                    result.put(location.getPlanetShortName(), signal.getCount());
                }
            }
        }
        return result;
    }

    record DataDto(Map<String, Integer> planetsWithGeoSignals) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

}
