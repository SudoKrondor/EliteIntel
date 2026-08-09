package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@RegisterRowMapper(ShipRouteDao.RouteMapper.class)
public interface ShipRouteDao {


    @SqlUpdate("""
            INSERT INTO ship_route 
                    (leg, systemName, remainingJumps, starClass, systemName, scoopable, x, y, z) 
                    VALUES (:leg, :systemName, :remainingJumps, :starClass, :systemName, :scoopable, :x, :y, :z)
                        ON CONFLICT(leg) DO UPDATE SET
                            systemName = excluded.systemName,
                            remainingJumps = excluded.remainingJumps,
                            starClass = excluded.starClass,
                            scoopable = excluded.scoopable,
                            x = excluded.x,
                            y = excluded.y,
                            z = excluded.z
            """)
    void upsert(@BindBean ShipRouteDao.Route leg);

    @SqlQuery("SELECT * FROM ship_route order by leg")
    List<ShipRouteDao.Route> getAll();

    /**
     * System the plotted route ends at, or null when no route is plotted.
     * <p>
     * Its own query rather than the last element of {@link #getAll()}: callers
     * poll this frequently and a long route is a hundred rows.
     */
    @SqlQuery("SELECT systemName FROM ship_route ORDER BY leg DESC LIMIT 1")
    String getFinalSystemName();

    @SqlUpdate("DELETE FROM ship_route where systemName = :starSystem")
    void delete(String starSystem);

    @SqlUpdate("DELETE FROM ship_route")
    void clear();





    class RouteMapper implements RowMapper<ShipRouteDao.Route> {

        @Override public Route map(ResultSet rs, StatementContext ctx) throws SQLException {
            Route route = new Route();
            route.leg = rs.getInt("leg");
            route.x = rs.getDouble("x");
            route.y = rs.getDouble("y");
            route.z = rs.getDouble("z");
            route.remainingJumps = rs.getInt("remainingJumps");
            route.starClass = rs.getString("starClass");
            route.systemName = rs.getString("systemName");
            route.scoopable = rs.getBoolean("scoopable");
            return route;
        }
    }


    class Route {
        private Integer leg;
        private Double x, y, z;
        private Integer remainingJumps;
        private String starClass;
        private String systemName;
        private Boolean scoopable;
        public Route() {
        }

        public Integer getLeg() {
            return leg;
        }

        public void setLeg(Integer leg) {
            this.leg = leg;
        }

        public Double getX() {
            return x;
        }

        public void setX(Double x) {
            this.x = x;
        }

        public Double getY() {
            return y;
        }

        public void setY(Double y) {
            this.y = y;
        }

        public Double getZ() {
            return z;
        }

        public void setZ(Double z) {
            this.z = z;
        }

        public Integer getRemainingJumps() {
            return remainingJumps;
        }

        public void setRemainingJumps(Integer remainingJumps) {
            this.remainingJumps = remainingJumps;
        }

        public String getStarClass() {
            return starClass;
        }

        public void setStarClass(String starClass) {
            this.starClass = starClass;
        }

        public String getSystemName() {
            return systemName;
        }

        public void setSystemName(String systemName) {
            this.systemName = systemName;
        }

        public Boolean getScoopable() {
            return scoopable;
        }

        public void setScoopable(Boolean scoopable) {
            this.scoopable = scoopable;
        }
    }
}
