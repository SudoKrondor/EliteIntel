package elite.intel.db.managers;

import elite.intel.db.dao.FssSurveyDao;
import elite.intel.db.util.Database;

import java.util.Optional;

/**
 * What the discovery scanner and the FSS have said about a system's bodies: how many there are, and
 * whether they have all been found.
 */
public final class FssSurveyManager {

    private static FssSurveyManager instance;

    private FssSurveyManager() {
    }

    public static synchronized FssSurveyManager getInstance() {
        if (instance == null) {
            instance = new FssSurveyManager();
        }
        return instance;
    }

    /**
     * A system's survey as far as it has gone.
     *
     * @param bodyCount      stars and planets the honk counted, the primary star included
     * @param allBodiesFound the FSS has resolved every one of them
     */
    public record Survey(int bodyCount, boolean allBodiesFound) {
    }

    public void recordBodyCount(long systemAddress, int bodyCount) {
        if (systemAddress == 0 || bodyCount <= 0) return;
        Database.withDao(FssSurveyDao.class, dao -> {
            dao.recordBodyCount(systemAddress, bodyCount);
            return null;
        });
    }

    public void markAllBodiesFound(long systemAddress, int bodyCount) {
        if (systemAddress == 0 || bodyCount <= 0) return;
        Database.withDao(FssSurveyDao.class, dao -> {
            dao.markAllBodiesFound(systemAddress, bodyCount);
            return null;
        });
    }

    /**
     * The survey of this system, or empty when it has never been honked.
     */
    public Optional<Survey> find(long systemAddress) {
        FssSurveyDao.Survey row = Database.withDao(FssSurveyDao.class, dao -> dao.find(systemAddress));
        return row == null ? Optional.empty() : Optional.of(new Survey(row.bodyCount(), row.allBodiesFound() != 0));
    }
}
