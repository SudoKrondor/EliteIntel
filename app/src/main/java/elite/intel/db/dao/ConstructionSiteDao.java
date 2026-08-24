package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * The colonisation construction sites the commander has hauled to, and what each still wants.
 * <p>
 * Every parameter is annotated: an IDE build does not pass {@code -parameters}, so an unannotated one
 * binds to nothing and fails at runtime rather than at compile time.
 */
@RegisterRowMapper(ConstructionSiteDao.SiteMapper.class)
@RegisterRowMapper(ConstructionSiteDao.RequirementMapper.class)
public interface ConstructionSiteDao {

    @SqlUpdate("""
            INSERT INTO construction_site (marketId, stationName, starSystem, systemAddress, progress, complete, failed, visitedAt)
            VALUES (:marketId, :stationName, :starSystem, :systemAddress, :progress, :complete, :failed, :visitedAt)
            ON CONFLICT(marketId) DO UPDATE SET
                stationName   = COALESCE(excluded.stationName, construction_site.stationName),
                starSystem    = COALESCE(excluded.starSystem, construction_site.starSystem),
                systemAddress = COALESCE(excluded.systemAddress, construction_site.systemAddress),
                progress      = excluded.progress,
                complete      = excluded.complete,
                failed        = excluded.failed,
                visitedAt     = excluded.visitedAt
            """)
    void saveSite(@BindBean Site site);

    @SqlQuery("SELECT * FROM construction_site WHERE marketId = :marketId")
    Site findSite(@Bind("marketId") long marketId);

    /**
     * The build the commander is working on, or null when none is - either they have never visited one, or
     * they have said they are done with the last.
     */
    @SqlQuery("SELECT * FROM construction_site WHERE isCurrent = 1 LIMIT 1")
    Site currentSite();

    @SqlUpdate("UPDATE construction_site SET isCurrent = 0")
    void clearCurrent();

    @SqlUpdate("UPDATE construction_site SET isCurrent = 1 WHERE marketId = :marketId")
    void markCurrent(@Bind("marketId") long marketId);

    /**
     * Makes one site the current build, which by definition demotes every other. Landing at a second depot
     * is all it takes - there is no separate "switch build" step for a commander to forget.
     */
    @Transaction
    default void makeCurrent(long marketId) {
        clearCurrent();
        markCurrent(marketId);
    }

    @SqlUpdate("UPDATE construction_site SET visitedAt = :visitedAt WHERE marketId = :marketId")
    void touchVisitedAt(@Bind("marketId") long marketId, @Bind("visitedAt") String visitedAt);

    /**
     * Records that the ship is standing on this depot's pad right now, without touching its manifest.
     * <p>
     * WHY this is separate from {@link #replaceManifest}: that one is guarded by a fingerprint, because the
     * game republishes an unchanged manifest every 15-30 seconds and rewriting seventeen rows each time is
     * work for nothing. But being ON the pad is not a fact about the manifest, and hanging it off a write
     * that an unchanged manifest skips is how the card went on showing the LAST site the commander landed at
     * after they flew back to the first one - measured live at Divis Gateway and Johri Horizons.
     * <p>
     * The timestamp moves too. An identical republish is still a fresh reading of the site's own panel, so a
     * manifest that has not moved in hours must not be captioned AS OF LAST VISIT while the commander is
     * standing in front of it.
     */
    @Transaction
    default void arrivedAt(long marketId, String visitedAt) {
        makeCurrent(marketId);
        if (visitedAt != null && !visitedAt.isBlank()) touchVisitedAt(marketId, visitedAt);
    }

    @SqlQuery("SELECT * FROM construction_site ORDER BY visitedAt DESC")
    List<Site> listSites();

    @SqlUpdate("""
            INSERT OR REPLACE INTO construction_requirement (marketId, symbol, gameName, requiredAmount, providedAmount, payment)
            VALUES (:marketId, :symbol, :gameName, :requiredAmount, :providedAmount, :payment)
            """)
    void saveRequirement(@BindBean Requirement requirement);

    /**
     * Clears a site's manifest before the incoming one is written. The journal republishes the manifest
     * in full, so a line that has vanished from it - the architect changed the build - has to vanish here
     * too, and updating row by row would leave it behind forever.
     */
    @SqlUpdate("DELETE FROM construction_requirement WHERE marketId = :marketId")
    void clearRequirements(@Bind("marketId") long marketId);

    @SqlQuery("SELECT * FROM construction_requirement WHERE marketId = :marketId ORDER BY symbol")
    List<Requirement> listRequirements(@Bind("marketId") long marketId);

    /**
     * How many of a site's lines still want tonnes. What the shopping command's visibility turns on, kept
     * as a count so the check does not have to read and map the whole manifest on every classification.
     */
    @SqlQuery("SELECT COUNT(*) FROM construction_requirement WHERE marketId = :marketId AND requiredAmount > providedAmount")
    int countOutstanding(@Bind("marketId") long marketId);

