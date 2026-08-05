package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.queries.carrier.CarrierOwnership;
import elite.intel.ai.brain.actions.handlers.queries.carrier.CarrierView;
import elite.intel.ai.brain.actions.handlers.queries.struct.AiDataStruct;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

/**
 * A carrier's plotted voyage: where it is going, how far, how long, and whether it can afford the tritium. Serves
 * the commander's own fleet carrier and their squadron's alike - {@link CarrierOwnership} reads which one from the
 * utterance.
 * <p>
 * Destination, jump count, ETA-at-destination and refuel stops are all facets of one plotted route, so they are one
 * tool. Splitting them produced tools whose trigger phrases ("carrier route" / "carrier destination") the model
 * could not tell apart - and the latter collided outright with the phrase that types a destination into the game.
 */
@RegisterQuery
public class AnalyzeCarrierVoyageQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_carrier_voyage";

    /**
     * A carrier jump takes twenty minutes of cooldown plus lockdown, whatever the distance.
     */
    private static final int MINUTES_PER_JUMP = 20;

    private static final String INSTRUCTIONS = """
            Answer the commander's question about the carrier's plotted route.
            
            Data fields:
            - carrier: which carrier this is. Name it in the answer so the commander knows which one you mean.
            - finalDestination: the system the route ends at
            - route: the remaining jump stops in order (leg = 1 for the next jump, systemName,
              hasIcyRing = can refuel here, isPristine = pristine ring)
            - totalJumps: jumps remaining to the final destination
            - timeToFinalDestinationInMinutes: total travel time, pre-computed
            - travelTimeHours / travelTimeMinutes: the same travel time already split into hours and minutes
            - currentFuelSupply: tritium available now, in tons
            - fuelRequired: tritium needed to complete the full route, in tons
            - fuelBalance: currentFuelSupply minus fuelRequired. Positive is a surplus, negative is a shortfall.
            - refuelSystems: every system on the route with an icy ring, in route order
            - nearestRefuelSystem: the first of those
            - jumpsToNearestRefuelStop: jumps from here to nearestRefuelSystem
            
            Rules:
            - Answer only what was asked. Do not volunteer unrequested data. No data dumps.
            - For destination questions: use finalDestination.
            - For jump-count or ETA questions: use totalJumps and the pre-computed travel time. Do not
              recalculate them.
            - For fuel questions: use fuelRequired and fuelBalance.
            - For refuel-stop questions: use refuelSystems, nearestRefuelSystem and jumpsToNearestRefuelStop.
              When those three fields are absent there is nowhere to refuel on this route: say so plainly.
              Never name a system that is not in refuelSystems.
            """;

    /**
     * Says "already plotted" and owns the word "analyse" outright: this tool and
     * {@code calculate_fleet_carrier_route} both answer to "carrier route", and a model that reads only the noun
     * picks the command - which plots a new route from the clipboard instead of answering the question asked.
     */
    @Override
    public String llmDescription() {
        return "ANALYSE AND REPORT the route a carrier ALREADY has plotted: its final destination, the jumps "
                + "remaining, travel time, tritium needed, the fuel balance, and which stops along the way can "
                + "refuel it. Use for every question about an existing route - including \"analyse the carrier "
                + "route\" and \"where can we refuel\" - and never to plot a new one. Covers the commander's own "
                + "fleet carrier by default, and the squadron carrier when the commander says \"squadron\".";
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        CarrierView carrier = CarrierView.forUtterance(originalUserInput);
        SortedMap<Integer, CarrierJump> route = carrier.route();
        if (route.isEmpty()) {
            return process(StringUtls.localizedResponse("query.carrier.noRoutePlotted"));
        }

        CarrierDataDto data = carrier.data();
        DataDto voyage = summarise(
                carrier.ownership(),
                carrier.finalDestination(),
                route,
                data.getFuelLevel() + data.getFuelReserve(),
                carrier.totalFuelRequired());
        return process(new AiDataStruct(INSTRUCTIONS, voyage), originalUserInput);
    }

    /**
     * Summarises a non-empty, leg-ordered route into the data the model answers from.
     * <p>
     * Everything counted here is relative to where the carrier is NOW, because the legs already travelled are
     * deleted from the store as the carrier arrives. The surviving map keys are the original leg numbers, so they
     * must never be read as a position: a carrier three jumps into a five-jump route has keys 4 and 5 left, and
     * reporting "refuel in 4 jumps" when only 2 remain is the bug this method exists to not have.
     */
    static DataDto summarise(CarrierOwnership ownership,
                             String finalDestination,
                             SortedMap<Integer, CarrierJump> route,
                             int currentFuelSupply,
                             int fuelRequired) {
        int totalJumps = route.size();
        int totalMinutes = totalJumps * MINUTES_PER_JUMP;

        List<RouteStop> stops = new ArrayList<>();
        List<String> refuelSystems = new ArrayList<>();
        // Null rather than zero so the serializer (NON_EMPTY) drops this alongside the other two refuel fields,
        // instead of leaving a bare "jumpsToNearestRefuelStop: 0" for the model to misread as "refuel here".
        Integer jumpsToNearestRefuelStop = null;
        String nearestRefuelSystem = null;

        int jumpsFromHere = 0;
        for (CarrierJump jump : route.values()) {
            jumpsFromHere++;
            stops.add(new RouteStop(jumpsFromHere, jump.getSystemName(), jump.getHasIcyRing(), jump.isPristine()));
            if (!jump.getHasIcyRing()) {
                continue;
            }
            refuelSystems.add(jump.getSystemName());
            if (nearestRefuelSystem == null) {
                nearestRefuelSystem = jump.getSystemName();
                jumpsToNearestRefuelStop = jumpsFromHere;
            }
        }

        return new DataDto(
                ownership.label(),
                finalDestination,
                stops,
                totalJumps,
                totalMinutes,
                totalMinutes / 60,
                totalMinutes % 60,
                currentFuelSupply,
                fuelRequired,
                currentFuelSupply - fuelRequired,
                refuelSystems,
                nearestRefuelSystem,
                jumpsToNearestRefuelStop);
    }

    record DataDto(
            String carrier,
            String finalDestination,
            List<RouteStop> route,
            int totalJumps,
            int timeToFinalDestinationInMinutes,
            int travelTimeHours,
            int travelTimeMinutes,
            int currentFuelSupply,
            int fuelRequired,
            int fuelBalance,
            List<String> refuelSystems,
            String nearestRefuelSystem,
            Integer jumpsToNearestRefuelStop
    ) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    record RouteStop(int leg, String systemName, boolean hasIcyRing, boolean isPristine)
            implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
