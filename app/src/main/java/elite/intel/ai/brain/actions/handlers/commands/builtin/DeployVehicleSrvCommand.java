package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.db.managers.ShipSettingsManager;
import elite.intel.db.managers.ShipLoadoutManager;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.gameapi.SurfaceVehicle;
import elite.intel.gameapi.SurfaceVehicleDeployment;
import elite.intel.gameapi.ShipVehicleHangar;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import elite.intel.session.ui.UINavigator;
import elite.intel.util.StringUtls;

import java.util.ArrayList;
import java.util.List;

/**
 * Deploys a surface vehicle from the ship's planetary vehicle hangar.
 *
 * <p><b>Why this is no longer one key sequence.</b> A multi-bay hangar opens onto a list of bays, and the
 * old sequence always took the top one - so a commander with a Scorpion in bay 2 got a Scarab, every time,
 * with nothing to indicate the command had understood them differently. The bay is now chosen explicitly,
 * one step down the list per bay.
 *
 * <p><b>And why the ship's state is no longer one check.</b> The Scarab and Scorpion are driven out of a
 * ship sitting on the surface. The Rhino is dropped from one hovering above it. "Landed" was the whole
 * gate before and is now only two thirds of it, which is also why this command is offered to the model
 * while hovering and answers for itself, rather than disappearing from the offered set at the exact
 * altitude the Rhino needs.
 *
 * <p>The Nomad is not deployed from here. It flies, it has its own bay, and none of these conditions
 * describe it.
 */
@RegisterCommand
public final class DeployVehicleSrvCommand implements IntelCommand {
    public static final String ID = "deploy_vehicle_srv";
    private static final String PARAM_BAY = "bay";
    private static final String PARAM_VEHICLE = "vehicle";
    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();

    @Override
    public String llmDescription() {
        return "Deploy a surface vehicle (SRV Scarab, SRV Scorpion or SRV Rhino) from the ship's planetary "
                + "vehicle hangar. Pass 'vehicle' when the commander names one ('deploy the Scarab'), or "
                + "'bay' when they name a bay ('deploy from bay 3'). Omit both and bay 1 is used. Not a "
                + "ship undock, fighter, or Nomad.";
    }

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec bay = new ActionParameterSpec(
                PARAM_BAY,
                "number",
                false,
                "Which hangar bay to deploy from, 1 to " + SurfaceVehicleDeployment.MAX_BAYS
                        + ". Omit it when the commander does not name a bay.",
                List.of("1", "2", "3", "4"),
                "\"bay three\"/\"from bay 3\"/\"third bay\" -> 3; omit when no bay is mentioned.");
        bay.validate();

        ActionParameterSpec vehicle = new ActionParameterSpec(
                PARAM_VEHICLE,
                "string",
                false,
                "Which surface vehicle to deploy, when the commander names one instead of a bay. The bay "
                        + "holding it is looked up from the commander's own hangar configuration.",
                List.of("SCARAB", "SCORPION", "RHINO"),
                "\"deploy the Scarab\"/\"launch my Scorpion\"/\"drop the Rhino\" -> that vehicle; omit "
                        + "when no vehicle is named. The Nomad is NOT one of these.",
                List.of("SCARAB", "SCORPION", "RHINO"));
        vehicle.validate();

        return List.of(bay, vehicle);
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public String id() {
        return ID;
    }

