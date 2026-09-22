package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.ReminderContact;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.search.spansh.station.SearchRadii;
import elite.intel.gameapi.search.spansh.station.outfitting.OutfittingHit;
import elite.intel.gameapi.search.spansh.station.outfitting.OutfittingSearch;
import elite.intel.gameapi.search.spansh.station.outfitting.StockedModule;
import elite.intel.gameapi.search.spansh.station.outfitting.WantedModule;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

import java.util.List;
import java.util.stream.Collectors;

/**
 * "Where do I buy this module, and take me there" - the outfitting twin of {@link CommodityTradeSearch}.
 * <p>
 * Reached from the same spoken command as a commodity, once the words turned out to name a module: the
 * search, the spoken answer, the reminder and the route are the same job as for a good, with the hold and
 * the trade profile left out because a module takes no cargo space and has no supply to size a load to.
 */
final class ShipModuleSearch {

    private ShipModuleSearch() {
    }

    /**
     * Finds the outfitting to buy {@code wanted} at and plots a route to it.
     *
     * @param distance      search radius in light years
     * @param returnClosest true for the nearest shop, false for the cheapest listing
     * @return a localized message when the search could not be run, found nothing, or ended without a new
     * route being plotted; null when the answer has already been spoken and the route is under way
     */
    static String findAndPlot(WantedModule wanted, int distance, boolean returnClosest) {
        LocationDao.Coordinates coordinates = LocationManager.getInstance().getGalacticCoordinates();
        if (coordinates == null) {
            return StringUtls.localizedResponse("handler.module.noCoords");
        }
        String asked = spoken(wanted);
        String searchMode = StringUtls.localizedResponse(returnClosest ? "handler.module.modeNearest" : "handler.module.modeBest");
        VegaRuntime.narrator().filler(StringUtls.localizedResponse("handler.module.searching", asked, distance, searchMode), false);

        // WHY: same pad rule the trade route and trader searches apply - a large ship is only shown ports it
        // can dock at, otherwise the route ends at an outpost it has to turn around and leave.
        List<OutfittingHit> found = OutfittingSearch.find(wanted, coordinates.x(), coordinates.y(), coordinates.z(),
                PlayerSession.getInstance().getPrimaryStarName(), distance,
                ShipManager.getInstance().requireLargePad(), returnClosest);
        if (found.isEmpty()) {
            // How far it actually looked, not what was asked for: the search widens on its own, and reporting
            // the radius the commander named would understate the sweep.
            return StringUtls.localizedResponse("handler.module.noMatch", asked, SearchRadii.widening(distance).getLast());
        }

        OutfittingHit hit = found.getFirst();
        String stocked = stockedList(wanted, hit.modules());
        String answer = StringUtls.localizedResponse("handler.module.headTo",
                hit.starSystem(), hit.stationName(), hit.stationType(), wanted.label(), stocked);
        // The search widens the radius rather than call a module nonexistent, so when the answer lies outside
        // what the commander asked for he is told.
        if (hit.distanceLy() > distance) {
            answer += " " + StringUtls.localizedResponse("handler.commodity.beyondRange", distance, Math.round(hit.distanceLy()));
        }
        VegaRuntime.narrator().filler(answer, false);

        // The errand as it is read back on arrival in that system: the port, the module and what it stocks.
        ReminderManager.getInstance().setReminder(
                StringUtls.localizedResponse("handler.module.reminder", wanted.label(), hit.stationName(), hit.stationType(), stocked),
                hit.starSystem(), hit.stationName(), ReminderContact.OUTFITTING);

        // The plotter's own outcome, not a discarded return: it declines to re-plot a route the commander is
        // already flying, and that is worth hearing rather than a silent no-map.
        return new RoutePlotter().plotRoute(hit.starSystem());
    }

    /**
     * "6D at 449,430 credits, 6E at 107,860 credits" - every listing that fits, cheapest first, so a
     * commander who named a size but no class hears which classes are on the shelf. A broad request names
     * the module on each listing - "Beam Laser 3E at 74,650 credits" - since the station may stock any of
     * the family.
     */
    private static String stockedList(WantedModule wanted, List<StockedModule> modules) {
        return modules.stream()
                .map(module -> StringUtls.localizedResponse("handler.module.stocked",
                        wanted.isFamily() ? module.name() + " " + module.designation() : module.designation(),
                        module.price()))
                .collect(Collectors.joining(", "));
    }

    /**
     * The request as the commander hears it echoed: "Gimbal Beam Laser", "Fuel Scoop 6B", "Fuel Scoop 6".
     */
    private static String spoken(WantedModule wanted) {
        StringBuilder spoken = new StringBuilder();
        if (wanted.mount() != null) spoken.append(wanted.mount()).append(' ');
        spoken.append(wanted.label());
        if (wanted.size() != null || wanted.rating() != null) {
            spoken.append(' ');
            if (wanted.size() != null) spoken.append(wanted.size());
            if (wanted.rating() != null) spoken.append(wanted.rating());
        }
        return spoken.toString();
    }
}
