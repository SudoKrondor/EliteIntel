package elite.intel.db.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * How far the journal backfill has read.
 * <p>
 * One row, id 1. It holds the name of the last journal file read to the end, so a second run over a
 * commander's archive skips what it has already seen instead of re-reading years of flying.
 */
public interface HuntingGroundScanDao {

    @SqlQuery("SELECT lastJournal FROM hunting_ground_scan WHERE id = 1")
    String lastJournal();

    @SqlUpdate("UPDATE hunting_ground_scan SET lastJournal = :lastJournal, lastScanAt = :scannedAt WHERE id = 1")
    void recordProgress(@Bind("lastJournal") String lastJournal, @Bind("scannedAt") String scannedAt);
}
