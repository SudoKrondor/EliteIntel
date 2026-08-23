package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.ReminderContact;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.gameapi.search.spansh.station.refuel.RefuelStation;
import elite.intel.gameapi.search.spansh.station.refuel.RefuelStationSearch;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

import java.util.List;

/**
 * "I have no fuel scoop and I need fuel" - finds the nearest station that sells it and plots a route there.
 * <p>
 * Fuel is bought at the station services menu rather than off a commodity market, so no market search finds
 * it; {@link RefuelStationSearch} asks Spansh about the Refuel service itself, and about the pads, because a
 * station a ship cannot land on has no fuel as far as that ship is concerned.
 * <p>
 * WHY a station in the CURRENT system is a perfectly good answer here, where the sibling searches
 * ({@code CurrentSystemFilter}) throw those away: the others end in a route plot, and a route to the system
 * you are standing in is not a destination. Fuel is the one errand where "you are already there" is the best
 * possible news - so that case is spoken and the route plot skipped, rather than the answer being discarded
 * and the commander sent to burn fuel reaching a neighbour.
 */
@RegisterCommand
public final class FindFuelStationCommand implements IntelCommand {
    public static final String ID = "find_fuel_station";

    /**
     * The radius to search while the ship's jump range is not known yet. Roughly a well-equipped mid-size
     * hull's range, and wide enough that the first sweep normally answers inside the bubble.
     */
    private static final int DEFAULT_RANGE_LY = 50;

    private static final String PARAM_MAX_DISTANCE = "max_distance";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private final LocationManager locationManager = LocationManager.getInstance();
    private final ReminderManager reminderManager = ReminderManager.getInstance();
    private final ShipManager shipManager = ShipManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec maxDistance = new ActionParameterSpec(
                PARAM_MAX_DISTANCE, "number", false,
                "Maximum search radius in light years (ly). If omitted, the ship's jump range is used.",
                List.of("30", "80"),
                "Extract the distance limit in light years if the commander states one, ALWAYS as digits: "
                        + "the 30 in 'find a fuel station within 30 ly', and 200 for 'within two hundred light years'.");
        maxDistance.validate();
        return List.of(maxDistance);
    }

    @Override
    public String llmDescription() {
        return "Find the nearest station where the ship can dock and buy fuel, and plot a route to it. "
                + "For a ship with no fuel scoop, which cannot refuel at a star. "
                + "Not a commodity market search: fuel is a station service, not cargo.";
    }

    @Override
    public String id() {
        return ID;
    }

    /// Route plotting available anywhere in the game
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        LocationDao.Coordinates coordinates = locationManager.getGalacticCoordinates();
        if (!isKnownPosition(coordinates)) {
            return StringUtls.localizedResponse("handler.refuel.noCoords");
        }

        int jumpRange = jumpRangeLy();
        // Through the shared reader like every other "find X within Y light years" command, so a radius
        // spoken in words ("two hundred") is honoured instead of silently becoming the default.
        int distance = GetNumberFromParam.extractRangeParameter(params, searchRadiusLy(jumpRange)).intValue();

        List<RefuelStation> found = RefuelStationSearch.nearest(
                coordinates.x(), coordinates.y(), coordinates.z(), distance, shipManager.requiredPadSize());
        if (found.isEmpty()) {
            // How far it actually looked, not what was asked for: the search widens twice on its own, and
            // reporting the radius the commander named would understate the sweep by a factor of twenty.
            return StringUtls.localizedResponse("handler.refuel.notFound",
                    RefuelStationSearch.radiiToTry(distance).getLast());
        }

        RefuelStation station = found.getFirst();
        boolean alreadyHere = isHere(station);
        String announcement = alreadyHere
                ? StringUtls.localizedResponse("handler.refuel.here",
                station.stationName(), Math.round(station.arrivalLs()))
                : plottedAnnouncement(station, jumpRange);

        reminderManager.setReminder(announcement, station.starSystem(), station.stationName(), ReminderContact.REFUEL);
        if (!alreadyHere) {
            new RoutePlotter().plotRoute(station.starSystem());
        }
        return announcement;
    }

    /**
     * The answer for a station the commander has to travel to, with a warning when it is further than one
     * jump.
     * <p>
     * The search widens rather than call the galaxy dry, so an answer past what the ship can cross in a
     * single hop is worth saying out loud: a ship low on fuel may not get there, and being told the number
     * is what lets the commander decide to call for a fuel delivery instead. Silent when the jump range is
     * not known yet, because a warning measured against a guess is worse than no warning.
     */
    private String plottedAnnouncement(RefuelStation station, int jumpRange) {
        String announcement = StringUtls.localizedResponse("handler.refuel.plotted",
                station.stationName(), station.starSystem(), Math.round(station.distanceLy()));
        if (jumpRange > 0 && station.distanceLy() > jumpRange) {
            announcement += " " + StringUtls.localizedResponse("handler.refuel.beyondJumpRange", jumpRange);
        }
        return announcement;
    }

    /**
     * Sol really does sit at 0,0,0 - and so does a position we have recorded nothing for, because the unset
     * value and the real one are the same three zeroes. Only the star's name separates them, and the
     * difference matters more here than anywhere: a commander short of fuel out in the black would otherwise
     * be sent to the nearest station to SOL, thousands of light years away, and told it was nearby.
     */
    private boolean isKnownPosition(LocationDao.Coordinates coordinates) {
        if (coordinates == null) return false;
        boolean atOrigin = coordinates.x() == 0 && coordinates.y() == 0 && coordinates.z() == 0;
        return !atOrigin || "Sol".equalsIgnoreCase(playerSession.getPrimaryStarName());
    }

    /**
     * Whether the station is in the system the commander is already in. Both tests, because either input can
     * be the stale one: a reported distance of zero means the same system without trusting session state at
     * all, and the name comparison still catches it when the coordinates were what lagged.
     */
    private boolean isHere(RefuelStation station) {
        String current = playerSession.getPrimaryStarName();
        return station.distanceLy() <= 0
                || (current != null && current.strip().equalsIgnoreCase(station.starSystem().strip()));
    }

    /**
     * What the ship can cross in one jump, or zero while the game has not sent a loadout. Zero is kept as
     * zero rather than defaulted, because this figure is also spoken back to the commander as "the jump
     * range of this ship" and a default would be a made-up number in that sentence.
     */
    private int jumpRangeLy() {
        ShipLoadOutDto loadout = playerSession.getShipLoadout();
        return loadout == null ? 0 : (int) loadout.getMaxJumpRange();
    }

    /**
     * The radius to search when the commander names none: one jump, which is as far as a ship short of fuel
     * can simply go. Falls back to {@value #DEFAULT_RANGE_LY} ly when the jump range is unknown, because a
     * radius of zero finds nothing however many times it is widened.
     */
    private int searchRadiusLy(int jumpRange) {
        return jumpRange < 1 ? DEFAULT_RANGE_LY : jumpRange;
    }
}
