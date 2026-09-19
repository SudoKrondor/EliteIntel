package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.dao.ExoMasteryDao.Body;
import elite.intel.db.managers.ExoMasteryManager;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.TTSFriendlyNumberConverter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * "We have done this whole system already": writes off every Exo-Mastery body still open in the
 * system the commander is in.
 *
 * <p>The game remembers every organism a commander has ever sampled and refuses a second sample; the
 * journal never says so, and a commander who did the sampling before installing the app has no
 * journal for it at all. Without this the route command would keep sending them back. The per-body
 * version of the same correction is {@code set_current_body_exobiology_survey_complete}, offered when
 * they are at one body - this one is for the commander who arrives at the system the route command
 * picked and recognises the whole place.
 *
 * <p>One direction only, and deliberately: the undo is per body, through the other command, because a
 * misfire here is heard at once - it names the bodies and the credits written off - and putting one
 * body back is a sentence, while putting a system back that was in fact done would be the game
 * refusing every landing.
 */
@RegisterCommand
public final class FlagExoMasterySystemHarvestedCommand implements IntelCommand {
    public static final String ID = "flag_exo_mastery_system_harvested";

    private final ExoMasteryManager exoMastery = ExoMasteryManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String llmDescription() {
        return "Record that the commander has ALREADY sampled every exobiology body in the CURRENT star system "
                + "in a past play session, so the Exo-Mastery route stops offering this system. Whole system only - "
                + "for one planet use set_current_body_exobiology_survey_complete. Never use it to report a scan "
                + "just performed; the journal records those on its own.";
    }

    @Override
    public String id() {
        return ID;
    }

    /**
     * Offered only in a catalogued system with something still open in it: said anywhere else the
     * phrase has nothing to act on, and the guard keeps a stray "we're done here" from reaching a
     * command that writes a stored row.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return currentCataloguedSystem() != null;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        Long systemAddress = currentCataloguedSystem();
        if (systemAddress == null) {
            // Re-checked rather than trusted from visibility: a jump can land between the turn being
            // offered and the words arriving.
            return StringUtls.localizedResponse("handler.exoMastery.nothingOpenHere");
        }
        List<Body> written = exoMastery.completeSystem(systemAddress);
        if (written.isEmpty()) {
            return StringUtls.localizedResponse("handler.exoMastery.nothingOpenHere");
        }
        long value = written.stream().mapToLong(Body::value).sum();
        String names = written.stream().map(ExoMasteryManager::shortBodyName).collect(Collectors.joining(", "));
        return StringUtls.localizedResponse("handler.exoMastery.systemWrittenOff",
                written.get(0).starSystem(), names, TTSFriendlyNumberConverter.formatCreditsForSpeech(value));
    }

    /**
     * The current system's address when the catalogue is loaded and still has an open body in it.
     */
    private Long currentCataloguedSystem() {
        if (!exoMastery.isEnabled()) return null;
        LocationData<Long, Long> here = playerSession.getLocationData();
        if (here == null || here.getSystemAddress() == null || here.getSystemAddress() == 0) return null;
        return exoMastery.hasRemaining(here.getSystemAddress()) ? here.getSystemAddress() : null;
    }
}
