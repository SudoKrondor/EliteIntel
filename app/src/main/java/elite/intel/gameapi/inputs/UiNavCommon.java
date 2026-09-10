package elite.intel.gameapi.inputs;

import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.session.Status;
import elite.intel.session.ui.UINavigator;

import static elite.intel.ai.hands.Bindings.GameCommand.*;

public class UiNavCommon {

    /**
     * The galaxy map toggle for wherever the commander is standing.
     * <p>
     * Elite binds the map separately per vehicle context, and the ship binding is <em>inert</em> in an
     * SRV or on foot: the tap lands, nothing happens, and no journal or Status line says so. Every place
     * that reaches for the map therefore has to pick by context, and picking wrongly is indistinguishable
     * from a keystroke that never arrived.
     * <p>
     * This lives in one place because the rule had been written out three times and the fourth caller -
     * {@link #close()} below - was missed: it tapped the ship binding whatever the commander was in, so
     * EliteIntel could open an SRV galaxy map and then be unable to close it. Reported 2026-09-09, and
     * invisible to any commander whose ship and SRV map keys are the same key, which is the layout the
     * editor already nudges toward.
     * <p>
     * Named for the key rather than for what it does: the binding is a toggle, so the same step opens a
     * shut map and shuts an open one.
     */
    public static GameInputStep galaxyMapToggleStep() {
        Status status = Status.getInstance();
        if (status.isOnFoot()) {
            return GameInputStep.bindingTap(BINDING_GALAXY_MAP_HUMANOID.getGameBinding());
        }
        if (status.isInSrv()) {
            return GameInputStep.bindingTap(BINDING_GALAXY_MAP_BUGGY.getGameBinding());
        }
        return GameInputStep.bindingTap(BINDING_GALAXY_MAP.getGameBinding());
    }

    /**
     * The system map toggle for wherever the commander is standing - see
     * {@link #galaxyMapToggleStep()}, which this mirrors exactly.
     */
    public static GameInputStep systemMapToggleStep() {
        Status status = Status.getInstance();
        if (status.isOnFoot()) {
            return GameInputStep.bindingTap(BINDING_SYSTEM_MAP_HUMANOID.getGameBinding());
        }
        if (status.isInSrv()) {
            return GameInputStep.bindingTap(BINDING_LOCAL_MAP_BUGGY.getGameBinding());
        }
        return GameInputStep.bindingTap(BINDING_LOCAL_MAP.getGameBinding());
    }

    public static void close() {
        Status status = Status.getInstance();
        if (status.isSystemMapOpen()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(systemMapToggleStep()));
        }
        if (status.isGalaxyMapOpen()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(galaxyMapToggleStep()));
        }
        new UINavigator().closeOpenPanel();
        GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_EXIT_KEY.getGameBinding())));
    }


    public static void prepToKnownUiPositionWhileInTheShipAtStation() {
        GameControllerBus.publish(GameInputSequenceEvent.of(
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_DOWN.getGameBinding()),
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_DOWN.getGameBinding()),
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_DOWN.getGameBinding())
        ));
    }
}
