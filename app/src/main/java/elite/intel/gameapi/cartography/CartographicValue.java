package elite.intel.gameapi.cartography;

import java.util.List;
import java.util.Locale;

/**
 * What Universal Cartographics pays for a body, and for a system, before the deduction taken at the counter.
 * <p>
 * The formula is the community's reverse-engineering of the game (MattG, Frontier forums, updated for
 * Odyssey in 2022). It reproduces that thread's median table to the credit, and the commander's own sales
 * where they can be isolated: a lone T Tauri star sold for exactly 1,201, and three honked systems of one
 * scanned star each for exactly 12,103 - the stars plus 500 for every body the honk counted and nobody scanned.
 * <p>
 * Where it cannot be exact, and why:
 * <ul>
 *   <li>The first discoverer's multiplier goes to whoever SELLS first. {@code WasDiscovered:false} only says
 *       nobody had sold the body when it was scanned, so on a well-travelled route the projection is high.</li>
 *   <li>A body the honk counted but nothing scanned is priced at the 500 floor, which is what a rock or an
 *       icy body pays. A gas giant the commander never looked at is worth more than that.</li>
 *   <li>A partly terraformable world pays only part of its terraforming bonus, and the journal does not say
 *       which part. It is counted in full.</li>
 *   <li>The 10,000-per-body bonus for mapping a whole system is reported by the same thread but did not show
 *       in the commander's sales, so it is left out.</li>
 * </ul>
 * The value at the counter is a few percent lower again: 92% of base plus bonus in the commander's sales.
 */
public final class CartographicValue {

    private static final double Q = 0.56591828;

    private static final double STAR_K = 1200;
    private static final double NEUTRON_STAR_OR_BLACK_HOLE_K = 22628;
    private static final double WHITE_DWARF_K = 14057;
    private static final double SUPERMASSIVE_BLACK_HOLE_K = 33.5678;

    private static final double FIRST_DISCOVERY = 2.6;
    private static final double MAPPED = 3.3333333333;
    private static final double FIRST_MAPPED = 8.0956;
    private static final double FIRST_DISCOVERED_AND_MAPPED = 3.699622554;
    private static final double ODYSSEY_MAPPING_SHARE = 0.3;
    private static final double ODYSSEY_MAPPING_MINIMUM = 555;
    private static final double EFFICIENCY = 1.25;
    private static final double BODY_MINIMUM = 500;

    /**
     * Paid per body the honk counted, once the FSS has found every one of them in a system nobody had found.
     */
    private static final long FULL_SCAN_PER_BODY = 1000;

    private CartographicValue() {
    }

    /**
     * What the system's data is worth: every body on record, the honk bonus the primary star carries, and the
     * full-scan bonus once the FSS is finished in a first discovery.
     *
     * @param bodies         the system's bodies on record, stars and planets
     * @param honkBodyCount  the bodies the honk counted, or null when the system was not honked
     * @param allBodiesFound the FSS has found every body the honk counted
     */
    public static long system(List<CartographicBody> bodies, Integer honkBodyCount, boolean allBodiesFound) {
        long total = 0;
        for (CartographicBody body : bodies) {
            total += body(body);
        }
        if (honkBodyCount == null) return total;

        boolean firstDiscovery = bodies.stream()
                .filter(CartographicBody::primaryStar)
                .anyMatch(CartographicValue::firstDiscoverer);
        long others = 0;
        for (CartographicBody body : bodies) {
            if (body.primaryStar()) continue;
            total += honkShare(body);
            others++;
        }
        // The honk prices every body it counted, scanned or not; the primary star is the one carrying the bonus.
        long unscanned = Math.max(0, honkBodyCount - 1 - others);
        total += unscanned * Math.round(BODY_MINIMUM * (firstDiscovery ? FIRST_DISCOVERY : 1));
        if (allBodiesFound && firstDiscovery) total += FULL_SCAN_PER_BODY * honkBodyCount;
        return total;
    }

    /**
     * What one body's data sells for.
     */
    public static long body(CartographicBody body) {
        if (body.isStar()) {
            return Math.round(starBase(body) * (firstDiscoverer(body) ? FIRST_DISCOVERY : 1));
        }
        double k = planetK(body);
        double mapping = 1;
        if (body.mapped()) {
            boolean firstMapped = !body.wasMapped();
            if (firstDiscoverer(body) && firstMapped) mapping = FIRST_DISCOVERED_AND_MAPPED;
            else if (firstMapped) mapping = FIRST_MAPPED;
            else mapping = MAPPED;
        }
        double value = planetBase(k, body.massEM()) * mapping;
        if (body.mapped()) {
            if (!bubbleBody(body)) value += Math.max(value * ODYSSEY_MAPPING_SHARE, ODYSSEY_MAPPING_MINIMUM);
            if (body.efficient()) value *= EFFICIENCY;
        }
        value = Math.max(BODY_MINIMUM, value);
        return Math.round(value * (firstDiscoverer(body) ? FIRST_DISCOVERY : 1));
    }

    /**
     * What a body adds to the primary star's honk bonus: a third of its base value, never under 500.
     */
    static long honkShare(CartographicBody body) {
        double multiplier = firstDiscoverer(body) ? FIRST_DISCOVERY : 1;
        if (body.isStar()) return Math.round(starBase(body) * multiplier / 3);
        return Math.round(Math.max(BODY_MINIMUM, planetBase(planetK(body), body.massEM()) / 3) * multiplier);
    }

    /**
     * Whether the commander will be paid as the first discoverer, if nobody sells it first.
     * <p>
     * A body in the bubble can be undiscovered and yet mapped - charted before discovery existed. It still pays
     * the discoverer's multiplier, but not the Odyssey mapping share; see {@link #bubbleBody}.
     */
    private static boolean firstDiscoverer(CartographicBody body) {
        return !body.wasDiscovered();
    }

    private static boolean bubbleBody(CartographicBody body) {
        return !body.wasDiscovered() && body.wasMapped();
    }

    private static double starBase(CartographicBody star) {
        double k = starK(star.starType());
        return k + star.stellarMass() * k / 66.25;
    }

    private static double planetBase(double k, double massEM) {
        return k + k * Q * Math.pow(massEM, 0.2);
    }

    private static double starK(String starType) {
        String type = starType.trim();
        if (type.equals("N") || type.equals("H")) return NEUTRON_STAR_OR_BLACK_HOLE_K;
        if (type.equals("SupermassiveBlackHole")) return SUPERMASSIVE_BLACK_HOLE_K;
        if (type.startsWith("D")) return WHITE_DWARF_K;
        return STAR_K;
    }

    /**
     * The planet's k: its class's base, plus the terraforming bonus when it can be terraformed. An Earth-like
     * world is paid the bonus whether or not the journal calls it terraformable.
     */
    private static double planetK(CartographicBody planet) {
        String planetClass = planet.planetClass() == null ? "" : planet.planetClass().toLowerCase(Locale.ROOT);
        boolean terraformable = planet.terraformable();
        return switch (planetClass) {
            case "metal rich body" -> 21790 + (terraformable ? 105678 : 0);
            case "ammonia world" -> 96932;
            case "sudarsky class i gas giant" -> 1656;
            case "high metal content body", "sudarsky class ii gas giant" -> 9654 + (terraformable ? 100677 : 0);
            case "earthlike body" -> 64831 + 116295;
            case "water world" -> 64831 + (terraformable ? 116295 : 0);
            default -> 300 + (terraformable ? 93328 : 0);
        };
    }
}
