package elite.intel.gameapi.search.edsm.dto;

public enum MaterialsType {
    EDMS_MATERIAL("materials"), EDMS_ENCODED("data"),
    GAME_RAW("Raw"),
    GAME_MANUFACTURED("Manufactured"),
    GAME_ENCODED("Encoded"),
    GAME_UNKNOWN("Unknown")
    ;

    private String type;

    MaterialsType(String type){
        this.type = type;
    }

    public String getType(){
        return this.type;
    }

    /**
     * Maps a journal material category to its inventory type. The journal writes the same three
     * categories three different ways, so all of them are accepted:
     * <ul>
     *   <li>title-case — {@code "Manufactured"}, in a Category field;</li>
     *   <li>lower-case — {@code "manufactured"}, in MaterialTrade's TraderType;</li>
     *   <li>as a game token — {@code "$MICRORESOURCE_CATEGORY_Manufactured;"}, in MissionCompleted's
     *       MaterialsReward.</li>
     * </ul>
     * An unknown or absent category yields {@link #GAME_UNKNOWN} rather than throwing: the category
     * only labels a material, so a strange one must not cost us the count.
     */
    public static MaterialsType fromJournalCategory(String category) {
        String value = unwrapToken(category);
        if (GAME_RAW.type.equalsIgnoreCase(value)) return GAME_RAW;
        if (GAME_MANUFACTURED.type.equalsIgnoreCase(value)) return GAME_MANUFACTURED;
        if (GAME_ENCODED.type.equalsIgnoreCase(value)) return GAME_ENCODED;
        return GAME_UNKNOWN;
    }

    /**
     * Strips the {@code $MICRORESOURCE_CATEGORY_...;} wrapper, leaving anything else untouched.
     */
    private static String unwrapToken(String category) {
        if (category == null) return null;
        String value = category.trim();
        if (!value.startsWith("$")) return value;
        if (value.endsWith(";")) value = value.substring(0, value.length() - 1);
        int lastUnderscore = value.lastIndexOf('_');
        return lastUnderscore < 0 ? value : value.substring(lastUnderscore + 1);
    }
}