    /**
     * Offered whenever the commander is in the ship at a planetary body, landed or not.
     * <p>
     * WHY not just "landed" as before: the Rhino deploys from a hover, so gating visibility on landed would
     * withhold the command at precisely the altitude it is wanted. Being at a body at all is the honest
     * precondition - everything finer than that is a refusal this command can explain, and explaining beats
     * vanishing from the offered set for a reason the model would have to guess at.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() && (status.isLanded() || status.hasLatLong());
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        String namedVehicle = requestedVehicleText(params);
        SurfaceVehicle vehicle = SurfaceVehicle.fromStored(namedVehicle);
        // Named something, and it is not a vehicle we carry the vocabulary for - the Nomad, most likely,
        // which comes out of its own bay entirely. Falling through with a null vehicle would quietly
        // deploy bay 1 instead, which is the wrong vehicle in answer to a clear request.
        if (namedVehicle != null && !namedVehicle.isBlank() && vehicle == null) {
            return StringUtls.localizedResponse("handler.vehicle.unknownVehicle", namedVehicle.trim());
        }

        SurfaceVehicleDeployment.Decision decision = SurfaceVehicleDeployment.decide(
                ShipVehicleHangar.isFitted(),
                configuredBays(),
                requestedBay(params),
                vehicle,
                situation());

        if (!decision.isAllowed()) {
            return refusal(decision);
        }

        GameControllerBus.publish(GameInputSequenceEvent.of(bayKeySequence(decision.bay())));
        navigator.assumeDefaultState(StatusFlags.GuiFocus.CENTRAL_PANEL);
        return StringUtls.localizedResponse("handler.vehicle.deploying",
                decision.vehicle().displayName(), decision.bay());
    }

    /**
     * The role panel sequence that opens one bay.
     * <p>
     * Everything up to the bay list is fixed: focus the role panel, drive the cursor to a known corner, then
     * across to the hangar. The list then opens on bay 1, and each further bay is one step down it - which is
     * the whole of what a multi-bay hangar needed and never had.
     */
    private static GameInputStep[] bayKeySequence(int bay) {
        List<GameInputStep> steps = new ArrayList<>(List.of(
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_FOCUS_ROLE_PANEL.getGameBinding()),
                // Ensure the cursor is at the top before navigating to the SRV option.
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_LEFT.getGameBinding()),
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_LEFT.getGameBinding()),
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_UP.getGameBinding()),
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_DOWN.getGameBinding()),
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_DOWN.getGameBinding()),
                // Into the bay list, which opens on bay 1.
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_RIGHT.getGameBinding())));

        for (int step = 1; step < bay; step++) {
            steps.add(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_DOWN.getGameBinding()));
        }

        steps.add(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_ACTIVATE.getGameBinding()));
        return steps.toArray(new GameInputStep[0]);
    }

    /**
     * The bay the commander named, or null when they did not.
     * <p>
     * A value that is not a number at all reads as "not named" rather than as bay zero: the commander said
     * something the model could not turn into a bay, and bay 1 is what they meant by saying nothing.
     */
    /**
     * The vehicle name as the model passed it, untouched. Parsed here rather than inside the decision so
     * that the decision deals only in vehicles that exist, and the "never heard of it" case keeps the
     * words the commander actually used.
     */
    private static String requestedVehicleText(JsonObject params) {
        JsonElement named = params == null ? null : params.get(PARAM_VEHICLE);
        if (named == null || named.isJsonNull()) return null;
        try {
            return named.getAsString();
        } catch (UnsupportedOperationException | IllegalStateException notAString) {
            return null;
        }
    }

    private static Integer requestedBay(JsonObject params) {
        JsonElement bay = params == null ? null : params.get(PARAM_BAY);
        if (bay == null || bay.isJsonNull()) return null;
        try {
            return bay.getAsInt();
        } catch (NumberFormatException | UnsupportedOperationException | IllegalStateException notANumber) {
            return null;
        }
    }

    /**
     * What the commander says is in each bay of the ship they are flying, or an empty list when we do not
     * know which ship that is - which reads downstream as "not configured".
     */
    private static List<SurfaceVehicle> configuredBays() {
        ShipLoadOutDto loadout = ShipLoadoutManager.getInstance().get();
        if (loadout == null) return List.of();
        ShipSettingsDao.ShipSettings settings = ShipSettingsManager.getInstance().getSettings(loadout.getShipId());
        return settings == null ? List.of() : settings.vehicleBays();
    }

    private SurfaceVehicleDeployment.ShipSituation situation() {
        return new SurfaceVehicleDeployment.ShipSituation(
                status.isLanded(),
                status.getStatus().getAltitude(),
                status.hasLatLong());
    }

    /**
     * Turns a refusal into the one thing the commander can act on. Each carries what they need to fix it:
     * the altitude band, the bay number that was heard, or where the setting lives.
     */
    private static String refusal(SurfaceVehicleDeployment.Decision decision) {
        return switch (decision.refusal()) {
            case NO_VEHICLE_BAY -> StringUtls.localizedResponse("handler.vehicle.noHangar");
            case BAYS_NOT_CONFIGURED -> StringUtls.localizedResponse("handler.vehicle.baysNotConfigured");
            case NO_SUCH_BAY -> StringUtls.localizedResponse("handler.vehicle.noSuchBay",
                    decision.requestedBay(), SurfaceVehicleDeployment.MAX_BAYS);
            case BAY_EMPTY -> StringUtls.localizedResponse("handler.vehicle.bayEmpty", decision.requestedBay());
            case NOT_LANDED -> StringUtls.localizedResponse("handler.vehicle.notLanded");
            case WRONG_ALTITUDE -> StringUtls.localizedResponse("handler.vehicle.wrongAltitude",
                    (int) SurfaceVehicle.RHINO_MIN_ALTITUDE_METRES,
                    (int) SurfaceVehicle.RHINO_MAX_ALTITUDE_METRES);
            case VEHICLE_NOT_LOADED -> StringUtls.localizedResponse("handler.vehicle.notLoaded",
                    decision.requestedVehicle().displayName());
            case BAY_HOLDS_OTHER -> StringUtls.localizedResponse("handler.vehicle.bayHoldsOther",
                    decision.requestedBay(), decision.vehicle().displayName(),
                    decision.requestedVehicle().displayName());
        };
    }
}
