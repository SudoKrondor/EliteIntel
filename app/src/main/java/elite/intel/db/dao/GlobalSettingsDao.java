package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

@RegisterRowMapper(GlobalSettingsDao.GlobalSettingEntityMapper.class)
public interface GlobalSettingsDao {

    @SqlQuery("SELECT * FROM global_settings WHERE id = 1")
    GlobalSettingsDao.GlobalSettings get();

    @SqlUpdate("""
                    INSERT OR REPLACE INTO global_settings (id, autoSpeedUpForFtl,
                                                                autoLightsForFtl,
                                                                autoNightVisionOff,
                                                                autoCargoScoopRetractForFtl,
                                                                autoLandingGearUpForFtl,
                                                                autoHardpointsRetractForFtl,
                                                                autoGearUpOnTakeOff,
                                                                autoExitUiBeforeOpeningAnotherWindow,
                                                                autoLightsOffForSrvDeployment,
                                                                autoFighterOutFighterDocking,
                                                                announceJumpRoute,
                                                                announceJumpTraffic,
                                                                announceJumpDeaths,
                                                                announceRemainingJumps,
                                                                announceFuelAvailable)
                    VALUES (1, :autoSpeedUpForFtl,
                                :autoLightsForFtl,
                                :autoNightVisionOff,
                                :autoCargoScoopRetractForFtl,
                                :autoLandingGearUpForFtl,
                                :autoHardpointsRetractForFtl,
                                :autoGearUpOnTakeOff,
                                :autoExitUiBeforeOpeningAnotherWindow,
                                :autoLightsOffForSrvDeployment,
                                :autoFighterOutFighterDocking,
                                :announceJumpRoute,
                                :announceJumpTraffic,
                                :announceJumpDeaths,
                                :announceRemainingJumps,
                                :announceFuelAvailable)
            """)
    void save(@BindBean GlobalSettings settings);


    class GlobalSettingEntityMapper implements RowMapper<GlobalSettings> {
        @Override
        public GlobalSettings map(ResultSet rs, StatementContext ctx) throws SQLException {
            GlobalSettings entity = new GlobalSettings();
            entity.setAutoSpeedUpForFtl(rs.getBoolean("autoSpeedUpForFtl"));
            entity.setAutoLightsForFtl(rs.getBoolean("autoLightsForFtl"));
            entity.setAutoNightVisionOff(rs.getBoolean("autoNightVisionOff"));
            entity.setAutoCargoScoopRetractForFtl(rs.getBoolean("autoCargoScoopRetractForFtl"));
            entity.setAutoLandingGearUpForFtl(rs.getBoolean("autoLandingGearUpForFtl"));
            entity.setAutoHardpointsRetractForFtl(rs.getBoolean("autoHardpointsRetractForFtl"));
            entity.setAutoGearUpOnTakeOff(rs.getBoolean("autoGearUpOnTakeOff"));
            entity.setAutoExitUiBeforeOpeningAnotherWindow(rs.getBoolean("autoExitUiBeforeOpeningAnotherWindow"));
            entity.setAutoLightsOffForSrvDeployment(rs.getBoolean("autoLightsOffForSrvDeployment"));
            entity.setAutoFighterOutFighterDocking(rs.getBoolean("autoFighterOutFighterDocking"));
            entity.setAnnounceJumpRoute(rs.getBoolean("announceJumpRoute"));
            entity.setAnnounceJumpTraffic(rs.getBoolean("announceJumpTraffic"));
            entity.setAnnounceJumpDeaths(rs.getBoolean("announceJumpDeaths"));
            entity.setAnnounceRemainingJumps(rs.getBoolean("announceRemainingJumps"));
            entity.setAnnounceFuelAvailable(rs.getBoolean("announceFuelAvailable"));
            return entity;
        }
    }


    class GlobalSettings {
        boolean autoSpeedUpForFtl;
        boolean autoLightsForFtl;
        boolean autoNightVisionOff;
        boolean autoCargoScoopRetractForFtl;
        boolean autoLandingGearUpForFtl;
        boolean autoHardpointsRetractForFtl;
        boolean autoGearUpOnTakeOff;
        boolean autoExitUiBeforeOpeningAnotherWindow;
        boolean autoLightsOffForSrvDeployment;
        boolean autoFighterOutFighterDocking;
        boolean announceJumpRoute;
        boolean announceJumpTraffic;
        boolean announceJumpDeaths;
        boolean announceRemainingJumps;
        boolean announceFuelAvailable;

