package elite.intel.gameapi.search.spansh.station.outfitting;

/**
 * One listing in a station's outfitting that answered the search.
 *
 * @param size   the slot size, 1 to 8 (0 for a utility mount)
 * @param rating the quality letter, A to E
 * @param mount  a weapon's mount word from Spansh, or null for anything that is not a weapon
 * @param price  credits, as Spansh last saw it
 */
public record StockedModule(int size, String rating, String mount, long price) {

    /**
     * The designation a pilot reads on the outfitting screen: "6B".
     */
    public String designation() {
        return size + rating;
    }
}
