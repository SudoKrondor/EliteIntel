package elite.intel.gameapi.missions;

/**
 * A star system the commander has seen resource extraction sites in, and how far away it is.
 * <p>
 * A hunting ground exists because the commander flew there - the game announces its sites on
 * arrival - so this is knowledge the app owns outright and never has to ask a service for.
 *
 * @param distanceLy from wherever the query was asked, so a caller can report it without redoing
 *                   the arithmetic
 */
public record HuntingGround(String starSystem, ResourceSiteProfile sites, double distanceLy) {
}
