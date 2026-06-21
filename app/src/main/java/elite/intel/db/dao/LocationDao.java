package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@RegisterRowMapper(LocationDao.LocationMapper.class)
public interface LocationDao {

    @SqlUpdate("""
            INSERT INTO location (inGameId, locationName, primaryStar, systemAddress, json)
            VALUES (:inGameId, :locationName, :primaryStar, :systemAddress, :json)
            ON CONFLICT(locationName) DO UPDATE SET
                json = excluded.json,
                inGameId = excluded.inGameId,
                systemAddress = excluded.systemAddress
            """)
    void upsert(
            @Bind("inGameId") long inGameId,
            @Bind("locationName") String locationName,
            @Bind("primaryStar") String primaryStar,
            @Bind("systemAddress") Long systemAddress,
            @Bind("json") String json
    );


    @SqlQuery("select * from location where inGameId = 1 and json like '%\":systemId\"%'")
    LocationDao.Location findBySystemAddressAndInGameId(@Bind("systemId") Long systemId, @Bind("inGameId") Long inGameId);

    @SqlQuery("SELECT * FROM location WHERE inGameId = :inGameId AND :primaryStar = primaryStar")
    LocationDao.Location findByInGameIdAndPrimaryStar(Long inGameId, String primaryStar);

    @SqlQuery("SELECT * FROM location WHERE primaryStar = :primaryStar")
    List<Location> findByPrimaryStar(@Bind("primaryStar") String primaryStar);

    @SqlQuery("select * from location where primaryStar = :starSystem and json like '%\"PRIMARY_STAR\"%'")
    Location findPrimaryStar(String starSystem);

    @SqlQuery("""
            SELECT location.primaryStar,
                json ->> '$.X' AS x,
                json ->> '$.Y' AS y,
                json ->> '$.Z' AS z
            from location where primaryStar = (select current_primary_star from player) and json ->> '$.X' != 0 and json ->> '$.Y' !=0 and json ->> '$.Z' !=0 LIMIT 1;
            """)
    @RegisterConstructorMapper(Coordinates.class)
    Coordinates currentCoordinates();

    @SqlQuery("select * from location where systemAddress = :systemAddress and json->> '$.planetName' = :planetName")
    Location findPrimaryBySystemAddress(long systemAddress, String planetName);

    @SqlQuery("select * from location where systemAddress = :systemAddress and json->> '$.bodyId' = :bodyId")
    Location findPrimaryBySystemAddress(long systemAddress, Long bodyId);

    @SqlQuery("select * from location where systemAddress = :systemAddress and json like '%\"locationType\": \"PRIMARY_STAR\"%'")
    Location findPrimaryBySystemAddress(long systemAddress);

    @SqlQuery("select * from location where systemAddress = :systemAddress")
    List<Location> findAllBySystemAddress(long systemAddress);


    @SqlQuery("select * from location where json ->> '$.marketID' = :marketID")
    Location findByMarketId(long marketID);

    @SqlQuery("select * from location where locationName = :locationName")
    Location findByLocationName(@Bind("locationName") String locationName);

    @SqlQuery(""" 
            select * from location  where json like '%"locationType": "STATION"%' and locationName != '' and systemAddress= :systemAddress;
            """)
    List<Location> findStationsInCurrentStarSystem(@Bind("systemAddress") long systemAddress);


    class LocationMapper implements RowMapper<LocationDao.Location> {
        @Override
        public LocationDao.Location map(ResultSet rs, StatementContext ctx) throws SQLException {
            LocationDao.Location entity = new LocationDao.Location(
                    rs.getLong("id"),
                    rs.getLong("inGameId"),
                    rs.getString("locationName"),
                    rs.getString("primaryStar"),
                    rs.getLong("systemAddress"),
                    rs.getString("json")
            );
            return entity;
        }
    }

    record Coordinates(String primaryStar,  double x, double y, double z) {

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
