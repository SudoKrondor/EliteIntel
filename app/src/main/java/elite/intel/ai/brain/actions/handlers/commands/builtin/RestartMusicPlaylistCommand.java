package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.JukeboxManager;
import elite.intel.jukebox.JukeboxPlayer;
import elite.intel.util.StringUtls;

/**
 * Takes the playlist back to its first track.
 * <p>
 * WHY this is worth a command of its own next to {@code play_music}: "play music" carries on where the
 * commander left off, which is right nearly always and wrong exactly when they want the album, or the
 * audiobook, from the beginning again. Neither phrasing can stand in for the other, and the playlist now
 * rolls round on its own at the end, so this is the only way back to the top by voice.
 */
@RegisterCommand
public final class RestartMusicPlaylistCommand implements IntelCommand {
    public static final String ID = "restart_music_playlist_from_first_track";

    @Override
    public String llmDescription() {
        return "Start the commander's own music playlist again from its very first track. Use for 'restart "
                + "the playlist' or 'play from the top'. Unlike play_music this does not resume where the "
                + "music was paused - it goes back to the beginning of the list.";
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
        player.playFromTop();
        return StringUtls.localizedResponse("handler.jukebox.restarted");
    }
}
