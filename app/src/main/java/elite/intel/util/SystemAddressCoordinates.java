package elite.intel.util;

import java.util.Optional;

/**
 * Recovers a star system's approximate galactic position from its journal {@code SystemAddress}.
 *
 * <p>WHY this exists: coordinates otherwise reach the app only from an event that carries StarPos or
 * from EDSM, and neither is available for a system in uncharted space that the commander has not
 * flown to himself — a fleet carrier arrival he was not aboard for being the case that matters. The
 * SystemAddress is always present on those events, and it encodes the position.
 *
 * <p>The address packs a mass code and the system's boxel — the cube the galaxy generator placed it
 * in — whose edge is 10 ly for mass code {@code a} and doubles per code up to 1280 ly for {@code h}.
 * The boxel is all the address knows: this returns its centre, so the error is at most half an edge
 * on each axis. Good enough to answer "how far" and "how many jumps"; never good enough to plot to.
 *
 * <p>Only the journal's own field is read. No game memory, no process inspection.
 */
public final class SystemAddressCoordinates {

    /**
     * Galaxy-corner offsets the boxel grid is measured from, in light years.
     */
    private static final double ORIGIN_X = -49985;
    private static final double ORIGIN_Y = -40985;
    private static final double ORIGIN_Z = -24105;

    private static final int MASS_CODE_BITS = 3;
    private static final int MASS_CODE_MASK = 0b111;
    private static final int HIGHEST_MASS_CODE = 7;

    private SystemAddressCoordinates() {
        // static decoder for a single wire format.
    }

    /**
     * The centre of the boxel the address names, or empty when there is no address to decode.
     *
     * @param systemAddress the journal SystemAddress; null or non-positive yields empty.
     */
    public static Optional<BoxelCentre> decode(Long systemAddress) {
        if (systemAddress == null || systemAddress <= 0) return Optional.empty();

        long address = systemAddress;
        int massCode = (int) (address & MASS_CODE_MASK);
        // Each step up in mass code doubles the boxel edge, so one fewer bit is needed per axis.
        int spareBits = HIGHEST_MASS_CODE - massCode;

        int shift = MASS_CODE_BITS;
        long z = readBits(address, shift, spareBits + 7);
        shift += spareBits + 7;
        long y = readBits(address, shift, spareBits + 6);
        shift += spareBits + 6;
        long x = readBits(address, shift, spareBits + 7);

        int edgeLy = 10 << massCode;
        double half = edgeLy / 2.0;
        return Optional.of(new BoxelCentre(
                x * edgeLy + ORIGIN_X + half,
                y * edgeLy + ORIGIN_Y + half,
                z * edgeLy + ORIGIN_Z + half,
                edgeLy));
    }

    private static long readBits(long value, int shift, int width) {
        return (value >>> shift) & ((1L << width) - 1);
    }

    /**
     * The centre of a boxel, with the edge length that bounds how wrong it can be: the true position
     * is somewhere in the cube, so no axis is off by more than {@code edgeLy / 2}.
     */
    public record BoxelCentre(double x, double y, double z, int edgeLy) {

        /**
         * Worst-case error of this position on any one axis, in light years.
         */
        public double maxErrorLy() {
            return edgeLy / 2.0;
        }
    }
}
