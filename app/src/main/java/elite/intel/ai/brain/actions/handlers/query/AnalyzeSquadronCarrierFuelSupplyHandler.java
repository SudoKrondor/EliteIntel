package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.db.managers.SquadronCarrierManager;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

public class AnalyzeSquadronCarrierFuelSupplyHandler extends BaseQueryAnalyzer implements IntelQuery {

    @Override public String id() { return "analyze_squadron_carrier_fuel_supply_handler"; }

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        SquadronCarrierManager squadronCarrierManager = SquadronCarrierManager.getInstance();
        int fuelSupply = squadronCarrierManager.get().getFuelLevel();
        int fuelReserve = squadronCarrierManager.get().getFuelReserve();
        int maxRangeOnCurrentSupply = 500 * (fuelSupply / 100);
        int maxRangeUsingReserve = 500 * ((fuelSupply + fuelReserve) / 100);
        String instructions = """
                Report squadron carrier fuel status.
                
                Data fields:
                - fuelSupply: current tritium in supply depot in tons
                - fuelReserve: tritium held in reserve in tons
                - maxRangeOnCurrentSupply: estimated range using fuelSupply only in light years
                - maxRangeUsingReserve: estimated range using fuelSupply and fuelReserve combined in light years
                
                State only the values the user asked about.
                """;
        return process(new AiDataStruct(instructions, new DataDto(fuelSupply, fuelReserve, maxRangeOnCurrentSupply, maxRangeUsingReserve)), originalUserInput);
    }

    record DataDto(int fuelSupply, int fuelReserve, int maxRangeOnCurrentSupply,
                   int maxRangeUsingReserve) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
