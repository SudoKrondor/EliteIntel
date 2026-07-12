package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.companion.CompanionRuntime;
import elite.intel.db.FuzzySearch;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.search.spansh.stellarobjects.ReserveLevel;
import elite.intel.search.spansh.stellarobjects.StellarObjectSearch;
import elite.intel.search.spansh.stellarobjects.StellarObjectSearchResultDto;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

import java.util.List;
import java.util.Optional;

import static elite.intel.util.StringUtls.capitalizeWords;

/**
 * Self-describing "find mining site" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindMiningSiteHandler,
 * routed through CommandRegistry via the self-describing model.
 * <p>
 * Also serves tritium (fleet-carrier fuel), which is an ordinary mineable commodity in {@code key}. The separate
 * find_tritium_mining_site command that once existed was a workaround for an STT engine that could not transcribe
 * "tritium"; the current engine can, so the workaround is gone.
 */
@RegisterCommand
public final class FindMiningSiteCommand implements IntelCommand {
    public static final String ID = "find_mining_site";

    @Override
    public String llmDescription() {
        return "Find and plot a route to the nearest pristine mining site (ring/asteroid field) that yields the commodity in 'key', within 'max_distance' ly. Tritium (fleet-carrier fuel) is one such commodity.";
    }


    private static final int MAX_DEFAULT_RANGE = 1000;

    private static final String PARAM_KEY = "key";
    private static final String PARAM_MAX_DISTANCE = "max_distance";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "string", true,
                "The commodity (mineable material) to mine, e.g. painite, platinum, low temperature diamonds.",
                List.of("painite", "platinum"),
                "Extract the material name verbatim in lower case; do not translate.");
        key.validate();
        ActionParameterSpec maxDistance = new ActionParameterSpec(
                PARAM_MAX_DISTANCE, "number", false,
                "Maximum galactic search radius in light years (ly). If omitted, a default range is used.",
                List.of("100", "500"),
                "Extract the distance limit in light years if the commander states one.");
        maxDistance.validate();
        return List.of(key, maxDistance);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    /**
     * Maps and routes are available anywhere in the game.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        JsonElement mat = params.get(PARAM_KEY);
        if (mat == null) {
            return StringUtls.localizedLlm("handler.miningSite.didNotCatch");
        }

        String material =
                capitalizeWords(
                        FuzzySearch.fuzzyCommodityMatch(
                                mat.getAsString(), 8
                        )
                );
        // The model may hand back a non-numeric radius ("three hundred"); this is how every sibling search
        // command reads max_distance, and it falls back to the default rather than throwing.
        int range = GetNumberFromParam.extractRangeParameter(params, MAX_DEFAULT_RANGE).intValue();

        // The Spansh ring search takes seconds; every other long search command voices a filler so the
        // commander is not left in silence.
        CompanionRuntime.narrator().filler(
                StringUtls.localizedLlm("handler.miningSite.searching", material, range), false);

        StellarObjectSearchResultDto miningLocations = StellarObjectSearch.getInstance()
                .findRings(
                        material,
                        ReserveLevel.PRISTINE,
                        LocationManager.getInstance().getGalacticCoordinates(),
                        range
                );

        if (miningLocations == null || miningLocations.getResults().isEmpty()) {
            return StringUtls.localizedLlm("handler.miningSite.notFound");
        }

        Optional<StellarObjectSearchResultDto.Result> result = miningLocations.getResults().stream().findFirst();
        if (result.isPresent()) {
            RoutePlotter routePlotter = new RoutePlotter();
            routePlotter.plotRoute(result.get().getSystemName());
            String reminder = StringUtls.localizedLlm("handler.miningSite.found", result.get().getSystemName(), result.get().getBodyName());
            ReminderManager.getInstance().setReminder(reminder, result.get().getSystemName());
            return reminder;
        } else {
            return StringUtls.localizedLlm("handler.miningSite.notFoundInRange");
        }
    }
}
