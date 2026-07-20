package elite.intel.ai.brain.actions.handlers.queries.carrier;

import elite.intel.db.managers.FleetCarrierManager;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.db.managers.SquadronCarrierManager;
import elite.intel.db.managers.SquadronCarrierRouteManager;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * One carrier's data, whichever carrier it is. The four managers behind a carrier (fleet/squadron x carrier/route)
 * expose the same shape under different names; this hides that split so a query reads its data once instead of
 * once per owner.
 */
public final class CarrierView {

    private final CarrierOwnership ownership;
    private final Supplier<CarrierDataDto> data;
    private final Supplier<Map<Integer, CarrierJump>> route;
    private final Supplier<Integer> totalFuelRequired;
    private final Supplier<String> finalDestination;

    /**
     * Package-private so a test can inject fixtures in place of the four carrier managers.
     */
    CarrierView(CarrierOwnership ownership,
                Supplier<CarrierDataDto> data,
                Supplier<Map<Integer, CarrierJump>> route,
                Supplier<Integer> totalFuelRequired,
                Supplier<String> finalDestination) {
        this.ownership = ownership;
        this.data = data;
        this.route = route;
        this.totalFuelRequired = totalFuelRequired;
        this.finalDestination = finalDestination;
    }

    /**
     * The carrier the commander named in this utterance; their own unless they said "squadron".
     */
    public static CarrierView forUtterance(String utterance) {
        return of(CarrierOwnership.resolve(utterance));
    }

    /**
     * Reads the managers lazily, so constructing a view touches no database.
     */
    public static CarrierView of(CarrierOwnership ownership) {
        return switch (ownership) {
            case FLEET -> new CarrierView(ownership,
                    () -> FleetCarrierManager.getInstance().get(),
                    () -> FleetCarrierRouteManager.getInstance().getFleetCarrierRoute(),
                    () -> FleetCarrierRouteManager.getInstance().getTotalFuelRequired(),
                    () -> FleetCarrierRouteManager.getInstance().getFinalDestination());
            case SQUADRON -> new CarrierView(ownership,
                    () -> SquadronCarrierManager.getInstance().get(),
                    () -> SquadronCarrierRouteManager.getInstance().getSquadronCarrierRoute(),
                    () -> SquadronCarrierRouteManager.getInstance().getTotalFuelRequired(),
                    () -> SquadronCarrierRouteManager.getInstance().getFinalDestination());
        };
    }

    public CarrierOwnership ownership() {
        return ownership;
    }

    /**
     * Never null: an unknown carrier reads as an empty {@link CarrierDataDto}.
     */
    public CarrierDataDto data() {
        return data.get();
    }

    /**
     * Whether the game has told us anything about this carrier yet. A carrier the commander does not own, or one
     * whose management panel has never been opened this session, reads as all-zero rather than as an absence.
     */
    public boolean hasData() {
        CarrierDataDto carrier = data();
        return carrier.getTotalBalance() != 0 || carrier.getFuelLevel() != 0;
    }

    /**
     * The plotted route in jump order; empty when no route is plotted. Sorted by leg, as a type guarantee: the
     * legs already travelled are deleted from the store, so the remaining keys are neither dense nor 1-based and
     * callers must not read position out of them.
     */
    public SortedMap<Integer, CarrierJump> route() {
        return new TreeMap<>(route.get());
    }

    /**
     * Tritium the full plotted route needs, in tons; zero when no route is plotted.
     */
    public int totalFuelRequired() {
        Integer required = totalFuelRequired.get();
        return required == null ? 0 : required;
    }

    /**
     * The plotted route's destination system, or null when no route is plotted.
     */
    public String finalDestination() {
        return finalDestination.get();
    }
}