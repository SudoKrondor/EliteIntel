package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.db.FuzzySearch;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.search.spansh.stellarobjects.ReserveLevel;
import elite.intel.search.spansh.stellarobjects.StellarObjectSearch;
import elite.intel.search.spansh.stellarobjects.StellarObjectSearchResultDto;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.Optional;

import static elite.intel.util.StringUtls.capitalizeWords;

/**
 * Self-describing "find mining site" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindMiningSiteHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class FindMiningSiteCommand implements IntelCommand {
    public static final String ID = "find_mining_site";

    @Override public String llmDescription() { return "Find a nearby mining site for a commodity."; }


    private static final int MAX_DEFAULT_RANGE = 1000;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();
        if (!status.isInMainShip()) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.miningSite.boardShip"));
        }

        JsonElement mat = params.get("key");
        JsonElement distance = params.get("max_distance");
        if (mat == null) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.miningSite.didNotCatch"));
        }

        String material =
                capitalizeWords(
                        FuzzySearch.fuzzyCommodityMatch(
                                mat.getAsString(), 8
                        )
                );

        StellarObjectSearchResultDto miningLocations = StellarObjectSearch.getInstance()
                .findRings(
                        material,
                        ReserveLevel.PRISTINE,
                        LocationManager.getInstance().getGalacticCoordinates(),
                        distance == null ? MAX_DEFAULT_RANGE : distance.getAsInt()
                );

        if (miningLocations == null || miningLocations.getResults().isEmpty()) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.miningSite.notFound"));
        }

        Optional<StellarObjectSearchResultDto.Result> result = miningLocations.getResults().stream().findFirst();
        if (result.isEmpty()) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.miningSite.notFoundInRange"));
        }
        RoutePlotter routePlotter = new RoutePlotter();
        routePlotter.plotRoute(result.get().getSystemName());
        String reminder = StringUtls.localizedLlm("handler.miningSite.found", result.get().getSystemName(), result.get().getBodyName());
        ReminderManager.getInstance().setReminder(reminder, result.get().getSystemName());
        return CommandOutcome.speak(reminder);
    }
}
