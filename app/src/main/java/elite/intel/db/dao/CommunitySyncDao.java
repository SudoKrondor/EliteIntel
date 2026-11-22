package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This install's anonymous community identity and when it last uploaded and downloaded. One row, made by the
 * migration. Callers go through {@code CommunityManager}.
 */
@RegisterRowMapper(CommunitySyncDao.StateMapper.class)
public interface CommunitySyncDao {

    @SqlQuery("SELECT * FROM community_sync WHERE id = 1")
    State get();

    @SqlUpdate("UPDATE community_sync SET uid = :uid, refreshToken = :refreshToken WHERE id = 1")
    void saveIdentity(@Bind("uid") String uid, @Bind("refreshToken") String encryptedRefreshToken);

    @SqlUpdate("UPDATE community_sync SET lastPingAt = :at WHERE id = 1")
    void markPinged(@Bind("at") String at);

    @SqlUpdate("UPDATE community_sync SET lastReportAt = :at, lastReportDigest = :digest WHERE id = 1")
    void markReported(@Bind("at") String at, @Bind("digest") String digest);

    @SqlUpdate("UPDATE community_sync SET lastReportAt = NULL, lastReportDigest = NULL WHERE id = 1")
    void clearReport();

    @SqlUpdate("UPDATE community_sync SET lastDownloadAt = :at, setsEtag = :etag WHERE id = 1")
    void markDownloaded(@Bind("at") String at, @Bind("etag") String etag);

    @SqlUpdate("UPDATE community_sync SET contributors = :contributors WHERE id = 1")
    void saveContributors(@Bind("contributors") Integer contributors);

    class StateMapper implements RowMapper<State> {
        @Override
        public State map(ResultSet rs, StatementContext ctx) throws SQLException {
            int count = rs.getInt("contributors");
            // WHY: read straight after the column, because wasNull() answers for the last column read.
            Integer contributors = rs.wasNull() ? null : count;
            return new State(rs.getString("uid"), rs.getString("refreshToken"), rs.getString("lastPingAt"),
                    rs.getString("lastReportAt"), rs.getString("lastReportDigest"), rs.getString("lastDownloadAt"),
                    rs.getString("setsEtag"), contributors);
        }
    }

    /**
     * Times are ISO-8601 instants. {@code refreshToken} is encrypted. {@code lastReportDigest} identifies what was
     * last uploaded, so an unchanged report is not uploaded again. {@code contributors} is how many installs stand behind
     * the downloaded sets, or null before a download that said.
     */
    record State(String uid,
                 String refreshToken,
                 String lastPingAt,
                 String lastReportAt,
                 String lastReportDigest,
                 String lastDownloadAt,
                 String setsEtag,
                 Integer contributors) {
    }
}
