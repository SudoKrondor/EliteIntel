package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.db.managers.FleetCarrierManager;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

public class AnalyzeFleetCarrierFuelSupplyHandler extends BaseQueryAnalyzer implements IntelQuery {

    @Override public String id() { return "analyze_fleet_carrier_fuel_supply_handler"; }

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        FleetCarrierManager fleetCarrierManager = FleetCarrierManager.getInstance();
        int fuelSupply = fleetCarrierManager.get().getFuelLevel();
        int fuelReserve = fleetCarrierManager.get().getFuelReserve();
        int maxRangeOnCurrentSupply = 500 * (fuelSupply / 100);
        int maxRangeUsingReserve = 500 * ((fuelSupply + fuelReserve) / 100);
        String instructions = """
                Report fleet carrier fuel status.
                
                Data fields:
                - fuelSupply: current tritium in supply depot in tons
                - fuelReserve: tritium held in reserve in tons
                - maxRangeOnCurrentSupply: estimated range using fuelSupply only in light years
                - maxRangeUsingReserve: estimated range using fuelSupply and fuelReserve combined in light years
                
                State only the values the user asked about.
                """;
        return process(new AiDataStruct(instructions, new DataDto(fuelSupply, fuelReserve, maxRangeOnCurrentSupply, maxRangeUsingReserve)), originalUserInput);
    }

    record DataDto(int fuelSupply, int fuelReserve, int maxRangeOnCurrentSupply, int maxRangeUsingReserve) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