    /**
     * Writes a site and its whole manifest as one unit.
     * <p>
     * Transactional because the two halves are one fact: a reader that caught the gap between the delete
     * and the re-insert would see a site with an empty shopping list and report the build finished.
     */
    @Transaction
    default void replaceManifest(Site site, List<Requirement> requirements) {
        saveSite(site);
        // Standing on this pad is what makes it the current build - see makeCurrent.
        makeCurrent(site.getMarketId());
        clearRequirements(site.getMarketId());
        for (Requirement requirement : requirements == null ? List.<Requirement>of() : requirements) {
            if (requirement == null || requirement.getSymbol() == null) continue;
            requirement.setMarketId(site.getMarketId());
            saveRequirement(requirement);
        }
    }

    @SqlUpdate("DELETE FROM construction_site")
    void clearSites();

    @SqlUpdate("DELETE FROM construction_requirement")
    void clearAllRequirements();

    class SiteMapper implements RowMapper<Site> {
        @Override
        public Site map(ResultSet rs, StatementContext ctx) throws SQLException {
            Site site = new Site();
            site.setMarketId(rs.getLong("marketId"));
            site.setStationName(rs.getString("stationName"));
            site.setStarSystem(rs.getString("starSystem"));
            long systemAddress = rs.getLong("systemAddress");
            site.setSystemAddress(rs.wasNull() ? null : systemAddress);
            site.setProgress(rs.getDouble("progress"));
            site.setComplete(rs.getBoolean("complete"));
            site.setFailed(rs.getBoolean("failed"));
            site.setVisitedAt(rs.getString("visitedAt"));
            return site;
        }
    }

    class RequirementMapper implements RowMapper<Requirement> {
        @Override
        public Requirement map(ResultSet rs, StatementContext ctx) throws SQLException {
            Requirement requirement = new Requirement();
            requirement.setMarketId(rs.getLong("marketId"));
            requirement.setSymbol(rs.getString("symbol"));
            requirement.setGameName(rs.getString("gameName"));
            requirement.setRequiredAmount(rs.getInt("requiredAmount"));
            requirement.setProvidedAmount(rs.getInt("providedAmount"));
            requirement.setPayment(rs.getLong("payment"));
            return requirement;
        }
    }

    /**
     * One construction site, as of the last time the commander was on its pad.
     */
    class Site {
        private long marketId;
        private String stationName;
        private String starSystem;
        private Long systemAddress;
        private double progress;
        private boolean complete;
        private boolean failed;
        private String visitedAt;

        public Site() {
        }

        public long getMarketId() {
            return marketId;
        }

        public void setMarketId(long marketId) {
            this.marketId = marketId;
        }

        public String getStationName() {
            return stationName;
        }

        public void setStationName(String stationName) {
            this.stationName = stationName;
        }

        public String getStarSystem() {
            return starSystem;
        }

        public void setStarSystem(String starSystem) {
            this.starSystem = starSystem;
        }

        public Long getSystemAddress() {
            return systemAddress;
        }

        public void setSystemAddress(Long systemAddress) {
            this.systemAddress = systemAddress;
        }

        /**
         * Delivered tonnes over required tonnes, in {@code [0,1]}, as the journal reported it.
         */
        public double getProgress() {
            return progress;
        }

        public void setProgress(double progress) {
            this.progress = progress;
        }

        public boolean isComplete() {
            return complete;
        }

        public void setComplete(boolean complete) {
            this.complete = complete;
        }

        public boolean isFailed() {
            return failed;
        }

        public void setFailed(boolean failed) {
            this.failed = failed;
        }

        /**
         * ISO-8601 timestamp of the last manifest we saw from this site's pad.
         */
        public String getVisitedAt() {
            return visitedAt;
        }

        public void setVisitedAt(String visitedAt) {
            this.visitedAt = visitedAt;
        }
    }

    /**
     * One line of a site's manifest.
     */
    class Requirement {
        private long marketId;
        private String symbol;
        private String gameName;
        private int requiredAmount;
        private int providedAmount;
        private long payment;

        public Requirement() {
        }

        public long getMarketId() {
            return marketId;
        }

        public void setMarketId(long marketId) {
            this.marketId = marketId;
        }

        /**
         * Bare lower-case journal symbol; joins with the cargo hold and the commodities table.
         */
        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        /**
         * The commodity as the GAME named it, for a good the commodities table carries no symbol for.
         */
        public String getGameName() {
            return gameName;
        }

        public void setGameName(String gameName) {
            this.gameName = gameName;
        }

        public int getRequiredAmount() {
            return requiredAmount;
        }

        public void setRequiredAmount(int requiredAmount) {
            this.requiredAmount = requiredAmount;
        }

        /**
         * Delivered by everyone who has hauled here, not just by us.
         */
        public int getProvidedAmount() {
            return providedAmount;
        }

        public void setProvidedAmount(int providedAmount) {
            this.providedAmount = providedAmount;
        }

        public long getPayment() {
            return payment;
        }

        public void setPayment(long payment) {
            this.payment = payment;
        }

        /**
         * Tonnes the site still wants. Never negative: an over-delivered line is finished, not owed.
         */
        public int outstanding() {
            return Math.max(0, requiredAmount - providedAmount);
        }
    }
}
