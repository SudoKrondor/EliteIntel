package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.db.dao.ExoMasteryDao.Body;
import elite.intel.db.dao.ExoMasteryDao.Site;
import elite.intel.db.managers.ExoMasteryManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.search.PermitLockedSystems;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.TTSFriendlyNumberConverter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * "Take me to the money": plots a route to the richest system on the Exo-Mastery catalogue that still
 * has a body to sample.
 *
 * <p>Richest by what is <em>left</em>, not by what the catalogue said: a system the commander has half
 * sampled keeps its other half on offer, and one they have finished drops off. When the best system is
 * the one they are standing in there is nothing to plot, so the answer names the bodies still open
 * here instead, and the next-best system only if they ask again after finishing.
 *
 * <p>Distinct from {@code navigate_to_bio_sample_codex_entry}, which is surface navigation to the next
 * organism on the planet under the ship. This is the jump before that: which system to fly to at all.
 * The two are never offered together - this one needs the main-ship cockpit for the galaxy map, and
 * withdraws itself once the ship has landed.
 */
@RegisterCommand
public final class NavigateToExoMasterySiteCommand implements IntelCommand {
    public static final String ID = "navigate_to_exo_mastery_site";

    /**
     * How many of the richest sites to consider, so a permit-locked one at the head still leaves an answer.
     */
    private static final int RICHEST_CANDIDATES = 5;

    private final ExoMasteryManager exoMastery = ExoMasteryManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String llmDescription() {
        return "Plot a route to the highest-paying exobiology star system on the Exo-Mastery list that the "
                + "commander has not sampled out yet, and leave a reminder naming the planets to land on there. "
                + "Use for 'take me to the next exobiology site' - a whole system to fly to, not the next "
                + "organism on the planet we are already on.";
    }

    @Override
    public String id() {
        return ID;
    }

    /**
     * Offered only with the catalogue loaded, in the main-ship cockpit (the galaxy map is a ship-only
     * bind) and off the ground - on the surface the question is where the next organism is, not where
     * the next system is.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return !status.isLanded() && exoMastery.isEnabled();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        if (!exoMastery.isEnabled()) {
            return StringUtls.localizedResponse("handler.exoMastery.notEnabled");
        }
        // The richest site the commander may fly to: a few are asked for in case the richest sits behind a
        // permit, which no catalogue entry says anything about.
        List<Site> open = PermitLockedSystems.reachable(exoMastery.richestRemaining(RICHEST_CANDIDATES), Site::starSystem);
        if (open.isEmpty()) {
            return StringUtls.localizedResponse("handler.exoMastery.allHarvested");
        }
        Site best = open.getFirst();
        LocationData<Long, Long> here = playerSession.getLocationData();
        boolean alreadyThere = here != null && here.getSystemAddress() != null && here.getSystemAddress() == best.systemAddress();
        if (alreadyThere) {
            return StringUtls.localizedResponse("handler.exoMastery.alreadyHere",
                    best.starSystem(), bodyList(exoMastery.remainingBodiesIn(best.systemAddress())));
        }

        List<Body> bodies = exoMastery.remainingBodiesIn(best.systemAddress());
        String bodyList = bodyList(bodies);
        VegaRuntime.narrator().filler(StringUtls.localizedResponse("handler.exoMastery.plotting",
                best.starSystem(),
                TTSFriendlyNumberConverter.formatCountForSpeech(bodies.size()),
                TTSFriendlyNumberConverter.formatCreditsForSpeech(best.remainingValue())), false);
        // The errand for the arrival announcement: which planets to land on. The system is the reminder's
        // own column, so the text names only what to do there.
        ReminderManager.getInstance().setReminder(
                StringUtls.localizedResponse("handler.exoMastery.reminder", bodyList), best.starSystem());
        // The find was spoken as filler above, so the plotter's note is all that is left to say - and it
        // says something only when no new route was plotted.
        return new RoutePlotter().plotRouteAnd(null, best.starSystem());
    }

    private static String bodyList(List<Body> bodies) {
        return bodies.stream().map(ExoMasteryManager::shortBodyName).collect(Collectors.joining(", "));
    }
}
