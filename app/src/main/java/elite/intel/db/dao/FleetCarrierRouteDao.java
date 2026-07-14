package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@RegisterRowMapper(FleetCarrierRouteDao.FleetCarrierRouteMapper.class)
public interface FleetCarrierRouteDao {

    @SqlUpdate("""
            INSERT INTO fleet_carrier_route
                    (leg, distance, systemName, fuelUsed, remainingFuel, hasIcyRing, isPristine, x, y, z)
            VALUES (:leg, :distance, :systemName, :fuelUsed, :remainingFuel, :hasIcyRing, :pristine, :x, :y, :z)
    """)
    void save(@BindBean FleetCarrierRouteLeg leg);

    @SqlQuery("SELECT * FROM fleet_carrier_route")
    List<FleetCarrierRouteDao.FleetCarrierRouteLeg> getAll();

    @SqlUpdate("DELETE FROM fleet_carrier_route")
    void clear();

    /**
     * The route table only ever holds the legs still to fly, so every change to it is a whole-table
     * replacement. Transactional: a crash between the delete and the inserts would otherwise leave
     * the commander with no route at all.
     */
    @Transaction
    default void replaceAll(List<FleetCarrierRouteLeg> legs) {
        clear();
        for (FleetCarrierRouteLeg leg : legs) {
            save(leg);
        }
    }

    /**
     * WHY TRIM and NOCASE: the stored name comes from Spansh and the name looked up comes from the
     * journal. They name the same system but need not agree on case or padding, and an exact match
     * that misses reads an arrival as off-route.
     */
    @SqlQuery("SELECT * FROM fleet_carrier_route WHERE TRIM(systemName) COLLATE NOCASE = :starSystem")
    FleetCarrierRouteLeg findByPrimaryStarName(String starSystem);


    class FleetCarrierRouteMapper implements RowMapper<FleetCarrierRouteLeg> {

        @Override public FleetCarrierRouteLeg map(ResultSet rs, StatementContext ctx) throws SQLException {
            FleetCarrierRouteLeg route = new FleetCarrierRouteLeg();
            route.setLeg(rs.getInt("leg"));
            route.setDistance(rs.getDouble("distance"));
            route.setSystemName(rs.getString("systemName"));
            route.setFuelUsed(rs.getInt("fuelUsed"));
            route.setRemainingFuel(rs.getInt("remainingFuel"));
            route.setHasIcyRing(rs.getBoolean("hasIcyRing"));
            route.setPristine(rs.getBoolean("isPristine"));
            route.setX(rs.getDouble("x"));
            route.setY(rs.getDouble("y"));
            route.setZ(rs.getDouble("z"));
            return route;
        }
    }


    class FleetCarrierRouteLeg {
        private Integer leg;
        private String systemName;
        private Double distance;
        private Integer fuelUsed;
        private Integer remainingFuel;
        private Boolean hasIcyRing;
        private Boolean pristine;
        private Double x, y, z;

        public FleetCarrierRouteLeg() {
        }

        public Integer getLeg() {
            return leg;
        }

        public void setLeg(Integer leg) {
            this.leg = leg;
        }

        public String getSystemName() {
            return systemName;
        }

        public void setSystemName(String systemName) {
            this.systemName = systemName;
        }

        public Double getDistance() {
            return distance;
        }

        public void setDistance(Double distance) {
            this.distance = distance;
        }

        public Integer getFuelUsed() {
            return fuelUsed;
        }

        public void setFuelUsed(Integer fuelUsed) {
            this.fuelUsed = fuelUsed;
        }

        public Integer getRemainingFuel() {
            return remainingFuel;
        }

        public void setRemainingFuel(Integer remainingFuel) {
            this.remainingFuel = remainingFuel;
        }

        public Boolean getHasIcyRing() {
            return hasIcyRing;
        }

        public void setHasIcyRing(Boolean hasIcyRing) {
            this.hasIcyRing = hasIcyRing;
        }

        public Boolean getPristine() {
            return pristine;
        }

        public void setPristine(Boolean pristine) {
            this.pristine = pristine;
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
    }
}
