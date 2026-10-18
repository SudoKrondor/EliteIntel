package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.gameapi.FuelScoop;
import elite.intel.gameapi.data.FsdTarget;
import elite.intel.gameapi.gamestate.dtos.NavRouteDto;
import elite.intel.gameapi.inputs.PreFtlChecks;
import elite.intel.gameapi.inputs.UiNavCommon;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_JUMP_TO_HYPERSPACE;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_TARGET_NEXT_ROUTE_SYSTEM;

/**
 * Self-describing "jump to hyperspace" command.
 * Owns its own execution: body migrated 1:1 from the legacy JumpToHyperspaceHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class JumpToHyperspaceCommand implements IntelCommand {
    public static final String ID = "jump_to_hyperspace";

    @Override
    public String llmDescription() {
        return "Engage the frame shift drive to jump to the next system on the plotted route (hyperspace jump).";
    }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    /** A hyperspace jump needs open space: not while docked or landed. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() && !status.isDocked() && !status.isLanded();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_TARGET_NEXT_ROUTE_SYSTEM.getGameBinding())));
        UiNavCommon.close();
        GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.delay(150)));

        JumpTarget target = jumpTarget(ShipRouteManager.getInstance().getOrderedRoute(), playerSession.getFsdTarget());
        if (target != null) {
            String message;
            // Both halves matter: the commander asked to hear about fuel, and this ship can actually
            // scoop it. Without a scoop "refuel possible" names a supply they cannot reach. See FuelScoop.
            if (FuelScoop.announceFuelStars()) {
                message = StringUtls.localizedResponse("handler.fsd.jumping", target.name(), target.starClass(), target.fuelStatus());
            } else {
                message = StringUtls.localizedResponse("handler.fsd.jumpingNoFuel", target.name(), target.starClass());
            }
            VegaRuntime.narrator().filler(message, false);
        }

        Status status = Status.getInstance();

        if (status.isFsdCharging()) return null;

        if (status.isFsdMassLocked()) {
            return StringUtls.localizedResponse("handler.supercruise.massLocked");
        } else if (status.isFsdCooldown()) {
            return StringUtls.localizedResponse("handler.supercruise.cooldown");
        } else if (status.isInMainShip()) {
            PreFtlChecks.preJumpCheck(status, StringUtls.localizedResponse("handler.supercruise.preparingFtl"));
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_JUMP_TO_HYPERSPACE.getGameBinding())));
        } else {
            return StringUtls.localizedResponse("handler.supercruise.notInShip");
        }
        return null;
    }

    /**
     * What the jump is about to reach, as it is announced: the plotted route's next leg, or the stored FSD target
     * when no route is plotted. Null when neither names anything.
     * <p>
     * WHY the route first: the key press above selects the next route system, so that leg is the jump. The stored
     * target is only as fresh as the last {@code FSDTarget} the app heard, and the one the game wrote while the app
     * was closed never reached it - after a restart mid-route the announcement named a star two jumps back. It is
     * also saved on a background thread behind three EDSM lookups, so even live it can trail the key press.
     */
    static JumpTarget jumpTarget(List<NavRouteDto> route, FsdTarget stored) {
        if (route != null && !route.isEmpty()) {
            NavRouteDto next = route.getFirst();
            return new JumpTarget(orUnknown(next.getName()), orUnknown(next.getStarClass()), FuelScoop.starStatus(next.getStarClass()));
        }
        if (stored == null) return null;
        return new JumpTarget(orUnknown(stored.getName()), orUnknown(stored.getStarClass()), orUnknown(stored.getFuelStarStatus()));
    }

    private static String orUnknown(String value) {
        return value == null ? "unknown" : value;
    }

    record JumpTarget(String name, String starClass, String fuelStatus) {
    }
}
