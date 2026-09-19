package elite.intel.gameapi.signals;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How many conflict zones of each intensity a system was seen to hold.
 * <p>
 * The counts are what one FSS sweep reported, not a running total. Every arrival re-emits the whole
 * signal set, so a system visited five times still has the zones it has.
 */
public record ConflictZoneProfile(int low, int medium, int high, int powerplay) {

    /**
     * Zones the commander can drop into for combat bonds. Powerplay zones are excluded because
     * they are a different fight, for a power rather than a faction.
     */
    public int factionWarZones() {
        return low + medium + high;
    }

    public int count(ConflictZoneIntensity intensity) {
        return switch (intensity) {
            case LOW -> low;
            case MEDIUM -> medium;
            case HIGH -> high;
            case POWERPLAY -> powerplay;
        };
    }

    /**
     * The faction-war intensities present and their counts, hardest first, so a caller reading the
     * profile aloud or into a HUD row does not have to know the enum.
     */
    public Map<ConflictZoneIntensity, Integer> present() {
        // WHY not an EnumMap: it iterates in declaration order, which is lowest first.
        Map<ConflictZoneIntensity, Integer> present = new LinkedHashMap<>();
        for (ConflictZoneIntensity intensity : new ConflictZoneIntensity[]{
                ConflictZoneIntensity.HIGH, ConflictZoneIntensity.MEDIUM, ConflictZoneIntensity.LOW}) {
            int count = count(intensity);
            if (count > 0) present.put(intensity, count);
        }
        return present;
    }
}
