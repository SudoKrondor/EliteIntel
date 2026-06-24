package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.db.managers.FleetCarrierManager;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.search.spansh.carrierroute.CarrierJump;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.Map;


@RegisterQuery
public class AnalyzeFleetCarrierFinalDestinationQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_fleet_carrier_final_destination";

    @Override public String llmDescription() { return "Report the fleet carrier's final destination."; }


    @Override public String id() { return ID; }


    private final FleetCarrierRouteManager manager = FleetCarrierRouteManager.getInstance();
    private final FleetCarrierManager carrierManager = FleetCarrierManager.getInstance();

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {


        Map<Integer, CarrierJump> carrierRoute = manager.getFleetCarrierRoute();
        Integer totalFuelRequired = manager.getTotalFuelRequired();
        int totalMinutes = carrierRoute.size() * 20;
        int travelHours = totalMinutes / 60;
        int travelMins = totalMinutes % 60;
        String travelTime = travelHours > 0
                ? travelHours + " hours " + travelMins + " minutes"
                : travelMins + " minutes";
        Integer numJumps = carrierRoute.size();
        String finalDestination = manager.getFinalDestination();

        int fuelReserve = carrierManager.get().getFuelReserve();
        int fuelLevel = carrierManager.get().getFuelLevel();
        int fuelBalance = (fuelLevel + fuelReserve) - totalFuelRequired;
        String iceRingStops = calculateIceRingStops(carrierRoute);

        String instructions = """
                Answer the user's question about the fleet carrier's planned route.
                
                Data fields:
                - finalDestination: the destination system
                - travelTime: total travel time, pre-formatted as "X hours Y minutes" - speak it as-is
                - totalFuelRequired: tritium needed for the full journey in tons
                - numJumps: number of legs/stops/jumps to destination
                - refuelLocations: star systems with ice rings where the carrier can refuel
                - fuelBalance: positive = tritium surplus in tons, negative = shortfall in tons
                
                Rules:
                - Answer only what was asked. Do not volunteer unrequested data.
                - For destination questions: use finalDestination.
                - For ETA or travel time questions: use travelTime.
                - For jump count questions: use numJumps.
                - For fuel questions: use totalFuelRequired and fuelBalance.
                - For refuel stop questions: use refuelLocations.
                - Only mention fuelBalance if the user asked about fuel or range.
                """;

        return process(new AiDataStruct(instructions, new DataDto(finalDestination, totalFuelRequired, fuelBalance, travelTime, numJumps, iceRingStops)), originalUserInput);
    }

    private String calculateIceRingStops(Map<Integer, CarrierJump> carrierRoute) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ice ring stop at ");
        carrierRoute.forEach((k, v) -> {
            if (v.getHasIcyRing()) {
                sb.append(v.getSystemName()).append(",");
            }
        });
        return sb.toString().substring(0, sb.length() - 1);
    }

    record DataDto(String finalDestination, int fuelRequired, int fuelBalance, String travelTime, int numJumps,
                   String refuelLocations) implements ToYamlConvertable {

        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }


}
