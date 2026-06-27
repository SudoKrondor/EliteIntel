package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.FuzzySearch;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.search.spansh.stellarobjects.ReserveLevel;
import elite.intel.search.spansh.stellarobjects.StellarObjectSearch;
import elite.intel.search.spansh.stellarobjects.StellarObjectSearchResultDto;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;
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

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                "key", "string", true,
                "The commodity (mineable material) to mine, e.g. painite, platinum, low temperature diamonds.",
                List.of("painite", "platinum"),
                "Extract the material name verbatim in lower case; do not translate.");
        key.validate();
        ActionParameterSpec maxDistance = new ActionParameterSpec(
                "max_distance", "number", false,
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

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();
        if (!status.isInMainShip()) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.miningSite.boardShip")));
            return;
        }

        JsonElement mat = params.get("key");
        JsonElement distance = params.get("max_distance");
        if (mat == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.miningSite.didNotCatch")));
            return;
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
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.miningSite.notFound")));
            return;
        }

        Optional<StellarObjectSearchResultDto.Result> result = miningLocations.getResults().stream().findFirst();
        if (result.isPresent()) {
            RoutePlotter routePlotter = new RoutePlotter();
            routePlotter.plotRoute(result.get().getSystemName());
            String reminder = StringUtls.localizedLlm("handler.miningSite.found", result.get().getSystemName(), result.get().getBodyName());
            ReminderManager.getInstance().setReminder(reminder, result.get().getSystemName());
            GameEventBus.publish(new AiVoxResponseEvent(reminder));
        } else {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.miningSite.notFoundInRange")));
        }
    }
}
