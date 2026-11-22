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

/**
 * Each Supertonic voice's own speed, volume and EQ, one row per voice the commander has tuned (created by
 * {@code 12016__supertonic_voice_tuning.sql}).
 * <p>
 * Callers go through {@link elite.intel.db.managers.SupertonicVoiceTuningManager}, which holds the table in
 * memory so the synthesis thread never reads it.
 */
@RegisterRowMapper(SupertonicVoiceTuningDao.TuningMapper.class)
public interface SupertonicVoiceTuningDao {

    @SqlQuery("SELECT * FROM supertonic_voice_tuning")
    List<Tuning> all();

    // WHY: every column is listed. INSERT OR REPLACE writes a whole row, so a column left out would be reset
    // to its default on the next save.
    @SqlUpdate("""
            INSERT OR REPLACE INTO supertonic_voice_tuning (voice, speed, volume, eq1, eq2, eq3, eq4, eq5, eq6)
            VALUES (:voice, :speed, :volume, :eq1, :eq2, :eq3, :eq4, :eq5, :eq6)
            """)
    void save(@BindBean Tuning tuning);

    @SqlUpdate("DELETE FROM supertonic_voice_tuning WHERE voice = :voice")
    void delete(@Bind("voice") String voice);


    class TuningMapper implements RowMapper<Tuning> {
        @Override
        public Tuning map(ResultSet rs, StatementContext ctx) throws SQLException {
            Tuning entity = new Tuning();
            entity.setVoice(rs.getString("voice"));
            entity.setSpeed(rs.getInt("speed"));
            entity.setVolume(rs.getInt("volume"));
            entity.setEq1(rs.getInt("eq1"));
            entity.setEq2(rs.getInt("eq2"));
            entity.setEq3(rs.getInt("eq3"));
            entity.setEq4(rs.getInt("eq4"));
            entity.setEq5(rs.getInt("eq5"));
            entity.setEq6(rs.getInt("eq6"));
            return entity;
        }
    }


    class Tuning {
        String voice;
        int speed;
        int volume;
        int eq1;
        int eq2;
        int eq3;
        int eq4;
        int eq5;
        int eq6;

        public String getVoice() {
            return voice;
        }

        public void setVoice(String voice) {
            this.voice = voice;
        }

        public int getSpeed() {
            return speed;
        }

        public void setSpeed(int speed) {
            this.speed = speed;
        }

        public int getVolume() {
            return volume;
        }

        public void setVolume(int volume) {
            this.volume = volume;
        }

        public int getEq1() {
            return eq1;
        }

        public void setEq1(int eq1) {
            this.eq1 = eq1;
        }

        public int getEq2() {
            return eq2;
        }

        public void setEq2(int eq2) {
            this.eq2 = eq2;
        }

        public int getEq3() {
            return eq3;
        }

        public void setEq3(int eq3) {
            this.eq3 = eq3;
        }

        public int getEq4() {
            return eq4;
        }

        public void setEq4(int eq4) {
            this.eq4 = eq4;
        }

        public int getEq5() {
            return eq5;
        }

        public void setEq5(int eq5) {
            this.eq5 = eq5;
        }

        public int getEq6() {
            return eq6;
        }

        public void setEq6(int eq6) {
            this.eq6 = eq6;
        }
    }
}
