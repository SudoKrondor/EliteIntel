package elite.intel.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Which surface vehicle is out, by the journal's symbol for it - so a Nomad can be told from a wheeled SRV.
 * <p>
 * <b>Why this exists.</b> The game reports the Nomad as an SRV: Status.json sets the InSRV flag and no other
 * bit, in {@code Flags} or {@code Flags2}, sets it apart. Yet it flies, and takes its flight and light controls
 * the way a ship does - its lights are a plain on/off with no high beam. Only the journal names it, so this
 * remembers what the journal said:
 * <ul>
 *   <li>{@code LaunchVessel} - the Nomad deployed ({@code VesselType "lander01"})</li>
 *   <li>{@code LaunchSRV} - a wheeled or hovering SRV deployed ({@code testbuggy}, {@code mev_rhino}, ...)</li>
 *   <li>{@code LoadGame} - the game started with the commander already sitting in it ({@code Ship "Lander01"})</li>
 *   <li>{@code DockSRV} - any of them stowed, the Nomad included</li>
 *   <li>{@code Embark} with {@code SRV:true} - climbing back in from on foot. It names the vehicle only by its
 *       {@code ID}, which is how a commander who logged in ON FOOT gets back into a Nomad left out on the surface:
 *       their {@code LoadGame} names the suit, and no launch event follows.</li>
 * </ul>
 * The {@code ID} is a hangar slot, not a vehicle: the same number has held a Scarab and later a Rhino. So the
 * symbol last seen with each ID is what counts, and an ID never seen with one says nothing.
 * <p>
 * In memory only, and refilled by the pre-scan, which reads the last journals from the top.
 * <p>
 * The test is POSITIVE for the Nomad: nothing known, or any other symbol, answers "not a Nomad", which is
 * exactly how the app behaved before the Nomad was told apart. An unseen vehicle can only fall back to that.
 */
public final class DeployedVehicle {

    /**
     * The journal's symbol for the Nomad, spelled {@code lander01} by the launch events and {@code Lander01} by LoadGame.
     */
    static final String NOMAD_SYMBOL = "lander01";

    private static final DeployedVehicle INSTANCE = new DeployedVehicle();

    private final AtomicReference<String> symbol = new AtomicReference<>();

    /**
     * Hangar slot id to the vehicle symbol a launch or dock event last named for it.
     */
    private final Map<Integer, String> symbolById = new ConcurrentHashMap<>();

    private DeployedVehicle() {
    }

    public static DeployedVehicle getInstance() {
        return INSTANCE;
    }

    /**
     * Records the vehicle the commander is now flying or driving, by the journal's symbol for it.
     */
    public void boarded(String vehicleSymbol) {
        symbol.set(vehicleSymbol);
    }

    /**
     * A launch: the commander is in it, and the slot id now holds it.
     */
    public void launched(int id, String vehicleSymbol) {
        learn(id, vehicleSymbol);
        boarded(vehicleSymbol);
    }

    /**
     * Back in a vehicle from on foot, named only by its slot id. An id no launch or dock has named leaves the
     * record alone: within a session the record is already right, and across one an unknown id can only fall
     * back to plain SRV behaviour.
     */
    public void reboarded(int id) {
        String known = symbolById.get(id);
        if (known != null) {
            boarded(known);
        }
    }

    /**
     * The vehicle is back aboard the ship. The slot id keeps naming it, for the next time it is boarded.
     */
    public void stowed(int id, String vehicleSymbol) {
        learn(id, vehicleSymbol);
        stowed();
    }

    public void stowed() {
        symbol.set(null);
    }

    /**
     * Forgets everything, slot ids included.
     */
    public void reset() {
        symbol.set(null);
        symbolById.clear();
    }

    private void learn(int id, String vehicleSymbol) {
        if (vehicleSymbol != null && !vehicleSymbol.isBlank()) {
            symbolById.put(id, vehicleSymbol);
        }
    }

    /**
     * Whether the vehicle last recorded is the Nomad. Says nothing about whether the commander is in it.
     */
    public boolean isNomad() {
        return NOMAD_SYMBOL.equalsIgnoreCase(symbol.get());
    }
}
