package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.JukeboxManager;
import elite.intel.jukebox.JukeboxPlayer;
import elite.intel.util.StringUtls;

/**
 * Goes back to the track played before this one.
 */
@RegisterCommand
public final class PreviousMusicTrackCommand implements IntelCommand {
    public static final String ID = "play_previous_music_track";

    @Override
    public String llmDescription() {
        return "Go back to the previous track in the commander's own music playlist. Use for 'previous "
                + "track' or 'go back a song'.";
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        if (JukeboxManager.getInstance().size() == 0) {
            // WHY answer rather than be hidden: a command withdrawn from the model makes the companion say
            // it has no such function, which is both untrue and useless. Naming the actual problem tells the
            // commander exactly what to do about it.
            return StringUtls.localizedResponse("handler.jukebox.noMusic");
        }
        JukeboxPlayer player = JukeboxPlayer.getInstance();
        player.start();
        player.previous();
        return StringUtls.localizedResponse("handler.jukebox.wentBack");
    }
}
