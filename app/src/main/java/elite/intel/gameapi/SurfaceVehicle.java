package elite.intel.gameapi;

import java.util.Locale;

/**
 * A surface vehicle the ship can carry in its planetary vehicle hangar.
 * <p>
 * <b>Why the app has to be told which is which.</b> Nothing in the journal says what is loaded into a
 * hangar bay - the game reports the hangar module, not its contents - and the bays cannot be identified by
 * deploying one to look. The commander tells us in the ship's settings, and this is the vocabulary they
 * choose from.
 * <p>
 * The Nomad is deliberately absent. It flies, it comes out of its own bay, and none of the conditions
 * below describe it.
 */
public enum SurfaceVehicle {

    /**
     * The original buggy. Deploys from a landed ship.
     */
    SCARAB(Deployment.LANDED, "Scarab"),

    /**
     * The combat buggy. Deploys from a landed ship, exactly as the Scarab does.
     */
    SCORPION(Deployment.LANDED, "Scorpion"),

    /**
     * <b>The one that breaks the old rule.</b> The Rhino is dropped rather than driven out, so the ship is
     * hovering above the surface instead of sitting on it - which is why deployment can no longer be gated
     * on "landed" alone.
     */
    RHINO(Deployment.HOVERING, "Rhino");

    /**
     * What the ship has to be doing for this vehicle to leave the bay.
     */
    public enum Deployment {
        /**
         * Sitting on the surface. Landed, which is not the same as docked.
         */
        LANDED,
        /**
         * Holding station above the surface rather than sitting on it. Not gated on how high: see
         * {@code SurfaceVehicleDeployment.ShipSituation}.
         */
        HOVERING
    }

    private final Deployment deployment;
    private final String displayName;

    SurfaceVehicle(Deployment deployment, String displayName) {
        this.deployment = deployment;
        this.displayName = displayName;
    }

    public Deployment deployment() {
        return deployment;
    }

    /**
     * The name to show and to speak.
     * <p>
     * WHY not {@link #name()}: the enum constant is an identifier, and a payload field holding one ends up
     * being read out - "deploying SCARAB from bay one". These are Frontier's proper nouns, so they are the
     * same in every language and are deliberately not translated.
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Reads a stored bay setting, tolerating the unset case.
     *
     * @return the vehicle, or null when the bay has never been configured or holds a name this build does
     * not know - a value written by a later version must read as "not set" rather than throw
     */
    public static SurfaceVehicle fromStored(String stored) {
        if (stored == null || stored.isBlank()) return null;
        try {
            return valueOf(stored.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
