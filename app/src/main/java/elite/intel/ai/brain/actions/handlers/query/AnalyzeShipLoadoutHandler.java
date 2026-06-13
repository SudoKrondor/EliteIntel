package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.gameapi.journal.events.dto.shiploadout.EngineeringDto;
import elite.intel.gameapi.journal.events.dto.shiploadout.ModuleDto;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.*;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

public class AnalyzeShipLoadoutHandler extends BaseQueryAnalyzer implements QueryHandler {

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        PlayerSession playerSession = PlayerSession.getInstance();
        ShipLoadOutDto shipLoadout = playerSession.getShipLoadout();
        if (shipLoadout == null) return process(StringUtls.localizedLlm("query.noData"));

        Map<String, Object> facts = ShipFactsExtractor.extractFacts(shipLoadout);

        Map<String, Float> damagedModules = new HashMap<>();
        boolean hasEngineeredModules = false;
        List<ModuleDto> modules = shipLoadout.getModules() != null ? shipLoadout.getModules() : java.util.Collections.emptyList();
        List<String> installedModuleNames = new ArrayList<>();
        for (ModuleDto module : modules) {
            if (module.getHealthPercentage() < 100.0f) {
                damagedModules.put(module.getSlot(), module.getHealthPercentage());
            }
            if (module.getEngineering() != null) {
                hasEngineeredModules = true;
            }
            if (module.getItem() != null && !ShipFactsExtractor.isCosmeticSlot(module.getSlot())) {
                installedModuleNames.add(module.getItem());
            }
        }

        // Pre-compute module presence so the LLM cannot hallucinate yes/no answers
        String modulePresence = computeModulePresence(originalUserInput, installedModuleNames);

        String instructions = """
                Answer the user's question about ship loadout, health, or capabilities.

                modulePresence contains pre-computed Java search results - these are facts, not suggestions.
                For any yes/no module question, read modulePresence and report it exactly. Do not reason further.
                
                Data fields:
                - modulePresence: pre-computed search result for what the user asked about. AUTHORITATIVE.
                - installedModules: flat list of every module item name fitted on this ship.
                - data: map of ship modules by slot (moduleName, moduleHealthPercentage, engineering)
                  - shipModel: the ship's manufacturer model name (e.g. Mandalay, Anaconda). NOT the classification.
                  - maxJumpRange: jump range in light years
                  - cargoCapacity: cargo in tonnes
                - damagedModules: pre-computed map of slots with health below 100 percent (slot -> healthPercentage)
                - hasEngineeredModules: true if any module has engineering modifications

                IMPORTANT: shipModel is the hull type, not the role classification.
                Derive classification by evaluating the installed modules against the priority rules below.

                Ship classification - evaluate in this exact priority order, stop at first match:

                1. PASSENGER: any moduleName contains "Passengercabin" → Passenger. Stop.

                2. MINING: moduleName contains "Refinery" AND at least one hardpoint moduleName contains
                   "Mining" → Mining. Stop. (Cargo racks on a mining ship store ore, not trade goods.)

                3. DISCOVERY: moduleName contains "Detailedsurfacescanner" AND fuel scoop present
                   AND maxJumpRange >= 65 → Discovery.
                   Discovery ships may carry weapons for deep-space self-defence - weapons do NOT disqualify.
                   Mining lasers on a discovery ship are for surface material sampling, not mining classification.
                   Stop.
                
                4. COMBAT: weapons present in hardpoints (non-mining, non-point-defence hardpoints)
                   AND no cargo racks (no moduleName contains "Cargorack") → Combat. Stop.
                
                5. TRADE: moduleName contains "Cargorack" and cargo racks dominate the internal slots
                   → Trade. Defensive weapons are acceptable on a trade ship. Stop.
                
                6. HOPPER: no cargo, no DSS, no refinery, no passenger cabins, no dominant weapons.
                   Small light ship focused on range and speed. Stop.
                
                Jump range tiers: high = 65+ ly, mid = 30-64 ly, low = below 30 ly.
                
                Response patterns - apply the FIRST match only:
                1. If the question names a specific module (e.g. fuel scoop, refinery, shield): answer YES or NO first.
                   Then add the module name and class only if fitted.
                2. General loadout / classification question (no specific module asked): summarise key modules.
                3. Damage question: use damagedModules. If empty, say no damage detected.
                """;

