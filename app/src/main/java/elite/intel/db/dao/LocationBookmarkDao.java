package elite.intel.db.dao;

import elite.intel.gameapi.bookmarks.LocationBookmark;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * The commander's bookmarked places, read back in the commander's own order - the order they are numbered in.
 * A new bookmark goes to the bottom.
 */
@RegisterRowMapper(LocationBookmarkDao.BookmarkMapper.class)
public interface LocationBookmarkDao {

    @SqlUpdate("""
            INSERT INTO location_bookmark (kind, starSystem, stationName, planetName, latitude, longitude, displayName,
                                           createdAt, position)
            VALUES (:kind, :starSystem, :stationName, :planetName, :latitude, :longitude, :displayName, :createdAt,
                    (SELECT COALESCE(MAX(position), 0) + 1 FROM location_bookmark))
            """)
    void insert(@Bind("kind") String kind,
                @Bind("starSystem") String starSystem,
                @Bind("stationName") String stationName,
                @Bind("planetName") String planetName,
                @Bind("latitude") Double latitude,
                @Bind("longitude") Double longitude,
                @Bind("displayName") String displayName,
                @Bind("createdAt") long createdAt);

    @SqlQuery("SELECT * FROM location_bookmark ORDER BY position, id")
    List<LocationBookmark> inOrder();

    @SqlQuery("SELECT COUNT(*) FROM location_bookmark")
    int count();

    @SqlUpdate("DELETE FROM location_bookmark WHERE id = :id")
    int delete(@Bind("id") long id);

    @SqlUpdate("UPDATE location_bookmark SET displayName = :displayName WHERE id = :id")
    int rename(@Bind("id") long id, @Bind("displayName") String displayName);

    @SqlQuery("SELECT position FROM location_bookmark WHERE id = :id")
    int position(@Bind("id") long id);

    @SqlUpdate("UPDATE location_bookmark SET position = :position WHERE id = :id")
    void setPosition(@Bind("id") long id, @Bind("position") int position);

    /**
     * Trades the places of two bookmarks on the list.
     */
    @Transaction
    default void swap(long id, long otherId) {
        int position = position(id);
        setPosition(id, position(otherId));
        setPosition(otherId, position);
    }

    class BookmarkMapper implements RowMapper<LocationBookmark> {
        @Override
        public LocationBookmark map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new LocationBookmark(
                    rs.getLong("id"),
                    LocationBookmark.Kind.valueOf(rs.getString("kind")),
                    rs.getString("starSystem"),
                    rs.getString("stationName"),
                    rs.getString("planetName"),
                    nullableDouble(rs, "latitude"),
                    nullableDouble(rs, "longitude"),
                    rs.getString("displayName"));
        }

        private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
            double value = rs.getDouble(column);
            return rs.wasNull() ? null : value;
        }
    }
}
