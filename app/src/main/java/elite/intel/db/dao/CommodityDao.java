package elite.intel.db.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

/**
 * The commodity catalogue: the English name Spansh matches on, the game symbol the journal reports, and
 * one display name per language.
 * <p>
 * Rows are seeded by migration, and a good the seed does not know is learned from the journal by symbol
 * (see {@code CommodityCatalogue}). Such a row may have no English name yet - {@code commodity} is
 * NOT NULL UNIQUE, so it then holds the bare symbol as a stand-in with {@code english_pending} set, and
 * every reader below goes through {@link #ENGLISH_NAME} so the stand-in is never spoken, matched, or
 * offered to Spansh as a trade name.
 */
public interface CommodityDao {

    /**
     * The English name as a spoken form: NULL while a learned row's English is still pending.
     */
    String ENGLISH_NAME = "CASE WHEN english_pending = 1 THEN NULL ELSE commodity END";

    /**
     * The display name for a row: the given language column, else English, else the word the game client
     * shows for a name learned from the journal.
     */
    String SPOKEN_NAME = "COALESCE(<col>, " + ENGLISH_NAME + ", <gameCol>)";

    @SqlQuery("SELECT LOWER(commodity) FROM commodities WHERE english_pending = 0 ORDER BY commodity")
    List<String> getAllNamesLowerCase();

    @SqlQuery("SELECT commodity FROM commodities WHERE LOWER(commodity) = LOWER(:name) LIMIT 1")
    String getOriginalCase(@Bind("name") String name);

    /**
     * Every spoken form in a language: the display name for that language and the word the game client
     * itself shows, so the commander can always say what is on their HUD whatever language they talk to
     * us in. Lower-cased, for fuzzy matching.
     */
    @SqlQuery("SELECT spoken FROM ("
            + " SELECT LOWER(" + SPOKEN_NAME + ") AS spoken FROM commodities"
            + " UNION SELECT LOWER(<gameCol>) FROM commodities"
            + ") WHERE spoken IS NOT NULL ORDER BY spoken")
    List<String> getAllLocalizedNamesLowerCase(@Define("col") String col, @Define("gameCol") String gameCol);

    /**
     * The English name behind a spoken form, or the symbol stand-in for a learned good that has no
     * English yet - the one canonical key such a row has, and what {@link #getSymbolByEnglishName} and
     * {@link #getLocalizedByEnglishName} resolve from.
     */
    @SqlQuery("SELECT commodity FROM commodities"
            + " WHERE LOWER(" + SPOKEN_NAME + ") = LOWER(:localizedName)"
            + "    OR LOWER(<gameCol>) = LOWER(:localizedName)"
            + " LIMIT 1")
    String getEnglishByLocalizedName(@Define("col") String col,
                                     @Define("gameCol") String gameCol,
                                     @Bind("localizedName") String localizedName);

    @SqlQuery("SELECT " + SPOKEN_NAME + " FROM commodities WHERE LOWER(commodity) = LOWER(:englishName) LIMIT 1")
    String getLocalizedByEnglishName(@Define("col") String col,
                                     @Define("gameCol") String gameCol,
                                     @Bind("englishName") String englishName);

    @SqlQuery("SELECT " + SPOKEN_NAME + " FROM commodities WHERE LOWER(symbol) = LOWER(:symbol) LIMIT 1")
    String getLocalizedBySymbol(@Define("col") String col,
                                @Define("gameCol") String gameCol,
                                @Bind("symbol") String symbol);

    /**
     * Whether the row behind an English name (or a symbol stand-in) carries a real English name - the
     * name Spansh matches on. False for a good learned from a non-English client until curation or an
     * English client supplies it.
     */
    @SqlQuery("SELECT COUNT(*) > 0 FROM commodities WHERE LOWER(commodity) = LOWER(:englishName) AND english_pending = 0")
    boolean hasEnglishName(@Bind("englishName") String englishName);

