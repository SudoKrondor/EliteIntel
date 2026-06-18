package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.QueryIds;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.db.managers.SquadronCarrierManager;
import elite.intel.db.managers.SquadronCarrierRouteManager;
import elite.intel.search.spansh.carrierroute.CarrierJump;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.Map;

@RegisterQuery
public class AnalyzeSquadronCarrierFinalDestinationQueryCommand extends BaseQueryAnalyzer implements IntelQuery {

    @Override public String id() { return QueryIds.SQUADRON_CARRIER_ROUTE_FINAL_DESTINATION; }


    private final SquadronCarrierRouteManager routeManager = SquadronCarrierRouteManager.getInstance();
    private final SquadronCarrierManager carrierManager = SquadronCarrierManager.getInstance();

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        Map<Integer, CarrierJump> carrierRoute = routeManager.getSquadronCarrierRoute();
        Integer totalFuelRequired = routeManager.getTotalFuelRequired() == null ? 0 : routeManager.getTotalFuelRequired();
        int totalMinutes = carrierRoute.size() * 20;
        int travelHours = totalMinutes / 60;
        int travelMins = totalMinutes % 60;
        String travelTime = travelHours > 0
                ? travelHours + " hours " + travelMins + " minutes"
                : travelMins + " minutes";
        Integer numJumps = carrierRoute.size();
        String finalDestination = routeManager.getFinalDestination();

        int fuelReserve = carrierManager.get().getFuelReserve();
        int fuelLevel = carrierManager.get().getFuelLevel();
        int fuelBalance = (fuelLevel + fuelReserve) - totalFuelRequired;
        String iceRingStops = calculateIceRingStops(carrierRoute);

        String instructions = """
                Answer the user's question about the squadron carrier's planned route.
                
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
