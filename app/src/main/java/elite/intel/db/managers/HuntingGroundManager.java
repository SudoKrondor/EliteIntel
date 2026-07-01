package elite.intel.db.managers;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.dao.PirateHuntingGroundsDao;
import elite.intel.db.dao.PirateHuntingGroundsDao.HuntingGround;
import elite.intel.db.dao.PirateMissionProviderDao;
import elite.intel.db.dao.PirateMissionProviderDao.MissionProvider;
import elite.intel.db.util.Database;
import elite.intel.eventbus.GameEventBus;
import elite.intel.search.spansh.starsystems.StarSystemResult;
import elite.intel.util.StringUtls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HuntingGroundManager {

    private static volatile HuntingGroundManager instance;

    private HuntingGroundManager() {
        // Private constructor to prevent instantiation
    }

    public static HuntingGroundManager getInstance() {
        if (instance == null) {
            synchronized (HuntingGroundManager.class) {
                if (instance == null) {
                    instance = new HuntingGroundManager();
                }
            }
        }
        return instance;
    }


    /// Saves partial we do not have faction names from INTRA data.
    public PirateMissionTuple<HuntingGround, List<MissionProvider>>
    savePartialPair(StarSystemResult.SystemRecord provider, StarSystemResult.SystemRecord target) {

        HuntingGround pirateFaction = Database.withDao(PirateHuntingGroundsDao.class, dao -> {
            HuntingGround targetEntity = new HuntingGround();
            targetEntity.setStarSystem(target.getName());
            targetEntity.setX(target.getX());
            targetEntity.setY(target.getY());
            targetEntity.setZ(target.getZ());
            dao.save(targetEntity);
            return dao.findByStarSystem(target.getName());
        });

        List<MissionProvider> providers = Database.withDao(PirateMissionProviderDao.class, dao -> {
            List<MissionProvider> list = dao.findMissionProviderForTargetSystem(provider.getName(), target.getName());
            if (list != null && !list.isEmpty()) return list;

            MissionProvider entity = new MissionProvider();
            entity.setTargetFactionID(pirateFaction.getId());
            entity.setStarSystem(provider.getName());
            entity.setX(provider.getX());
            entity.setY(provider.getY());
            entity.setZ(provider.getZ());
            entity.setTargetSystem(target.getName());
            dao.upsert(entity);
            return dao.findMissionProviderForTargetStarSystem(target.getName(), "any");
        });

        return new PirateMissionTuple<>(pirateFaction, providers);
    }

    public List<PirateMissionTuple<HuntingGround, List<MissionProvider>>> findInProviderForTargetStarSystem(
            @Nonnull String targetStarSystem, @Nullable String currentStarSystemOrNull
    ) {
        HuntingGround targetEntity = Database.withDao(
                PirateHuntingGroundsDao.class, dao -> dao.findByStarSystem(targetStarSystem)
        );

        // nothing found, return empty list, and fall back to net search
        if (targetEntity == null) return new ArrayList<>();

        List<MissionProvider> providers = getMissionProviders(targetEntity, currentStarSystemOrNull);
        return Collections.singletonList(new PirateMissionTuple<>(targetEntity, providers));
    }


    public List<MissionProvider> findConfirmedMissionProviders() {
        return Database.withDao(
                PirateMissionProviderDao.class,
                PirateMissionProviderDao::findProvidersWithMostTargetSystems
        );
    }


    public List<PirateMissionTuple<HuntingGround, List<MissionProvider>>> findTargetSystemInRangeForRecon(Coordinates coordinates) {
        HuntingGround targetEntity = Database.withDao(
                PirateHuntingGroundsDao.class, dao -> dao.findNearestRecon(coordinates.x(), coordinates.y(), coordinates.z())
        );
        /// nothing found, return empty list, and fall back to net search
        if (targetEntity == null) return new ArrayList<>();

        /// Try hunting ground with most mission providers
        List<MissionProvider> mostProvidersAgainstSameStarSystem = Database.withDao(
                PirateMissionProviderDao.class,
                PirateMissionProviderDao::findProvidersWithMostTargetSystems
        );

        /// if we have mission providers targeting a same star system, return the list
        if (mostProvidersAgainstSameStarSystem != null && mostProvidersAgainstSameStarSystem.size() > 1) {
            @SuppressWarnings("OptionalGetWithoutIsPresent") /// guaranteed by the check above
                    List<MissionProvider> missionProviders = Database.withDao(PirateMissionProviderDao.class,
                    dao -> dao.findMissionProviderForTargetStarSystem(
                            mostProvidersAgainstSameStarSystem.stream().findFirst().get().getTargetSystem(),
                            null
                    )
            );
            return Collections.singletonList(new PirateMissionTuple<>(targetEntity, missionProviders));
        }

        ///
        List<MissionProvider> providers = getMissionProviders(targetEntity, null);
        return Collections.singletonList(new PirateMissionTuple<>(targetEntity, providers));
    }


    private List<MissionProvider> getMissionProviders(HuntingGround targetEntity, String currentStarSystem) {
        if (targetEntity == null) return new ArrayList<>();
        return Database.withDao(
                PirateMissionProviderDao.class, dao -> dao.findMissionProviderForTargetStarSystem(
                        targetEntity.getStarSystem(), currentStarSystem == null ? "any" : currentStarSystem
                )
        );
    }

    public int updateTargetFaction(String destinationSystem, String targetFaction) {
        if (targetFaction == null || destinationSystem == null) return 0;

        Database.withDao(PirateHuntingGroundsDao.class, dao -> {
            dao.updateTargetFaction(destinationSystem, targetFaction);
            return Void.class;
        });

        return Database.withDao(PirateHuntingGroundsDao.class, dao -> {
            HuntingGround byFactionName = dao.findByFactionName(targetFaction, destinationSystem);
            return byFactionName == null ? -1 : byFactionName.getId();
        });
    }

    public void updateProviderFaction(String providerStarSystem, int targetFactionId, String providerFaction, String targetSystem) {
        if (providerStarSystem == null || targetFactionId == 0 || providerFaction == null) return;

        Database.withDao(PirateMissionProviderDao.class, dao -> {
            MissionProvider provider = dao.findMissionProviderForTargetFaction(providerStarSystem, targetFactionId);
            if (provider == null) {
                provider = new MissionProvider();
                provider.setStarSystem(providerStarSystem);
                provider.setTargetFactionID(targetFactionId);
                provider.setMissionProviderFaction(providerFaction);
                provider.setTargetSystem(targetSystem);
                dao.upsert(provider);
            }
            dao.updateFaction(provider.getId(), providerFaction);
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("speech.pirate.providerFactionUpdated", providerFaction)));
            return Void.class;
        });
    }

    public String findStarSystemForFactionName(String targetFaction) {
        return Database.withDao(PirateHuntingGroundsDao.class, dao -> dao.findByFactionName(targetFaction));
    }

    public void ignoreHuntingGround(String starSystemName) {
        Database.withDao(PirateHuntingGroundsDao.class, dao -> {
            dao.ignoreHuntingGround(starSystemName);
            return Void.TYPE;
        });
    }

    public void confirmTargetReconResourceSite(String primaryStarName) {
        Database.withDao(PirateHuntingGroundsDao.class, dao -> {
            HuntingGround system = dao.findByStarSystem(primaryStarName);
            if (system != null && !system.isHasResSite()) {
                dao.confirm(primaryStarName);
                GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("speech.pirate.reconConfirmed", primaryStarName)));
            }
            return Void.class;
        });
    }

    public void clear() {
        Database.withDao(PirateHuntingGroundsDao.class, dao -> {
            dao.clear();
            return Void.TYPE;
        });
        Database.withDao(PirateMissionProviderDao.class, dao -> {
            dao.clear();
            return Void.TYPE;
        });
    }


    public static class PirateMissionTuple<K, V> {
        K target;
        V missionProvider;

        PirateMissionTuple(K key, V value) {
            this.target = key;
            this.missionProvider = value;
        }

        public K getTarget() {
            return target;
        }

        public V getMissionProvider() {
            return missionProvider;
        }
    }
}
