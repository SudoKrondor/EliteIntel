package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.QueryIds;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.db.managers.SquadronCarrierManager;
import elite.intel.db.managers.SquadronCarrierRouteManager;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.search.spansh.carrierroute.CarrierJump;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RegisterQuery
public class AnalyzeSquadronCarrierRouteQueryCommand extends BaseQueryAnalyzer implements IntelQuery {

    @Override public String id() { return QueryIds.SQUADRON_CARRIER_ROUTE_ANALYSIS; }


    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        PlayerSession playerSession = PlayerSession.getInstance();
        SquadronCarrierManager squadronCarrierManager = SquadronCarrierManager.getInstance();
        SquadronCarrierRouteManager squadronCarrierRouteManager = SquadronCarrierRouteManager.getInstance();
        Map<Integer, CarrierJump> carrierRoute = squadronCarrierRouteManager.getSquadronCarrierRoute();
        CarrierDataDto carrierData = playerSession.getSquadronCarrierData();
        Integer fuelRequired = squadronCarrierRouteManager.getTotalFuelRequired();
        int fuelReserve = squadronCarrierManager.get().getFuelReserve();

        if (carrierData == null || carrierRoute == null) {
            return process(StringUtls.localizedLlm("query.noData"));
        }

        int fuelSupply = carrierData.getFuelLevel() + fuelReserve;
        int timeToArrivalInMinutes = carrierRoute.size() * 20;
        int totalJumps = carrierRoute.size();
        int jumpsToNearestRefuelStop = 0;
        String nearestRefuelSystem = "none";
        for (Integer key : new TreeMap<>(carrierRoute).keySet()) {
            CarrierJump jump = carrierRoute.get(key);
            if (jump.getHasIcyRing()) {
                jumpsToNearestRefuelStop = key;
                nearestRefuelSystem = jump.getSystemName();
                break;
            }
        }
        int fuelDeficit = (fuelRequired != null && fuelRequired > fuelSupply) ? fuelRequired - fuelSupply : 0;

        List<RouteStop> routeStops = new ArrayList<>();
        for (CarrierJump jump : new TreeMap<>(carrierRoute).values()) {
            routeStops.add(new RouteStop(jump.getLeg(), jump.getSystemName(), jump.getHasIcyRing(), jump.isPristine()));
        }

        String instructions = """
                Answer the user's question about the squadron carrier route.
                
                Data fields:
                - route: ordered jump stops (leg = jump number, systemName, hasIcyRing = can refuel here, isPristine = pristine ring)
                - totalJumps: total jumps to final destination
                - timeToFinalDestinationInMinutes: total travel time (twenty minutes per jump, pre-computed)
                - currentFuelSupply: tritium available in tons
                - fuelRequired: tritium needed to complete the full route in tons
                - fuelDeficit: additional tritium needed (zero means fuel is sufficient)
                - jumpsToNearestRefuelStop: jumps to the nearest icy ring system (zero if none on route)
                - nearestRefuelSystem: name of the nearest icy ring system
                
                Rules:
                - For total jumps or ETA: use totalJumps and timeToFinalDestinationInMinutes directly. Do not recalculate.
                - For refuel stop questions: use jumpsToNearestRefuelStop and nearestRefuelSystem.
                - For fuel sufficiency: use fuelDeficit (zero = sufficient, positive = shortage in tons).
                - For route waypoints: reference route by systemName.
                - Answer only what was asked. No data dumps.
                """;
        return process(new AiDataStruct(instructions, new DataDto(routeStops, fuelSupply, fuelRequired, fuelDeficit, totalJumps, timeToArrivalInMinutes, jumpsToNearestRefuelStop, nearestRefuelSystem)), originalUserInput);
    }

    private record DataDto(
            List<RouteStop> route,
            int currentFuelSupply,
            Integer fuelRequired,
            int fuelDeficit,
            int totalJumps,
            int timeToFinalDestinationInMinutes,
            int jumpsToNearestRefuelStop,
            String nearestRefuelSystem
    ) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    private record RouteStop(int leg, String systemName, boolean hasIcyRing,
                             boolean isPristine) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
