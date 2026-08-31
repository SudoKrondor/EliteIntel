package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.jukebox.JukeboxPlayer;
import elite.intel.jukebox.PlaybackOrder;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Switches the playlist between playing in order and playing at random.
 * <p>
 * The state is a parameter rather than a plain toggle so that "shuffle the music" and "play them in order"
 * each say what they want. A toggle would leave both meaning "whatever it is not now", which is only ever
 * right by luck when the commander cannot see the setting.
 */
@RegisterCommand
public final class ShuffleMusicCommand implements IntelCommand {
    public static final String ID = "shuffle_music_tracks";
    private static final String PARAM_STATE = "state";
    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    @Override
    public String llmDescription() {
        return "Choose whether the commander's own music plays in random order or in playlist order. Use "
                + "state=true to shuffle, state=false to play it in order.";
    }

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec state = new ActionParameterSpec(
                PARAM_STATE,
                "boolean",
                true,
                "True to play the music in random order, false to play it in playlist order.",
                List.of("true", "false"),
                "shuffle/random/mix it up -> true; in order/sequential/stop shuffling -> false.");
        state.validate();
        return List.of(state);
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        JsonElement state = params == null ? null : params.get(PARAM_STATE);
        boolean shuffle = state != null && !state.isJsonNull() && state.getAsBoolean();
        JukeboxPlayer.getInstance().setPlaybackOrder(shuffle ? PlaybackOrder.RANDOM : PlaybackOrder.SEQUENTIAL);
        return StringUtls.localizedResponse(shuffle
                ? "handler.jukebox.shuffling" : "handler.jukebox.inOrder");
    }
}