    /**
     * Returns the non-localized game symbol (FDevIDs {@code symbol}, e.g. "AtmosphericExtractors")
     * for an English commodity name. This is what a Cargo event's {@code Name} field holds
     * (lower-cased in the journal), so it is the value to match cargo inventory against.
     * Returns {@code null} for legacy/Powerplay goods that FDevIDs no longer lists.
     */
    @SqlQuery("SELECT symbol FROM commodities WHERE LOWER(commodity) = LOWER(:englishName) LIMIT 1")
    String getSymbolByEnglishName(@Bind("englishName") String englishName);

    /**
     * The reverse lookup: the English commodity name for a game symbol, whatever case the symbol
     * arrives in. This is the bridge from a journal field to a Spansh query - Spansh matches the
     * English name exactly, and a mission's own {@code Commodity_Localised} is written in the game's
     * language, which is not necessarily English and not necessarily the app's language either.
     * Returns {@code null} for legacy/Powerplay goods that carry no symbol, and for a learned good
     * whose English name is still pending - there is nothing Spansh would match.
     */
    @SqlQuery("SELECT " + ENGLISH_NAME + " FROM commodities WHERE LOWER(symbol) = LOWER(:symbol) LIMIT 1")
    String getEnglishBySymbol(@Bind("symbol") String symbol);

    // optional – one-time init if table empty
    @SqlQuery("SELECT COUNT(*) FROM commodities")
    int count();

    @SqlBatch("INSERT OR IGNORE INTO commodities (commodity) VALUES (:name)")
    void insertAll(@Bind("name") List<String> names);

    // ── learning ─────────────────────────────────────────────────────────────

    /**
     * Registers a good the catalogue does not know by symbol. {@code commodity} is the English name when
     * the caller has one, otherwise the symbol itself with {@code englishPending} set. Guarded by symbol,
     * not by the UNIQUE name: a seeded row already carrying the symbol under its curated spelling must
     * not gain a lower-case twin.
     */
    @SqlUpdate("""
            INSERT OR IGNORE INTO commodities (commodity, symbol, english_pending)
            SELECT :commodity, :symbol, :englishPending
             WHERE NOT EXISTS (SELECT 1 FROM commodities WHERE LOWER(symbol) = LOWER(:symbol))
            """)
    void insertIfMissing(@Bind("commodity") String commodity,
                         @Bind("symbol") String symbol,
                         @Bind("englishPending") boolean englishPending);

    /**
     * Attaches a symbol to a seeded row that has none - the legacy goods FDevIDs stopped listing - once an
     * English client shows the two together. Until then such a row cannot be matched against the hold.
     */
    @SqlUpdate("UPDATE commodities SET symbol = :symbol WHERE symbol IS NULL AND LOWER(commodity) = LOWER(:commodity)")
    void attachSymbol(@Bind("commodity") String commodity, @Bind("symbol") String symbol);

    /**
     * Supplies a translation the row does not have yet; a column already filled is left exactly as it is,
     * because curated data always outranks a journal sighting.
     */
    @SqlUpdate("UPDATE commodities SET <col> = :name WHERE LOWER(symbol) = LOWER(:symbol) AND <col> IS NULL")
    void fillNameIfNull(@Define("col") String col, @Bind("symbol") String symbol, @Bind("name") String name);

    /**
     * Replaces the symbol stand-in with the real English name, once an English client has supplied one.
     * The UNIQUE constraint is checked explicitly: a name another row already carries is a curation
     * question, not something to write over.
     */
    @SqlUpdate("""
            UPDATE commodities
               SET commodity = :name, english_pending = 0
             WHERE LOWER(symbol) = LOWER(:symbol)
               AND english_pending = 1
               AND NOT EXISTS (SELECT 1 FROM commodities WHERE commodity = :name)
            """)
    void fillEnglishIfPending(@Bind("symbol") String symbol, @Bind("name") String name);
}
