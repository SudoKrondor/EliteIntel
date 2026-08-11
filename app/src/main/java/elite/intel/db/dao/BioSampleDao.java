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

@RegisterRowMapper(BioSampleDao.BioSampleMapper.class)
public interface BioSampleDao {

    @SqlUpdate("""
            INSERT OR REPLACE INTO bio_samples (key, json)
            VALUES(:key, :json)
            ON CONFLICT(key) DO UPDATE SET
            json = excluded.json
            """)
    void upsert(@BindBean BioSampleDao.BioSample data);


    @SqlQuery("SELECT * FROM bio_samples WHERE key = :key")
    BioSample get(@BindBean BioSampleDao.BioSample key);


    @SqlUpdate("DELETE FROM bio_samples")
    void clear();

    @SqlQuery("SELECT * FROM bio_samples")
    BioSampleDao.BioSample[] listAll();

    @SqlQuery("SELECT * FROM bio_samples WHERE json LIKE '%' || :planetName || '%' and json LIKE '%' || :primaryStar|| '%'")
    List<BioSample> findByPlanetName(@Bind("primaryStar") String primaryStar, @Bind("planetName") String planetName);

    class BioSampleMapper implements RowMapper<BioSampleDao.BioSample> {

        @Override public BioSample map(ResultSet rs, StatementContext ctx) throws SQLException {
            BioSample bioSample = new BioSample();
            bioSample.setKey(rs.getString("key"));
            bioSample.setJson(rs.getString("json"));
            return bioSample;
        }
    }


    class BioSample {
        private String key;
        private String json;

        public BioSample() {
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getJson() {
            return json;
        }

        public void setJson(String json) {
            this.json = json;
        }
    }
}
