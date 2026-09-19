package elite.intel.db.dao;

import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The single home for engineering materials: identity, translations, and how many the commander holds.
 * <p>
 * Every row is keyed by {@code symbol} — the journal's non-localized {@code Name} field, e.g.
 * {@code basicconductors} or {@code guardian_powercell}. That is the only material identifier the game
 * guarantees to be stable, so it is what every write path matches on. {@code Name_Localised} is a display
 * string that changes with the client's language and must never be used as a key.
 * <p>
 * This replaces the retired {@code materials} table, which was keyed by
 * {@code capitalizeWords(journal Name)} and so stored the same material under two different spellings
 * depending on which event wrote it.
 */
@RegisterRowMapper(MaterialNameDao.MaterialMapper.class)
public interface MaterialNameDao {

    /**
     * The English name as a spoken form: NULL while the row is a learned one whose English is still
     * pending, because {@code name} then holds the bare symbol as a stand-in for the NOT NULL UNIQUE
     * constraint (see migration 01054) and must never be spoken or matched.
     */
    String ENGLISH_NAME = "CASE WHEN english_pending = 1 THEN NULL ELSE name END";

    /**
     * The display name for a row: the given language column, else English, else the word the game client
     * shows for a name learned from the journal.
     */
    String SPOKEN_NAME = "COALESCE(<col>, " + ENGLISH_NAME + ", <gameCol>)";

    // ── identity ─────────────────────────────────────────────────────────────

    @SqlQuery("SELECT * FROM material_names WHERE symbol = :symbol")
    Material findBySymbol(@Bind("symbol") String symbol);

    @SqlQuery("SELECT * FROM material_names ORDER BY materialType, name")
    List<Material> listAll();

    // ── on-hand amounts ──────────────────────────────────────────────────────

    /**
     * Replaces the held amount. The Materials journal event is a full inventory snapshot, so its
     * counts are absolute and overwrite whatever we had.
     */
    @SqlUpdate("UPDATE material_names SET amount = :amount WHERE symbol = :symbol")
    void setAmount(@Bind("symbol") String symbol, @Bind("amount") int amount);

    /**
     * Adds to the held amount, never exceeding the material's storage cap. Used by MaterialCollected,
     * which reports a delta rather than a total.
     */
    @SqlUpdate("""
            UPDATE material_names
               SET amount = MIN(amount + :delta, COALESCE(maxCapacity, amount + :delta))
             WHERE symbol = :symbol
            """)
    void addAmount(@Bind("symbol") String symbol, @Bind("delta") int delta);

    /**
     * Deducts spent material, flooring at zero. Used by EngineerCraft and Synthesis.
     */
    @SqlUpdate("UPDATE material_names SET amount = MAX(amount - :used, 0) WHERE symbol = :symbol")
    void subtractAmount(@Bind("symbol") String symbol, @Bind("used") int used);

    @SqlUpdate("UPDATE material_names SET amount = 0")
    void clearAmounts();

    /**
     * Replaces the entire held inventory in one transaction: everything not listed goes to zero,
     * except the symbols in {@code preserve}, which are left exactly as they are.
     * <p>
     * Used by the startup reconstruction, whose input is anchored on a Materials snapshot — a full
     * inventory listing that omits anything held at zero — so "absent means none" is exactly right.
     * The transaction matters because the wipe and the rewrite must never be observable apart: a query
     * landing between them would otherwise report an empty hold.
     * <p>
     * {@code preserve} carries the materials the live journal has already reported on since app start.
     * The rebuild runs concurrently with the live parser, and a live report is newer than anything a
     * replay can know, so it wins.
     */
    @Transaction
    default void replaceAllAmounts(Collection<Holding> holdings, Set<String> preserve) {
        for (Material material : listAll()) {
            if (!preserve.contains(material.getSymbol())) setAmount(material.getSymbol(), 0);
        }
        for (Holding holding : holdings) {
            if (preserve.contains(holding.symbol())) continue;
            setAmount(holding.symbol(), holding.amount());
        }
    }

    /**
     * One row of a full-inventory rewrite. The row must already exist - the caller registers unknown
     * symbols first, because which name column a new row gets is a language decision the DAO does not make.
     */
    record Holding(String symbol, int amount) {
    }

    // ── learning ─────────────────────────────────────────────────────────────

    /**
     * Registers a material this build has never seen, so a game update that adds one does not silently
     * drop its count. Seeded rows already exist for everything known as of Odyssey; this is the safety net.
     * <p>
     * {@code name} is the English name when the caller has one, otherwise the bare symbol with
     * {@code englishPending} set - the English column is NOT NULL UNIQUE and a symbol is the one stand-in
     * guaranteed unique. Translations are left null for {@link #fillNameIfNull} to supply.
     */
    @SqlUpdate("""
            INSERT OR IGNORE INTO material_names (symbol, name, materialType, english_pending)
            VALUES (:symbol, :name, :materialType, :englishPending)
            """)
    void insertIfMissing(@Bind("symbol") String symbol,
                         @Bind("name") String name,
                         @Bind("materialType") String materialType,
                         @Bind("englishPending") boolean englishPending);

    /**
     * Supplies a translation the row does not have yet. A column already filled - by a migration or an
     * earlier sighting - is left exactly as it is: curated data always outranks a journal sighting.
     */
    @SqlUpdate("UPDATE material_names SET <col> = :name WHERE symbol = :symbol AND <col> IS NULL")
    void fillNameIfNull(@Define("col") String col, @Bind("symbol") String symbol, @Bind("name") String name);

