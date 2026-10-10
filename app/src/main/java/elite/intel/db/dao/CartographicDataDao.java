package elite.intel.db.dao;

import elite.intel.gameapi.cartography.CartographicBody;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * The unsold exploration data each ship carries: the bodies scanned and the systems honked.
 */
@RegisterRowMapper(CartographicDataDao.BodyMapper.class)
@RegisterRowMapper(CartographicDataDao.SystemMapper.class)
public interface CartographicDataDao {

    /**
     * Records a body as the scan describes it. A repeat scan refreshes the description but never forgets a
     * mapping already done.
     */
    @SqlUpdate("""
            INSERT INTO cartographic_body (shipId, bodyName, systemAddress, starSystem, primaryStar, starType,
                                           stellarMass, planetClass, terraformable, massEM, wasDiscovered, wasMapped)
            VALUES (:shipId, :bodyName, :systemAddress, :starSystem, :primaryStar, :starType,
                    :stellarMass, :planetClass, :terraformable, :massEM, :wasDiscovered, :wasMapped)
            ON CONFLICT (shipId, bodyName) DO UPDATE SET
                systemAddress = excluded.systemAddress,
                starSystem    = excluded.starSystem,
                primaryStar   = excluded.primaryStar,
                starType      = excluded.starType,
                stellarMass   = excluded.stellarMass,
                planetClass   = excluded.planetClass,
                terraformable = excluded.terraformable,
                massEM        = excluded.massEM,
                wasDiscovered = excluded.wasDiscovered,
                wasMapped     = excluded.wasMapped
            """)
    void upsertBody(@Bind("shipId") long shipId,
                    @Bind("bodyName") String bodyName,
                    @Bind("systemAddress") long systemAddress,
                    @Bind("starSystem") String starSystem,
                    @Bind("primaryStar") boolean primaryStar,
                    @Bind("starType") String starType,
                    @Bind("stellarMass") double stellarMass,
                    @Bind("planetClass") String planetClass,
                    @Bind("terraformable") boolean terraformable,
                    @Bind("massEM") double massEM,
                    @Bind("wasDiscovered") boolean wasDiscovered,
                    @Bind("wasMapped") boolean wasMapped);

    @SqlUpdate("UPDATE cartographic_body SET mapped = 1, efficient = :efficient WHERE shipId = :shipId AND bodyName = :bodyName")
    int markMapped(@Bind("shipId") long shipId, @Bind("bodyName") String bodyName, @Bind("efficient") boolean efficient);

    @SqlUpdate("""
            INSERT INTO cartographic_system (shipId, systemAddress, starSystem, honkBodyCount)
            VALUES (:shipId, :systemAddress, :starSystem, :honkBodyCount)
            ON CONFLICT (shipId, systemAddress) DO UPDATE SET
                starSystem    = excluded.starSystem,
                honkBodyCount = excluded.honkBodyCount
            """)
    void upsertHonk(@Bind("shipId") long shipId,
                    @Bind("systemAddress") long systemAddress,
                    @Bind("starSystem") String starSystem,
                    @Bind("honkBodyCount") int honkBodyCount);

    @SqlUpdate("UPDATE cartographic_system SET allBodiesFound = 1 WHERE shipId = :shipId AND systemAddress = :systemAddress")
    int markAllBodiesFound(@Bind("shipId") long shipId, @Bind("systemAddress") long systemAddress);

    @SqlQuery("SELECT * FROM cartographic_body WHERE shipId = :shipId")
    List<StoredBody> bodies(@Bind("shipId") long shipId);

    @SqlQuery("SELECT * FROM cartographic_system WHERE shipId = :shipId")
    List<StoredSystem> systems(@Bind("shipId") long shipId);

    @SqlUpdate("DELETE FROM cartographic_body WHERE shipId = :shipId AND starSystem = :starSystem")
    int deleteBodiesOf(@Bind("shipId") long shipId, @Bind("starSystem") String starSystem);

    @SqlUpdate("DELETE FROM cartographic_system WHERE shipId = :shipId AND starSystem = :starSystem")
    int deleteSystem(@Bind("shipId") long shipId, @Bind("starSystem") String starSystem);

    @SqlUpdate("DELETE FROM cartographic_body WHERE shipId = :shipId")
    int deleteAllBodies(@Bind("shipId") long shipId);

    @SqlUpdate("DELETE FROM cartographic_system WHERE shipId = :shipId")
    int deleteAllSystems(@Bind("shipId") long shipId);

    /**
     * A body row: the body, and the system it is filed under.
     */
    record StoredBody(long systemAddress, CartographicBody body) {
    }

    /**
     * A honked system.
     */
    record StoredSystem(long systemAddress, int honkBodyCount, boolean allBodiesFound) {
    }

    class BodyMapper implements RowMapper<StoredBody> {
        @Override
        public StoredBody map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new StoredBody(rs.getLong("systemAddress"), new CartographicBody(
                    rs.getString("bodyName"),
                    rs.getBoolean("primaryStar"),
                    rs.getString("starType"),
                    rs.getDouble("stellarMass"),
                    rs.getString("planetClass"),
                    rs.getBoolean("terraformable"),
                    rs.getDouble("massEM"),
                    rs.getBoolean("wasDiscovered"),
                    rs.getBoolean("wasMapped"),
                    rs.getBoolean("mapped"),
                    rs.getBoolean("efficient")));
        }
    }

    class SystemMapper implements RowMapper<StoredSystem> {
        @Override
        public StoredSystem map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new StoredSystem(rs.getLong("systemAddress"), rs.getInt("honkBodyCount"),
                    rs.getBoolean("allBodiesFound"));
        }
    }
}
