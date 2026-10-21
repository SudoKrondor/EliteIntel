package elite.intel.gameapi.population;

import java.util.List;

/**
 * Shares a star system's population out among its ports by size, for the commander who asks how many people
 * live where they are. The game says nothing below system level, so every figure here is an estimate; see
 * {@link PortSize}.
 * <p>
 * Each port takes a share in proportion to its {@link PortSize#capacity()} and never more than that capacity.
 * Figures are rounded to two significant digits, because a seven-digit head count read out exactly claims a
 * precision the estimate does not have.
 *
 * @param systemPopulation the system's population as the arrival event reported it
 * @param ports            the size of every port known in the system, including the one being asked about
 */
public record PopulationEstimate(long systemPopulation, List<PortSize> ports) {

    public PopulationEstimate {
        if (systemPopulation < 0) {
            throw new IllegalArgumentException("Population cannot be negative, got " + systemPopulation);
        }
        ports = List.copyOf(ports);
    }

    /**
     * The estimated population of one port of this size, rounded.
     */
    public long of(PortSize port) {
        return roundToTwoDigits(shareOf(port));
    }

    /**
     * The estimated population of several ports together - a planet's settlements - rounded once, as a total.
     */
    public long ofAll(List<PortSize> group) {
        return roundToTwoDigits(group.stream().mapToLong(this::shareOf).sum());
    }

    private long shareOf(PortSize port) {
        long totalCapacity = ports.stream().mapToLong(PortSize::capacity).sum();
        if (totalCapacity == 0) return 0;
        long share = Math.round((double) systemPopulation * port.capacity() / totalCapacity);
        return Math.min(share, port.capacity());
    }

    static long roundToTwoDigits(long value) {
        if (value < 100) return value;
        long scale = (long) Math.pow(10, (long) Math.log10(value) - 1);
        return Math.round((double) value / scale) * scale;
    }
}
