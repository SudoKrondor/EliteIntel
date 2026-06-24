package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.MaterialDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.ArrayList;
import java.util.List;

@RegisterQuery
public class AnalyzeMaterialsOnPlanetQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_planet_materials";

    @Override public String llmDescription() { return "Report the raw materials available on the targeted planet."; }


    @Override public String id() { return ID; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //GameEventBus.publish(new AiVoxResponseEvent("Analyzing geological data. Stand by."));

        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        if (currentLocation.getBodyId() < 0) return process(StringUtls.localizedLlm("query.noLocationData"));

        List<MaterialDto> materials = currentLocation.getMaterials();
        String instructions = """
                Answer the user's question about raw materials on this planet.
                
                Data fields:
                - materials: list of materials on this planet
                  - material: material name
                  - rarity: percentage presence (lower = rarer, higher = more common)
                
                Rules:
                - Use only the materials list. Never invent or guess values.
                - "X rarest materials"       → sort by rarity ASC, return first X names
                - "X most common materials"  → sort by rarity DESC, return first X names
                - "most and least common"    → return top 3 most common and top 3 rarest
                - "is X present / does it have X" → check by name (case-insensitive), answer yes or no
                - "percentage of X"          → return the rarity value to two decimal places, or say not available
                - "list all materials"       → return all material names
                - Output only the requested answer. No explanations.
                """;

        if (materials.isEmpty()) {
            return process(StringUtls.localizedLlm("query.planet.noMaterials"));
        } else {
            return process(new AiDataStruct(instructions, new DataDto(toMaterialData(materials))), originalUserInput);
        }
    }

    private List<MaterialData> toMaterialData(List<MaterialDto> materials) {
        ArrayList<MaterialData> data = new ArrayList<>();
        for (MaterialDto m : materials) {
            data.add(new MaterialData(m.getMaterialName(), Math.round(m.getMaterialPercentage() * 100.0) / 100.0));
        }
        return data;
    }

    record MaterialData(String material, double rarity) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    record DataDto(List<MaterialData> materials) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
