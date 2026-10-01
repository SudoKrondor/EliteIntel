package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.dao.DestinationReminderDao;
import elite.intel.db.managers.CommoditySearchResultManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.SearchExclusionManager;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Strikes a star system from every search that ends in a plotted route - "this" system, the one the ship is in,
 * or "that" one, where the last search sent the commander.
 * <p>
 * WHY the commander has to be able to say this: Spansh keeps a station on file after it is gone. Measured
 * 2026-09-27, a Battle Weapons search named Oz Prospect in Hyades Sector WI-S b4-4, a commander-colonised system,
 * and there was no starport there - its owner had pulled it down. Asking again named the same system, because
 * nothing had changed in Spansh. The seven-day market window drops such a station within a week; this is for the
 * week before that, and for anything else the commander has seen for themselves.
 * <p>
 * WHY "that" system as well as "this" one: a system the commander has been to can be checked in the system map
 * from anywhere, and one they have not can only be checked by flying there. Either way the system named is the
 * one the last search sent them to, which is what the destination reminder holds - every route search writes it.
 * <p>
 * Dangerous, so VEGA asks first, and the question names the system AND which one it is - "the current system" or
 * "the destination system". "That" is worked out at run time, and in Russian "ту" and "эту" (that, this) are one
 * syllable apart, so a misheard word must come back as an audibly different question and not just a system name
 * the commander has to catch. For the same reason the Russian and Ukrainian aliases say "текущую"/"поточну"
 * (current) for this one.
 * <p>
 * Undone by {@link AllowStarSystemInSearchesAgainCommand}, from inside the system only.
 */
@RegisterCommand
public final class ExcludeStarSystemFromSearchesCommand implements IntelCommand {

    public static final String ID = "exclude_star_system_from_searches";

    static final String PARAM_SYSTEM = "system";
    static final String CURRENT = "current";
    static final String DESTINATION = "destination";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec system = new ActionParameterSpec(
                PARAM_SYSTEM, "string", false,
                "Which star system: 'current' = the one the ship is in now, 'destination' = the one the last "
                        + "search sent us to.",
                List.of(CURRENT, DESTINATION),
                "'current' for THIS or the CURRENT system, or HERE, 'destination' for THAT or the DESTINATION system.",
                List.of(CURRENT, DESTINATION));
        system.validate();
        return List.of(system);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Exclude a star system from every search from now on, because a station a search named there does "
                + "not exist or cannot be used.";
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    /**
     * Edits what is on file and taps no game binding, so it can be said from anywhere.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    /**
     * Putting a system back takes being there again, which for "that" system means flying to it, so VEGA asks
     * before striking one.
     */
    @Override
    public boolean isDangerous() {
        return true;
    }

    @Override
    public String confirmationPrompt(JsonObject params) {
        String starSystem = target(params);
        if (starSystem == null) return null;
        return StringUtls.localizedResponse(wantsDestination(params)
                ? "handler.searchExclusion.confirmDestination"
                : "handler.searchExclusion.confirmCurrent", starSystem);
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        String starSystem = target(params);
        if (starSystem == null) {
            return StringUtls.localizedResponse(wantsDestination(params)
                    ? "handler.searchExclusion.noDestination"
                    : "handler.searchExclusion.positionUnknown");
        }
        if (!SearchExclusionManager.getInstance().exclude(starSystem)) {
            return StringUtls.localizedResponse("handler.searchExclusion.alreadyExcluded", starSystem);
        }
        dropErrandTo(starSystem);
        return StringUtls.localizedResponse("handler.searchExclusion.excluded", starSystem);
    }

    /**
     * The system the call means, or null when it cannot be known: no position yet, or no search destination.
     */
    private static String target(JsonObject params) {
        String starSystem = wantsDestination(params)
                ? destinationSystem()
                : PlayerSession.getInstance().getPrimaryStarName();
        return starSystem == null || starSystem.isBlank() ? null : starSystem;
    }

    /**
     * "This system" unless the commander said "that" one. A call with no argument means the system the ship is in,
     * the one there is no doubt about.
     */
    private static boolean wantsDestination(JsonObject params) {
        JsonElement system = params == null ? null : params.get(PARAM_SYSTEM);
        return system != null && system.isJsonPrimitive() && DESTINATION.equalsIgnoreCase(system.getAsString().strip());
    }

    private static String destinationSystem() {
        DestinationReminderDao.Reminder reminder = ReminderManager.getInstance().getReminder();
        return reminder == null ? null : reminder.getStarSystem();
    }

    /**
     * The errand and its overlay card point at the system just struck off, and would keep sending the commander
     * to a port that is not there.
     */
    private static void dropErrandTo(String starSystem) {
        String destination = destinationSystem();
        if (destination != null && SearchExclusionManager.key(destination).equals(SearchExclusionManager.key(starSystem))) {
            ReminderManager.getInstance().clear();
            CommoditySearchResultManager.getInstance().clear();
        }
    }
}
