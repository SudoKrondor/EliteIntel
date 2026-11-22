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
 * @param hardware  every VID/PID pair this entry claims, primary and alternatives together. Its order is not the
 *                  primary's: ask {@link #primary()} for that
 * @param primary   the element's own pair, as opposed to an {@code <Alternative>}, or {@code null} for an element
 *                  carrying no pair of its own. Always one of {@code hardware}
 */
public record DeviceEntry(String name, Set<HardwareId> hardware, HardwareId primary) {

    public DeviceEntry {
        // WHY: the primary is a fact about the element, not a position in the set. Held as its own component after
        // review (2026-10-08), because a set's order is lost by any copy, and the primary was being read as "the
        // first pair" - which for an element with only alternatives would be an alternative.
        if (primary != null && !hardware.contains(primary)) {
            throw new IllegalArgumentException("<" + name + ">'s primary " + primary + " is not among its pairs");
        }
    }

    /**
     * An entry built from its pairs alone. A single pair is the primary; with several, nothing says which is the
     * element's own, so there is no primary - only the parser, which reads the element, knows.
     */
    public DeviceEntry(String name, Set<HardwareId> hardware) {
        this(name, hardware, hardware.size() == 1 ? hardware.iterator().next() : null);
    }

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
