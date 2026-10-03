package elite.intel.db.dao;

import elite.intel.db.util.LocationFlags;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

@RegisterRowMapper(LocationDao.LocationMapper.class)
public interface LocationDao {

    /**
     * Shadows the shared {@code location} table with one that also carries this commander's own fields
     * ({@code visitFlags}), so every query below reads its rows exactly as before and the mapper lays the
     * commander's fields over the shared JSON. See {@link LocationFlags}.
     */
    String WITH_VISITS = """
            WITH location AS (SELECT l.id, l.inGameId, l.locationName, l.primaryStar, l.systemAddress, l.json,
                                     v.flags AS visitFlags
                                FROM main.location l
                                LEFT JOIN location_visit v ON v.locationName = l.locationName)
            """;

    /**
     * Writes a location as its two halves: the place itself to the shared file, and what this commander did
     * there to theirs. A location the commander did nothing at leaves no row of theirs behind.
     */
    @Transaction
    default void save(long inGameId, String locationName, String primaryStar, Long systemAddress, String json) {
        LocationFlags.Split split = LocationFlags.split(json);
        upsert(inGameId, locationName, primaryStar, systemAddress, split.sharedJson());
        if (split.flags() == null) {
            deleteVisit(locationName);
        } else {
            upsertVisit(locationName, split.flags());
        }
    }

    @SqlUpdate("""
            INSERT INTO location_visit (locationName, flags) VALUES (:locationName, :flags)
            ON CONFLICT(locationName) DO UPDATE SET flags = excluded.flags
            """)
    void upsertVisit(@Bind("locationName") String locationName, @Bind("flags") String flags);

    @SqlUpdate("DELETE FROM location_visit WHERE locationName = :locationName")
    void deleteVisit(@Bind("locationName") String locationName);

    // WHY primaryStar is updated on conflict: a row is keyed on its name and keeps that name for life, but a
    // carrier's row moves system with the carrier. Left out of the update, the column kept the system the row
    // was first saved in through every jump, so a lookup by system name found the carrier where it no longer was.
    @SqlUpdate("""
            INSERT INTO location (inGameId, locationName, primaryStar, systemAddress, json)
            VALUES (:inGameId, :locationName, :primaryStar, :systemAddress, :json)
            ON CONFLICT(locationName) DO UPDATE SET
                json = excluded.json,
                inGameId = excluded.inGameId,
                primaryStar = excluded.primaryStar,
                systemAddress = excluded.systemAddress
            """)
    void upsert(
            @Bind("inGameId") long inGameId,
            @Bind("locationName") String locationName,
            @Bind("primaryStar") String primaryStar,
            @Bind("systemAddress") Long systemAddress,
            @Bind("json") String json
    );


    @SqlQuery(WITH_VISITS + "select * from location where inGameId = 1 and json like '%\":systemId\"%'")
    LocationDao.Location findBySystemAddressAndInGameId(@Bind("systemId") Long systemId, @Bind("inGameId") Long inGameId);

    @SqlQuery(WITH_VISITS + "SELECT * FROM location WHERE inGameId = :inGameId AND :primaryStar = primaryStar")
    LocationDao.Location findByInGameIdAndPrimaryStar(@Bind("inGameId") Long inGameId, @Bind("primaryStar") String primaryStar);

    @SqlQuery(WITH_VISITS + "SELECT * FROM location WHERE primaryStar = :primaryStar")
    List<Location> findByPrimaryStar(@Bind("primaryStar") String primaryStar);

    @SqlQuery(WITH_VISITS + "select * from location where primaryStar = :starSystem and json like '%\"PRIMARY_STAR\"%'")
    Location findPrimaryStar(@Bind("starSystem") String starSystem);

    /**
     * The name of the system at this address, read from the record's own starName rather than the primaryStar
     * column: until the upsert above learned to update that column, a carrier kept the name of the system it was
     * first saved in through every jump, so the column can still disagree with the record on an older database.
     */
    @SqlQuery("select json->>'$.starName' from location where systemAddress = :systemAddress and json->>'$.starName' is not null and json->>'$.starName' != '' limit 1")
    String findStarName(@Bind("systemAddress") long systemAddress);

    @SqlQuery("""
            SELECT location.primaryStar,
                json ->> '$.X' AS x,
                json ->> '$.Y' AS y,
                json ->> '$.Z' AS z
            from location where primaryStar = (select current_primary_star from player) and json ->> '$.X' != 0 and json ->> '$.Y' !=0 and json ->> '$.Z' !=0 LIMIT 1;
            """)
    @RegisterConstructorMapper(Coordinates.class)
    Coordinates currentCoordinates();

    @SqlQuery(WITH_VISITS + "select * from location where systemAddress = :systemAddress and json->> '$.planetName' = :planetName")
    Location findPrimaryBySystemAddress(@Bind("systemAddress") long systemAddress, @Bind("planetName") String planetName);

    @SqlQuery(WITH_VISITS + "select * from location where systemAddress = :systemAddress and json->> '$.bodyId' = :bodyId")
    Location findPrimaryBySystemAddress(@Bind("systemAddress") long systemAddress, @Bind("bodyId") Long bodyId);

