package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.RoutePlotter;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.FuzzySearch;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.EventBusManager;
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

    private static final int MAX_DEFAULT_RANGE = 1000;

    @Override
    public String id() {
        return CommandIds.FIND_MINING_SITE;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();
        if (!status.isInMainShip()) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.miningSite.boardShip")));
            return;
        }

        JsonElement mat = params.get("key");
        JsonElement distance = params.get("max_distance");
        if (mat == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.miningSite.didNotCatch")));
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
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.miningSite.notFound")));
            return;
        }

        Optional<StellarObjectSearchResultDto.Result> result = miningLocations.getResults().stream().findFirst();
        if (result.isPresent()) {
            RoutePlotter routePlotter = new RoutePlotter();
            routePlotter.plotRoute(result.get().getSystemName());
            String reminder = StringUtls.localizedLlm("handler.miningSite.found", result.get().getSystemName(), result.get().getBodyName());
            ReminderManager.getInstance().setReminder(reminder, result.get().getSystemName());
            EventBusManager.publish(new AiVoxResponseEvent(reminder));
        } else {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.miningSite.notFoundInRange")));
        }
    }
}
