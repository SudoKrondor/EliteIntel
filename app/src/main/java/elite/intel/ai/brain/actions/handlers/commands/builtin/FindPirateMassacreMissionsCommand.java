package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.missions.MassacrePair;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;

import static elite.intel.util.StringUtls.getIntSafely;

/**
 * Where pirate massacre contracts can be stacked, answered from the commander's own record of
 * where they have taken such contracts and where they have seen resource extraction sites.
 * <p>
 * A pair needs both halves. A provider system whose factions issue against a system the commander
 * has never seen sites in is not an answer - it is a system with nowhere to do the killing.
 */
@RegisterCommand
public final class FindPirateMassacreMissionsCommand implements IntelCommand {

    public static final String ID = "find_pirate_massacre_missions";

    private static final String PARAM_KEY = "key";
    private static final int DEFAULT_RANGE_LY = 100;

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

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
                + "the commander's own recorded history, within the range in light years given by 'key'.";
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
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
        int range = rangeLy(params);

        Coordinates here = locationManager.currentCoordinates();
        if (here == null) return StringUtls.localizedResponse("handler.pirate.positionUnknown");

        List<MassacrePair> pairs = huntingGrounds.bestPairs(here, range);
        if (pairs.isEmpty()) return StringUtls.localizedResponse("handler.pirate.noPairsKnown", range);

        String spoken = HuntingGroundSpeech.pair(pairs.getFirst());
        if (pairs.size() == 1) return spoken;
        return spoken + " " + StringUtls.localizedResponse("handler.pirate.pairAlsoKnown", pairs.size() - 1);
    }

    private int rangeLy(JsonObject params) {
        if (params == null || params.get(PARAM_KEY) == null || params.get(PARAM_KEY).isJsonNull()) {
            return DEFAULT_RANGE_LY;
        }
        Integer stated = getIntSafely(params.get(PARAM_KEY).getAsString());
        return stated == null || stated <= 0 ? DEFAULT_RANGE_LY : stated;
    }

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "number", false,
                "Maximum search range in light years (ly). If omitted, a default range is used.",
                List.of("50", "100"),
                "Extract the range limit in light years if the commander states one; otherwise omit it.");
        key.validate();
        return List.of(key);
    }
}
