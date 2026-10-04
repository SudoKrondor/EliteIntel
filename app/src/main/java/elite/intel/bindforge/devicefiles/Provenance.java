package elite.intel.bindforge.devicefiles;

import java.util.Locale;

/**
 * Who put a device entry in an installation's {@code DeviceMappings.xml}, and therefore whether BindForge may
 * touch it.
 * <p>
 * Not a label. This is a permission: getting it wrong means offering to rename or clear a device the user does
 * not own, and a game update then restoring Frontier's file would leave {@code .binds} pointing at a name that
 * no longer exists.
 * <p>
 * The file itself cannot answer this - it is a flat list of elements with no attribute separating Frontier's
 * from the player's - so the answer comes from comparing against the stock reference BindForge ships. See
 * {@link FrontierStockDevices}.
 */
public enum Provenance {

    /** Frontier shipped it. Its name and VID/PID pairs are their definition, and BindForge never edits one. */
    FRONTIER,

    /**
     * The player added it before BindForge ever ran.
     * <p>
     * Never inferred from absence. The shipped reference goes stale - Frontier adds hardware and BindForge
     * will not always ship an update first - so an entry simply missing from it is {@link #UNKNOWN}. This
     * value is set when the user confirms the entry is theirs.
     */
    USER_PREEXISTING,

    /** BindForge wrote it, so it is ours to change. */
    BINDFORGE,

    /**
     * Nobody has established who owns it, which is the safe default rather than a failure.
     * <p>
     * Means <em>do not claim it, do not touch it</em>. It exists because the stock reference is a capture at
     * a moment in time: without this, a device Frontier shipped after that capture would be recorded as the
     * player's, and BindForge would offer to rename something it does not own.
     */
    UNKNOWN;

    /**
     * The value as the database stores it - lowercase, matching the {@code CHECK} constraint on
     * {@code bindforge_device_installs.provenance}.
     */
    public String stored() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Reads a stored value back.
     *
     * @throws IllegalArgumentException if the column holds something outside the four, which would mean the
     *                                  {@code CHECK} constraint had been bypassed
     */
    public static Provenance fromStored(String stored) {
        return valueOf(stored.toUpperCase(Locale.ROOT));
    }
}
