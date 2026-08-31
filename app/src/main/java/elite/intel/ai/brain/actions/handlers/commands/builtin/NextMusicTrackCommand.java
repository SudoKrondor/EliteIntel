package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.JukeboxManager;
import elite.intel.jukebox.JukeboxPlayer;
import elite.intel.util.StringUtls;

/**
 * Skips the track that is playing.
 */
@RegisterCommand
public final class NextMusicTrackCommand implements IntelCommand {
    public static final String ID = "skip_to_next_music_track";

    @Override
    public String llmDescription() {
        return "Skip to the next track in the commander's own music playlist. Use for 'next track' or "
                + "'skip this song'. Nothing to do with the ship's navigation route.";
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
        player.next();
        return StringUtls.localizedResponse("handler.jukebox.skipped");
    }
}
