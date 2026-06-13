package elite.intel.db.dao;

import elite.intel.gameapi.MissionType;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@RegisterRowMapper(MissionDao.MissionMapper.class)
public interface MissionDao {

    @SqlUpdate("""
            INSERT OR REPLACE INTO missions (key, missionType, mission, keywords)
            VALUES(:key, :missionType, :mission, :keywords)
            ON CONFLICT(key) DO UPDATE SET
            mission = excluded.mission,
            missionType = excluded.missionType,
            keywords=excluded.keywords
            """)
    void upsert(@BindBean Mission mission, @Bind String missionType);


    @SqlQuery("SELECT * FROM missions WHERE key = :key")
    Mission get(@Bind("key") Long key);

    @SqlQuery("SELECT * FROM missions WHERE missionType IN (<missionTypes>)")
    List<Mission> findForMissionType(@BindList("missionTypes") MissionType... missionTypes);

    @SqlQuery("SELECT * FROM missions")
    List<Mission> findAny();

    @SqlQuery("SELECT DISTINCT missionType FROM missions")
    String[] getAvailableMissionTypes();

    @SqlUpdate("DELETE FROM missions WHERE key = :missionId")
    void delete(Long missionId);

    @SqlQuery("SELECT * FROM missions WHERE LOWER(keywords) LIKE '%' || LOWER(:keyword) || '%'")
    List<Mission> findByKeyword(@Bind("keyword") String keyword);

    @SqlUpdate("delete from missions where id > 0")
    void clear();


    class MissionMapper implements RowMapper<Mission> {

        @Override public Mission map(ResultSet rs, StatementContext ctx) throws SQLException {
            Mission mission = new Mission();
            mission.setKey(rs.getLong("key"));
            mission.setMission(rs.getString("mission"));
            mission.setKeywords(rs.getString("keywords"));
            mission.setMissionType(MissionType.valueOf(rs.getString("missionType")));
            return mission;
        }
    }


    class Mission {
        private Long key;
        private String mission;
        private String keywords;
        private MissionType missionType;

        public Mission() {
        }

        public Long getKey() {
            return key;
        }

        public void setKey(Long key) {
            this.key = key;
        }

        public String getMission() {
            return mission;
        }

        public void setMission(String mission) {
            this.mission = mission;
        }

        public MissionType getMissionType() {
            return missionType;
        }

        public void setMissionType(MissionType missionType) {
            this.missionType = missionType;
        }

        public String getKeywords() {
            return keywords;
        }

        public void setKeywords(String keywords) {
            this.keywords = keywords;
        }
    }
}
