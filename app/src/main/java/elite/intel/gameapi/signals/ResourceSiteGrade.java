package elite.intel.gameapi.signals;

/**
 * The four grades of Resource Extraction Site, and the signal symbols the game files them under.
 * <p>
 * WHY the symbol and not {@code SignalName_Localised}: the localised name is written by the game
 * client in whatever language it runs in, and the app language and the game language are not the
 * same setting. The symbol is the same string everywhere.
 * <p>
 * The order is the order of danger and reward. {@code LOW} is beginner work with thin bounties,
 * {@code HAZARDOUS} draws engineered ships and pays accordingly, and {@code STANDARD} is the
 * unmarked site that sits between them.
 */
public enum ResourceSiteGrade {

    LOW("$MULTIPLAYER_SCENARIO77_TITLE;"),
    STANDARD("$MULTIPLAYER_SCENARIO14_TITLE;"),
    HIGH("$MULTIPLAYER_SCENARIO78_TITLE;"),
    HAZARDOUS("$MULTIPLAYER_SCENARIO79_TITLE;");

    private final String signalSymbol;

    ResourceSiteGrade(String signalSymbol) {
        this.signalSymbol = signalSymbol;
    }

    /**
     * The grade this symbol names, or null when it is not a resource site at all.
     * <p>
     * Two events carry it: {@code FSSSignalDiscovered.SignalName}, which reports the sites a system
     * holds, and {@code SupercruiseDestinationDrop.Type}, which reports the one the ship just dropped
     * into.
     * <p>
     * WHY null rather than a default grade: a scenario the game added and this enum has not met yet
     * must not be counted as a Low site. It is not a resource site until we know it is one.
     */
    public static ResourceSiteGrade fromSymbol(String symbol) {
        if (symbol == null) return null;
        for (ResourceSiteGrade grade : values()) {
            if (grade.signalSymbol.equals(symbol)) return grade;
        }
        return null;
    }
}
