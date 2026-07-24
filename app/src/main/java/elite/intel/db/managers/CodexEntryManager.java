package elite.intel.db.managers;

import elite.intel.db.dao.CodexEntryDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.CodexEntryEvent;
import elite.intel.gameapi.journal.events.dto.TargetLocation;

import java.util.List;

public final class CodexEntryManager {

    private static CodexEntryManager instance;

    private CodexEntryManager() {
    }

    public static synchronized CodexEntryManager getInstance() {
        if (instance == null) {
            instance = new CodexEntryManager();
        }
        return instance;
    }

    public List<CodexEntryDao.CodexEntry> findAll() {
        return Database.withDao(CodexEntryDao.class, dao -> dao.findAll());
    }

    public List<CodexEntryDao.CodexEntry> getForPlanet(String starSystem, Long bodyId) {
        return Database.withDao(CodexEntryDao.class, dao -> {
            List<CodexEntryDao.CodexEntry> forPlanet = dao.getForPlanet(bodyId, starSystem);
            return forPlanet.isEmpty() ? null : forPlanet;
        });
    }

    public void clearCompleted(String starSystem, Long bodyId, String entry) {
        Database.withDao(CodexEntryDao.class, dao -> {
            dao.clearCompleted(entry, starSystem, bodyId);
            return null;
        });
    }

    public void save(CodexEntryEvent event) {
        Database.withDao(CodexEntryDao.class, dao -> {
            CodexEntryDao.CodexEntry entry = new CodexEntryDao.CodexEntry();
            entry.setBodyId(event.getBodyID());
            entry.setStarSystem(event.getSystem());
            entry.setLatitude(event.getLatitude());
            entry.setLongitude(event.getLongitude());
            entry.setSubCategory(event.getSubCategoryLocalised());
            entry.setVoucherAmount(event.getVoucherAmount());
            entry.setEntryName(event.getNameLocalised());
            entry.setEntrySymbol(event.getName()); // non-localized FDev symbol, for language-independent matching
            dao.save(entry);
            return null;
        });
    }

    public boolean checkIfExist(String starSystem, Long bodyId, String entry) {
        return Database.withDao(CodexEntryDao.class, dao -> dao.contains(entry, starSystem, bodyId));
    }

    public void clear() {
        Database.withDao(CodexEntryDao.class, dao -> {
            dao.clear();
            return null;
        });
    }

    public void deleteTrackedEntry(TargetLocation tracking) {
        Database.withDao(CodexEntryDao.class, dao -> {
            dao.deleteForCoordinates(tracking.getLatitude(), tracking.getLongitude());
            return Void.TYPE;
        });
    }
}