        public boolean isAutoSpeedUpForFtl() {
            return autoSpeedUpForFtl;
        }

        public void setAutoSpeedUpForFtl(boolean onOff) {
            this.autoSpeedUpForFtl = onOff;
        }

        public boolean isAutoLightsForFtl() {
            return autoLightsForFtl;
        }

        public void setAutoLightsForFtl(boolean onOff) {
            this.autoLightsForFtl = onOff;
        }

        public boolean isAutoNightVisionOff() {
            return autoNightVisionOff;
        }

        public void setAutoNightVisionOff(boolean onOff) {
            this.autoNightVisionOff = onOff;
        }

        public boolean isAutoCargoScoopRetractForFtl() {
            return autoCargoScoopRetractForFtl;
        }

        public void setAutoCargoScoopRetractForFtl(boolean onOff) {
            this.autoCargoScoopRetractForFtl = onOff;
        }

        public boolean isAutoLandingGearUpForFtl() {
            return autoLandingGearUpForFtl;
        }

        public void setAutoLandingGearUpForFtl(boolean onOff) {
            this.autoLandingGearUpForFtl = onOff;
        }

        public boolean isAutoHardpointsRetractForFtl() {
            return autoHardpointsRetractForFtl;
        }

        public void setAutoHardpointsRetractForFtl(boolean autoHardpointsRetractForFtl) {
            this.autoHardpointsRetractForFtl = autoHardpointsRetractForFtl;
        }

        public boolean isAutoGearUpOnTakeOff() {
            return autoGearUpOnTakeOff;
        }

        public void setAutoGearUpOnTakeOff(boolean autoGearUpOnTakeOff) {
            this.autoGearUpOnTakeOff = autoGearUpOnTakeOff;
        }

        public boolean isAutoExitUiBeforeOpeningAnotherWindow() {
            return autoExitUiBeforeOpeningAnotherWindow;
        }

        public void setAutoExitUiBeforeOpeningAnotherWindow(boolean autoExitUiBeforeOpeningAnotherWindow) {
            this.autoExitUiBeforeOpeningAnotherWindow = autoExitUiBeforeOpeningAnotherWindow;
        }

        public boolean isAutoLightsOffForSrvDeployment() {
            return autoLightsOffForSrvDeployment;
        }

        public void setAutoLightsOffForSrvDeployment(boolean autoLightsOffForSrvDeployment) {
            this.autoLightsOffForSrvDeployment = autoLightsOffForSrvDeployment;
        }

        public boolean isAutoFighterOutFighterDocking() {
            return autoFighterOutFighterDocking;
        }

        public void setAutoFighterOutFighterDocking(boolean autoFighterOutFighterDocking) {
            this.autoFighterOutFighterDocking = autoFighterOutFighterDocking;
        }

        public boolean isAnnounceJumpRoute() {
            return announceJumpRoute;
        }

        public void setAnnounceJumpRoute(boolean announceJumpRoute) {
            this.announceJumpRoute = announceJumpRoute;
        }

        public boolean isAnnounceJumpTraffic() {
            return announceJumpTraffic;
        }

        public void setAnnounceJumpTraffic(boolean announceJumpTraffic) {
            this.announceJumpTraffic = announceJumpTraffic;
        }

        public boolean isAnnounceJumpDeaths() {
            return announceJumpDeaths;
        }

        public void setAnnounceJumpDeaths(boolean announceJumpDeaths) {
            this.announceJumpDeaths = announceJumpDeaths;
        }

        public boolean isAnnounceRemainingJumps() {
            return announceRemainingJumps;
        }

        public void setAnnounceRemainingJumps(boolean announceRemainingJumps) {
            this.announceRemainingJumps = announceRemainingJumps;
        }

        public boolean isAnnounceFuelAvailable() {
            return announceFuelAvailable;
        }

        public void setAnnounceFuelAvailable(boolean announceFuelAvailable) {
            this.announceFuelAvailable = announceFuelAvailable;
        }
    }
}
