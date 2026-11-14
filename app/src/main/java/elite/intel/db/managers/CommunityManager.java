package elite.intel.db.managers;

import elite.intel.community.CrowdSet;
import elite.intel.db.dao.CommunitySyncDao;
import elite.intel.db.dao.CrowdMassacreSetDao;
import elite.intel.db.dao.MassacreMissionDao;
import elite.intel.db.dao.MassacreMissionDao.Shareable;
import elite.intel.db.util.Database;
import elite.intel.util.Cypher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * What the community sync keeps between runs, and the massacre sets it trades: this install's anonymous identity,
 * when it last uploaded and downloaded, what every commander on this install can share, and the sets other
 * commanders reported.
 */
public class CommunityManager {

    private static final Logger log = LogManager.getLogger(CommunityManager.class);

    /**
     * Joins a list into one column, on the separator the hunting-ground ledger uses: a character no faction or
     * station name carries.
     */
    static final String SEPARATOR = "|";

    private static volatile CommunityManager instance;

    private CommunityManager() {
    }

    public static CommunityManager getInstance() {
        if (instance == null) {
            synchronized (CommunityManager.class) {
                if (instance == null) {
                    instance = new CommunityManager();
                }
            }
        }
        return instance;
    }

    // ---------------------------------------------------------------- identity and sync state

    public CommunitySyncDao.State state() {
        return Database.withDao(CommunitySyncDao.class, CommunitySyncDao::get);
    }

    public void saveIdentity(String uid, String refreshToken) {
        Database.withDao(CommunitySyncDao.class, dao -> {
            dao.saveIdentity(uid, Cypher.encrypt(refreshToken));
            return Void.TYPE;
        });
    }

    /**
     * The stored refresh token, or null when there is none or this computer cannot read it.
     * <p>
     * WHY an unreadable token is not an error: a database restored from another computer carries a token written
     * under that computer's key. The cost of losing it is one new anonymous account, so the sync simply signs up
     * again.
     */
    public String refreshToken() {
        String stored = state().refreshToken();
        if (stored == null || stored.isEmpty()) return null;
        try {
            return Cypher.decrypt(stored);
        } catch (RuntimeException e) {
            log.warn("Community refresh token is unreadable on this computer, a new anonymous account will be made");
            return null;
        }
    }

    public void markPinged(Instant at) {
        Database.withDao(CommunitySyncDao.class, dao -> {
            dao.markPinged(at.toString());
            return Void.TYPE;
        });
    }

    public void markReported(Instant at, String digest) {
        Database.withDao(CommunitySyncDao.class, dao -> {
            dao.markReported(at.toString(), digest);
            return Void.TYPE;
        });
    }

    public void clearReport() {
        Database.withDao(CommunitySyncDao.class, dao -> {
            dao.clearReport();
            return Void.TYPE;
        });
    }

    public void markDownloaded(Instant at, String etag) {
        Database.withDao(CommunitySyncDao.class, dao -> {
            dao.markDownloaded(at.toString(), etag);
            return Void.TYPE;
        });
    }

    // ---------------------------------------------------------------- what this install shares

    /**
     * Everything every commander on this install can share: the open commander's ledger, and the ledger in each
     * other commander's file - a journal import fills those, and the install's report covers all of them.
     */
    public List<Shareable> shareable() throws IOException {
        List<Shareable> all = new ArrayList<>(Database.withDao(MassacreMissionDao.class, MassacreMissionDao::shareable));
        for (String fid : Database.otherCommandersOnDisk()) {
            all.addAll(Database.inCommanderFile(fid,
                    () -> Database.withDao(MassacreMissionDao.class, MassacreMissionDao::shareable)));
        }
        return all;
    }

    /**
     * How many provider and target pairs this install can share, counted across every commander file.
     */
    public int shareableSetCount() throws IOException {
        Set<String> pairs = new HashSet<>();
        for (Shareable s : shareable()) {
            pairs.add((s.providerSystem() + SEPARATOR + s.targetSystem()).toLowerCase(Locale.ROOT));
        }
        return pairs.size();
    }

    // ---------------------------------------------------------------- what others shared

    /**
     * Replaces every stored set with these, in one transaction, so a failure part way keeps yesterday's sets.
     */
    public void replaceCrowdSets(List<CrowdSet> sets) {
        try (Handle handle = Database.init()) {
            handle.useTransaction(h -> {
                CrowdMassacreSetDao dao = h.attach(CrowdMassacreSetDao.class);
                dao.deleteAll();
                for (CrowdSet s : sets) {
                    dao.insert(s.providerSystem(), s.targetSystem(), s.x(), s.y(), s.z(),
                            String.join(SEPARATOR, s.providerFactions()), String.join(SEPARATOR, s.stations()),
                            s.targetFaction(), s.stackDepth(), s.reporters(), s.lastSeen());
                }
            });
        }
    }

    public void saveContributors(Integer contributors) {
        Database.withDao(CommunitySyncDao.class, dao -> {
            dao.saveContributors(contributors);
            return Void.TYPE;
        });
    }

    public int crowdSetCount() {
        return Database.withDao(CrowdMassacreSetDao.class, CrowdMassacreSetDao::count);
    }

    /**
     * How many downloaded sets this commander's own ledger can vouch for: the ones VEGA would offer, at any distance.
     */
    public int usableCrowdSetCount() {
        return Database.withDao(CrowdMassacreSetDao.class, CrowdMassacreSetDao::countUsable);
    }
}
