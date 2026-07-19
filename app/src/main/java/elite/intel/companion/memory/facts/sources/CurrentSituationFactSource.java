package elite.intel.companion.memory.facts.sources;

import elite.intel.companion.memory.facts.MemoryFactContext;
import elite.intel.companion.memory.facts.MemoryFactSource;
import elite.intel.companion.memory.facts.RegisterMemoryFactSource;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.i18n.EventsTextProvider;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSession;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;

import java.util.List;

/**
 * Always-on live fact for the commander's physical situation. It gives every COMMANDER prompt an explicit state
 * such as docked, on foot, landed, normal-space flight, or supercruise instead of making the model infer it from
 * the offered tools.
 */
@RegisterMemoryFactSource
public final class CurrentSituationFactSource implements MemoryFactSource {

    private static final String ID = "situation";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isRelevant(MemoryFactContext context) {
        return context != null && context.source() == ThoughtSource.COMMANDER;
    }

    @Override
    public List<String> factsFor(MemoryFactContext context) {
        return List.of(format(currentSituation()));
    }

    /** Formats the canonical {@link PlayerSituation} in stable English for the model context. */
    static String format(PlayerSituation situation) {
        return EventsTextProvider.getText(Language.EN, "game.situation.label") + ": "
                + EventsTextProvider.getText(Language.EN, situation.i18nKey());
    }

    private static PlayerSituation currentSituation() {
        Status status = Status.getInstance();
        PlayerSituation flagOnly = status.getSituation(null);
        if (flagOnly != PlayerSituation.IN_SHIP_DEEP_SPACE
                && flagOnly != PlayerSituation.IN_SHIP_ORBIT) {
            return flagOnly;
        }

        try {
            LocationDto location = LocationManager.getInstance()
                    .findByLocationData(PlayerSession.getInstance().getLocationData());
            return status.getSituation(location);
        } catch (RuntimeException ignored) {
            // Location only refines normal-space flight into a planetary ring; flags still give a truthful fallback.
            return flagOnly;
        }
    }
}
