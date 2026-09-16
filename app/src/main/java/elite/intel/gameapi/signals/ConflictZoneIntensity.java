package elite.intel.gameapi.signals;

/**
 * The kinds of conflict zone, and the signal symbol families the game files them under.
 * <p>
 * WHY the symbol and not {@code SignalName_Localised}: the localised name is written by the game
 * client in whatever language it runs in, and the app language and the game language are not the
 * same setting. The symbol is the same string everywhere.
 * <p>
 * The three intensities are the order of danger and reward: a low-intensity zone is a handful of
 * small ships, a high-intensity one brings capital-class support and pays the best bonds.
 * {@code POWERPLAY} is a different mechanic altogether - a power's war rather than a faction's -
 * and is counted so a system's rows are honest, but never offered as somewhere to go and fight.
 */
public enum ConflictZoneIntensity {

    LOW("$Warzone_PointRace_Low"),
    MEDIUM("$Warzone_PointRace_Med"),
    HIGH("$Warzone_PointRace_High"),
    POWERPLAY("$Warzone_Powerplay_");

    private final String symbolPrefix;

    ConflictZoneIntensity(String symbolPrefix) {
        this.symbolPrefix = symbolPrefix;
    }

    /**
     * The intensity this symbol names, or null when it is not a conflict zone at all.
     * <p>
     * The symbol carries the zone's index after a colon ({@code $Warzone_PointRace_Low:#index=3;}),
     * so it is matched on its prefix. Two events carry it: {@code FSSSignalDiscovered.SignalName},
     * which reports the zones a system holds, and {@code SupercruiseDestinationDrop.Type}, which
     * reports the one the ship just dropped into.
     * <p>
     * WHY null rather than a default: a war zone family this enum has not met yet (the Thargoid
     * ones, say) must not be counted as a low-intensity faction war. It is not a conflict zone
     * until we know it is one.
     */
    public static ConflictZoneIntensity fromSymbol(String symbol) {
        if (symbol == null) return null;
        for (ConflictZoneIntensity intensity : values()) {
            if (symbol.startsWith(intensity.symbolPrefix)) return intensity;
        }
        return null;
    }
}
