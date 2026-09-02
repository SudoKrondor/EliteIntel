package elite.intel.gameapi;

import java.util.List;

/**
 * Decides whether a surface vehicle can be deployed right now, and from which bay.
 * <p>
 * <b>Why this is a class and not a few ifs inside the command.</b> Every input is something the command
 * cannot produce in a test - the ship's loadout, the commander's saved bay settings, the live flags and
 * the altitude - and every output is a sentence the commander hears. Kept apart, the whole decision table
 * is exercised without a game, a database or a keystroke, and the command is left with what it is actually
 * for: pressing keys.
 * <p>
 * <b>Why a refusal is never silence.</b> Each way this can fail is a different thing for the commander to
 * do about it - configure the bays, land, climb, descend, say a bay that exists, carry the vehicle they
 * asked for - so each is its own {@link Refusal} rather than a shared "cannot do that now". A deployment
 * that quietly does nothing looks like the app missing the command.
 */
public final class SurfaceVehicleDeployment {

    /**
     * How many bays the largest hangar holds, and therefore how many the commander can configure.
     */
    public static final int MAX_BAYS = 4;

    /**
     * The bay used when the commander names neither a bay nor a vehicle.
     */
    public static final int DEFAULT_BAY = 1;

    /**
     * Why a deployment was refused. Each maps to its own spoken line.
     */
    public enum Refusal {
        /**
         * The ship is not carrying a planetary vehicle hangar at all.
         */
        NO_VEHICLE_BAY,
        /**
         * A hangar is fitted but the commander has never said what is in it.
         */
        BAYS_NOT_CONFIGURED,
        /**
         * A bay number was asked for that no hangar has. Carries the number that was heard.
         */
        NO_SUCH_BAY,
        /**
         * The bay exists and is configurable, but this one was left empty.
         */
        BAY_EMPTY,
        /**
         * A vehicle was asked for by name, and no bay holds one.
         */
        VEHICLE_NOT_LOADED,
        /**
         * A bay and a vehicle were both named, and that bay holds something else.
         */
        BAY_HOLDS_OTHER,
        /**
         * A Scarab or Scorpion, and the ship is not sitting on the surface.
         */
        NOT_LANDED,
        /**
         * A Rhino, and the ship is not hovering within the band it drops from.
         */
        WRONG_ALTITUDE
    }

    /**
     * The outcome. Either a bay to deploy from, or a reason not to.
     *
     * @param bay              the bay to deploy from, 1-based, when allowed
     * @param vehicle          what is in that bay - the vehicle being deployed when allowed, and on a
     *                         {@link Refusal#BAY_HOLDS_OTHER} what is actually in the bay that was named
     * @param refusal          why not, when refused
     * @param requestedBay     the bay number as it was asked for, so a nonsense one can be quoted back
     * @param requestedVehicle the vehicle asked for by name, when one was
     */
    public record Decision(int bay, SurfaceVehicle vehicle, Refusal refusal,
                           int requestedBay, SurfaceVehicle requestedVehicle) {

        public boolean isAllowed() {
            return refusal == null;
        }

        static Decision allow(int bay, SurfaceVehicle vehicle) {
            return new Decision(bay, vehicle, null, bay, null);
        }

        static Decision refuse(Refusal refusal, int requestedBay) {
            return new Decision(0, null, refusal, requestedBay, null);
        }

        static Decision refuse(Refusal refusal, int requestedBay,
                               SurfaceVehicle inBay, SurfaceVehicle requested) {
            return new Decision(0, inBay, refusal, requestedBay, requested);
        }
    }

    /**
     * The ship's situation, as far as this decision is concerned.
     *
     * @param landed         the ship is sitting on the surface - landed, which is not docked
     * @param altitudeMetres height above the surface, meaningful only while {@code overSurface}
     * @param overSurface    the ship is close enough to a body to have a latitude and longitude at all,
     *                       which is what separates "hovering too high" from "not at a planet"
     */
    public record ShipSituation(boolean landed, double altitudeMetres, boolean overSurface) {
    }

    private SurfaceVehicleDeployment() {
    }

