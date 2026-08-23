package elite.intel.ui.overlay;

import elite.intel.db.dao.DestinationReminderDao.Reminder;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.ReminderContact;
import elite.intel.gameapi.gamestate.dtos.NavRouteDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Projects the plotted route into a HUD objective: where the commander is going, what the next hop is, and how
 * many jumps are left.
 * <p>
 * <b>The route is the card.</b> When the app worked the destination out itself (a material trader, a technology
 * broker, interstellar factors, Vista Genomics) it also left a standing reminder naming the port and the
 * contact, and that detail is added here, but <em>only</em> when the route is actually plotted to that same
 * system. A reminder is cleared only on request, so an old one lingers in the database indefinitely; letting it
 * describe the card on its own meant a route set to Groombridge 34 was announced as a Sterope II trade errand
 * from an hour earlier. Tying the detail to the route means a stale reminder can never be shown at all.
 * <p>
 * Ranked {@link HudObjective#PRIORITY_AMBIENT} and registered last, so it fills the card only when nothing else
 * has anything to say. A mission, a trade route or a mining run all describe what the commander is doing; this
 * only knows where they are pointed, which makes it the weakest claim on the one card that fits.
 * <p>
 * The jump count is {@code getOrderedRoute().size()}, the same figure {@code JumpCompletedSubscriber} speaks as
 * "jumps left" on arrival. Taking it from anywhere else would let the card and the voice disagree about one
 * route. Legs are the systems still ahead: the game rewrites NavRoute.json as they are flown, and the system
 * the commander is standing in is not among them.
 */
public class ShipRouteObjectiveSource implements HudObjectiveSource {

    private final Supplier<String> destination;
    private final Supplier<List<NavRouteDto>> route;
    private final Supplier<Reminder> reminder;

    public ShipRouteObjectiveSource() {
        this(() -> ShipRouteManager.getInstance().getDestination(),
                () -> ShipRouteManager.getInstance().getOrderedRoute(),
                () -> ReminderManager.getInstance().getReminder());
    }

    /**
     * Seam for tests.
     */
    ShipRouteObjectiveSource(Supplier<String> destination, Supplier<List<NavRouteDto>> route,
                             Supplier<Reminder> reminder) {
        this.destination = destination;
        this.route = route;
        this.reminder = reminder;
    }

    @Override
    public Optional<HudObjective> currentObjective() {
        String finalSystem = trimToNull(destination.get());
        if (finalSystem == null) return Optional.empty();

        List<NavRouteDto> legs = route.get();
        int jumps = legs == null ? 0 : legs.size();
        Reminder errand = errandAt(finalSystem);
        ReminderContact contact = errand == null ? null : ReminderContact.parseOrNull(errand.getContact());

        List<HudRow> rows = new ArrayList<>();
        rows.add(HudRow.of(HudText.get("overlay.card.row.destination"), finalSystem.toUpperCase(),
                HudRow.State.GOOD));
        station(errand).ifPresent(station ->
                rows.add(HudRow.of(HudText.get("overlay.card.row.station"), station.toUpperCase())));
        specialisation(contact).ifPresent(type ->
                rows.add(HudRow.of(HudText.get("overlay.card.row.type"), type)));
        nextWaypoint(legs, finalSystem).ifPresent(next ->
                rows.add(HudRow.of(HudText.get("overlay.card.row.next"), next.toUpperCase())));
        if (jumps > 0) {
            rows.add(HudRow.of(HudText.get("overlay.card.row.jumps"), String.valueOf(jumps)));
        }

        // No subtitle: the destination is a labelled row, and naming it twice on one card reads as an error.
        return Optional.of(new HudObjective(
                "ship-route",
                title(contact),
                null,
                rows,
                HudObjective.PRIORITY_AMBIENT));
    }

    /**
     * The standing reminder, but only when it describes the system this route ends at. Anything else is an
     * errand from an earlier journey and has nothing to say about where the commander is going now.
     */
    private Reminder errandAt(String finalSystem) {
        Reminder stored = reminder.get();
        if (stored == null) return null;
        String system = trimToNull(stored.getStarSystem());
        return system != null && system.equalsIgnoreCase(finalSystem) ? stored : null;
    }

    private static Optional<String> station(Reminder errand) {
        return errand == null ? Optional.empty() : Optional.ofNullable(trimToNull(errand.getStationName()));
    }

    /**
     * The headline names who the commander is going to see when the app worked the destination out, and
     * otherwise just says a route is plotted.
     */
    static String title(ReminderContact contact) {
        if (contact == null) return HudText.get("overlay.card.title.route");
        return switch (contact) {
            case MATERIAL_TRADER_RAW, MATERIAL_TRADER_MANUFACTURED, MATERIAL_TRADER_ENCODED ->
                    HudText.get("overlay.card.title.materialTrader");
            case TECHNOLOGY_BROKER_HUMAN, TECHNOLOGY_BROKER_GUARDIAN ->
                    HudText.get("overlay.card.title.technologyBroker");
            case INTERSTELLAR_FACTORS -> HudText.get("overlay.card.title.interstellarFactors");
            case VISTA_GENOMICS -> HudText.get("overlay.card.title.vistaGenomics");
            case REFUEL -> HudText.get("overlay.card.title.refuel");
        };
    }

    /**
     * Which flavour of the contact, for the families that have several. Kept out of the title so the headline
     * stays a short phrase and the long word lands in a row, drawn right-aligned with room to spare.
     */
    static Optional<String> specialisation(ReminderContact contact) {
        if (contact == null) return Optional.empty();
        return switch (contact) {
            case MATERIAL_TRADER_RAW -> Optional.of(HudText.get("overlay.card.value.materialRaw"));
            case MATERIAL_TRADER_MANUFACTURED -> Optional.of(HudText.get("overlay.card.value.materialManufactured"));
            case MATERIAL_TRADER_ENCODED -> Optional.of(HudText.get("overlay.card.value.materialEncoded"));
            case TECHNOLOGY_BROKER_HUMAN -> Optional.of(HudText.get("overlay.card.value.brokerHuman"));
            case TECHNOLOGY_BROKER_GUARDIAN -> Optional.of(HudText.get("overlay.card.value.brokerGuardian"));
            case INTERSTELLAR_FACTORS, VISTA_GENOMICS, REFUEL -> Optional.empty();
        };
    }

    /**
     * The next system to jump to, or empty when that is the destination itself: on the last hop the destination
     * row already names it, and a card that says the same thing twice reads as a mistake.
     */
    static Optional<String> nextWaypoint(List<NavRouteDto> legs, String finalSystem) {
        if (legs == null || legs.isEmpty()) return Optional.empty();
        String next = trimToNull(legs.getFirst().getName());
        if (next == null || next.equalsIgnoreCase(finalSystem)) return Optional.empty();
        return Optional.of(next);
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
