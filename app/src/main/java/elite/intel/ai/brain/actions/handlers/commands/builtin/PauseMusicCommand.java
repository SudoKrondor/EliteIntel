package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.jukebox.JukeboxPlayer;
import elite.intel.util.StringUtls;

/**
 * Silences the music without losing the commander's place in it.
 * <p>
 * <b>Why "stop the music" is aliased here rather than to the transport's stop.</b> Spoken, "stop" means
 * "stop playing", not "go back to the beginning". A commander three quarters of the way through an
 * audiobook chapter who says "stop the music" and later "play music" expects the chapter to continue, and
 * would be badly served by starting it again. The Stop button on the Jukebox tab still rewinds, because
 * there the commander can see exactly what it did and press play with the track still selected.
 */
@RegisterCommand
public final class PauseMusicCommand implements IntelCommand {
    public static final String ID = "pause_music_playback";

    @Override
    public String llmDescription() {
        return "Pause the commander's own music, keeping the position in the current track so it can be "
                + "resumed. Use for 'pause the music' and also for 'stop the music' - spoken, both mean "
                + "silence it, not restart it.";
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        JukeboxPlayer.getInstance().pause();
        return StringUtls.localizedResponse("handler.jukebox.paused");
    }
}
