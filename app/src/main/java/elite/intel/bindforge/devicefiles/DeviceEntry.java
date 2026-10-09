package elite.intel.bindforge.devicefiles;

import java.util.Set;

/**
 * One device element from a {@code DeviceMappings.xml}.
 * <p>
 * The element tag is the identity - {@code <VPCPanel>}, {@code <GamePad>} - and it is what a {@code .binds}
 * file names in a {@code Device=} attribute. An entry owns a <strong>set</strong> of hardware ids rather than
 * one: {@code <GamePad>} alone carries a primary pair plus 79 {@code <Alternative>} pairs.
 *
 * @param name      the element tag, which is also the {@code .buttonMap} filename stem
 * @param hardware  every VID/PID pair this entry claims, primary and alternatives together - in file order,
 *                  primary first, when {@link DeviceMappingsParser} read it
 */
public record DeviceEntry(String name, Set<HardwareId> hardware) {

    /**
     * A USB vendor and product id, as the device reports them.
     * <p>
     * Held uppercase because <strong>hex case is not a convention in Frontier's own file</strong> - audited
     * across all 136 pairs in the stock copy, PID is uppercase in 39, lowercase in 35 and digits-only in 62.
     * Two entries naming the same hardware in different cases are the same hardware.
     */
    public record HardwareId(String vid, String pid) {
        public HardwareId {
            vid = vid == null ? "" : vid.trim().toUpperCase();
            pid = pid == null ? "" : pid.trim().toUpperCase();
        }

        @Override
        public String toString() {
            return "VID " + vid + " PID " + pid;
        }
    }
}
