package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@RegisterRowMapper(NeutronStarRouteDao.LegMapper.class)
public interface NeutronStarRouteDao {

    @SqlUpdate("""
            INSERT INTO neutron_star_route
                (leg, systemAddress, systemName, distanceJumped, distanceLeft, jumps, neutronStar, x, y, z)
            VALUES
                (:leg, :systemAddress, :systemName, :distanceJumped, :distanceLeft, :jumps, :neutronStar, :x, :y, :z)
            """)
    void save(@BindBean Route.Leg leg);

    @SqlQuery("SELECT * FROM neutron_star_route ORDER BY leg ASC")
    List<Route.Leg> getAll();

    @SqlQuery("SELECT * FROM neutron_star_route ORDER BY leg ASC LIMIT 1")
    Route.Leg getNextLeg();

    @SqlUpdate("DELETE FROM neutron_star_route WHERE systemAddress = :systemAddress")
    void deleteBySystemAddress(@Bind("systemAddress") long systemAddress);

    @SqlUpdate("DELETE FROM neutron_star_route")
    void clear();


    class LegMapper implements RowMapper<Route.Leg> {
        @Override
        public Route.Leg map(ResultSet rs, StatementContext ctx) throws SQLException {
            Route.Leg leg = new Route.Leg();
            leg.setLeg(rs.getInt("leg"));
            leg.setSystemAddress(rs.getLong("systemAddress"));
            leg.setSystemName(rs.getString("systemName"));
            leg.setDistanceJumped(rs.getDouble("distanceJumped"));
            leg.setDistanceLeft(rs.getDouble("distanceLeft"));
            leg.setJumps(rs.getInt("jumps"));
            leg.setNeutronStar(rs.getBoolean("neutronStar"));
            leg.setX(rs.getDouble("x"));
            leg.setY(rs.getDouble("y"));
            leg.setZ(rs.getDouble("z"));
            return leg;
        }
    }


    class Route {
        private final List<Leg> legs;

        public Route(List<Leg> legs) {
            this.legs = legs;
        }

        public List<Leg> getLegs() {
            return legs;
        }

        public boolean isEmpty() {
            return legs == null || legs.isEmpty();
        }

        public int size() {
            return legs == null ? 0 : legs.size();
        }


        public static class Leg {
            private int leg;
            private long systemAddress;
            private String systemName;
            private double distanceJumped;
            private double distanceLeft;
            private int jumps;
            private boolean neutronStar;
            private double x;
            private double y;
            private double z;

            public int getLeg() {
                return leg;
            }

            public void setLeg(int leg) {
                this.leg = leg;
            }

            public long getSystemAddress() {
                return systemAddress;
            }

            public void setSystemAddress(long systemAddress) {
                this.systemAddress = systemAddress;
            }

            public String getSystemName() {
                return systemName;
            }

            public void setSystemName(String systemName) {
                this.systemName = systemName;
            }

            public double getDistanceJumped() {
                return distanceJumped;
            }

            public void setDistanceJumped(double distanceJumped) {
                this.distanceJumped = distanceJumped;
            }

            public double getDistanceLeft() {
                return distanceLeft;
            }

            public void setDistanceLeft(double distanceLeft) {
                this.distanceLeft = distanceLeft;
            }

            public int getJumps() {
                return jumps;
            }

            public void setJumps(int jumps) {
                this.jumps = jumps;
            }

            public boolean isNeutronStar() {
                return neutronStar;
            }

            public void setNeutronStar(boolean neutronStar) {
                this.neutronStar = neutronStar;
            }

            public double getX() {
                return x;
            }

            public void setX(double x) {
                this.x = x;
            }

            public double getY() {
                return y;
            }

            public void setY(double y) {
                this.y = y;
            }

            public double getZ() {
                return z;
            }

            public void setZ(double z) {
                this.z = z;
            }
        }
    }
}
