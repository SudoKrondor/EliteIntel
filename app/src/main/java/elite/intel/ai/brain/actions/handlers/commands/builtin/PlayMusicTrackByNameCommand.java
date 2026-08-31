package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.dao.JukeboxDao;
import elite.intel.db.managers.JukeboxManager;
import elite.intel.jukebox.JukeboxPlayer;
import elite.intel.jukebox.TrackSearch;
import elite.intel.util.StringUtls;

import java.util.List;
import java.util.Optional;

/**
 * Plays a particular track the commander named out loud.
 * <p>
 * Every alias for this carries the title as an argument, so it never competes with plain "play music" for a
 * bare phrase - one is a request for a named thing and the other is a request for any music at all.
 */
@RegisterCommand
public final class PlayMusicTrackByNameCommand implements IntelCommand {
    public static final String ID = "play_music_track_by_name";
    private static final String PARAM_KEY = "key";
    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    @Override
    public String llmDescription() {
        return "Play one particular track from the commander's own music playlist, named by its title or "
                + "its artist. Only for when they name what they want to hear; use play_music for "
                + "'play music' with nothing named.";
    }

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY,
                "string",
                true,
                "The track title or artist the commander asked for, as they said it.",
                List.of("Aphelion Drift", "Stellar Cartography"),
                "Take the title exactly as spoken; do not correct or expand it.");
        key.validate();
        return List.of(key);
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
        JsonElement key = params == null ? null : params.get(PARAM_KEY);
        if (key == null || key.isJsonNull() || key.getAsString().isBlank()) {
            return StringUtls.localizedResponse("handler.jukebox.trackNotFound");
        }
        List<JukeboxDao.Track> playlist = JukeboxManager.getInstance().playlist();
        if (playlist.isEmpty()) {
            return StringUtls.localizedResponse("handler.jukebox.noMusic");
        }
        Optional<JukeboxDao.Track> found = TrackSearch.find(playlist, key.getAsString());
        if (found.isEmpty()) {
            // Deliberately not "playing something close": the wrong track is worse than none, because the
            // commander then has to work out what happened and undo it.
            return StringUtls.localizedResponse("handler.jukebox.trackNotFound");
        }
        JukeboxPlayer player = JukeboxPlayer.getInstance();
        player.start();
        player.playTrack(found.get().getId());
        return StringUtls.localizedResponse("handler.jukebox.playingTrack", found.get().displayTitle());
    }
}
