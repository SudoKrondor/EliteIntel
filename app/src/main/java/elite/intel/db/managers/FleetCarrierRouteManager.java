package elite.intel.db.managers;

import elite.intel.db.dao.FleetCarrierRouteDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;
import elite.intel.session.PlayerSession;

import java.util.*;

/**
 * The plotted fleet carrier route, which by definition holds only the legs still to fly.
 *
 * <p>Every read and every write passes through {@link CarrierRouteLegs#stillToFly}, so the system
 * the carrier is sitting in can never be handed out as a leg — not from a fresh plot, not from a
 * route the previous run left in the table, and not from a leg an arrival failed to clear. See
 * {@link CarrierRouteLegs} for the rule itself.
 */
public class FleetCarrierRouteManager {

    private static volatile FleetCarrierRouteManager instance;

    private FleetCarrierRouteManager() {
        // enforce singleton pattern.
    }

    public static FleetCarrierRouteManager getInstance() {
        if (instance == null) {
            synchronized (FleetCarrierRouteManager.class) {
                if (instance == null) {
                    instance = new FleetCarrierRouteManager();
                }
            }
        }
        return instance;
    }

    /**
     * Stores a freshly plotted route, keyed by the plotter's leg order.
     *
     * <p>An empty plot is a failed plot (Spansh unreachable, no route found) and leaves the existing
     * route alone. A plot that is nothing but the carrier's current system means the voyage is over,
     * and clears it.
     */
    public void setFleetCarrierRoute(Map<Integer, CarrierJump> fleetCarrierRoute) {
        if (fleetCarrierRoute == null || fleetCarrierRoute.isEmpty()) return;

        List<CarrierJump> plotted = new ArrayList<>(new TreeMap<>(fleetCarrierRoute).values());
        List<CarrierJump> remaining = CarrierRouteLegs.stillToFly(plotted, currentCarrierSystem());

        Database.withDao(FleetCarrierRouteDao.class, dao -> {
            dao.replaceAll(remaining.stream().map(FleetCarrierRouteManager::dtoToEntity).toList());
            return null;
        });
    }

    /**
     * The legs still to fly, keyed 1..n with leg 1 the next jump.
     */
    public Map<Integer, CarrierJump> getFleetCarrierRoute() {
        String here = currentCarrierSystem();
        List<CarrierJump> stored = Database.withDao(FleetCarrierRouteDao.class, dao -> dao.getAll().stream()
                .sorted(Comparator.comparing(FleetCarrierRouteDao.FleetCarrierRouteLeg::getLeg))
                .map(FleetCarrierRouteManager::entityToDto)
                .toList());

        Map<Integer, CarrierJump> result = new TreeMap<>();
        for (CarrierJump leg : CarrierRouteLegs.stillToFly(stored, here)) {
            result.put(leg.getLeg(), leg);
        }
        return result;
    }

    /**
     * WHY summed in Java rather than by the table: the tritium still to burn covers the legs still to
     * fly, and a {@code SUM(fuelUsed)} over the table would also charge for legs already behind us.
     */
    public Integer getTotalFuelRequired() {
        return getFleetCarrierRoute().values().stream().mapToInt(CarrierJump::getFuelUsed).sum();
    }

    private static CarrierJump entityToDto(FleetCarrierRouteDao.FleetCarrierRouteLeg leg) {
        CarrierJump jump = new CarrierJump();
        jump.setLeg(leg.getLeg());
        jump.setDistance(leg.getDistance());
        jump.setFuelUsed(leg.getFuelUsed());
        jump.setHasIcyRing(leg.getHasIcyRing());
        jump.setPristine(leg.getPristine());
        jump.setRemainingFuel(leg.getRemainingFuel());
        jump.setSystemName(leg.getSystemName());
        jump.setX(leg.getX());
        jump.setY(leg.getY());
        jump.setZ(leg.getZ());
        return jump;
    }

    private static FleetCarrierRouteDao.FleetCarrierRouteLeg dtoToEntity(CarrierJump jump) {
        FleetCarrierRouteDao.FleetCarrierRouteLeg leg = new FleetCarrierRouteDao.FleetCarrierRouteLeg();
        leg.setLeg(jump.getLeg());
        leg.setDistance(jump.getDistance());
        leg.setFuelUsed(jump.getFuelUsed());
        leg.setHasIcyRing(jump.getHasIcyRing());
        leg.setPristine(jump.isPristine());
        leg.setRemainingFuel(jump.getRemainingFuel());
        leg.setSystemName(CarrierRouteLegs.normalise(jump.getSystemName()));
        leg.setX(jump.getX());
        leg.setY(jump.getY());
        leg.setZ(jump.getZ());
        return leg;
    }

    /**
     * Records that the carrier has arrived at {@code starName}: that leg and every leg before it are
     * behind us. Idempotent, and a no-op for a system the route never mentioned.
     */
    public void removeLeg(String starName) {
        String arrival = CarrierRouteLegs.normalise(starName);
        if (arrival == null) return;

        Database.withDao(FleetCarrierRouteDao.class, dao -> {
            List<CarrierJump> stored = dao.getAll().stream()
                    .sorted(Comparator.comparing(FleetCarrierRouteDao.FleetCarrierRouteLeg::getLeg))
                    .map(FleetCarrierRouteManager::entityToDto)
                    .toList();

            List<CarrierJump> remaining = CarrierRouteLegs.stillToFly(stored, arrival);
            if (remaining.size() == stored.size()) return null; // off-route arrival: nothing consumed

            dao.replaceAll(remaining.stream().map(FleetCarrierRouteManager::dtoToEntity).toList());
            return null;
        });
    }

    public void clear() {
        Database.withDao(FleetCarrierRouteDao.class, dao -> {
            dao.clear();
            return null;
        });
    }

    /**
     * The plotted leg that ends at the given system, or null when the system is not on the route.
     *
     * <p>Deliberately reads the table unfiltered: the arrival handler calls this for the system the
     * carrier just reached, precisely the leg {@link #getFleetCarrierRoute()} has stopped reporting,
     * and needs its tritium cost and coordinates before {@link #removeLeg} consumes it.
     *
     * <p>WHY: this used to answer with an empty CarrierJump instead of null, which silently made
     * every {@code == null} check at the call sites unreachable. Callers could not tell "arrived at
     * a plotted leg" from "arrived somewhere off-route", and read fuelUsed 0 and coordinates 0,0,0
     * from the placeholder.
     */
    public CarrierJump findByPrimaryStar(String starSystem) {
        String system = CarrierRouteLegs.normalise(starSystem);
        if (system == null) return null;
        return Database.withDao(FleetCarrierRouteDao.class, dao -> {
            FleetCarrierRouteDao.FleetCarrierRouteLeg leg = dao.findByPrimaryStarName(system);
            return leg == null ? null : entityToDto(leg);
        });
    }

    /**
     * Where the route ends, or null once the carrier is there.
     */
    public String getFinalDestination() {
        return getFleetCarrierRoute().values().stream()
                .max(Comparator.comparingInt(CarrierJump::getLeg))
                .map(CarrierJump::getSystemName)
                .orElse(null);
    }

    /**
     * WHY read outside the route DAO block: it opens a connection of its own.
     */
    private static String currentCarrierSystem() {
        return PlayerSession.getInstance().getCurrentFleetCarrierSystem();
    }
}
