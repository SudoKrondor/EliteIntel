package elite.intel.gameapi.search.spansh.station;

import java.util.ArrayList;
import java.util.List;

/**
 * The radii a station search sweeps, in order, rather than report that a thing which exists is nowhere.
 *
 * <p>Each rung is only reached when the one before it found nothing, so a commander inside the bubble still
 * gets a single round trip. A commander who cannot reach the answer still needs to be told where it is; the
 * caller compares the winner's distance against what was asked for and says so.
 */
public final class SearchRadii {

    /**
     * Human space is roughly this wide around Sol, so a last sweep this far covers every fixed station there
     * is.
     */
    public static final int INHABITED_BUBBLE_LY = 1000;

    private SearchRadii() {
    }

    /**
     * The stated radius, then double it, then the whole inhabited bubble.
     * <p>
     * Public because a caller reporting that nothing was found has to say how far the search actually looked,
     * which is the last rung and not the radius it was given.
     */
    public static List<Integer> widening(int maxDistanceLy) {
        // Guarded so a commander who asks for a galaxy-wide radius does not overflow it into a negative one.
        int widened = maxDistanceLy > Integer.MAX_VALUE / 2 ? maxDistanceLy : maxDistanceLy * 2;
        List<Integer> radii = new ArrayList<>();
        radii.add(maxDistanceLy);
        if (widened > maxDistanceLy) radii.add(widened);
        // Never NARROWS an already-wide ask: a commander who said 2000 ly keeps his 4000 ly second sweep.
        if (INHABITED_BUBBLE_LY > widened) radii.add(INHABITED_BUBBLE_LY);
        return List.copyOf(radii);
    }
}
