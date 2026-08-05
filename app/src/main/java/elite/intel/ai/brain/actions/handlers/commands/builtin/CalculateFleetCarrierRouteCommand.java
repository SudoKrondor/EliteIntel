package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.session.Status;
import elite.intel.util.FleetCarrierRouteCalculator;

/**
 * Owns its own execution: body migrated 1:1 from the legacy CalculateFleetCarrierRouteHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class CalculateFleetCarrierRouteCommand implements IntelCommand {
    public static final String ID = "calculate_fleet_carrier_route";

    /**
     * States what it consumes and what it replaces, because the neighbouring query answers questions about the
     * route this one creates. "Analyse the carrier route" chose this command over that query - and this command
     * takes its destination from the clipboard, so a wrong pick does not merely answer the wrong question, it
     * plots a route to whatever the commander last copied.
     */
    @Override
    public String llmDescription() {
        return "PLOT A NEW multi-jump tritium route for the commander's own fleet carrier to a destination read "
                + "from the system clipboard, replacing any route already plotted. Use only when the commander "
                + "asks to create, plot, plan or re-calculate a route. Never use it to describe, analyse or "
                + "answer questions about a route that already exists.";
    }


    @Override
    public String id() {
        return ID;
    }

    /** App-side bookkeeping (no game input); executable in any location. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        return FleetCarrierRouteCalculator.calculate();
    }
}