        return process(new AiDataStruct(instructions, new DataDto(modulePresence, facts, installedModuleNames, damagedModules, hasEngineeredModules)), originalUserInput);
    }

    record DataDto(String modulePresence, Map<String, Object> data, List<String> installedModules,
                   Map<String, Float> damagedModules, boolean hasEngineeredModules) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    // Maps common spoken words to the substring found in raw module item names
    private static final Map<String, String> MODULE_KEYWORDS = ofEntries(
            entry("fuel scoop", "fuelscoop"),
            entry("refinery", "refinery"),
            entry("shield", "shieldgenerator"),
            entry("dss", "detailedsurfacescanner"),
            entry("detailed surface scanner", "detailedsurfacescanner"),
            entry("cargo rack", "cargorack"),
            entry("hull reinforcement", "hullreinforcement"),
            entry("frame shift drive", "hyperdrive"),
            entry("fsd", "hyperdrive"),
            entry("thrusters", "engine"),
            entry("life support", "lifesupport"),
            entry("power plant", "powerplant"),
            entry("power distributor", "powerdistributor"),
            entry("sensors", "sensors")
    );

    private static String computeModulePresence(String userInput, List<String> installedModules) {
        if (userInput == null || userInput.isBlank()) return "no query";
        String inputLower = userInput.toLowerCase();
        for (java.util.Map.Entry<String, String> entry : MODULE_KEYWORDS.entrySet()) {
            if (inputLower.contains(entry.getKey())) {
                String keyword = entry.getValue();
                Optional<String> match = installedModules.stream()
                        .filter(m -> m.toLowerCase().contains(keyword))
                        .findFirst();
                return match.map(s -> entry.getKey() +
                        ": INSTALLED (" + StringUtls.toReadableModuleName(s) + ")").orElseGet(
                        () -> entry.getKey() + ": NOT INSTALLED"
                );
            }
        }
        return "no specific module detected in query";
    }

    public static class ShipFactsExtractor {
        public static boolean isCosmeticSlot(String slot) {
            if (slot == null) return false;
            String s = slot.toLowerCase();
            return s.startsWith("decal")
                    || s.startsWith("shipid")
                    || s.startsWith("shipname")
                    || s.startsWith("shipkit")
                    || s.startsWith("paintjob")
                    || s.startsWith("enginecolour")
                    || s.startsWith("weaponcolour")
                    || s.startsWith("vesselvoice")
                    || s.startsWith("shipcockpit")
                    || s.equals("fueltank");
        }

        public static Map<String, Object> extractFacts(ShipLoadOutDto loadout) {
            Map<String, Object> facts = new HashMap<>();
            if (loadout == null) {
                return facts;
            }

            facts.put("shipMake", loadout.getShipMake());
            facts.put("shipName", loadout.getShipName());
            facts.put("shipIdent", loadout.getShipIdent());
            facts.put("maxJumpRange", loadout.getMaxJumpRange());
            facts.put("cargoCapacity", loadout.getCargoCapacity());
            facts.put("unladenMass", loadout.getUnladenMass());

            if (loadout.getModules() != null) {
                for (ModuleDto module : loadout.getModules()) {
                    if (isCosmeticSlot(module.getSlot())) continue;
                    StringBuilder sb = new StringBuilder();
                    EngineeringDto engineering = module.getEngineering();
                    if (engineering != null) {
                        String bluPrintName = engineering.getBlueprintName();
                        sb.append("Engineering: ").append(bluPrintName).append(". ");
                        sb.append(" Modifiers: ");
                        engineering.getModifiers().forEach(data -> {
                            sb.append(data.getLabel()).append(", ");
                        });
                    }
                    facts.put(module.getSlot(), new ShipModule(module.getItem(), module.getHealthPercentage(), sb.toString()));
                }
            }
            return facts;
        }
    }

    record ShipModule(String moduleName, double moduleHealthPercentage, String engineering) implements ToYamlConvertable {

        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
