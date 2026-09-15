package elite.intel.gameapi.signals;

/**
 * A star system with conflict zones in it, who is fighting there, and how far away it is.
 *
 * @param warType    the game's own word - {@code war}, {@code civilwar} or {@code election} - or
 *                   null when no arrival has named the sides yet
 * @param faction1   one side, or null with {@code warType}
 * @param faction2   the other side, or null with {@code warType}
 * @param distanceLy from wherever the query was asked, so a caller can report it without redoing
 *                   the arithmetic
 */
public record WarZone(String starSystem,
                      ConflictZoneProfile zones,
                      String warType,
                      String faction1,
                      String faction2,
                      double distanceLy) {

    public boolean sidesKnown() {
        return faction1 != null && !faction1.isBlank() && faction2 != null && !faction2.isBlank();
    }
}
