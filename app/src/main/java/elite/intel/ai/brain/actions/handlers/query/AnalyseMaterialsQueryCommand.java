package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.MaterialsDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.List;
import java.util.Set;

import static elite.intel.util.StringUtls.capitalizeWords;

@RegisterQuery
public class AnalyseMaterialsQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_material_inventory";

    @Override public String llmDescription() { return "Report the commander's engineering materials and cargo commodities."; }


    @Override public String id() { return ID; }


    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                "key", "string", false,
                "The engineering material or cargo commodity to report on, e.g. iron, sulphur, tritium.",
                List.of("iron", "tritium"),
                "Extract the material/commodity name verbatim in lower case; do not translate. Omit it for a general request.");
        key.validate();
        return List.of(key);
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    // Common question words that are never the item being queried
    private static final Set<String> SKIP_TOKENS = Set.of(
            "how", "much", "many", "any", "have", "has", "our", "the", "are",
            "did", "what", "does", "show", "list", "check", "get", "some",
            "got", "give", "tell", "you", "can", "could", "left", "still", "current"
    );

    /**
     * When the LLM fails to extract the key param, scan the original input
     * word-by-word and return the first token that fuzzy-matches a material or
     * commodity name. Returns null if nothing survives the threshold.
     */
    private String extractQueryFromInput(String input) {
        if (input == null || input.isBlank()) return null;
        for (String token : input.toLowerCase().replaceAll("[^\\p{L}\\s]", "").split("\\s+")) {
            if (token.length() < 3 || SKIP_TOKENS.contains(token)) continue;
            if (FuzzySearch.fuzzyInventorySearch(token, 8) != null
                    || FuzzySearch.fuzzyCommodityMatch(token, 3) != null) {
                return token;
            }
        }
        return null;
    }

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        JsonElement key = params.get("key");
        String query = (key != null) ? key.getAsString() : null;

        if (query == null || query.isBlank()) {
            query = extractQueryFromInput(originalUserInput);
        }
        if (query == null || query.isBlank()) {
            return process(StringUtls.localizedLlm("query.materials.specify"));
        }

        // 1. Try engineering materials first
        String materialName = capitalizeWords(FuzzySearch.fuzzyInventorySearch(query, 8));
        if (materialName != null) {
            MaterialsDao.Material data = Database.withDao(MaterialsDao.class, dao -> dao.findByExactName(materialName));
            if (data != null) {
                String instructions = """
                        Answer the user's question about this material in the ship's inventory.
                        
                        Data fields:
                        - materialName: name of the material
                        - materialType: category of the material
                        - amount: current units held
                        - maxCap: maximum storage capacity in units
                        
                        State the amount held and maximum capacity. Answer only what was asked.
                        """;
                return process(new AiDataStruct(instructions, new MaterialDataDto(data)), originalUserInput);
            }
        }

        // 2. Try commodity in the cargo hold
        String commodityName = capitalizeWords(FuzzySearch.fuzzyCommodityMatch(query, 3));
        if (commodityName != null) {
            GameEvents.CargoEvent cargo = PlayerSession.getInstance().getShipCargo();
            if (cargo != null && cargo.getInventory() != null) {
                GameEvents.Inventory item = cargo.getInventory().stream()
                        .filter(i -> i.getName() != null && i.getName().equalsIgnoreCase(commodityName))
                        .findFirst().orElse(null);
                if (item != null) {
                    String instructions = """
                            Answer the user's question about this commodity in the cargo hold.
                            
                            Data fields:
                            - commodityName: name of the commodity
                            - count: units held (tonnes)
                            - stolen: stolen units

                            State the amount held. Answer only what was asked.
                            """;
                    return process(new AiDataStruct(instructions, new CargoItemDto(commodityName, item.getCount(), item.getStolen())), originalUserInput);
                } else {
                    return process(StringUtls.localizedLlm("query.materials.notInCargo", commodityName));
                }
            }
        }

        // 3. Not found in either
        return process(StringUtls.localizedLlm("query.materials.notFound", query));
    }

    record MaterialDataDto(MaterialsDao.Material materials) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    record CargoItemDto(String commodityName, double count, double stolen) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
