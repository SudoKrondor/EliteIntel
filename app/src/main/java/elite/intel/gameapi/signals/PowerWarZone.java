package elite.intel.gameapi.signals;

import java.util.List;

/**
 * A star system with power conflict zones in it, which powers are there, and how far away it is.
 * <p>
 * The powerplay twin of {@link WarZone}: there are no two sides here, but every power present in the
 * system, one of which may hold it.
 *
 * @param zones            distinct power conflict zones the last sweep saw
 * @param controllingPower the power holding the system, or null when it is unoccupied
 * @param powers           every power present, the controlling one included, as the journal spells them
 * @param distanceLy       from wherever the query was asked
 */
public record PowerWarZone(String starSystem,
                           int zones,
                           String controllingPower,
                           List<String> powers,
                           double distanceLy) {

    public PowerWarZone {
        powers = powers == null ? List.of() : List.copyOf(powers);
    }

    /**
     * The powers present other than the one holding the system - who it is being contested by.
     */
    public List<String> contenders() {
        return powers.stream().filter(power -> !power.equals(controllingPower)).toList();
    }
}
