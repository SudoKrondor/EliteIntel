package elite.intel.gameapi.search.spansh.station;

/**
 * A station returned by one of the Spansh station searches, reduced to the two fields that decide
 * whether it is somewhere the commander actually has to travel to.
 *
 * <p>The station search DTOs are each shaped by their own endpoint and have nothing else in common,
 * so this stays deliberately narrow: it exists so {@link CurrentSystemFilter} can be written once
 * instead of once per search.
 */
public interface StationSearchHit {

    /**
     * Name of the system the station is in.
     */
    String getSystemName();

    /**
     * Light years from the reference coordinates the search was given.
     */
    double getDistance();
}
