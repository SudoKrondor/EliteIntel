package elite.intel.ai.brain.actions.command;

import com.google.gson.JsonObject;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;

/**
 * Base for the 26 "tap" commands: each subclass is a tiny no-arg class carrying its
 * own id + binding. Shared execute() publishes a single binding tap.
 * Routed through CommandRegistry as a self-describing command.
 */
public abstract class SimpleTapCommand implements IntelCommand {

    private final String id;
    private final String binding;

    protected SimpleTapCommand(String id, String binding) {
        this.id = id;
        this.binding = binding;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String bindingName() {
        return binding;
    }

    @Override
    public CommandKind kind() {
        return CommandKind.BINDING;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(binding)));
    }
}
