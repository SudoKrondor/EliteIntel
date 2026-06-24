package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

public class AnalyzeFuelStatusHandler extends BaseQueryAnalyzer implements IntelQuery {

    @Override public String id() { return "analyze_fuel_status_handler"; }

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //GameEventBus.publish(new AiVoxResponseEvent("Analyzing ship's data. Stand by."));
        //TODO: Convert info in to dtos, and write logic to figure out how much fuel is used per maximum range jump.
        PlayerSession playerSession = PlayerSession.getInstance();
        Status status = Status.getInstance();
        GameEvents.StatusEvent fuelStatus =status.getStatus();
        ShipLoadOutDto loadout = playerSession.getShipLoadout();

        if (loadout == null) {
            return process(StringUtls.localizedLlm("query.noData"));
        }

        Double currentFuel = null;
        Double currentReserve = null;
        if (fuelStatus != null && fuelStatus.getFuel() != null) {
            currentFuel = fuelStatus.getFuel().getFuelMain();
            currentReserve = fuelStatus.getFuel().getFuelReservoir();
        }

        Double mainTankCapacity = loadout.getFuelCapacity() != null ? loadout.getFuelCapacity().getMainTank() : null;
        Double reserveTankCapacity = loadout.getFuelCapacity() != null ? loadout.getFuelCapacity().getReserveTank() : null;

        String instructions = """
                Answer the user's question about ship fuel status.
                
                Data fields:
                - currentFuel: current fuel in main tank in tons
                - currentReserve: current fuel in reserve tank in tons
                - mainTankCapacity: maximum main tank capacity in tons
                - reserveTankCapacity: maximum reserve tank capacity in tons
                - maxJumpRange: maximum single jump range in light years

                Rules:
                - Answer only the specific value the user asked about.
                - If a value is null, say it is not available.
                """;

        return process(new AiDataStruct(instructions, new DataDto(currentFuel, currentReserve, mainTankCapacity, reserveTankCapacity, loadout.getMaxJumpRange())), originalUserInput);
    }


    record DataDto(Double currentFuel, Double currentReserve, Double mainTankCapacity, Double reserveTankCapacity,
                   double maxJumpRange) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
