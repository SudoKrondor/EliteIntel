package elite.intel.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * The discovery scanner's body count per system, and whether the FSS has found every body.
 */
public interface FssSurveyDao {

    /**
     * Records the honk's count. Leaves the found-all latch alone: in a one-body system the game logs
     * FSSAllBodiesFound before the FSSDiscoveryScan that counted it, and a re-honk must not undo it either.
     */
    @SqlUpdate("""
            INSERT INTO fss_survey (systemAddress, bodyCount) VALUES (:systemAddress, :bodyCount)
            ON CONFLICT(systemAddress) DO UPDATE SET bodyCount = excluded.bodyCount
            """)
    void recordBodyCount(@Bind("systemAddress") long systemAddress, @Bind("bodyCount") int bodyCount);

    @SqlUpdate("""
            INSERT INTO fss_survey (systemAddress, bodyCount, allBodiesFound) VALUES (:systemAddress, :bodyCount, 1)
            ON CONFLICT(systemAddress) DO UPDATE SET bodyCount = excluded.bodyCount, allBodiesFound = 1
            """)
    void markAllBodiesFound(@Bind("systemAddress") long systemAddress, @Bind("bodyCount") int bodyCount);

    @SqlQuery("SELECT systemAddress, bodyCount, allBodiesFound FROM fss_survey WHERE systemAddress = :systemAddress")
    @RegisterConstructorMapper(Survey.class)
    Survey find(@Bind("systemAddress") long systemAddress);

    record Survey(long systemAddress, int bodyCount, int allBodiesFound) {
    }
}
