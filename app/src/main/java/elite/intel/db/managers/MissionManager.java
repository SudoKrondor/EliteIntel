package elite.intel.db.managers;

import elite.intel.db.dao.MissionDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.MissionType;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.util.json.GsonFactory;

import java.util.*;
import java.util.stream.Collectors;

public class MissionManager {
    private static MissionManager instance;

    private MissionManager() {
    }

    public static synchronized MissionManager getInstance() {
        if (instance == null) {
            instance = new MissionManager();
        }
        return instance;
    }

    private static Map<Long, MissionDto> toMissionMap(List<MissionDao.Mission> missions) {
        Map<Long, MissionDto> result = new HashMap<>();
        missions.sort(Comparator.comparing(MissionDao.Mission::getKey));
        for (MissionDao.Mission mission : missions) {
            result.put(mission.getKey(), GsonFactory.getGson().fromJson(mission.getMission(), MissionDto.class));
        }
        return result;
    }

    public void save(MissionDto mission) {
        MissionDao.Mission data = new MissionDao.Mission();
        data.setKey(mission.getMissionId());
        data.setMission(mission.toJson());
        data.setMissionType(mission.getMissionType());
        data.setKeywords(mission.getMissionType().getKeywords());
        Database.withDao(MissionDao.class, dao -> {
                    dao.upsert(data, data.getMissionType().name());
                    return null;
                }
        );
    }


    public MissionDto getMission(long missionId) {
        return Database.withDao(MissionDao.class, dao -> {
            MissionDao.Mission mission = dao.get(missionId);
            if (mission == null) return null;
            return GsonFactory.getGson().fromJson(mission.getMission(), MissionDto.class);
        });
    }

    public MissionType[] getAvailableMissionTypes() {
        return Database.withDao(MissionDao.class, dao -> {
            String[] missionTypeNames = dao.getAvailableMissionTypes();
            MissionType[] result = new MissionType[missionTypeNames.length];
            for (int i = 0, typesLength = missionTypeNames.length; i < typesLength; i++) {
                result[i] = MissionType.valueOf(missionTypeNames[i]);
            }
            return result;
        });
    }

    public Map<Long, MissionDto> getMissions(MissionType... missionTypes) {
        return Database.withDao(MissionDao.class, dao -> toMissionMap(dao.findForMissionType(missionTypes)));
    }

    public Map<Long, MissionDto> getMissions() {
        return Database.withDao(MissionDao.class, dao -> toMissionMap(dao.findAny()));
    }

    public Set<String> getTargetFactions(MissionType... missionTypes) {
        Set<String> result = new HashSet<>();
        Map<Long, MissionDto> missions = getMissions(missionTypes);
        for (MissionDto mission : missions.values()) {
            result.add(mission.getMissionTargetFaction());
        }
        return result;
    }

    public void remove(Long missionId) {
        Database.withDao(MissionDao.class, dao -> {
            dao.delete(missionId);
            return Void.class;
        });
    }

    public MissionType[] getPirateMissionTypes() {
        return new ArrayList<>(
                Arrays.asList(
                        MissionType.MISSION_PIRATE_MASSACRE,
                        MissionType.MISSION_PIRATE_MASSACRE_WING
                )
        ).toArray(MissionType[]::new);
    }

    public MissionType getMissionType(String missionTypeName) {
        MissionType[] values = MissionType.values();
        return Arrays.stream(values).filter(type -> type.getMissionType().equalsIgnoreCase(missionTypeName)).findFirst().orElse(MissionType.UNKNOWN);
    }

    public List<MissionDto> findByKeyword(String keyword) {
        return Database.withDao(MissionDao.class, dao -> {
            List<MissionDao.Mission> entities = dao.findByKeyword(keyword);
            return entities.stream().map(mission ->
                    GsonFactory.getGson().fromJson(mission.getMission(), MissionDto.class)).collect(Collectors.toList()
            );
        });
    }

    public void clear() {
        Database.withDao(MissionDao.class, dao -> {
            dao.clear();
            return Void.TYPE;
        });
    }
}

