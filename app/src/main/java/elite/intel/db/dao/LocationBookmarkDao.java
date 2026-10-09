package elite.intel.db.dao;

import elite.intel.gameapi.bookmarks.LocationBookmark;
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
 * The commander's bookmarked places, read back newest first - the order they are numbered in.
 */
@RegisterRowMapper(LocationBookmarkDao.BookmarkMapper.class)
public interface LocationBookmarkDao {

    @SqlUpdate("""
            INSERT INTO location_bookmark (kind, starSystem, stationName, planetName, latitude, longitude, createdAt)
            VALUES (:kind, :starSystem, :stationName, :planetName, :latitude, :longitude, :createdAt)
            """)
    void insert(@Bind("kind") String kind,
                @Bind("starSystem") String starSystem,
                @Bind("stationName") String stationName,
                @Bind("planetName") String planetName,
                @Bind("latitude") Double latitude,
                @Bind("longitude") Double longitude,
                @Bind("createdAt") long createdAt);

    @SqlQuery("SELECT * FROM location_bookmark ORDER BY id DESC")
    List<LocationBookmark> newestFirst();

    @SqlQuery("SELECT COUNT(*) FROM location_bookmark")
    int count();

    @SqlUpdate("DELETE FROM location_bookmark WHERE id = :id")
    int delete(@Bind("id") long id);

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
                    nullableDouble(rs, "longitude"));
        }

        private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
            double value = rs.getDouble(column);
            return rs.wasNull() ? null : value;
        }
    }
}
