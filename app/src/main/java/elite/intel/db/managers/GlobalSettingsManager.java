package elite.intel.db.managers;

import elite.intel.db.dao.GlobalSettingsDao;
import elite.intel.db.util.Database;

public class GlobalSettingsManager {
    private static volatile GlobalSettingsManager instance;

    private GlobalSettingsManager() {
        // Private constructor to prevent instantiation
    }

    public static GlobalSettingsManager getInstance() {
        if (instance == null) {
            synchronized (GlobalSettingsManager.class) {
                if (instance == null) {
                    instance = new GlobalSettingsManager();
                }
            }
        }
        return instance;
    }

    public void setAutoSpeedUpForFtl(boolean autoSpeedUpForFtl) {
        Database.withDao(GlobalSettingsDao.class, dao -> {
            GlobalSettingsDao.GlobalSettings settings = dao.get();
            settings.setAutoSpeedUpForFtl(autoSpeedUpForFtl);
            dao.save(settings);
            return Void.TYPE;
        });
    }

    public boolean getAutoSpeedUpForFtl() {
        return Database.withDao(GlobalSettingsDao.class, dao -> dao.get().isAutoSpeedUpForFtl());
    }

    public void setAutoLightsForFtl(boolean autoLightsForFtl) {
        Database.withDao(GlobalSettingsDao.class, dao -> {
            GlobalSettingsDao.GlobalSettings settings = dao.get();
            settings.setAutoLightsForFtl(autoLightsForFtl);
            dao.save(settings);
            return Void.TYPE;
        });
    }

    public boolean getAutoLightsForFtl() {
        return Database.withDao(GlobalSettingsDao.class, dao -> dao.get().isAutoLightsForFtl());
    }

    public void setAutoNightVisionOffForSrv(boolean autoNightVisionOffForSrv) {
        Database.withDao(GlobalSettingsDao.class, dao -> {
            GlobalSettingsDao.GlobalSettings settings = dao.get();
            settings.setAutoNightVisionOff(autoNightVisionOffForSrv);
            dao.save(settings);
            return Void.TYPE;
        });
    }

    public boolean getAutoNightVisionOff() {
        return Database.withDao(GlobalSettingsDao.class, dao -> dao.get().isAutoNightVisionOff());
    }

    public void setAutoCargoScoopRetractForFtl(boolean autoCargoScoopRetractForFtl) {
        Database.withDao(GlobalSettingsDao.class, dao -> {
            GlobalSettingsDao.GlobalSettings settings = dao.get();
            settings.setAutoCargoScoopRetractForFtl(autoCargoScoopRetractForFtl);
            dao.save(settings);
            return Void.TYPE;
        });
    }

    public boolean getAutoCargoScoopRetractForFtl() {
        return Database.withDao(GlobalSettingsDao.class, dao -> dao.get().isAutoCargoScoopRetractForFtl());
    }

    public void setAutoLandingGearUpForFtl(boolean autoLandingGearUpForFtl) {
        Database.withDao(GlobalSettingsDao.class, dao -> {
            GlobalSettingsDao.GlobalSettings settings = dao.get();
            settings.setAutoLandingGearUpForFtl(autoLandingGearUpForFtl);
            dao.save(settings);
            return Void.TYPE;
        });
    }

    public boolean getAutoLandingGearUpForFtl() {
        return Database.withDao(GlobalSettingsDao.class, dao -> dao.get().isAutoLandingGearUpForFtl());
    }

    public void setAutoHardpointsRetractForFtl(boolean autoHardpointsRetractForFtl) {
        Database.withDao(GlobalSettingsDao.class, dao -> {
            GlobalSettingsDao.GlobalSettings settings = dao.get();
            settings.setAutoHardpointsRetractForFtl(autoHardpointsRetractForFtl);
            dao.save(settings);
            return Void.TYPE;
        });
    }

    public boolean getAutoHardpointsRetractForFtl() {
        return Database.withDao(GlobalSettingsDao.class, dao -> dao.get().isAutoHardpointsRetractForFtl());
    }

    public void setAutoGearUpOnTakeOff(boolean autoGearUpOnTakeOff) {
        Database.withDao(GlobalSettingsDao.class, dao -> {
            GlobalSettingsDao.GlobalSettings settings = dao.get();
            settings.setAutoGearUpOnTakeOff(autoGearUpOnTakeOff);
            dao.save(settings);
            return Void.TYPE;
        });
    }

    public boolean getAutoGearUpOnTakeOff() {
        return Database.withDao(GlobalSettingsDao.class, dao -> dao.get().isAutoGearUpOnTakeOff());
    }