    /**
     * Works out which bay to open, if any.
     * <p>
     * <b>Three ways the commander can say it.</b> A bay ("deploy from bay three"), a vehicle ("deploy the
     * Scarab"), or neither ("deploy the SRV"). Naming the vehicle is the one people actually reach for -
     * a commander knows they want the Scorpion, not that the Scorpion lives in bay 2 - so the vehicle is
     * looked up in the bay list rather than being a second way of saying a number.
     *
     * @param hasVehicleBay    whether the ship's loadout carries a planetary vehicle hangar
     * @param bays             what the commander says is in each bay, in bay order, nulls for the ones
     *                         they left empty. A shorter list is treated as the rest being empty.
     * @param requestedBay     the bay the commander named, or null
     * @param requestedVehicle the vehicle the commander named, or null
     */
    public static Decision decide(boolean hasVehicleBay,
                                  List<SurfaceVehicle> bays,
                                  Integer requestedBay,
                                  SurfaceVehicle requestedVehicle,
                                  ShipSituation situation) {
        int wanted = requestedBay == null ? DEFAULT_BAY : requestedBay;

        if (!hasVehicleBay) {
            return Decision.refuse(Refusal.NO_VEHICLE_BAY, wanted);
        }
        // Checked before anything else, because a commander who has configured nothing needs to hear that
        // once - not to be told bay 3 is empty and left guessing that the whole feature needs setting up.
        if (isUnconfigured(bays)) {
            return Decision.refuse(Refusal.BAYS_NOT_CONFIGURED, wanted);
        }

        // Named a vehicle and no bay: the bay list is the lookup table.
        if (requestedBay == null && requestedVehicle != null) {
            int holding = lowestBayHolding(bays, requestedVehicle);
            return holding == 0
                    ? Decision.refuse(Refusal.VEHICLE_NOT_LOADED, 0, null, requestedVehicle)
                    : gate(holding, requestedVehicle, situation);
        }

        // Deliberately quoted back rather than clamped. A bay number this far out came from the speech
        // recogniser mishearing, and silently deploying bay 1 instead would open the wrong bay while
        // sounding like it understood.
        if (wanted < 1 || wanted > MAX_BAYS) {
            return Decision.refuse(Refusal.NO_SUCH_BAY, wanted);
        }

        SurfaceVehicle inBay = at(bays, wanted);
        if (inBay == null) {
            return Decision.refuse(Refusal.BAY_EMPTY, wanted);
        }
        // Both named, and they disagree. Deploying the bay's actual contents would be the wrong vehicle
        // out of the right hole, so what is really in there is said instead.
        if (requestedVehicle != null && requestedVehicle != inBay) {
            return Decision.refuse(Refusal.BAY_HOLDS_OTHER, wanted, inBay, requestedVehicle);
        }
        return gate(wanted, inBay, situation);
    }

    /**
     * The last gate: whether the ship is doing what this particular vehicle needs.
     */
    private static Decision gate(int bay, SurfaceVehicle vehicle, ShipSituation situation) {
        return switch (vehicle.deployment()) {
            case LANDED -> situation.landed()
                    ? Decision.allow(bay, vehicle)
                    : Decision.refuse(Refusal.NOT_LANDED, bay);
            // A landed ship reports an altitude of zero and would otherwise read as "too low to drop",
            // which is true but unhelpful: the band is what matters, and being on the ground is outside it
            // either way. Being nowhere near a planet is the same refusal for the same reason.
            case HOVERING -> !situation.landed()
                    && situation.overSurface()
                    && vehicle.altitudeAllows(situation.altitudeMetres())
                    ? Decision.allow(bay, vehicle)
                    : Decision.refuse(Refusal.WRONG_ALTITUDE, bay);
        };
    }

    /**
     * The first bay holding this vehicle, 1-based, or 0 when none does.
     * <p>
     * The LOWEST on purpose: a commander carrying two Scarabs and asking for "the Scarab" means either,
     * and the lowest is both deterministic and the one they would have got by saying nothing.
     */
    private static int lowestBayHolding(List<SurfaceVehicle> bays, SurfaceVehicle vehicle) {
        for (int bay = 1; bay <= MAX_BAYS; bay++) {
            if (at(bays, bay) == vehicle) return bay;
        }
        return 0;
    }

    private static SurfaceVehicle at(List<SurfaceVehicle> bays, int bay) {
        return bays == null || bays.size() < bay ? null : bays.get(bay - 1);
    }

    /**
     * Whether the commander has said what is in any bay at all. One configured bay is enough to mean they
     * know the setting exists, so the emptiness of the others is then a real answer about those bays.
     */
    private static boolean isUnconfigured(List<SurfaceVehicle> bays) {
        return bays == null || bays.stream().allMatch(bay -> bay == null);
    }
}
