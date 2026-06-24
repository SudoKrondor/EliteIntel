package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.gamestate.dtos.NavRouteDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static elite.intel.util.NavigationUtils.calculateGalacticDistance;

@RegisterQuery
public class AnalyzeRouterQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_ship_route_remaining_jumps";

    @Override public String llmDescription() { return "Report the number of jumps remaining on the current route."; }


    @Override public String id() { return ID; }

    private static final Logger log = LogManager.getLogger(AnalyzeRouterQueryCommand.class);
    private final PlayerSession playerSession = PlayerSession.getInstance();

    private final ShipRouteManager shipRoute = ShipRouteManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    
    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //GameEventBus.publish(new AiVoxResponseEvent("Analyzing route telemetry. Stand by."));
        Collection<NavRouteDto> orderedRoute = shipRoute.getOrderedRoute();
        LocationDto here = locationManager.findByLocationData(playerSession.getLocationData());

        // Create NavRouteDto for current location
        NavRouteDto currentLocation = new NavRouteDto();
        currentLocation.setLeg(-1); // Distinct from route legs
        currentLocation.setName(here.getStarName());
        currentLocation.setX(here.getX());
        currentLocation.setY(here.getY());
        currentLocation.setZ(here.getZ());

        String distanceData = getDistanceDataForAnnouncement(currentLocation, orderedRoute);
        
        String instructions = """
                Answer the user's question about the current plotted ship route.
                
                Data fields:
                - data: ordered list of route stops (legNumber, starName, starClass, isScoopable, remainingJumps)
                - jumpsRemaining: total jumps remaining to the final destination
                - distanceToFinal: pre-computed straight-line and total route distance string
                
                Rules:
                - Answer only the specific question asked. Do not give unsolicited full-route summaries.
                - Only provide the full route list if the user says "full route", "all waypoints", "list all legs", or similar.
                - Never invent or calculate values not present in the data.
                
                How to answer common questions:
                - "next waypoint / next jump / next star": use data[0] - report starName, starClass, isScoopable, and remainingJumps.
                - "how many jumps left / remaining": use jumpsRemaining directly.
                - "distance / how far": report distanceToFinal verbatim.
                - "is it scoopable": use data[0].isScoopable for next stop, or scan data for exceptions if asking about full route.
                - "what class is the next star": use data[0].starClass.
                - "traffic / casualties / security": say no such information is available in the route data.
                - "full route": list all legs as - Leg N: starName, starClass, scoopable yes/no.
                
                Start directly with the answer. No preamble.
                """;
        return process(new AiDataStruct(instructions, new DataDto(toStopsData(orderedRoute), orderedRoute.size(), distanceData)), originalUserInput);
    }

    private List<StopData> toStopsData(Collection<NavRouteDto> orderedRoute) {
        LinkedList<StopData> result = new LinkedList<>();
        for(NavRouteDto leg : orderedRoute){
            result.add(new StopData(leg.getLeg(), leg.getRemainingJumps(), leg.getStarClass(), leg.getName(), leg.isScoopable()));
        }
        return result;
    }


    private static String getDistanceDataForAnnouncement(NavRouteDto currentSystem, Collection<NavRouteDto> route) {
        if (route.isEmpty()) {
            return "No route data available.";
        }

        double totalDistance = 0.0;
        NavRouteDto previous = currentSystem;

        // Sum distances for all legs, starting from current system
        for (NavRouteDto next : route) {
            double legDistance = calculateGalacticDistance(
                    previous.getX(), previous.getY(), previous.getZ(),
                    next.getX(), next.getY(), next.getZ()
            );
            totalDistance += legDistance;
            previous = next;
        }

        // Straight-line distance from current to a final system
        NavRouteDto last = route.stream().reduce((first, second) -> second).orElse(null);
        double straightLineDistance = calculateGalacticDistance(
                currentSystem.getX(), currentSystem.getY(), currentSystem.getZ(),
                last.getX(), last.getY(), last.getZ()
        );

        StringBuilder sb = new StringBuilder();
        sb.append("The straight-line distance is ");
        sb.append(String.format("%.2f", straightLineDistance));
        sb.append(" light years, ");
        sb.append(" The total route distance");
        sb.append(" is ");
        sb.append(String.format("%.2f", totalDistance));
        sb.append(" light years, accounting for "+route.size()+" actual jumps.");

        return sb.toString();
    }

    record StopData(int legNumber, int remainingJumps, String starClass, String starName, boolean isScoopable) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
    
    record DataDto(List<StopData> data, int jumpsRemaining, String distanceToFinal) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