    @SqlQuery(WITH_VISITS + "select * from location where systemAddress = :systemAddress and json like '%\"locationType\": \"PRIMARY_STAR\"%'")
    Location findPrimaryBySystemAddress(@Bind("systemAddress") long systemAddress);

    /**
     * Bodies in one system whose surface scan turned up biological genuses.
     * <p>
     * Filters on the array rather than on a key name because the genus entries changed shape
     * once: rows written before the rename carry the display name under {@code species} and
     * newer ones under {@code genusLocalised}. Both deserialise, and the great majority of
     * stored bodies are the older shape, so matching a key name would find almost none of them.
     */
    @SqlQuery(WITH_VISITS + "select * from location where systemAddress = :systemAddress and json_array_length(json, '$.genus') > 0")
    List<Location> findBioSignalBodies(@Bind("systemAddress") long systemAddress);

    @SqlQuery(WITH_VISITS + "select * from location where systemAddress = :systemAddress")
    List<Location> findAllBySystemAddress(@Bind("systemAddress") long systemAddress);


    @SqlQuery(WITH_VISITS + "select * from location where json ->> '$.marketID' = :marketID")
    Location findByMarketId(@Bind("marketID") long marketID);

    @SqlQuery(WITH_VISITS + "select * from location where locationName = :locationName")
    Location findByLocationName(@Bind("locationName") String locationName);

    @SqlQuery(WITH_VISITS + """
            select * from location  where json like '%"locationType": "STATION"%' and locationName != '' and systemAddress= :systemAddress;
            """)
    List<Location> findStationsInCurrentStarSystem(@Bind("systemAddress") long systemAddress);

    /**
     * Stations we have already visited, in systems that fall inside the given coordinate box.
     * <p>
     * A station row carries no galactic coordinates of its own - only the star it orbits does - so the
     * station is joined to any coordinate-bearing row of the same system. The box is deliberately a cube
     * rather than a sphere: SQLite has no sqrt, so the caller trims the corners with the real distance.
     * A system whose rows all sit at 0,0,0 has no coordinates recorded (that is the unset value, not Sol
     * as far as this row is concerned) and is skipped.
     */
    @SqlQuery("""
            SELECT station.json AS json, sys.x AS x, sys.y AS y, sys.z AS z
            FROM location station
            JOIN (SELECT systemAddress,
                         MAX(CAST(json ->> '$.X' AS REAL)) AS x,
                         MAX(CAST(json ->> '$.Y' AS REAL)) AS y,
                         MAX(CAST(json ->> '$.Z' AS REAL)) AS z
                  FROM location
                  WHERE NOT (json ->> '$.X' = 0 AND json ->> '$.Y' = 0 AND json ->> '$.Z' = 0)
                  GROUP BY systemAddress) sys ON sys.systemAddress = station.systemAddress
            WHERE station.json LIKE '%"locationType": "STATION"%'
              AND station.locationName != ''
              AND sys.x BETWEEN :minX AND :maxX
              AND sys.y BETWEEN :minY AND :maxY
              AND sys.z BETWEEN :minZ AND :maxZ
            """)
    @RegisterConstructorMapper(StationAtCoordinates.class)
    List<StationAtCoordinates> findStationsInCoordinateBox(
            @Bind("minX") double minX, @Bind("maxX") double maxX,
            @Bind("minY") double minY, @Bind("maxY") double maxY,
            @Bind("minZ") double minZ, @Bind("maxZ") double maxZ
    );


    class LocationMapper implements RowMapper<LocationDao.Location> {
        @Override
        public LocationDao.Location map(ResultSet rs, StatementContext ctx) throws SQLException {
            LocationDao.Location entity = new LocationDao.Location(
                    rs.getLong("id"),
                    rs.getLong("inGameId"),
                    rs.getString("locationName"),
                    rs.getString("primaryStar"),
                    rs.getLong("systemAddress"),
                    LocationFlags.merge(rs.getString("json"), visitFlags(rs))
            );
            return entity;
        }

        /**
         * The commander's own fields, when the query was built on {@link #WITH_VISITS}.
         */
        private static String visitFlags(ResultSet rs) throws SQLException {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                if ("visitFlags".equalsIgnoreCase(meta.getColumnLabel(i))) return rs.getString(i);
            }
            return null;
        }
    }

    record Coordinates(String primaryStar,  double x, double y, double z) {

    }

    /**
     * A stored station row together with the galactic coordinates of the system it sits in.
     */
    record StationAtCoordinates(String json, double x, double y, double z) {

    }

    record Location(long id, long inGameId, String locationName, String primaryStar, Long systemAddress, String json) {
        public long getId() {
            return id;
        }

        public long getInGameId() {
            return inGameId;
        }

        public String getLocationName() {
            return locationName;
        }

        public String getPrimaryStar() {
            return primaryStar;
        }

        public Long getSystemAddress() {
            return systemAddress;
        }

        public String getJson() {
            return json;
        }
    }
}
