package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.missions.MassacrePair;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Where pirate massacre contracts can be stacked, answered from the commander's own record of
 * where they have taken such contracts and where they have seen resource extraction sites, and while
 * the commander shares, from the sets other commanders reported.
 * <p>
 * A pair needs both halves. A provider system whose factions issue against a system the commander
 * has never seen sites in is not an answer - it is a system with nowhere to do the killing.
 */
@RegisterCommand
public final class FindPirateMassacreMissionsCommand implements IntelCommand {

    public static final String ID = "find_pirate_massacre_missions";

    private static final int DEFAULT_RANGE_LY = 100;

    private final HuntingGroundManager huntingGrounds = HuntingGroundManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Report where pirate-massacre missions can be stacked: a mission-provider system whose factions "
                + "issue kill contracts against a target system that has resource extraction sites. Answered from "
                + "known sets, within the range in light years given by 'key'.";
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return SearchRange.PARAMETERS;
    }

    /**
     * Reads what is already on file - no game input, so it answers from anywhere.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        int range = SearchRange.lightYears(params, DEFAULT_RANGE_LY);

        Coordinates here = locationManager.getGalacticCoordinates();
        if (here == null) return StringUtls.localizedResponse("handler.pirate.positionUnknown");

        List<MassacrePair> pairs = huntingGrounds.bestPairs(here, range);
        if (pairs.isEmpty()) return nothingKnown(here, range);

        String spoken = HuntingGroundSpeech.pair(pairs.getFirst());
        if (pairs.size() == 1) return spoken;
        return spoken + " " + StringUtls.localizedResponse("handler.pirate.pairAlsoKnown", pairs.size() - 1);
    }

    /**
     * WHY the sharing hint is given only here: this is the moment sharing would have helped. A commander who does
     * not share learns that it exists when the sets other commanders reported would have answered the question.
     */
    private String nothingKnown(Coordinates here, int range) {
        String nothing = StringUtls.localizedResponse("handler.pirate.noPairsKnown", range);
        if (GlobalSettingsManager.getInstance().getShareMassacreSets()) return nothing;
        if (!huntingGrounds.othersReportedNear(here, range)) return nothing;
        return nothing + " " + StringUtls.localizedResponse("handler.pirate.shareHint");
    }
}