    public void setAutoExitUiBeforeOpeningAnotherWindow(boolean autoExitUiBeforeOpeningAnotherWindow) {
        Database.withDao(GlobalSettingsDao.class, dao -> {
            GlobalSettingsDao.GlobalSettings settings = dao.get();
            settings.setAutoExitUiBeforeOpeningAnotherWindow(autoExitUiBeforeOpeningAnotherWindow);
            dao.save(settings);
            return Void.TYPE;
        });
    }

    public boolean getAutoExitUiBeforeOpeningAnotherWindow() {
        return Database.withDao(GlobalSettingsDao.class, dao -> dao.get().isAutoExitUiBeforeOpeningAnotherWindow());
    }

    public void setAutoLightsOffForSrvDeployment(boolean autoLightsOffForSrvDeployment) {
        Database.withDao(GlobalSettingsDao.class, dao -> {
            GlobalSettingsDao.GlobalSettings settings = dao.get();
            settings.setAutoLightsOffForSrvDeployment(autoLightsOffForSrvDeployment);
            dao.save(settings);
            return Void.TYPE;
        });
    }

    public boolean getAutoLightsOffForSrvDeployment() {
        return Database.withDao(GlobalSettingsDao.class, dao -> dao.get().isAutoLightsOffForSrvDeployment());
    }

    public void setAutoFighterOutFighterDocking(boolean autoFighterOutFighterDocking) {
        Database.withDao(GlobalSettingsDao.class, dao -> {
            GlobalSettingsDao.GlobalSettings settings = dao.get();
            settings.setAutoFighterOutFighterDocking(autoFighterOutFighterDocking);
            dao.save(settings);
            return Void.TYPE;
        });
    }

    public boolean getAutoFighterOutFighterDocking() {
        return Database.withDao(GlobalSettingsDao.class, dao -> dao.get().isAutoFighterOutFighterDocking());
    }

    public void setAnnounceJumpRoute(boolean announceJumpRoute) {
        Database.withDao(GlobalSettingsDao.class, dao -> {
            GlobalSettingsDao.GlobalSettings settings = dao.get();
            settings.setAnnounceJumpRoute(announceJumpRoute);
            dao.save(settings);
            return Void.TYPE;
        });
    }

    public boolean getAnnounceJumpRoute() {
        return Database.withDao(GlobalSettingsDao.class, dao -> dao.get().isAnnounceJumpRoute());
    }

    public void setAnnounceJumpTraffic(boolean announceJumpTraffic) {
        Database.withDao(GlobalSettingsDao.class, dao -> {
            GlobalSettingsDao.GlobalSettings settings = dao.get();
            settings.setAnnounceJumpTraffic(announceJumpTraffic);
            dao.save(settings);
            return Void.TYPE;
        });
    }

    public boolean getAnnounceJumpTraffic() {
        return Database.withDao(GlobalSettingsDao.class, dao -> dao.get().isAnnounceJumpTraffic());
    }

    public void setAnnounceJumpDeaths(boolean announceJumpDeaths) {
        Database.withDao(GlobalSettingsDao.class, dao -> {
            GlobalSettingsDao.GlobalSettings settings = dao.get();
            settings.setAnnounceJumpDeaths(announceJumpDeaths);
            dao.save(settings);
            return Void.TYPE;
        });
    }

    public boolean getAnnounceJumpDeaths() {
        return Database.withDao(GlobalSettingsDao.class, dao -> dao.get().isAnnounceJumpDeaths());
    }

    public void setAnnounceRemainingJumps(boolean announceRemainingJumps) {
        Database.withDao(GlobalSettingsDao.class, dao -> {
            GlobalSettingsDao.GlobalSettings settings = dao.get();
            settings.setAnnounceRemainingJumps(announceRemainingJumps);
            dao.save(settings);
            return Void.TYPE;
        });
    }

    public boolean getAnnounceRemainingJumps() {
        return Database.withDao(GlobalSettingsDao.class, dao -> dao.get().isAnnounceRemainingJumps());
    }

    public void setAnnounceFuelAvailable(boolean announceFuelAvailable) {
        Database.withDao(GlobalSettingsDao.class, dao -> {
            GlobalSettingsDao.GlobalSettings settings = dao.get();
            settings.setAnnounceFuelAvailable(announceFuelAvailable);
            dao.save(settings);
            return Void.TYPE;
        });
    }

    public boolean getAnnounceFuelAvailable() {
        return Database.withDao(GlobalSettingsDao.class, dao -> dao.get().isAnnounceFuelAvailable());
    }
}
