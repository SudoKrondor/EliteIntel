package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.JukeboxManager;
import elite.intel.jukebox.JukeboxPlayer;
import elite.intel.util.StringUtls;

/**
 * Starts the commander's own music, or picks it up where it was left.
 */
@RegisterCommand
public final class PlayMusicCommand implements IntelCommand {
    public static final String ID = "play_music";

    @Override
    public String llmDescription() {
        return "Start playing the commander's own music from their Jukebox playlist, or resume it where it "
                + "was paused. Use for 'play music' or 'resume the music'. This is their personal music "
                + "library, nothing to do with the ship or the game.";
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
        player.play();
        return StringUtls.localizedResponse("handler.jukebox.playing");
    }
}
