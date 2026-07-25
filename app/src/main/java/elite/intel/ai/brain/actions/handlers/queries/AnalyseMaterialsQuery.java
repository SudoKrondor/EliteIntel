package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.queries.struct.AiDataStruct;
import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.MaterialNameDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.List;
import java.util.Set;

@RegisterQuery
public class AnalyseMaterialsQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_material_inventory";

    @Override
    public String llmDescription() {
        return "Report how much of an engineering material or commodity the commander holds; the item name is in 'key' (raw, manufactured, encoded materials, and cargo).";
    }


    @Override public String id() { return ID; }


    private static final String PARAM_KEY = "key";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "string", false,
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
            if (FuzzySearch.fuzzyMaterialSymbol(token, 8) != null
                    || FuzzySearch.fuzzyCommodityMatch(token, 3) != null) {
                return token;
            }
        }
        return null;
    }

    /**
     * Matches a cargo inventory entry against a resolved commodity. The journal's Inventory
     * {@code Name} is the non-localized game symbol, lower-cased, so matching is
     * case-insensitive. Primary match is the DB {@code symbol}; the fallbacks cover legacy
     * goods FDevIDs no longer lists (no symbol) and single-word symbols equal to the name.
     */
    private static boolean matchesCommodity(GameEvents.Inventory item, String symbol, String englishName) {
        String name = item.getName();
        if (name == null) return false;
        if (symbol != null && name.equalsIgnoreCase(symbol)) return true;
        if (name.equalsIgnoreCase(englishName)) return true;
        String localised = item.getNameLocalised();
        return localised != null && localised.equalsIgnoreCase(englishName);
    }

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        JsonElement key = params.get(PARAM_KEY);
        String query = (key != null) ? key.getAsString() : null;

        if (query == null || query.isBlank()) {
            query = extractQueryFromInput(originalUserInput);
        }
        if (query == null || query.isBlank()) {
            return process(StringUtls.localizedLlm("query.materials.specify"));
        }

        // 1. Try engineering materials first
        String materialSymbol = FuzzySearch.fuzzyMaterialSymbol(query, 8);
        if (materialSymbol != null) {
            MaterialNameDao.Material data = Database.withDao(MaterialNameDao.class, dao -> dao.findBySymbol(materialSymbol));
            if (data != null) {
                String instructions = """
                        Answer the user's question about this material in the ship's inventory.

                        Data fields:
                        - materialName: name of the material, already in the correct language. Use it verbatim.
                        - materialType: category of the material
                        - grade: rarity, 1 (very common) to 5 (very rare)
                        - amount: current units held; 0 means the commander holds none
                        - maxCap: maximum storage capacity in units

                        State the amount held and maximum capacity. Answer only what was asked.
                        """;
                MaterialDataDto dto = new MaterialDataDto(
                        FuzzySearch.localizedMaterialName(materialSymbol),
                        data.getMaterialType(),
                        data.getGrade(),
                        data.getAmount(),
                        data.getMaxCapacity());
                return process(new AiDataStruct(instructions, dto), originalUserInput);
            }
        }

        // 2. Try commodity in the cargo hold.
        // fuzzyCommodityMatch resolves the (possibly localized) spoken word to the English
        // commodity name in the DB. Cargo Inventory 'Name', however, is the non-localized game
        // symbol (e.g. "atmosphericextractors" for "Atmospheric Processors"), so we match on the
        // symbol from the commodities table rather than the display name.
        String commodityName = FuzzySearch.fuzzyCommodityMatch(query, 3);
        if (commodityName != null) {
            String symbol = FuzzySearch.commoditySymbol(commodityName);
            GameEvents.CargoEvent cargo = PlayerSession.getInstance().getShipCargo();
            if (cargo != null && cargo.getInventory() != null) {
                GameEvents.Inventory item = cargo.getInventory().stream()
                        .filter(i -> matchesCommodity(i, symbol, commodityName))
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

    record MaterialDataDto(String materialName, String materialType, int grade, int amount, int maxCap)
            implements ToYamlConvertable {
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