    /**
     * Replaces the symbol stand-in with the real English name, once an English client has supplied one.
     * Guarded against the UNIQUE constraint explicitly: a name already carried by another row means
     * Frontier renamed a symbol, and that is a curation question rather than something to write over.
     */
    @SqlUpdate("""
            UPDATE material_names
               SET name = :name, english_pending = 0
             WHERE symbol = :symbol
               AND english_pending = 1
               AND NOT EXISTS (SELECT 1 FROM material_names WHERE name = :name)
            """)
    void fillEnglishIfPending(@Bind("symbol") String symbol, @Bind("name") String name);

    // ── name resolution ──────────────────────────────────────────────────────

    /**
     * Every spoken form that can identify a material: the display name in the commander's language
     * (falling back to English where Frontier shipped no translation, which is exactly what that client
     * shows on screen, and then to the game client's own column for a name learned there), the word the
     * game client itself shows, and any aliases. Lower-cased, for fuzzy matching.
     * <p>
     * The game column is offered in its own right so the commander can always say the word on their HUD,
     * whatever language they talk to us in.
     */
    @SqlQuery("SELECT spoken FROM ("
            + " SELECT LOWER(" + SPOKEN_NAME + ") AS spoken FROM material_names"
            + " UNION SELECT LOWER(<gameCol>) FROM material_names"
            + " UNION SELECT LOWER(alias) FROM material_aliases WHERE lang = :lang"
            + ") WHERE spoken IS NOT NULL")
    List<String> getAllSpokenFormsLowerCase(@Define("col") String col,
                                            @Define("gameCol") String gameCol,
                                            @Bind("lang") String lang);

    /**
     * Resolves any spoken form — localized name, the game client's word, or alias — back to the journal symbol.
     */
    @SqlQuery("SELECT symbol FROM ("
            + " SELECT symbol, LOWER(" + SPOKEN_NAME + ") AS spoken FROM material_names"
            + " UNION ALL SELECT symbol, LOWER(<gameCol>) AS spoken FROM material_names"
            + " UNION ALL SELECT symbol, LOWER(alias) AS spoken FROM material_aliases WHERE lang = :lang"
            + ") WHERE spoken = LOWER(:spokenForm) LIMIT 1")
    String getSymbolBySpokenForm(@Define("col") String col,
                                 @Define("gameCol") String gameCol,
                                 @Bind("lang") String lang,
                                 @Bind("spokenForm") String spokenForm);

    /**
     * The display name to speak: the given language column, else English, else the word the game client
     * shows for a name learned from the journal. Null only for a symbol that is unknown or has no name yet.
     */
    @SqlQuery("SELECT " + SPOKEN_NAME + " FROM material_names WHERE symbol = :symbol")
    String getLocalizedNameBySymbol(@Define("col") String col,
                                    @Define("gameCol") String gameCol,
                                    @Bind("symbol") String symbol);

    @SqlQuery("SELECT COUNT(*) FROM material_names")
    int count();

    class MaterialMapper implements RowMapper<Material> {
        @Override
        public Material map(ResultSet rs, StatementContext ctx) throws SQLException {
            // maxCapacity and grade are null for Frontier's "Unknown" placeholder row, which is a
            // display string rather than a real material, and for any material learned from the journal,
            // which never states a grade. Read each immediately before its wasNull() check — wasNull()
            // reports on the most recent getter call only.
            int maxCapacity = rs.getInt("maxCapacity");
            if (rs.wasNull()) maxCapacity = 0;
            int grade = rs.getInt("grade");
            if (rs.wasNull()) grade = 0;
            return new Material(
                    rs.getLong("id"),
                    rs.getString("symbol"),
                    rs.getBoolean("english_pending") ? null : rs.getString("name"),
                    rs.getString("materialType"),
                    rs.getInt("amount"),
                    maxCapacity,
                    grade
            );
        }
    }

    class Material implements ToJsonConvertible {
        private final long id;
        private final String symbol;
        private final String name;
        private final String materialType;
        private final int amount;
        private final int maxCapacity;
        private final int grade;

        public Material(long id, String symbol, String name, String materialType,
                        int amount, int maxCapacity, int grade) {
            this.id = id;
            this.symbol = symbol;
            this.name = name;
            this.materialType = materialType;
            this.amount = amount;
            this.maxCapacity = maxCapacity;
            this.grade = grade;
        }

        public long getId() {
            return id;
        }

        /**
         * The journal's non-localized {@code Name}, e.g. {@code basicconductors}. The identity of the row.
         */
        public String getSymbol() {
            return symbol;
        }

        /**
         * The English display name, e.g. {@code Basic Conductors}; null for a material learned from a
         * non-English client whose English name is still pending.
         */
        public String getName() {
            return name;
        }

        public String getMaterialType() {
            return materialType;
        }

        public int getAmount() {
            return amount;
        }

        /**
         * The storage cap, or 0 when it is not known - a learned material, whose grade the journal never
         * states. Callers treat 0 as "say nothing about capacity", never as a full hold.
         */
        public int getMaxCapacity() {
            return maxCapacity;
        }

        /**
         * Frontier's material grade, 1 (very common) to 5 (very rare). Determines the storage cap.
         */
        public int getGrade() {
            return grade;
        }

        @Override
        public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }
    }
}
