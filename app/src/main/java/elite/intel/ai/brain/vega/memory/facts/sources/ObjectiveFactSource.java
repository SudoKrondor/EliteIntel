package elite.intel.ai.brain.vega.memory.facts.sources;

import elite.intel.ai.brain.vega.memory.facts.MemoryFactContext;
import elite.intel.ai.brain.vega.memory.facts.MemoryFactSource;
import elite.intel.ai.brain.vega.memory.facts.RegisterMemoryFactSource;
import elite.intel.ai.brain.vega.model.ThoughtSource;
import elite.intel.db.dao.DestinationReminderDao.Reminder;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.ReminderContact;
import elite.intel.session.PlayerSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Always-on fact source for the errand the commander is on: the standing reminder, which is the app's own record of
 * why this trip is happening. It is set by the commands that plan a trip (a trade route, a broker or trader search)
 * as well as by the commander directly, so it answers "what are we doing here" without the model having to ask for
 * it - the same errand the HUD's route card and {@code query_reminder} report, so the three cannot disagree.
 * <p>
 * Ambient for a commander turn rather than gated on a subject, because an objective is context for every turn and
 * not an answer to one question. It is silent whenever no reminder is set, which is the normal state between jobs.
 * The stored sentence is prose in the language it was created in and goes in last, so the shared line cap trims it
 * rather than the destination and contact that a model can act on.
 *
 * <h2>Only while the errand is still ahead</h2>
 * A reminder outlives the trip it was set for: it stays in the table until something overwrites or clears it. Once
 * the commander has left, presenting it as the <em>current</em> objective grounds every later turn in a job that is
 * already done. The HUD's route card refuses the same reminder for the same reason (see
 * {@code ShipRouteObjectiveSource.errandAt}), so a card and a spoken line would also disagree about whether there
 * is an objective at all.
 * <p>
 * The errand counts as current while the commander is in its system or has a route plotted to it, and a reminder
 * that names no system is always current because there is no place for it to be stale about. Otherwise this source
 * stays silent, and {@code query_reminder} still reports the standing errand when the commander asks for it.
 */
@RegisterMemoryFactSource
public final class ObjectiveFactSource implements MemoryFactSource {

    /**
     * Provenance label for the {@code <fact source="...">} attribute.
     */
    private static final String ID = "objective";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isRelevant(MemoryFactContext context) {
        return context != null && context.source() == ThoughtSource.COMMANDER;
    }

    @Override
    public boolean isAmbient() {
        return true;
    }

    @Override
    public List<String> factsFor(MemoryFactContext context) {
        Reminder reminder = ReminderManager.getInstance().getReminder();
        if (reminder == null) {
            return List.of();
        }
        if (!isAhead(FactLine.value(reminder.getStarSystem()),
                PlayerSession.getInstance().getPrimaryStarName(),
                ShipRouteManager.getInstance().getDestination())) {
            return List.of();
        }
        String fact = format(
                FactLine.value(reminder.getStarSystem()),
                FactLine.value(reminder.getStationName()),
                ReminderContact.parseOrNull(reminder.getContact()),
                FactLine.value(reminder.getReminder()));
        return fact.isBlank() ? List.of() : List.of(fact);
    }

    /**
     * Whether the errand is still ahead of the commander: they are in its system, or a route is plotted to it. A
     * reminder with no system of its own is not about a place and is always current. Pure and package-visible so
     * the rule can be pinned without a session, a route or a database.
     */
    static boolean isAhead(String reminderSystem, String currentSystem, String routeDestination) {
        if (reminderSystem == null) {
            return true;
        }
        return reminderSystem.equalsIgnoreCase(FactLine.value(currentSystem))
                || reminderSystem.equalsIgnoreCase(FactLine.value(routeDestination));
    }

    /**
     * Builds the single objective line, destination first and the saved sentence last. Returns empty when the
     * reminder holds nothing at all. Pure and package-visible for testing.
     */
    static String format(String system, String station, ReminderContact contact, String errand) {
        List<String> parts = new ArrayList<>();
        if (system != null) {
            parts.add(system);
        }
        if (station != null) {
            parts.add("station " + station);
        }
        if (contact != null) {
            parts.add(errand(contact));
        }
        if (errand != null) {
            parts.add(errand);
        }
        return parts.isEmpty() ? "" : FactLine.capped("current objective", parts);
    }

    /**
     * What the commander is at the destination to do, in the game's own words for the service.
     * <p>
     * Written out per contact rather than derived from the constant name, for the two reasons
     * {@link ReminderContact} gives itself: the constant is an identifier and not a label, so spelling it out would
     * let a rename silently change what the model is told, and a new constant would arrive with words nobody chose.
     * The switch is exhaustive, so adding one is a compile error here rather than a surprise in a prompt. Not read
     * from the overlay's bundle: that is the commander's UI language, and the facts block is English for the model.
     */
    static String errand(ReminderContact contact) {
        return switch (contact) {
            case MATERIAL_TRADER_RAW -> "see the raw material trader";
            case MATERIAL_TRADER_MANUFACTURED -> "see the manufactured material trader";
            case MATERIAL_TRADER_ENCODED -> "see the encoded data trader";
            case TECHNOLOGY_BROKER_HUMAN -> "see the human technology broker";
            case TECHNOLOGY_BROKER_GUARDIAN -> "see the guardian technology broker";
            case INTERSTELLAR_FACTORS -> "see interstellar factors";
            case VISTA_GENOMICS -> "sell exobiology data at Vista Genomics";
            case REFUEL -> "refuel there";
        };
    }
}
