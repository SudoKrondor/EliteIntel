package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RegisterQuery
public class AnalyzeDistanceToStellarObjectQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_distance_to_body";


    @Override public String id() { return ID; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //EventBusManager.publish(new AiVoxResponseEvent("Analyzing local star system data. Stand by."));

        Collection<LocationDto> locations = locationManager.findAllBySystemAddress(
                playerSession.getLocationData().getSystemAddress()
        );
        List<StellarObject> objects = new ArrayList<>();
        for (LocationDto loc : locations) {
            String name = loc.getPlanetShortName() != null && !loc.getPlanetShortName().isBlank()
                    ? loc.getPlanetShortName()
                    : loc.getStarName();
            if (loc.getStationName() != null && !loc.getStationName().isBlank()) {
                objects.add(new StellarObject(loc.getStationName(), loc.getStationType(), loc.getDistance()));
            } else if (name != null && !name.isBlank()) {
                objects.add(new StellarObject(name, loc.getBodyType(), loc.getDistance()));
            }
        }

        String instructions = """
                Return the distance in light seconds to the stellar object or station the user is asking about.
                
                Data fields:
                - objects: list of bodies in this star system (name, type, distanceLightSeconds)
                
                Rules:
                - Match the user's spoken name against the name field (allow partial or phonetic matches).
                - State the name and distanceLightSeconds of the matched object.
                - If no match found, say the object was not found in sensor data.
                """;

        return process(new AiDataStruct(instructions, new DataDto(objects)), originalUserInput);
    }

    record DataDto(List<StellarObject> objects) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    record StellarObject(String name, String type, double distanceLightSeconds) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
