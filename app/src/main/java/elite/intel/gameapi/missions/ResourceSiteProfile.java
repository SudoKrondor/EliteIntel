package elite.intel.gameapi.missions;

import elite.intel.gameapi.signals.ResourceSiteGrade;

import java.util.EnumMap;
import java.util.Map;

/**
 * How many resource extraction sites of each grade a system was seen to hold.
 * <p>
 * The counts are what one FSS sweep reported, not a running total. Every arrival re-emits the whole
 * signal set, so a system visited five times still has the sites it has.
 * <p>
 * All four counts may be zero and the system still have sites: a hunting ground carried over from
 * the old workflow was confirmed by hand, which recorded that it had sites but never which. That is
 * what {@link #gradesKnown()} distinguishes.
 */
public record ResourceSiteProfile(int standard, int low, int high, int hazardous) {

    public int total() {
        return standard + low + high + hazardous;
    }

    public boolean gradesKnown() {
        return total() > 0;
    }

    /**
     * The larger count of each grade across the two profiles.
     * <p>
     * WHY per grade rather than picking the richer profile whole: one sweep may catch the hazardous
     * sites and another the low ones, and the system has both.
     */
    public ResourceSiteProfile max(ResourceSiteProfile other) {
        if (other == null) return this;
        return new ResourceSiteProfile(
                Math.max(standard, other.standard),
                Math.max(low, other.low),
                Math.max(high, other.high),
                Math.max(hazardous, other.hazardous)
        );
    }

    public int count(ResourceSiteGrade grade) {
        return switch (grade) {
            case LOW -> low;
            case STANDARD -> standard;
            case HIGH -> high;
            case HAZARDOUS -> hazardous;
        };
    }

    /**
     * The grades present and their counts, worst grade first, so a caller reading the profile aloud
     * or into a HUD row does not have to know the enum.
     */
    public Map<ResourceSiteGrade, Integer> present() {
        Map<ResourceSiteGrade, Integer> present = new EnumMap<>(ResourceSiteGrade.class);
        for (ResourceSiteGrade grade : ResourceSiteGrade.values()) {
            int count = count(grade);
            if (count > 0) present.put(grade, count);
        }
        return present;
    }
}
